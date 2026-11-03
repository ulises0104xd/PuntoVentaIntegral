<#
.SYNOPSIS
    Restaura un respaldo (.sql) creado con respaldo.ps1.
.DESCRIPTION
    ATENCIÓN: reemplaza el contenido actual de las tablas de la base indicada. Cierre la aplicación antes de restaurar.
    Si la base no existe, la crea. Pide confirmación a menos que se use -Si.
.EXAMPLE
    .\scripts\restaurar.ps1 -Archivo .\respaldos\punto_venta_20261030_120000.sql
#>
param(
    [Parameter(Mandatory = $true)][string]$Archivo,
    [string]$Base = "punto_venta",
    [string]$Servidor = "localhost",
    [int]$Puerto = 3306,
    [string]$Usuario = "root",
    [string]$Clave = $env:DB_PASSWORD,
    [string]$MysqlBin = "",
    [switch]$Si
)

$ErrorActionPreference = "Stop"

function Buscar-Herramienta([string]$nombre) {
    if ($MysqlBin -ne "") {
        $ruta = Join-Path $MysqlBin "$nombre.exe"
        if (Test-Path $ruta) { return $ruta }
        throw "No se encontró $nombre.exe en $MysqlBin"
    }
    $enPath = Get-Command "$nombre.exe" -ErrorAction SilentlyContinue
    if ($enPath) { return $enPath.Source }
    $candidatos = @("C:\xampp\mysql\bin\$nombre.exe") +
        (Get-ChildItem "C:\Program Files\MySQL\*\bin\$nombre.exe" -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })
    foreach ($c in $candidatos) { if (Test-Path $c) { return $c } }
    throw "No se encontró $nombre.exe. Indique la carpeta con -MysqlBin (por ejemplo C:\xampp\mysql\bin)."
}

if (-not (Test-Path $Archivo)) { throw "No existe el archivo $Archivo" }
$ruta = (Resolve-Path $Archivo).Path
if (-not $Si) {
    $r = Read-Host "Se reemplazarán los datos de la base '$Base' con el respaldo '$ruta'. ¿Continuar? (escriba SI)"
    if ($r -ne "SI") { Write-Host "Cancelado."; exit 1 }
}

$mysql = Buscar-Herramienta "mysql"
$conexion = @("--host=$Servidor", "--port=$Puerto", "--user=$Usuario", "--default-character-set=utf8mb4")
if ($Clave) { $conexion += "--password=$Clave" }

# El cliente mysql escribe advertencias en stderr; no deben detener el script (se revisa el código de salida).
$ErrorActionPreference = "Continue"
& $mysql @conexion "--execute=CREATE DATABASE IF NOT EXISTS ``$Base`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
$codigo = $LASTEXITCODE
if ($codigo -ne 0) { $ErrorActionPreference = "Stop"; throw "No se pudo crear o abrir la base $Base" }

$origen = $ruta -replace "\\", "/"
& $mysql @conexion $Base "--execute=SOURCE $origen"
$codigo = $LASTEXITCODE
$ErrorActionPreference = "Stop"
if ($codigo -ne 0) { throw "La restauración falló (código $codigo)." }
Write-Host "Restauración completada en la base '$Base'."
