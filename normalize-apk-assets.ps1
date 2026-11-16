param([Parameter(Mandatory=$true)][string]$Apk)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$resolved=[IO.Path]::GetFullPath($Apk)
$temporary=$resolved+'.normalized'
$source=[IO.Compression.ZipFile]::OpenRead($resolved)
$target=[IO.Compression.ZipFile]::Open($temporary,[IO.Compression.ZipArchiveMode]::Create)
try {
 foreach($entry in $source.Entries){
  $copy=$target.CreateEntry($entry.FullName.Replace('\','/'),[IO.Compression.CompressionLevel]::NoCompression)
  $input=$entry.Open();$output=$copy.Open()
  try {$input.CopyTo($output)} finally {$input.Dispose();$output.Dispose()}
 }
} finally {$source.Dispose();$target.Dispose()}
Move-Item -LiteralPath $temporary -Destination $resolved -Force
