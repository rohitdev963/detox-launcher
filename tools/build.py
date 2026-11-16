#!/usr/bin/env python3
"""Portable Detox APK build. No installer, downloads, automatic keys or private laptop config."""
import argparse
import getpass
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
import zipfile
sys.path.insert(0, str(Path(__file__).resolve().parent))
from public_assets import entries as public_asset_entries, stage as stage_public_assets

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'


def run(arguments, env=None):
    subprocess.run([str(arg) for arg in arguments], check=True, env=env)


def tool(directory, name):
    path = directory / (name + ('.exe' if os.name == 'nt' else ''))
    if not path.is_file():
        raise ValueError(f'Missing tool: {path}')
    return path


def normalized_archive(source, destination):
    with zipfile.ZipFile(source) as incoming, zipfile.ZipFile(destination, 'w', compression=zipfile.ZIP_STORED) as outgoing:
        names = set()
        for info in incoming.infolist():
            name = info.filename.replace('\\', '/')
            if name in names or name.startswith('/') or '..' in name.split('/'):
                raise ValueError('Invalid or duplicate packaged resource path')
            names.add(name)
            with incoming.open(info) as reader, outgoing.open(name, 'w') as writer:
                shutil.copyfileobj(reader, writer, 1024 * 1024)


def java_response(path, arguments):
    # javac understands quoted arguments; D8's @file instead accepts one raw argument per line.
    values = [str(value).replace('\\', '/') for value in arguments]
    if any('\n' in value or '\r' in value for value in values):
        raise ValueError('Newlines are not supported in build paths')
    path.write_text('\n'.join('"' + value.replace('"', '\\"') + '"' for value in values) + '\n', encoding='utf-8')


