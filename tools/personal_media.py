"""Explicit opt-in personal build profile; public builds never call this module."""
from pathlib import Path,PurePosixPath
import hashlib,json,re,shutil
from public_assets import entries

CATALOGS={'BundledWallpaperCatalog.java','BuiltInLiveVideoCatalog.java','GamingThemeCatalog.java','GamingMusicCatalog.java'}

def _asset(root,name):
    if not isinstance(name,str) or '\\' in name or ':' in name or PurePosixPath(name).is_absolute() or '..' in PurePosixPath(name).parts:
        raise ValueError('Unsafe personal asset path')
    original=root/'assets'/name;path=original.resolve()
    if original.is_symlink() or not path.is_relative_to((root/'assets').resolve()) or not path.is_file():
        raise ValueError('Personal asset missing or outside assets')
    return path

def load_profile(root,profile):
    root=Path(root).resolve();profile=Path(profile).resolve()
    if not profile.is_relative_to((root/'assets').resolve()):raise ValueError('Personal profile must stay inside local assets')
    config=json.loads(profile.read_text(encoding='utf-8'))
    if config.get('format')!='detox.personal.media' or config.get('version')!=1:raise ValueError('Unsupported personal profile')
    expected=set()
    for group,field,prefix in [('wallpapers','files','wallpapers/'),('live','assets',''),('gaming','files','gaming/videos/'),('music','assets','')]:
        value=config[group];names=value['names'];files=value[field]
        if not files or len(set(files))!=len(files) or len(names)!=len(files)+(1 if group=='wallpapers' else 0):raise ValueError('Misaligned personal catalog: '+group)
        expected.update(prefix+name for name in files)
    if config['wallpapers']['names'][-1]!='Photo' or len(config['wallpapers']['palettes'])!=len(config['wallpapers']['names']):raise ValueError('Wallpaper palettes or Photo entry misaligned')
    if any(len(row)!=5 or any(not isinstance(n,int) or n<0 or n>0xffffffff for n in row) for row in config['wallpapers']['palettes']):raise ValueError('Invalid wallpaper palettes')
    found=set()
    for row in config['assets']:
        name=row['path']
        if not name.startswith(('wallpapers/','gaming/videos/','gaming/music/')) or name in found:raise ValueError('Invalid or duplicate personal asset')
        path=_asset(root,name);data=path.read_bytes()
        if len(data)!=row['bytes'] or hashlib.sha256(data).hexdigest()!=row['sha256']:raise ValueError('Personal asset changed: '+name)
        found.add(name)
    if expected!=found:raise ValueError('Personal catalog and exact media manifest differ')
    return config

def _array(values):return '{'+','.join(json.dumps(value,ensure_ascii=True) for value in values)+'}'

def _replace_array(source,name,values):
    result,count=re.subn(r'(String\[\]\s+'+name+r'\s*=)\s*\{.*?\};',lambda m:m[1]+_array(values)+';',source,count=1,flags=re.S)
    if count!=1:raise ValueError('Catalog array not found: '+name)
    return result

def prepare(root,profile,stage,gen):
    root=Path(root).resolve();stage=Path(stage).resolve();gen=Path(gen).resolve()
    for path in (stage,gen):
        if not path.is_relative_to((root/'build').resolve()):raise ValueError('Personal staging must stay in build outputs')
    if stage.exists() and any(stage.iterdir()):raise ValueError('Personal staging must be empty')
    config=load_profile(root,profile);stage.mkdir(parents=True,exist_ok=True);gen.mkdir(parents=True,exist_ok=True)
    staged=[]
    for name,path in entries(root):
        if name.startswith('public/'):continue
        destination=stage/name;destination.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(path,destination);staged.append(name)
    for row in config['assets']:
        name=row['path'];destination=stage/name;destination.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(_asset(root,name),destination);staged.append(name)
    folder=root/'src/com/ansub/tvhome'
    source=(folder/'BundledWallpaperCatalog.java').read_text(encoding='utf-8');wall=config['wallpapers']
    source=_replace_array(_replace_array(source,'NAMES',wall['names']),'FILES',wall['files'])
    palette='{'+','.join('{'+','.join('0x%08X'%n for n in row)+'}' for row in wall['palettes'])+'}'
    source,count=re.subn(r'(int\[\]\[\]\s+PALETTES\s*=)\s*\{.*?\};',lambda m:m[1]+palette+';',source,count=1,flags=re.S)
    if count!=1:raise ValueError('Wallpaper palette field not found')
    source=source.replace('public/wallpapers/','wallpapers/')
    rotations=wall.get('rotate_landscape',[])
    predicate='||'.join(json.dumps(name)+'.equals(NAMES[index])' for name in rotations) or 'false'
    source=source.replace('static boolean landscapeRotation(int index){return false;}','static boolean landscapeRotation(int index){return index>=0&&index<size()&&('+predicate+');}')
    aliases=''.join('if('+json.dumps(old)+'.equals(saved))saved='+json.dumps(new)+';' for old,new in wall.get('aliases',{}).items())
    source=source.replace('static int selected(String saved){','static int selected(String saved){'+aliases)
    (gen/'BundledWallpaperCatalog.java').write_text(source,encoding='utf-8')
    source=(folder/'BuiltInLiveVideoCatalog.java').read_text(encoding='utf-8')
    source=_replace_array(_replace_array(source,'NAMES',config['live']['names']),'ASSETS',config['live']['assets'])
    (gen/'BuiltInLiveVideoCatalog.java').write_text(source,encoding='utf-8')
    source=(folder/'GamingThemeCatalog.java').read_text(encoding='utf-8')
    source=_replace_array(_replace_array(source,'NAMES',config['gaming']['names']),'FILES',config['gaming']['files']).replace('asset:public/videos/','asset:gaming/videos/')
    (gen/'GamingThemeCatalog.java').write_text(source,encoding='utf-8')
    music=config['music']
    source='package com.ansub.tvhome;\nfinal class GamingMusicCatalog {static final String[] NAMES='+_array(music['names'])+';private static final String[] ASSETS='+_array(music['assets'])+';static boolean available(){return NAMES.length>0;}static String asset(int index){if(index<0||index>=ASSETS.length)throw new IllegalArgumentException("Invalid music track");return ASSETS[index];}private GamingMusicCatalog(){}}\n'
    (gen/'GamingMusicCatalog.java').write_text(source,encoding='utf-8')
    return {'edition':'personal','assets':sorted(staged),'overrides':sorted(CATALOGS),'catalog_counts':{group:len(config[group]['names'])-(1 if group=='wallpapers' else 0) for group in ['wallpapers','live','gaming','music']}}
