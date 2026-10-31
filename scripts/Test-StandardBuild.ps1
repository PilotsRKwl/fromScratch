[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$work=Join-Path $root 'build/standard-check'
[void](New-Item -ItemType Directory -Path (Join-Path $work 'src') -Force)
$headers=@{'User-Agent'='FromScratch-standard-build-verification'}
$revision=(Invoke-RestMethod 'https://api.github.com/repos/runelite/plugin-hub-tooling/commits/master' -Headers $headers -TimeoutSec 20).sha
if($revision -notmatch '^[a-f0-9]{40}$'){throw 'Cannot verify official tooling revision.'}
$version=(Invoke-WebRequest 'https://raw.githubusercontent.com/runelite/plugin-hub/master/runelite.version' -UseBasicParsing -TimeoutSec 20).Content.Trim()
if($version -notmatch '^\d+\.\d+\.\d+$'){throw 'Cannot verify official Hub RuneLite version.'}
$base='https://raw.githubusercontent.com/runelite/plugin-hub-tooling/'+$revision+'/package/src/main/resources/net/runelite/pluginhub/packager/'
foreach($pair in @(@('standard-build.gradle','build.gradle'),@('standard-settings.gradle','settings.gradle'))){Invoke-WebRequest ($base+$pair[0]) -UseBasicParsing -TimeoutSec 20 -OutFile (Join-Path $work $pair[1])}
$packagerUrl='https://raw.githubusercontent.com/runelite/plugin-hub-tooling/'+$revision+'/package/src/main/java/net/runelite/pluginhub/packager/Plugin.java'
Invoke-WebRequest $packagerUrl -UseBasicParsing -TimeoutSec 20 -OutFile (Join-Path $work 'Plugin.java')
$packager=[IO.File]::ReadAllText((Join-Path $work 'Plugin.java'))
$nameRule=[regex]::Match($packager,'PLUGIN_INTERNAL_NAME_TEST\s*=\s*Pattern\.compile\("([^"]+)"\)')
if(!$nameRule.Success -or !$packager.Contains('PluginDescriptor.internalName must match the real internal name')){throw 'Official identifier validation changed; review the current packager.'}
Copy-Item -LiteralPath (Join-Path $root 'src/main') -Destination (Join-Path $work 'src') -Recurse -Force
# Hub supplies its client version externally; keep its standard build file byte-for-byte.
$init="allprojects { configurations.configureEach { resolutionStrategy.eachDependency { if (requested.group == 'net.runelite' && requested.name == 'client') { useVersion '$version' } } } }"
[IO.File]::WriteAllText((Join-Path $work 'hub-version.gradle'),$init,[Text.UTF8Encoding]::new($false))
& (Join-Path $root 'gradlew.bat') '-p' $work '--init-script' (Join-Path $work 'hub-version.gradle') 'jar' '--no-daemon' '--console=plain'
if($LASTEXITCODE -ne 0){throw 'Official standard-template build failed.'}
# Compilation alone does not validate the manifest or compiled descriptor.
$jar=Join-Path $work 'build/libs/standard-plugin-build.jar'
if(!(Test-Path -LiteralPath $jar)){throw 'Official standard JAR is missing.'}
$descriptor=(& javap -v -classpath $jar com.fromscratch.FromScratchPlugin | Out-String)
if($LASTEXITCODE -ne 0){throw 'Cannot inspect the compiled plugin descriptor.'}
$internalName=[regex]::Match($descriptor,'internalName="([^"]+)"').Groups[1].Value
$issues=New-Object 'System.Collections.Generic.List[string]'
$identifier='from-scratch'
if($identifier -cnotmatch $nameRule.Groups[1].Value){$issues.Add('Intended manifest identifier fails the official name rule.')}
if($internalName -cne $identifier){$issues.Add('Compiled internalName is '+$internalName+'; it must match plugins/'+$identifier+'. Resolve legacy storage compatibility before renaming it.')}
$properties=@{}
foreach($line in [IO.File]::ReadAllLines((Join-Path $root 'runelite-plugin.properties'))){if($line -match '^([^#=]+)=(.*)$'){$properties[$matches[1]]=$matches[2]}}
if($properties.build -cne 'standard' -or $properties.plugins -cne 'com.fromscratch.FromScratchPlugin'){$issues.Add('Production entry point or standard build metadata differs.')}
if($properties.ContainsKey('version')){$issues.Add('Hub metadata must derive its version from the source commit.')}
foreach($tag in ($properties.tags -split ',')){if($tag -cnotmatch '^[a-z0-9-]+$'){$issues.Add('Invalid discovery tag: '+$tag)}}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=[IO.Compression.ZipFile]::OpenRead($jar)
try {
    if(@($archive.Entries | Where-Object {$_.FullName -match '^com/fromscratch/(preview/|migration/|migrationTest/)|Test\.class$|^META-INF/services/net\.runelite\.client\.plugins\.Plugin$'}).Count){$issues.Add('Development classes or Plugin service registration entered the production JAR.')}
    $entry=$archive.GetEntry('com/fromscratch/FromScratchPlugin.class')
    if(!$entry){throw 'Production entry point missing from JAR.'}
    $stream=$entry.Open()
    try {$header=New-Object byte[] 8;if($stream.Read($header,0,8) -ne 8 -or $header[6] -ne 0 -or $header[7] -ne 55){$issues.Add('Production bytecode is not Java 11.')}} finally {$stream.Dispose()}
    if(!$archive.GetEntry('com/fromscratch/icon.png')){$issues.Add('JAR-safe navigation icon is missing.')}
} finally {$archive.Dispose()}
$evidence=[ordered]@{toolingRevision=$revision;runeLiteVersion=$version;templateSha256=(Get-FileHash -LiteralPath (Join-Path $work 'build.gradle')).Hash.ToLowerInvariant();status='local-windows-standard-template-compile-passed';expectedIdentifier=$identifier;compiledIdentifier=$internalName;identifierRule=$nameRule.Groups[1].Value;eligibility=$(if($issues.Count){'blocked'}else{'local-preflight-passed'});issues=@($issues.ToArray());jarSha256=(Get-FileHash -LiteralPath $jar).Hash.ToLowerInvariant();officialPackagerExecuted=$false;scope='Official standard template and source-derived packager rules, compiled descriptor/bytecode and artifact inspection. Does not execute the hosted packager, security review or native UI.'}
$evidence | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $work 'evidence.json') -Encoding UTF8
if($issues.Count){Write-Output ('Plugin Hub compatibility BLOCKED: '+($issues -join ' '));exit 1}
Write-Host 'PASS standard compilation, current identifier rules, metadata and production artifact preflight. Hosted packager/review remain separate.'
