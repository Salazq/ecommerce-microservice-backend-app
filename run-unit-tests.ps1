# Script para ejecutar tests unitarios en todos los microservicios
# Ejecuta los tests de aplicación de cada servicio

Write-Host "🧪 Ejecutando tests unitarios en todos los microservicios..." -ForegroundColor Yellow

$services = @(
    "service-discovery",
    "cloud-config", 
    "api-gateway",
    "proxy-client",
    "order-service",
    "product-service",
    "user-service",
    "shipping-service"
)

$successful = @()
$failed = @()

foreach ($service in $services) {
    if (Test-Path $service) {
        Write-Host "`n🔄 Ejecutando tests en $service..." -ForegroundColor Blue
        Push-Location $service
        
        try {
            # Ejecutar tests unitarios
            & .\mvnw.cmd test -Dtest=*ApplicationTests*
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✅ Tests en $service - EXITOSO" -ForegroundColor Green
                $successful += $service
            } else {
                Write-Host "❌ Tests en $service - FALLIDO" -ForegroundColor Red
                $failed += $service
            }
        }
        catch {
            Write-Host "❌ Error ejecutando tests en $service : $($_.Exception.Message)" -ForegroundColor Red
            $failed += $service
        }
        finally {
            Pop-Location
        }
    } else {
        Write-Host "⚠️  Directorio $service no encontrado" -ForegroundColor Yellow
        $failed += $service
    }
}

Write-Host "`n📊 RESUMEN DE TESTS UNITARIOS" -ForegroundColor Cyan
Write-Host "=============================" -ForegroundColor Cyan
Write-Host "✅ Exitosos: $($successful.Count)" -ForegroundColor Green
Write-Host "❌ Fallidos: $($failed.Count)" -ForegroundColor Red

if ($successful.Count -gt 0) {
    Write-Host "`n✅ Servicios exitosos:" -ForegroundColor Green
    foreach ($svc in $successful) {
        Write-Host "   - $svc" -ForegroundColor Green
    }
}

if ($failed.Count -gt 0) {
    Write-Host "`n❌ Servicios con errores:" -ForegroundColor Red
    foreach ($svc in $failed) {
        Write-Host "   - $svc" -ForegroundColor Red
    }
}

if ($failed.Count -eq 0) {
    Write-Host "`n🎉 ¡Todos los tests unitarios pasaron exitosamente!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n⚠️  Algunos tests fallaron. Revisa los errores arriba." -ForegroundColor Yellow
    exit 1
}
