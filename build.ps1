param(
 [ValidateSet('Debug','Release')][string]$Configuration='Debug',
 [string]$JavaHome=$env:JAVA_HOME,
 [string]$AndroidHome=$env:ANDROID_HOME,
 [Parameter(Mandatory=$true)][string]$KeyStore,
 [string]$PasswordEnvironment='DETOX_KEYSTORE_PASSWORD',
 [string]$KeyAlias='tvhome'
)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$root=$PSScriptRoot
if(!$AndroidHome){$AndroidHome=$env:ANDROID_SDK_ROOT}
if(!$JavaHome -or !$AndroidHome){throw 'Set JavaHome (Java 17) and AndroidHome (SDK platform 34/build-tools 34.0.0).'}
$java=Join-Path $JavaHome 'bin'
$bt=Join-Path $AndroidHome 'build-tools/34.0.0'
$androidJar=Join-Path $AndroidHome 'platforms/android-34/android.jar'
foreach($file in @("$java/javac.exe","$java/java.exe","$bt/aapt2.exe","$bt/d8.bat","$bt/apksigner.bat","$bt/zipalign.exe",$androidJar,$KeyStore)){
 if(!(Test-Path -LiteralPath $file -PathType Leaf)){throw "Missing build prerequisite: $file"}
}
if(![Environment]::GetEnvironmentVariable($PasswordEnvironment,'Process')){throw "Set the signing password in environment variable $PasswordEnvironment; do not pass it on the command line."}
function Invoke-BuildTool([string]$Tool,[string[]]$ToolArguments){
 & $Tool @ToolArguments
 if($LASTEXITCODE -ne 0){throw "Build tool failed: $([IO.Path]::GetFileName($Tool)) (exit $LASTEXITCODE)"}
}
$previousJava=$env:JAVA_HOME
try{
 $env:JAVA_HOME=$JavaHome
 $build=Join-Path $root "build/$Configuration"
 $resolved=[IO.Path]::GetFullPath($build)
 $buildRoot=[IO.Path]::GetFullPath((Join-Path $root 'build'))+[IO.Path]::DirectorySeparatorChar
 if(!$resolved.StartsWith($buildRoot,[StringComparison]::OrdinalIgnoreCase)){throw 'Unsafe build output path'}
 if(Test-Path -LiteralPath $resolved){Remove-Item -LiteralPath $resolved -Recurse -Force}
 New-Item -ItemType Directory -Force "$build/classes","$build/dex","$build/gen","$root/output"|Out-Null
 [xml]$manifest=Get-Content -LiteralPath "$root/AndroidManifest.xml" -Raw
 $android='http://schemas.android.com/apk/res/android'
 $manifest.manifest.application.SetAttribute('debuggable',$android,($Configuration -eq 'Debug').ToString().ToLowerInvariant())|Out-Null
 $manifest.Save("$build/AndroidManifest.xml")
 $version=$manifest.manifest.GetAttribute('versionName',$android)
 $apps=@(Get-Content -LiteralPath "$root/apps.txt"|Where-Object{$_ -notmatch '^\s*#' -and $_.Trim()}|ForEach-Object{
  $pkg=$_ -replace '\s',''
  if($pkg -notmatch '^[A-Za-z0-9_.]+$'){throw 'Invalid apps.txt package name'}
  '"'+$pkg+'",'
 })
 [IO.File]::WriteAllText("$build/gen/Apps.java",('package com.ansub.tvhome; final class Apps { static final String[] LIST={'+($apps -join '')+'};}'),[Text.UTF8Encoding]::new($false))
 # Package only reviewed runtime assets, even when personal media remains on the laptop.
 $publicAssets=Join-Path $build 'assets'
 New-Item -ItemType Directory -Force $publicAssets|Out-Null
 $assetRoot=[IO.Path]::GetFullPath((Join-Path $root 'assets'))+[IO.Path]::DirectorySeparatorChar
 $stageRoot=[IO.Path]::GetFullPath($publicAssets)+[IO.Path]::DirectorySeparatorChar
 $assetManifest=Get-Content -LiteralPath "$root/tools/public-assets.json" -Raw|ConvertFrom-Json
 $seenAssets=@{}
 foreach($asset in $assetManifest.assets){
  $name=[string]$asset.path
  if($name -notmatch '^(public/|fonts/|licenses/|ui/|phone_companion\.html$)' -or $name.Contains('\') -or $name.Split('/') -contains '..' -or $seenAssets.ContainsKey($name)){throw 'Invalid public asset path'}
  $seenAssets[$name]=$true
  $source=[IO.Path]::GetFullPath((Join-Path $assetRoot $name));$destination=[IO.Path]::GetFullPath((Join-Path $stageRoot $name))
  if(!$source.StartsWith($assetRoot,[StringComparison]::OrdinalIgnoreCase) -or !$destination.StartsWith($stageRoot,[StringComparison]::OrdinalIgnoreCase)){throw 'Asset path escaped staging'}
  $file=Get-Item -LiteralPath $source
  if($file.Length -ne $asset.bytes -or (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash -ne $asset.sha256){throw "Public asset changed: $name"}
  New-Item -ItemType Directory -Force ([IO.Path]::GetDirectoryName($destination))|Out-Null
  Copy-Item -LiteralPath $source -Destination $destination
 }
 Invoke-BuildTool "$bt/aapt2.exe" @('compile','--dir',"$root/res",'-o',"$build/resources.zip")
 Invoke-BuildTool "$bt/aapt2.exe" @('link','-o',"$build/base.apk",'-I',$androidJar,'-R',"$build/resources.zip",'--auto-add-overlay','--manifest',"$build/AndroidManifest.xml",'--min-sdk-version','21','--target-sdk-version','34','-A',$publicAssets)
 & "$root/normalize-apk-assets.ps1" -Apk "$build/base.apk"
 $sources=@(Get-ChildItem "$root/src","$build/gen" -Recurse -Filter '*.java'|ForEach-Object FullName)
 Invoke-BuildTool "$java/javac.exe" (@('-nowarn','-encoding','UTF-8','-source','8','-target','8','-bootclasspath',"$androidJar;$bt/core-lambda-stubs.jar",'-d',"$build/classes")+$sources)
 $dexArguments=@('--min-api','21','--lib',$androidJar,'--output',"$build/dex")
 $dexArguments+=if($Configuration -eq 'Debug'){'--debug'}else{'--release'}
 $dexArguments+=@(Get-ChildItem "$build/classes" -Recurse -Filter '*.class'|ForEach-Object FullName)
 [IO.File]::WriteAllLines("$build/d8.args",[string[]]$dexArguments,[Text.UTF8Encoding]::new($false))
 Invoke-BuildTool "$bt/d8.bat" @("@$build/d8.args")
 Copy-Item -LiteralPath "$build/base.apk" -Destination "$build/unsigned.apk"
 Invoke-BuildTool "$java/jar.exe" @('uf',"$build/unsigned.apk",'-C',"$build/dex",'classes.dex')
 Invoke-BuildTool "$bt/zipalign.exe" @('-f','4',"$build/unsigned.apk","$build/aligned.apk")
 $output=Join-Path $root "output/Detox-$version-$($Configuration.ToLowerInvariant()).apk"
 Invoke-BuildTool "$bt/apksigner.bat" @('sign','--ks',$KeyStore,'--ks-key-alias',$KeyAlias,'--ks-pass',"env:$PasswordEnvironment",'--out',$output,"$build/aligned.apk")
 Invoke-BuildTool "$bt/apksigner.bat" @('verify',$output)
 Invoke-BuildTool "$bt/zipalign.exe" @('-c','4',$output)
 Write-Output "Verified $Configuration APK: $output"
}finally{$env:JAVA_HOME=$previousJava}
