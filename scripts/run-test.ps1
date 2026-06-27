#Requires -Version 5.1
<#
.SYNOPSIS
  Run rewrite-recipes unit tests in Docker.

.EXAMPLE
  .\scripts\run-test.ps1
#>
& (Join-Path $PSScriptRoot "run-mvn.ps1") -MavenArgs @("-B", "test")
exit $LASTEXITCODE
