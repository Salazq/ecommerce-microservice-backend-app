# run-locust-test.ps1

# Configuración para prueba de carga
$locustFile = "locustfile.py"
$loadTestUsers = 10
$loadTestSpawnRate = 2
$loadTestDuration = "60s"

# Configuración para prueba de estrés 
$stressTestUsers = 50
$stressTestSpawnRate = 10
$stressTestDuration = "120s"

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
        [string]$outputDir
    )
    
    # Usar nombre fijo para sobrescribir archivos anteriores
    $reportPrefix = "$outputDir\load_test_${testType}"
    
    Write-Host "`n==================== PRUEBA DE $testType ===================="
    Write-Host "Ejecutando Locust ($testType) con $users usuarios por $duration..."
    Write-Host "Spawn rate: $spawnRate usuarios/segundo"
    Write-Host "Carpeta de resultados: $outputDir"
    Write-Host "Los archivos anteriores serán sobrescritos"
    Write-Host "============================================================`n"
    
    python -m locust -f $locustFile --headless -u $users -r $spawnRate -t $duration --csv=$reportPrefix
    
    Write-Host "`nInforme de $testType guardado en $outputDir como:"
    Write-Host "  - $reportPrefix`_stats.csv"
    Write-Host "  - $reportPrefix`_failures.csv"
    Write-Host "  - $reportPrefix`_stats_history.csv"
    
    if (Test-Path "$reportPrefix`_exceptions.csv") {
        Write-Host "  - $reportPrefix`_exceptions.csv"
    }
}

# ==================== EJECUTAR PRUEBAS ====================

# 1. Ejecutar prueba de carga
Run-LocustTest -testType "CARGA" -users $loadTestUsers -spawnRate $loadTestSpawnRate -duration $loadTestDuration -outputDir $loadResultsDir

Write-Host "Esperando 30 segundos antes de la prueba de estrés..."
Start-Sleep -Seconds 30

# 2. Ejecutar prueba de estrés  
Run-LocustTest -testType "ESTRES" -users $stressTestUsers -spawnRate $stressTestSpawnRate -duration $stressTestDuration -outputDir $stressResultsDir

# Regresar a la ubicación original
Set-Location $originalLocation