Remove-Item done.flag -ErrorAction SilentlyContinue
Write-Host "🔁 Starting kubectl port-forward..."
$proc = Start-Process -NoNewWindow -PassThru kubectl -ArgumentList 'port-forward', 'service/proxy-client', '8080:8080'

while (-not (Test-Path "done.flag") -and -not $proc.HasExited) {
    Start-Sleep -Seconds 2
}

if (-not $proc.HasExited) {
    Write-Host "🛑 Deteniendo port-forward..."
    $proc.Kill()
}