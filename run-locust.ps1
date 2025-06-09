# run-locust-test.ps1

# Parámetros del script
param(
    [string]$TargetIP = "localhost",
    [int]$LoadUsers = 10,
    [int]$LoadSpawnRate = 2,
    [string]$LoadDuration = "60s",
    [int]$StressUsers = 150,
    [int]$StressSpawnRate = 3,
    [string]$StressDuration = "60s"
)

# Configuración para prueba de carga
$locustFile = "locustfile.py"
$loadTestUsers = $LoadUsers
$loadTestSpawnRate = $LoadSpawnRate
$loadTestDuration = $LoadDuration

# Configuración para prueba de estrés 
$stressTestUsers = $StressUsers
$stressTestSpawnRate = $StressSpawnRate
$stressTestDuration = $StressDuration

Write-Host "Configuración de pruebas:" -ForegroundColor Yellow
Write-Host "IP objetivo: $TargetIP" -ForegroundColor Cyan
Write-Host "Prueba de carga: $loadTestUsers usuarios, $loadTestSpawnRate spawn rate, $loadTestDuration duración" -ForegroundColor Cyan
Write-Host "Prueba de estrés: $stressTestUsers usuarios, $stressTestSpawnRate spawn rate, $stressTestDuration duración" -ForegroundColor Cyan

# Guardar la ubicación actual
$originalLocation = Get-Location
Write-Host "Ubicación original: $originalLocation"

# Cambiar al directorio load-testing
$loadTestingDir = "load-testing"
if (Test-Path $loadTestingDir) {
    Set-Location $loadTestingDir
    Write-Host "Cambiando al directorio: $loadTestingDir"
} else {
    Write-Host "Error: No se encontró el directorio $loadTestingDir"
    exit 1
}

# Crear carpetas para los resultados si no existen
$loadResultsDir = "resultados-carga"
$stressResultsDir = "resultados-estres"

if (-not (Test-Path $loadResultsDir)) {
    New-Item -ItemType Directory -Path $loadResultsDir | Out-Null
    Write-Host "Creada carpeta: $loadResultsDir"
}

if (-not (Test-Path $stressResultsDir)) {
    New-Item -ItemType Directory -Path $stressResultsDir | Out-Null
    Write-Host "Creada carpeta: $stressResultsDir"
}

# Función para ejecutar prueba
function Run-LocustTest {
    param(
        [string]$testType,
        [int]$users,
        [int]$spawnRate,
        [string]$duration,
        [string]$outputDir,
        [string]$targetIP
    )
    
    # Usar nombre fijo para sobrescribir archivos anteriores
    $reportPrefix = "$outputDir\load_test_${testType}"
    
    Write-Host "`n==================== PRUEBA DE $testType ===================="
    Write-Host "Ejecutando Locust ($testType) con $users usuarios por $duration..."
    Write-Host "Spawn rate: $spawnRate usuarios/segundo"
    Write-Host "IP objetivo: $targetIP"
    Write-Host "Carpeta de resultados: $outputDir"
    Write-Host "Los archivos anteriores serán sobrescritos"
    Write-Host "============================================================`n"
    
    # Establecer la variable de entorno para la IP
    $env:TARGET_IP = $targetIP
    
    # Generar tanto CSV como HTML
    python -m locust -f $locustFile --headless -u $users -r $spawnRate -t $duration --csv=$reportPrefix --html="$reportPrefix`_report.html"
    
    Write-Host "Informe de $testType guardado en $outputDir como:"
    Write-Host "REPORTE HTML CON GRÁFICAS: $reportPrefix`_report.html"
    Write-Host " Archivos CSV:"
    Write-Host "    - $reportPrefix`_stats.csv"
    Write-Host "    - $reportPrefix`_failures.csv"
    Write-Host "    - $reportPrefix`_stats_history.csv"
    
    if (Test-Path "$reportPrefix`_exceptions.csv") {
        Write-Host "    - $reportPrefix`_exceptions.csv"
    }
}


# 1. Ejecutar prueba de carga
Run-LocustTest -testType "CARGA" -users $loadTestUsers -spawnRate $loadTestSpawnRate -duration $loadTestDuration -outputDir $loadResultsDir -targetIP $TargetIP

Write-Host "Esperando 30 segundos antes de la prueba de estrés..."
Start-Sleep -Seconds 30

# 2. Ejecutar prueba de estrés  
Run-LocustTest -testType "ESTRES" -users $stressTestUsers -spawnRate $stressTestSpawnRate -duration $stressTestDuration -outputDir $stressResultsDir -targetIP $TargetIP


Write-Host "`nPruebas completadas. Los reportes están disponibles en:" -ForegroundColor Green
Write-Host "Carga: $loadResultsDir" -ForegroundColor Cyan
Write-Host "Estrés: $stressResultsDir" -ForegroundColor Cyan
Write-Host "`nEjemplo de uso con IP personalizada:" -ForegroundColor Yellow
Write-Host ".\run-locust.ps1 -TargetIP '192.168.1.100'" -ForegroundColor White
Write-Host ".\run-locust.ps1 -TargetIP '10.0.0.50' -LoadUsers 20 -StressUsers 200" -ForegroundColor White

# Regresar a la ubicación original
Set-Location $originalLocation