# --- Función de espera activa para asegurar que el Pod esté en estado Running ---
function Wait-ForPod($podName, $namespace = "default") {
    $timeout = 60
    $elapsed = 0
    while ($elapsed -lt $timeout) {
        $status = kubectl get pods -l app=$podName -n $namespace -o jsonpath="{.items[0].status.phase}" 2>$null
        if ($status -eq "Running") {
            Write-Host "✅ Pod '$podName' is Running."
            return
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
        Write-Host "⏳ Waiting for pod '$podName'... ($elapsed/$timeout seconds)"
    }
    throw "❌ Timeout: Pod '$podName' is not running after $timeout seconds."
}

# --- Eliminar bandera si existe ---
Remove-Item done.flag -ErrorAction SilentlyContinue

# --- Esperar a que ambos pods estén listos ---
Wait-ForPod -podName "proxy-client"
Wait-ForPod -podName "service-discovery"

# --- Iniciar los port-forwards ---
Write-Host "🔁 Starting kubectl port-forward for proxy-client (8080)..."
$proc1 = Start-Process -NoNewWindow -PassThru kubectl -ArgumentList 'port-forward', 'service/proxy-client', '8080:8080'

Write-Host "🔁 Starting kubectl port-forward for service-discovery (8761)..."
$proc2 = Start-Process -NoNewWindow -PassThru kubectl -ArgumentList 'port-forward', 'service/service-discovery', '8761:8761'

# --- Mantener proceso en ejecución hasta que se cree el archivo done.flag ---
while (-not (Test-Path "done.flag") -and -not $proc1.HasExited -and -not $proc2.HasExited) {
    Start-Sleep -Seconds 2
}

# --- Detener los procesos si siguen activos ---
if ($proc1 -and -not $proc1.HasExited) {
    Write-Host "🛑 Deteniendo port-forward proxy-client..."
    $proc1.Kill()
}

if ($proc2 -and -not $proc2.HasExited) {
    Write-Host "🛑 Deteniendo port-forward service-discovery..."
    $proc2.Kill()
}
