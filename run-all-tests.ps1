Write-Host "Ejecutando todos los tests de Postman..." -ForegroundColor Yellow
newman run "postman-collections/01 - User Registration.postman_collection.json"
newman run "postman-collections/02 - Product Catalog.postman_collection.json" 
newman run "postman-collections/03 - Shopping Cart and Order.postman_collection.json"
newman run "postman-collections/04 - Order Fulfillment.postman_collection.json"
newman run "postman-collections/05 - Resource Deletion Tests.postman_collection.json"
Write-Host "Todos los tests han sido ejecutados!" -ForegroundColor Green