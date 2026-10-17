<#
  Genera la firma de release de ParSonic.

  Crea un keystore de produccion FUERA del repositorio (por defecto en
  %USERPROFILE%\parsonic-keys\) y un archivo keystore.properties.local en la
  raiz del proyecto, que esta en .gitignore y nunca se sube.

  La contrasena se genera aleatoriamente y NO se imprime por pantalla: queda
  guardada unicamente en keystore.properties.local. Guarda ese archivo (y el
  .jks) en un lugar seguro: sin ellos no podras firmar futuras actualizaciones.

  Uso:
    .\tools\generate-release-keystore.ps1
    .\tools\generate-release-keystore.ps1 -KeyStorePath C:\ruta\parsonic-release.jks
#>
[CmdletBinding()]
param(
    [string]$KeyStorePath = (Join-Path $env:USERPROFILE "parsonic-keys\parsonic-release.jks"),
    [string]$KeyAlias = "parsonic",
    [string]$DName = "CN=Andres Parra, OU=ParSonic, O=andrespar2, L=Bogota, C=CO",
    [int]$ValidityDays = 10000,
    [string]$JdkHome = $env:JAVA_HOME
)

$ErrorActionPreference = "Stop"

# --- Localizar keytool -------------------------------------------------------
if ([string]::IsNullOrWhiteSpace($JdkHome)) {
    $JdkHome = "C:\Users\Usuario\android-dev\jdk17\jdk-17.0.20.1+1"
}
$keytool = Join-Path $JdkHome "bin\keytool.exe"
if (-not (Test-Path $keytool)) {
    throw "No se encontro keytool en '$keytool'. Pasa -JdkHome con la ruta de un JDK 17."
}

# --- Rutas -------------------------------------------------------------------
$root = Split-Path -Parent $PSScriptRoot
$propsPath = Join-Path $root "keystore.properties.local"

if (Test-Path $KeyStorePath) {
    throw "Ya existe un keystore en '$KeyStorePath'. Borralo o indica otro -KeyStorePath (no sobrescribo claves existentes)."
}

$keyStoreDir = Split-Path -Parent $KeyStorePath
if (-not (Test-Path $keyStoreDir)) {
    New-Item -ItemType Directory -Path $keyStoreDir -Force | Out-Null
}

# --- Contrasena aleatoria (no se imprime) ------------------------------------
# El formato PKCS12 no admite contrasenas distintas para el almacen y la clave:
# keytool ignora -keypass y usa la del almacen. Por eso usamos una sola.
function New-RandomPassword {
    param([int]$Length = 32)
    $chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%&*+-_"
    $bytes = New-Object 'System.Byte[]' $Length
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
}

$storePassword = New-RandomPassword
$keyPassword = $storePassword

# --- Generar el keystore -----------------------------------------------------
Write-Host "Generando keystore de release en: $KeyStorePath"
& $keytool -genkeypair `
    -keystore $KeyStorePath `
    -alias $KeyAlias `
    -keyalg RSA -keysize 4096 -validity $ValidityDays `
    -storetype PKCS12 `
    -dname $DName `
    -storepass $storePassword `
    -keypass $keyPassword | Out-Null

if (-not (Test-Path $KeyStorePath)) {
    throw "keytool no genero el keystore."
}

# --- Escribir keystore.properties.local --------------------------------------
$storeFileEscaped = $KeyStorePath -replace '\\', '\\'
$content = @"
# Credenciales de produccion de ParSonic (NO subir al repositorio).
# Generado por tools/generate-release-keystore.ps1 el $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss').
storeFile=$storeFileEscaped
storePassword=$storePassword
keyAlias=$KeyAlias
keyPassword=$keyPassword
"@

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($propsPath, $content, $utf8NoBom)

Write-Host ""
Write-Host "Listo." -ForegroundColor Green
Write-Host "  Keystore : $KeyStorePath"
Write-Host "  Alias    : $KeyAlias"
Write-Host "  Props    : $propsPath (gitignored)"
Write-Host ""
Write-Host "IMPORTANTE: haz una copia de seguridad del .jks y de keystore.properties.local."
Write-Host "Sin ellos no podras firmar futuras actualizaciones de la app." -ForegroundColor Yellow
