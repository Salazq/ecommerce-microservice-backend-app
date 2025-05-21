# Este script reconstruye los servicios con las nuevas dependencias de actuator y ejecuta los contenedores

Write-Host "Reconstruyendo los servicios..."

# Compilar microservicios con Maven
Write-Host "Compilando con Maven..."
& "./mvnw" clean package -DskipTests

# Construir imágenes Docker
Write-Host "Construyendo imágenes Docker..."
docker-compose build

# Detener contenedores en ejecución
Write-Host "Deteniendo contenedores existentes..."
docker-compose down

# Iniciar los servicios
Write-Host "Iniciando los servicios..."
docker-compose up -d

Write-Host "Proceso completado. Los servicios se iniciarán en secuencia."
