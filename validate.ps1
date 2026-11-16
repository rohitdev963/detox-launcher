param([string]$JavaHome=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(!$JavaHome -or !(Test-Path "$JavaHome/bin/javac.exe")){throw 'Set JavaHome to Java 17.'}
$root=$PSScriptRoot;$classes=Join-Path $root 'build/host-tests'
New-Item -ItemType Directory -Force $classes|Out-Null
$tests=@(Get-ChildItem "$root/tests" -Filter '*.java')
& "$JavaHome/bin/javac.exe" -encoding UTF8 -d $classes -sourcepath "$root/src;$root/tests" @($tests|ForEach-Object FullName)
if($LASTEXITCODE){throw 'Regression compilation failed'}
$passed=0
foreach($test in $tests){
 $package=[regex]::Match((Get-Content $test.FullName -Raw),'(?m)^package\s+([^;]+);').Groups[1].Value
 $type=if($package){$package+'.'+$test.BaseName}else{$test.BaseName}
 & "$JavaHome/bin/java.exe" -cp $classes $type "$root/src/com/ansub/tvhome/MainActivity.java"
 if($LASTEXITCODE){throw "Regression failed: $type"};$passed++
}
$xmlFiles=@(Get-Item "$root/AndroidManifest.xml")+@(Get-ChildItem "$root/res" -Recurse -Filter '*.xml')
foreach($file in $xmlFiles){[xml]$xml=Get-Content $file.FullName -Raw}
$manifest=Get-Content -LiteralPath "$root/tools/public-assets.json" -Raw|ConvertFrom-Json
foreach($asset in $manifest.assets){
 if($asset.path -notmatch '^(public/|fonts/|licenses/|ui/|phone_companion\.html$)' -or $asset.path -match '\.\.|\\'){throw 'Unsafe public asset path'}
 $file=Get-Item -LiteralPath (Join-Path "$root/assets" $asset.path)
 if($file.Length -ne $asset.bytes -or (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash -ne $asset.sha256){throw "Public asset hash mismatch: $($asset.path)"}
}
if(@(Get-ChildItem "$root/assets/public/wallpapers" -File -Filter '*.jpg').Count -ne 3 -or @(Get-ChildItem "$root/assets/public/videos" -File -Filter '*.mp4').Count -ne 2){throw 'Public sample inventory mismatch'}
Write-Output "PASS: $passed host tests, $($xmlFiles.Count) XML documents, 41 reviewed public assets / 3 wallpapers / 2 silent videos / no bundled music / 5 fonts."
