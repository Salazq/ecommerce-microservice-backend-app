# --- Función de espera activa para asegurar que el Pod esté en estado Running ---
function Wait-ForPod($podName, $namespace = "default") {
    $timeout = 60
    $elapsed = 0
    while ($elapsed -lt $timeout) {
        $status = kubectl get pods -l app=$podName -n $namespace -o jsonpath="{.items[0].status.phase}" 2>$null
        if ($status -eq "Running") {
            Write-Host " Pod '$podName' is Running."
            return
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
        Write-Host "⏳ Waiting for pod '$podName'... ($elapsed/$timeout seconds)"
    }
    throw " Timeout: Pod '$podName' is not running after $timeout seconds."
}

# --- Eliminar bandera si existe ---
Remove-Item done.flag -ErrorAction SilentlyContinue

# --- Esperar a que el pod esté listo ---
Wait-ForPod -podName "proxy-client"

# --- Iniciar el port-forward ---
Write-Host "Starting kubectl port-forward for proxy-client (8080)..."
$proc1 = Start-Process -NoNewWindow -PassThru kubectl -ArgumentList 'port-forward', 'service/proxy-client', '8080:8080'

# --- Verificar que el proceso inició correctamente ---
Start-Sleep -Seconds 3

if ($proc1.HasExited) {
    Write-Host "ERROR: proxy-client port-forward failed to start"
    $proc1.ExitCode
}

# --- Mantener proceso en ejecución hasta que se cree el archivo done.flag ---
Write-Host "Port-forward started. Waiting for done.flag file..."
while (-not (Test-Path "done.flag") -and -not $proc1.HasExited) {
    Start-Sleep -Seconds 2
}

# --- Detener el proceso si sigue activo ---
if ($proc1 -and -not $proc1.HasExited) {
    Write-Host "Stopping port-forward proxy-client..."
    $proc1.Kill()
}

Write-Host "Port-forwards stopped."