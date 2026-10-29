[CmdletBinding()]
param([string]$InnoCompiler = '', [switch]$ImageOnly,
    [ValidatePattern('^\d+\.\d+\.\d+-beta\.\d+$')][string]$PackageVersion = '',
    [ValidatePattern('^v\d+\.\d+\.\d+(?:\.\d+)?-[a-z0-9-]+$')][string]$ReleaseVersion = '',
    [ValidatePattern('^\d+\.\d+\.\d+\.\d+$')][string]$WindowsVersion = '',
    [switch]$LocalTestCandidate,[switch]$Offline)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$project = Split-Path $PSScriptRoot
$utf8 = New-Object Text.UTF8Encoding($false)
function Write-Json([string]$Path,$Value) { [IO.File]::WriteAllText($Path,($Value|ConvertTo-Json -Depth 12)+"`n",$utf8) }
function Digest([string]$Path) { return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() }
function Invoke-Tool([string]$File,[string[]]$Arguments) { & $File @Arguments; if ($LASTEXITCODE -ne 0) { throw ('Packaging tool failed: '+(Split-Path $File -Leaf)) } }
function Invoke-Bounded([string]$File,[string]$Arguments) {
    $start=New-Object Diagnostics.ProcessStartInfo($File,$Arguments);$start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.WindowStyle='Hidden';$start.RedirectStandardError=$true;$start.RedirectStandardOutput=$true
    $process=New-Object Diagnostics.Process;$process.StartInfo=$start
    try { [void]$process.Start();$stdout=$process.StandardOutput.ReadToEndAsync();$stderr=$process.StandardError.ReadToEndAsync();if(!$process.WaitForExit(30000)){$process.Kill();throw 'Packaging verification timed out.'};$text=$stdout.Result+$stderr.Result;if($process.ExitCode -ne 0){throw ('Packaging verification failed: '+$text)};return $text }
    finally {$process.Dispose()}
}
Push-Location $project
try {
    $pin=Get-Content -LiteralPath beta/windows-native/toolchain.json -Raw|ConvertFrom-Json
    if($PackageVersion -or $ReleaseVersion -or $WindowsVersion){
        if(!$PackageVersion -or !$ReleaseVersion -or !$WindowsVersion){throw 'Specify package, release and Windows versions together.'}
        $pin.packageVersion=$PackageVersion;$pin.releaseVersion=$ReleaseVersion;$pin.windowsVersion=$WindowsVersion
    }
    if($pin.packageVersion -notmatch '^\d+\.\d+\.\d+-beta\.\d+$' -or $pin.windowsVersion -notmatch '^\d+\.\d+\.\d+\.\d+$' -or $pin.releaseVersion -notmatch '^v\d+\.\d+\.\d+(?:\.\d+)?-[a-z0-9-]+$'){throw 'Invalid pinned package/Windows/release version.'}
    $packageBase=($pin.packageVersion -split '-')[0]
    $releaseNumber=($pin.releaseVersion.Substring(1) -split '-')[0]
    $releaseWindows=if(($releaseNumber -split '\.').Count -eq 3){$releaseNumber+'.0'}else{$releaseNumber}
    if(!$pin.windowsVersion.StartsWith($packageBase+'.') -or $releaseWindows -ne $pin.windowsVersion){throw 'Pinned release, package and Windows versions disagree.'}
    $sourceStatus=(& git status --porcelain | Out-String).Trim()
    if($LASTEXITCODE -ne 0){throw 'Cannot verify committed source status.'}
    if($sourceStatus -and !$LocalTestCandidate){throw 'Final installers require a clean committed source tree. Use LocalTestCandidate only for unpublished tests.'}
    # Preserve every earlier local candidate while keeping the pinned release/package versions.
    $output=Join-Path $project ('build/distributions/'+$pin.releaseVersion+$(if($LocalTestCandidate){'/local-test-'+$pin.packageVersion+'-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss-fff')}else{''}))
    if(Test-Path -LiteralPath (Join-Path $output 'FromScratch-Beta-Setup.exe')){throw 'A candidate already exists for this version. Preserve it and choose a new unique beta version.'}
    $build=Join-Path $project 'build/native-installer';$downloads=Join-Path $build 'downloads'
    [void][IO.Directory]::CreateDirectory($downloads)
    $archive=Join-Path $downloads $pin.runtime.name
    if(!(Test-Path -LiteralPath $archive)) {
        $uri=[Uri]$pin.runtime.url
        if($uri.Host -ne 'github.com' -or !$uri.AbsolutePath.StartsWith('/adoptium/temurin11-binaries/releases/download/')){throw 'Runtime source must be an official pinned Temurin release.'}
        $ProgressPreference='SilentlyContinue';Invoke-WebRequest $uri -UseBasicParsing -OutFile $archive
    }
    if((Digest $archive) -ne $pin.runtime.sha256){throw 'Temurin runtime checksum differs. It will not be executed or packaged.'}
    # Reuse the existing beta build, bootstrap and unmodified upstream libraries. No old ZIP is replaced.
    & (Join-Path $PSScriptRoot 'Build-Beta.ps1') -PackageVersion $pin.packageVersion -ReleaseVersion $pin.releaseVersion -StageOnly -LocalTestCandidate:$LocalTestCandidate -Offline:$Offline
    if(!$?){throw 'Existing beta staging failed.'}
    $stage=Join-Path $project 'build/beta/stage'
    . (Join-Path $stage 'Common.ps1')
    $beta=Read-BetaPackage $stage
    & (Join-Path $PSScriptRoot 'Prepare-WindowsIcon.ps1')
    $work=Join-Path $build ('work-'+[Guid]::NewGuid().ToString('N'));[void][IO.Directory]::CreateDirectory($work)
    $image=Join-Path $work 'image';$payload=Join-Path $work 'payload'
    [void][IO.Directory]::CreateDirectory($image);[void][IO.Directory]::CreateDirectory($payload)
    $compiler=Join-Path $env:WINDIR 'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
    if(!(Test-Path -LiteralPath $compiler)){throw 'The Windows .NET Framework compiler is required on the developer machine.'}
    $assemblyInfo=Join-Path $work 'AssemblyInfo.cs'
    [IO.File]::WriteAllText($assemblyInfo,('using System.Reflection;'+"`n"+'[assembly: AssemblyVersion("'+$pin.windowsVersion+'")]'+"`n"+'[assembly: AssemblyFileVersion("'+$pin.windowsVersion+'")]'+"`n"+'[assembly: AssemblyInformationalVersion("'+$pin.releaseVersion+'")]'+"`n"),$utf8)
    Invoke-Tool $compiler @('/nologo','/target:winexe','/platform:x64','/optimize+',('/win32icon:'+(Join-Path $project 'beta\windows-native\FromScratch.ico')),'/reference:System.Windows.Forms.dll','/reference:System.Web.Extensions.dll',('/out:'+(Join-Path $image 'FromScratchBeta.exe')),(Join-Path $project 'beta\windows-native\Launcher.cs'),$assemblyInfo)
    Copy-Item -LiteralPath (Join-Path $project 'beta/windows-native/FromScratch.ico') -Destination $image
    Copy-Item -LiteralPath (Join-Path $stage 'lib') -Destination $payload -Recurse
    Copy-Item -LiteralPath (Join-Path $stage 'licenses') -Destination $payload -Recurse
    foreach($name in @('SOURCE.json','DEPENDENCIES.json')){Copy-Item -LiteralPath (Join-Path $stage $name) -Destination $payload}
    Copy-Item -LiteralPath (Join-Path $project 'beta/windows-native/THIRD_PARTY_NOTICES.txt') -Destination $payload
    Copy-Item -LiteralPath (Join-Path $project 'beta/windows-native/README.txt') -Destination $payload
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $runtimeUnzip=Join-Path $work 'runtime-extracted'
    [IO.Compression.ZipFile]::ExtractToDirectory($archive,$runtimeUnzip)
    $runtimeFolders=@(Get-ChildItem -LiteralPath $runtimeUnzip -Directory)
    if($runtimeFolders.Count -ne 1){throw 'Unexpected Temurin archive layout.'}
    $runtime=Join-Path $payload 'runtime'
    # Both paths are verified workspace-owned; no moving/removing any existing installation.
    if(!$runtimeFolders[0].FullName.StartsWith($work+'\') -or !$runtime.StartsWith($work+'\')){throw 'Runtime staging escaped the workspace.'}
    Move-Item -LiteralPath $runtimeFolders[0].FullName -Destination $runtime
    $javaVersion=Invoke-Bounded (Join-Path $runtime 'bin/java.exe') '-version'
    if($javaVersion -notmatch [regex]::Escape($pin.runtime.version)){throw 'The pinned runtime reports an unexpected version.'}
    if(!(Test-Path -LiteralPath (Join-Path $runtime 'legal/java.base/LICENSE'))){throw 'Temurin legal notices are missing.'}
    if(@(Get-ChildItem -LiteralPath $payload -Force -Recurse|Where-Object {$_.Attributes -band [IO.FileAttributes]::ReparsePoint}).Count){throw 'Linked files cannot enter the installer.'}
    $packagingInputs=@(Get-Item -LiteralPath 'scripts/Build-NativeInstaller.ps1','scripts/Build-Beta.ps1','scripts/Prepare-WindowsIcon.ps1','gradle/beta.gradle';Get-ChildItem -LiteralPath beta/windows-native -File)|Sort-Object FullName|ForEach-Object{[ordered]@{path=$_.FullName.Substring($project.Length+1).Replace('\','/');sha256=(Digest $_.FullName)}}
    Write-Json (Join-Path $payload 'native-build.json') ([ordered]@{format=1;releaseVersion=$pin.releaseVersion;packageVersion=$pin.packageVersion;sourceRevision=$beta.sourceRevision;gameplayTree=$beta.gameplayTree;packagingWorktreeDirty=((& git status --porcelain|Out-String).Trim().Length -gt 0);localTestCandidate=[bool]$LocalTestCandidate;runtime=$pin.runtime;compilerVersion=([Diagnostics.FileVersionInfo]::GetVersionInfo($compiler).FileVersion);compilerSha256=(Digest $compiler);launcherSource='beta/windows-native/Launcher.cs';packagingInputs=@($packagingInputs);status=$(if($LocalTestCandidate){'unpublished-local-test-candidate-not-for-distribution'}else{'unpublished-manual-acceptance-pending'})})
    $files=@(Get-ChildItem -LiteralPath $payload -File -Recurse|Sort-Object FullName|ForEach-Object{[ordered]@{path=$_.FullName.Substring($payload.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Digest $_.FullName)}})
    Write-Json (Join-Path $payload 'payload.json') ([ordered]@{format=1;releaseVersion=$pin.releaseVersion;packageVersion=$pin.packageVersion;sourceRevision=$beta.sourceRevision;mainClass=$beta.mainClass;files=$files})
    $manifestHash=Digest (Join-Path $payload 'payload.json')
    $relative='versions/'+$pin.packageVersion+'-'+$manifestHash.Substring(0,12)
    $destination=Join-Path $image $relative;[void][IO.Directory]::CreateDirectory((Split-Path $destination))
    Move-Item -LiteralPath $payload -Destination $destination
    Write-Json (Join-Path $image 'release.json') ([ordered]@{format=1;releaseVersion=$pin.releaseVersion;packageVersion=$pin.packageVersion;sourceRevision=$beta.sourceRevision;payload=$relative;manifestSha256=$manifestHash})
    [void][IO.Directory]::CreateDirectory($output)
    $imageManifest=@(Get-ChildItem -LiteralPath $image -File -Recurse|Sort-Object FullName|ForEach-Object{[ordered]@{path=$_.FullName.Substring($image.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Digest $_.FullName)}})
    Write-Json (Join-Path $output 'installer-files.json') $imageManifest
    $result=[ordered]@{releaseVersion=$pin.releaseVersion;packageVersion=$pin.packageVersion;sourceRevision=$beta.sourceRevision;localTestCandidate=[bool]$LocalTestCandidate;sourceWorktreeDirty=((& git status --porcelain|Out-String).Trim().Length -gt 0);image=$image;payload=$relative;output=$output;runtimeVersion=$pin.runtime.version;runtimeSha256=$pin.runtime.sha256;files=$imageManifest.Count;installer=$null}
    Write-Json (Join-Path $build 'latest-build.json') $result
    if($ImageOnly){Write-Host ('Native image prepared: '+$image);return}
    if(!$InnoCompiler){$candidates=@((Join-Path $build 'tools/inno/ISCC.exe'),(Join-Path ${env:ProgramFiles(x86)} 'Inno Setup 6/ISCC.exe'),(Join-Path $env:LOCALAPPDATA 'Programs/Inno Setup 6/ISCC.exe'));$InnoCompiler=$candidates|Where-Object{Test-Path -LiteralPath $_}|Select-Object -First 1}
    if(!$InnoCompiler -or !(Test-Path -LiteralPath $InnoCompiler)){throw 'Inno Setup 6.7.3 is required to compile Setup. Install it from https://jrsoftware.org/isdl.php after developer approval, then pass -InnoCompiler <ISCC.exe>. No downloaded installer is run by this script.'}
    # FromScratch.iss checks ISPP's real compiler version (PE metadata may say 0.0.0.0).
    $innoVersion=$pin.inno.version
    Invoke-Tool $InnoCompiler @(('/DImageDir='+$image),('/DOutputDir='+$output),('/DPackageVersion='+$releaseNumber),('/DWindowsVersion='+$pin.windowsVersion),('/DReleaseVersion='+$pin.releaseVersion),(Join-Path $project 'beta\windows-native\FromScratch.iss'))
    $setup=Join-Path $output 'FromScratch-Beta-Setup.exe';$setupHash=Digest $setup
    [IO.File]::WriteAllText($setup+'.sha256',$setupHash+'  FromScratch-Beta-Setup.exe'+"`n",$utf8)
    $result.installer=$setup;$result['sha256']=$setupHash;$result['signed']=((Get-AuthenticodeSignature -LiteralPath $setup).Status -eq 'Valid');$result['innoVersion']=$innoVersion;$result['innoCompilerSha256']=(Digest $InnoCompiler)
    Write-Json (Join-Path $output 'installer-build.json') $result;Write-Json (Join-Path $build 'latest-build.json') $result
    Write-Host ('Installer candidate: '+$setup);Write-Host ('SHA-256: '+$setupHash)
} finally { Pop-Location }
