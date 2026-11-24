$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
    if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw 'Instala Node.js compatible con Angular 22.' }
    if (-not (Test-Path -LiteralPath 'node_modules/@angular/cli/bin/ng.js')) {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw 'No se pudieron instalar las dependencias.' }
    }
    & npm.cmd start
    if ($LASTEXITCODE -ne 0) { throw 'Angular no pudo iniciar. Revisa el mensaje anterior.' }
}
finally { Pop-Location }
