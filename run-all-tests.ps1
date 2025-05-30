# Comando mejorado para ejecutar todos los tests de Postman con reportes
Write-Host "Ejecutando todos los tests de Postman con generación de reportes..." -ForegroundColor Yellow

# Crear carpeta para resultados si no existe
if (-not (Test-Path "newman-results")) {
    New-Item -ItemType Directory -Path "newman-results" | Out-Null
    Write-Host "Creada carpeta: newman-results" -ForegroundColor Green
}

# Ejecutar tests con reportes JSON y HTML
Write-Host "`n🧪 Ejecutando User Registration tests..." -ForegroundColor Cyan
newman run "postman-collections/01 - User Registration.postman_collection.json" `
    --reporters cli,json,html `
    --reporter-json-export "newman-results/01-user-registration-results.json" `
    --reporter-html-export "newman-results/01-user-registration-results.html"

Write-Host "`n📦 Ejecutando Product Catalog tests..." -ForegroundColor Cyan
newman run "postman-collections/02 - Product Catalog.postman_collection.json" `
    --reporters cli,json,html `
    --reporter-json-export "newman-results/02-product-catalog-results.json" `
    --reporter-html-export "newman-results/02-product-catalog-results.html"

Write-Host "`n🛒 Ejecutando Shopping Cart and Order tests..." -ForegroundColor Cyan
newman run "postman-collections/03 - Shopping Cart and Order.postman_collection.json" `
    --reporters cli,json,html `
    --reporter-json-export "newman-results/03-shopping-cart-order-results.json" `
    --reporter-html-export "newman-results/03-shopping-cart-order-results.html"

Write-Host "`n📋 Ejecutando Order Fulfillment tests..." -ForegroundColor Cyan
newman run "postman-collections/04 - Order Fulfillment.postman_collection.json" `
    --reporters cli,json,html `
    --reporter-json-export "newman-results/04-order-fulfillment-results.json" `
    --reporter-html-export "newman-results/04-order-fulfillment-results.html"

Write-Host "`n🗑️ Ejecutando Resource Deletion tests..." -ForegroundColor Cyan
newman run "postman-collections/05 - Resource Deletion Tests.postman_collection.json" `
    --reporters cli,json,html `
    --reporter-json-export "newman-results/05-resource-deletion-results.json" `
    --reporter-html-export "newman-results/05-resource-deletion-results.html"

Write-Host "`n✅ Todos los tests han sido ejecutados!" -ForegroundColor Green
Write-Host "📊 Reportes generados en la carpeta: newman-results/" -ForegroundColor Yellow
Write-Host "   - Archivos JSON: Para análisis programático" -ForegroundColor Gray
Write-Host "   - Archivos HTML: Para visualización en navegador" -ForegroundColor Gray