def arguments(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', default=os.environ.get('JAVA_HOME'))
    parser.add_argument('--android-sdk', default=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT'))
    parser.add_argument('--configuration', choices=['Debug', 'Release'], default='Debug')
    signing = parser.add_mutually_exclusive_group(required=True)
    signing.add_argument('--unsigned', action='store_true', help='Compile only; this APK cannot be installed')
    signing.add_argument('--keystore', type=Path, help='Your own existing signing key; never committed')
    parser.add_argument('--alias', default='detox-dev')
    parser.add_argument('--password-environment', default='DETOX_BUILD_PASSWORD')
    parser.add_argument('--output', type=Path, help='New APK path; existing files are never overwritten')
    return parser.parse_args(argv)


def main(argv=None):
    args = arguments(argv)
    if not args.java_home or not args.android_sdk:
        raise ValueError('Set JAVA_HOME and ANDROID_HOME (Java 17; SDK platform 34/build-tools 34.0.0).')
    java_home, sdk = Path(args.java_home).expanduser().resolve(), Path(args.android_sdk).expanduser().resolve()
    java, javac = tool(java_home / 'bin', 'java'), tool(java_home / 'bin', 'javac')
    bt = sdk / 'build-tools/34.0.0'
    aapt, align = tool(bt, 'aapt2'), tool(bt, 'zipalign')
    android_jar, lambda_jar = sdk / 'platforms/android-34/android.jar', bt / 'core-lambda-stubs.jar'
    d8, signer = bt / 'lib/d8.jar', bt / 'lib/apksigner.jar'
    for path in [android_jar, lambda_jar, d8, signer]:
        if not path.is_file():
            raise ValueError(f'Missing Android prerequisite: {path}')
    version_check = subprocess.run([str(javac), '-version'], capture_output=True, text=True, check=True)
    if not re.search(r'javac 17(?:\.|\s|$)', version_check.stdout + version_check.stderr):
        raise ValueError('This build uses Java 17; select a Java 17 JDK.')
    manifest = ET.parse(ROOT / 'AndroidManifest.xml')
    version = manifest.getroot().get(ANDROID + 'versionName')
    output = (args.output or ROOT / f'output/Detox-{version}-portable-{args.configuration.lower()}{"-unsigned" if args.unsigned else ""}.apk').resolve()
    if output.exists():
        raise ValueError(f'Output already exists; choose a new path: {output}')
    if any(parent == (ROOT / protected).resolve() for protected in ['src', 'assets', 'res', 'signing'] for parent in [output, *output.parents]):
        raise ValueError('APK output cannot be written into source/assets/resources/signing.')
    # Exact allowlist: unrelated/private files present on a maintainer's laptop never get packaged.
    list(public_asset_entries(ROOT))
    env = None
    if args.keystore:
        args.keystore = args.keystore.expanduser().resolve()
        if not args.keystore.is_file():
            raise ValueError('Provide an existing keystore. This script never creates a signing identity.')
        password = os.environ.get(args.password_environment)
        if not password:
            if not sys.stdin.isatty():
                raise ValueError('Signing password unavailable and no interactive terminal; no hidden prompt opened.')
            password = getpass.getpass('Your build keystore password (hidden): ')
        if not password:
            raise ValueError('No signing password supplied.')
        env = dict(os.environ)
        env[args.password_environment] = password
    build_root = ROOT / 'build'
    build_root.mkdir(exist_ok=True)
    try:
        # Only normal generated build files live here; no copy of the project source is made.
        with tempfile.TemporaryDirectory(prefix='portable-', dir=build_root) as folder:
            work = Path(folder)
            classes, dex, gen = work / 'classes', work / 'dex', work / 'gen'
            for path in [classes, dex, gen]:
                path.mkdir()
            public_assets = stage_public_assets(ROOT, work / 'assets')
            application = manifest.getroot().find('application')
            application.set(ANDROID + 'debuggable', str(args.configuration == 'Debug').lower())
            manifest.write(work / 'AndroidManifest.xml', encoding='utf-8', xml_declaration=True)
            packages = []
            for line in (ROOT / 'apps.txt').read_text(encoding='utf-8').splitlines():
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                if not re.fullmatch(r'[A-Za-z0-9_.]+', line):
                    raise ValueError('Invalid package name in apps.txt')
                packages.append('"' + line + '"')
            (gen / 'Apps.java').write_text('package com.ansub.tvhome; final class Apps { static final String[] LIST={' + ','.join(packages) + '};}\n', encoding='utf-8')
            run([aapt, 'compile', '--dir', ROOT / 'res', '-o', work / 'resources.zip'])
            run([aapt, 'link', '-o', work / 'base.apk', '-I', android_jar, '-R', work / 'resources.zip', '--auto-add-overlay', '--manifest', work / 'AndroidManifest.xml', '--min-sdk-version', '21', '--target-sdk-version', '34', '-A', public_assets])
            sources = sorted((ROOT / 'src').rglob('*.java')) + [gen / 'Apps.java']
            java_response(work / 'javac.args', ['-encoding', 'UTF-8', '-source', '8', '-target', '8', '-bootclasspath', str(android_jar) + os.pathsep + str(lambda_jar), '-d', classes, *sources])
            run([javac, '@' + str(work / 'javac.args')])
            dex_args = ['--min-api', '21', '--lib', str(android_jar), '--output', str(dex), '--debug' if args.configuration == 'Debug' else '--release'] + [str(p) for p in sorted(classes.rglob('*.class'))]
            (work / 'd8.args').write_text('\n'.join(dex_args) + '\n', encoding='utf-8')
            run([java, '-cp', d8, 'com.android.tools.r8.D8', '@' + str(work / 'd8.args')])
            normalized_archive(work / 'base.apk', work / 'unsigned.apk')
            with zipfile.ZipFile(work / 'unsigned.apk', 'a', compression=zipfile.ZIP_DEFLATED) as apk:
                apk.write(dex / 'classes.dex', 'classes.dex')
            run([align, '-f', '4', work / 'unsigned.apk', work / 'aligned.apk'])
            if args.unsigned:
                finished = work / 'aligned.apk'
            else:
                finished = work / 'signed.apk'
                run([java, '-jar', signer, 'sign', '--ks', args.keystore, '--ks-key-alias', args.alias, '--ks-pass', 'env:' + args.password_environment, '--out', finished, work / 'aligned.apk'], env=env)
                run([java, '-jar', signer, 'verify', finished])
            run([align, '-c', '4', finished])
            output.parent.mkdir(parents=True, exist_ok=True)
            # Exclusive creation also prevents races from replacing an existing official APK.
            try:
                with finished.open('rb') as reader, output.open('xb') as writer:
                    shutil.copyfileobj(reader, writer, 1024 * 1024)
            except Exception:
                # A failed copy can be incomplete; do not delete pre-existing output on FileExistsError.
                if not isinstance(sys.exc_info()[1], FileExistsError) and output.exists():
                    output.unlink()
                raise
            print(f'Built {"UNSIGNED (not installable)" if args.unsigned else "signed"} {args.configuration} APK: {output}')
    finally:
        if env is not None:
            env.pop(args.password_environment, None)


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print(f'Build failed: {error}', file=sys.stderr)
        sys.exit(1)
