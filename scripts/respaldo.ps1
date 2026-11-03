<#
.SYNOPSIS
    Crea un respaldo (archivo .sql) de la base de datos del Punto de Venta Integral.
.DESCRIPTION
    Usa mysqldump con una transacción consistente, de modo que se puede ejecutar con la aplicación abierta.
    El archivo se guarda en la carpeta "respaldos" del proyecto con fecha y hora en el nombre.
.EXAMPLE
    .\scripts\respaldo.ps1
.EXAMPLE
    .\scripts\respaldo.ps1 -Usuario root -Clave "mi_clave" -Carpeta "D:\Respaldos"
#>
param(
    [string]$Base = "punto_venta",
    [string]$Servidor = "localhost",
    [int]$Puerto = 3306,
    [string]$Usuario = "root",
    [string]$Clave = $env:DB_PASSWORD,
    [string]$Carpeta = (Join-Path $PSScriptRoot "..\respaldos"),
    [string]$MysqlBin = ""
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

$dump = Buscar-Herramienta "mysqldump"
New-Item -ItemType Directory -Force -Path $Carpeta | Out-Null
$archivo = Join-Path (Resolve-Path $Carpeta).Path ("{0}_{1:yyyyMMdd_HHmmss}.sql" -f $Base, (Get-Date))

$argumentos = @("--host=$Servidor", "--port=$Puerto", "--user=$Usuario")
if ($Clave) { $argumentos += "--password=$Clave" }
$argumentos += @("--single-transaction", "--default-character-set=utf8mb4", "--result-file=$archivo", $Base)

# mysqldump escribe advertencias (por ejemplo, "login sin contraseña") en stderr; no deben detener el script.
$ErrorActionPreference = "Continue"
& $dump @argumentos
$codigo = $LASTEXITCODE
$ErrorActionPreference = "Stop"
if ($codigo -ne 0) { throw "mysqldump terminó con el código $codigo; el respaldo no es válido." }

$tamano = (Get-Item $archivo).Length
if ($tamano -lt 1024) { throw "El respaldo ($archivo) es demasiado pequeño; revise la conexión y el nombre de la base." }
Write-Host "Respaldo creado: $archivo ($([math]::Round($tamano / 1KB)) KB)"
