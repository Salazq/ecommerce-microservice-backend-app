Remove-Item done.flag -ErrorAction SilentlyContinue
Write-Host "🔁 Starting kubectl port-forward for proxy-client (8080)..."
$proc1 = Start-Process -NoNewWindow -PassThru kubectl -ArgumentList 'port-forward', 'service/proxy-client', '8080:8080'

Write-Host "🔁 Starting kubectl port-forward for service-discovery (8761)..."
$proc2 = Start-Process -NoNewWindow -PassThru kubectl -ArgumentList 'port-forward', 'service/service-discovery', '8761:8761'

while (-not (Test-Path "done.flag") -and -not $proc1.HasExited -and -not $proc2.HasExited) {
    Start-Sleep -Seconds 2
}

if (-not $proc1.HasExited) {
    Write-Host "🛑 Deteniendo port-forward proxy-client..."
    $proc1.Kill()
}

if (-not $proc2.HasExited) {
    Write-Host "🛑 Deteniendo port-forward service-discovery..."
    $proc2.Kill()
}
