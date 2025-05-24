# PowerShell script to run E2E tests for the Ecommerce Microservices application
# This script provides various options for running the E2E test suite

param(
    [Parameter(HelpMessage="Specify which test to run: all, user, product, purchase, order, favorites")]
    [ValidateSet("all", "user", "product", "purchase", "order", "favorites")]
    [string]$TestType = "all",
    
    [Parameter(HelpMessage="Enable verbose logging")]
    [switch]$Verbose,
    
    [Parameter(HelpMessage="Skip pre-test health checks")]
    [switch]$SkipHealthCheck,
    
    [Parameter(HelpMessage="API Gateway URL")]
    [string]$ApiGatewayUrl = "http://localhost:30080"
)

# Color functions for better output
function Write-ColorOutput {
    param([string]$Message, [string]$Color = "White")
    
    switch($Color) {
        "Red" { Write-Host $Message -ForegroundColor Red }
        "Green" { Write-Host $Message -ForegroundColor Green }
        "Yellow" { Write-Host $Message -ForegroundColor Yellow }
        "Blue" { Write-Host $Message -ForegroundColor Blue }
        "Magenta" { Write-Host $Message -ForegroundColor Magenta }
        "Cyan" { Write-Host $Message -ForegroundColor Cyan }
        default { Write-Host $Message -ForegroundColor White }
    }
}

function Write-Header {
    param([string]$Title)
    Write-Host ""
    Write-ColorOutput "============================================" "Cyan"
    Write-ColorOutput "  $Title" "Cyan"
    Write-ColorOutput "============================================" "Cyan"
    Write-Host ""
}

function Write-Success {
    param([string]$Message)
    Write-ColorOutput "✓ $Message" "Green"
}

function Write-Error {
    param([string]$Message)
    Write-ColorOutput "✗ $Message" "Red"
}

function Write-Warning {
    param([string]$Message)
    Write-ColorOutput "⚠ $Message" "Yellow"
}

function Write-Info {
    param([string]$Message)
    Write-ColorOutput "ℹ $Message" "Blue"
}

# Health check function
function Test-ServiceHealth {
    param([string]$Url)
    
    try {
        $response = Invoke-RestMethod -Uri "$Url/actuator/health" -Method Get -TimeoutSec 10
        if ($response.status -eq "UP") {
            return $true
        }
    }
    catch {
        return $false
    }
    return $false
}

# Main script execution
Write-Header "Ecommerce E2E Test Runner"

# Change to project directory
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

Write-Info "Project directory: $projectRoot"
Write-Info "API Gateway URL: $ApiGatewayUrl"
Write-Info "Test type: $TestType"

# Health checks (unless skipped)
if (-not $SkipHealthCheck) {
    Write-Header "Health Checks"
    
    Write-Info "Checking API Gateway health..."
    if (Test-ServiceHealth -Url $ApiGatewayUrl) {
        Write-Success "API Gateway is healthy"
    } else {
        Write-Error "API Gateway health check failed"
        Write-Warning "You can skip health checks with -SkipHealthCheck if services are running"
        exit 1
    }
    
    # Check individual services through gateway
    $services = @("user-service", "product-service", "order-service", "payment-service", "shipping-service", "favourite-service")
    
    foreach ($service in $services) {
        Write-Info "Checking $service health..."
        if (Test-ServiceHealth -Url "$ApiGatewayUrl/$service") {
            Write-Success "$service is healthy"
        } else {
            Write-Warning "$service health check failed (may not have health endpoint)"
        }
    }
}

# Maven test execution
Write-Header "Running E2E Tests"

$mavenArgs = @("test")
$testClasses = @()

# Determine which tests to run
switch ($TestType) {
    "all" {
        $testClasses += "com.selimhorri.app.e2e.flows.*"
        Write-Info "Running all E2E test flows"
    }
    "user" {
        $testClasses += "UserRegistrationAndAuthE2ETest"
        Write-Info "Running User Registration and Authentication Flow"
    }
    "product" {
        $testClasses += "ProductAndCategoryManagementE2ETest"
        Write-Info "Running Product and Category Management Flow"
    }
    "purchase" {
        $testClasses += "CompletePurchaseFlowE2ETest"
        Write-Info "Running Complete Purchase Flow"
    }
    "order" {
        $testClasses += "OrderHistoryAndManagementE2ETest"
        Write-Info "Running Order History and Management Flow"
    }
    "favorites" {
        $testClasses += "FavoritesManagementE2ETest"
        Write-Info "Running Favorites Management Flow"
    }
}

# Build Maven command
if ($testClasses.Count -gt 0) {
    $testPattern = $testClasses -join ","
    $mavenArgs += "-Dtest=$testPattern"
}

$mavenArgs += "-Dspring.profiles.active=e2e"

# Add verbose logging if requested
if ($Verbose) {
    $mavenArgs += "-X"
    Write-Info "Verbose logging enabled"
}

# Set API Gateway URL as system property
$mavenArgs += "-De2e.api-gateway.base-url=$ApiGatewayUrl"

Write-Info "Maven command: ./mvnw $($mavenArgs -join ' ')"

# Execute tests
Write-Header "Test Execution"

$startTime = Get-Date

try {
    & .\mvnw.cmd @mavenArgs
    $exitCode = $LASTEXITCODE
    
    $endTime = Get-Date
    $duration = $endTime - $startTime
    
    Write-Header "Test Results"
    
    if ($exitCode -eq 0) {
        Write-Success "All tests passed successfully!"
        Write-Info "Total execution time: $($duration.ToString('mm\:ss'))"
    } else {
        Write-Error "Some tests failed (exit code: $exitCode)"
        Write-Info "Total execution time: $($duration.ToString('mm\:ss'))"
        Write-Info "Check the test output above for details"
    }
    
    # Test result summary
    Write-Header "Test Summary"
    
    $testResults = @{
        "User Registration and Auth" = if ($TestType -eq "all" -or $TestType -eq "user") { "Executed" } else { "Skipped" }
        "Product and Category Management" = if ($TestType -eq "all" -or $TestType -eq "product") { "Executed" } else { "Skipped" }
        "Complete Purchase Flow" = if ($TestType -eq "all" -or $TestType -eq "purchase") { "Executed" } else { "Skipped" }
        "Order History and Management" = if ($TestType -eq "all" -or $TestType -eq "order") { "Executed" } else { "Skipped" }
        "Favorites Management" = if ($TestType -eq "all" -or $TestType -eq "favorites") { "Executed" } else { "Skipped" }
    }
    
    foreach ($test in $testResults.GetEnumerator()) {
        if ($test.Value -eq "Executed") {
            Write-Info "$($test.Key): $($test.Value)"
        } else {
            Write-ColorOutput "$($test.Key): $($test.Value)" "Gray"
        }
    }
    
    exit $exitCode
    
} catch {
    Write-Error "Failed to execute tests: $($_.Exception.Message)"
    exit 1
}

Write-Header "Additional Information"
Write-Info "For more details about the E2E tests, see:"
Write-Info "  - src/test/java/com/selimhorri/app/e2e/README.md"
Write-Info "  - Test logs in the Maven output above"
Write-Info "  - Individual test class documentation"

Write-Host ""
Write-ColorOutput "Script completed at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" "Cyan"
