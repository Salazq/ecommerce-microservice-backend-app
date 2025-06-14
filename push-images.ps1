param(
    [string]$DockerHubUsername = "salazq",
    [string]$Tag = "latest",
    [switch]$SkipBuild,
    [switch]$PushOnly
)

$microservices = @(
    "service-discovery",
    "cloud-config", 
    "api-gateway",
    "proxy-client",
    "order-service",
    "product-service",
    "user-service",
    "shipping-service",
    "payment-service",
    "favourite-service",
    "nginx"
)

function Build-Service {
    param($ServiceName)
    if (Test-Path $ServiceName) {
        Push-Location $ServiceName
        docker build -t "${ServiceName}:${Tag}" . | Out-Null
        Pop-Location
    }
}

function Tag-And-Push-Service {
    param($ServiceName)
    $localImage = "${ServiceName}:${Tag}"
    $remoteImage = "${DockerHubUsername}/${ServiceName}:${Tag}"
    docker tag $localImage $remoteImage
    docker push $remoteImage
}

if (-not $PushOnly) {
    foreach ($service in $microservices) {
        if (-not $SkipBuild) {
            Build-Service $service
        }
        Tag-And-Push-Service $service
    }
} else {
    foreach ($service in $microservices) {
        Tag-And-Push-Service $service
    }
}
