#Requires -Version 5.1
<#
.SYNOPSIS
  Run Maven inside Docker (no host JDK/Maven required).

.DESCRIPTION
  Uses docker-compose.yml in the repo root. Maven dependencies are cached in the
  named volume anchor-rewrite-recipes-m2 — nothing is written to your host JAVA_HOME
  or global Maven install.

.PARAMETER MavenArgs
  Arguments passed to mvn (default: -B test).

.PARAMETER Preset
  Sets -Danchor.rewrite.preset=... for rewrite-maven-plugin (see docs/rewrite-presets.md).

.EXAMPLE
  .\scripts\run-mvn.ps1
  .\scripts\run-mvn.ps1 -MavenArgs @("-B", "test", "-Dtest=AddAnchorProbeCommentTest")
  .\scripts\run-mvn.ps1 -Preset com.anchor.migration.presets.DukesBankStackMigration -MavenArgs @("-B", "rewrite:run")
  .\scripts\run-mvn.ps1 -MavenArgs @("-B", "install", "-DskipTests")
#>
param(
    [string[]]$MavenArgs = @("-B", "test"),
    [string]$Preset = $null
)

$ErrorActionPreference = "Stop"

function Assert-DockerRunning {
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw @"
Docker is not available. Install Docker Desktop and ensure it is running, then retry.

  https://docs.docker.com/desktop/
"@
    }
}

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $RepoRoot
try {
    Assert-DockerRunning
    if ($Preset) {
        $MavenArgs += "-Danchor.rewrite.preset=$Preset"
    }
    Write-Host "==> rewrite-recipes (Docker Maven)" -ForegroundColor Cyan
    Write-Host "    mvn $($MavenArgs -join ' ')" -ForegroundColor DarkGray
    docker compose run --rm mvn mvn @MavenArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
