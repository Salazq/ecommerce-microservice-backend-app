# E2E Tests Installation and Setup Guide

This document provides step-by-step instructions for setting up and running the E2E tests for the Ecommerce Microservices application.

## Prerequisites

### 1. System Requirements
- **Java 11 or higher**: Verify with `java -version`
- **Maven 3.6+**: Verify with `mvn -version`
- **Docker Desktop**: For Kubernetes cluster
- **Kubernetes**: Minikube, Docker Desktop Kubernetes, or any K8s cluster
- **PowerShell**: For running the provided scripts (Windows)

### 2. Kubernetes Cluster Setup

#### Option A: Docker Desktop Kubernetes (Recommended)
1. Install Docker Desktop
2. Enable Kubernetes in Docker Desktop settings
3. Wait for Kubernetes to start (green status)

#### Option B: Minikube
```powershell
# Install Minikube
choco install minikube

# Start Minikube
minikube start

# Enable ingress (optional)
minikube addons enable ingress
```

### 3. Deploy Microservices

#### Deploy all services to Kubernetes:
```powershell
# Navigate to project root
cd c:\Integrador\Taller_2_ingesoft\ecommerce-microservice-backend-app

# Deploy all services using kubectl
kubectl apply -f k8s/

# Wait for all pods to be ready
kubectl get pods --watch
```

#### Verify deployments:
```powershell
# Check all services are running
kubectl get services

# Verify API Gateway is exposed on NodePort 30080
kubectl get service api-gateway

# Test API Gateway connectivity
curl http://localhost:30080/actuator/health
```

## Installation Steps

### Step 1: Clone and Setup Project
```powershell
# If not already done, navigate to the project
cd c:\Integrador\Taller_2_ingesoft\ecommerce-microservice-backend-app

# Verify project structure
ls src/test/java/com/selimhorri/app/e2e/
```

### Step 2: Install Dependencies
The E2E testing dependencies are already configured in `pom.xml`:
- RestAssured for API testing
- JUnit 5 for test framework
- Spring Boot Test for integration testing
- Awaitility for async operations

```powershell
# Install dependencies (done automatically during test execution)
./mvnw dependency:resolve-sources
```

### Step 3: Configure Test Environment

#### Verify configuration file:
Check `src/test/resources/application-e2e.yml` contains correct settings:

```yaml
e2e:
  api-gateway:
    host: localhost
    port: 30080
    base-url: http://localhost:30080
```

#### Environment Variables (Optional):
You can override configuration using environment variables:
```powershell
$env:E2E_API_GATEWAY_HOST = "localhost"
$env:E2E_API_GATEWAY_PORT = "30080"
$env:SPRING_PROFILES_ACTIVE = "e2e"
```

### Step 4: Verify Service Health

#### Manual health check:
```powershell
# API Gateway
Invoke-RestMethod -Uri "http://localhost:30080/actuator/health"

# Individual services (through gateway)
Invoke-RestMethod -Uri "http://localhost:30080/user-service/actuator/health"
Invoke-RestMethod -Uri "http://localhost:30080/product-service/actuator/health"
Invoke-RestMethod -Uri "http://localhost:30080/order-service/actuator/health"
# ... etc for other services
```

#### Automated health check (using our script):
```powershell
# Run with health checks
.\run-e2e-tests.ps1 -TestType all
```

## Running the Tests

### Option 1: Using PowerShell Script (Recommended)
```powershell
# Run all tests
.\run-e2e-tests.ps1

# Run specific test types
.\run-e2e-tests.ps1 -TestType user
.\run-e2e-tests.ps1 -TestType product
.\run-e2e-tests.ps1 -TestType purchase
.\run-e2e-tests.ps1 -TestType order
.\run-e2e-tests.ps1 -TestType favorites

# Run with verbose logging
.\run-e2e-tests.ps1 -TestType all -Verbose

# Skip health checks (if services are known to be running)
.\run-e2e-tests.ps1 -TestType all -SkipHealthCheck
```

### Option 2: Direct Maven Commands
```powershell
# Run all E2E tests
./mvnw test -Dtest="com.selimhorri.app.e2e.flows.*" -Dspring.profiles.active=e2e

# Run test suite
./mvnw test -Dtest="E2ETestSuite" -Dspring.profiles.active=e2e

# Run individual test classes
./mvnw test -Dtest="UserRegistrationAndAuthE2ETest" -Dspring.profiles.active=e2e
./mvnw test -Dtest="ProductAndCategoryManagementE2ETest" -Dspring.profiles.active=e2e
./mvnw test -Dtest="CompletePurchaseFlowE2ETest" -Dspring.profiles.active=e2e
./mvnw test -Dtest="OrderHistoryAndManagementE2ETest" -Dspring.profiles.active=e2e
./mvnw test -Dtest="FavoritesManagementE2ETest" -Dspring.profiles.active=e2e
```

### Option 3: IDE Integration
1. Open the project in IntelliJ IDEA or Eclipse
2. Install JUnit 5 plugin if not already installed
3. Right-click on test classes or packages
4. Select "Run Tests" or "Debug Tests"
5. Ensure VM options include: `-Dspring.profiles.active=e2e`

## Troubleshooting

### Common Issues and Solutions

#### 1. Connection Refused Errors
```
java.net.ConnectException: Connection refused
```

**Solution:**
- Verify Kubernetes cluster is running: `kubectl cluster-info`
- Check API Gateway pod status: `kubectl get pods | grep api-gateway`
- Verify service exposure: `kubectl get service api-gateway`
- Test connectivity: `curl http://localhost:30080/actuator/health`

#### 2. Authentication Failures
```
401 Unauthorized or JWT token issues
```

**Solution:**
- Verify user-service is running: `kubectl get pods | grep user-service`
- Check user-service logs: `kubectl logs deployment/user-service`
- Ensure database connectivity for user-service
- Verify JWT configuration in user-service

#### 3. Test Data Conflicts
```
409 Conflict or duplicate key errors
```

**Solution:**
- Tests use unique identifiers, but if conflicts occur:
- Clear test data manually from databases
- Restart the problematic service pods
- Ensure test cleanup is working properly

#### 4. Timeout Issues
```
java.net.SocketTimeoutException: Read timed out
```

**Solution:**
- Increase timeout values in `application-e2e.yml`
- Check service performance and resource allocation
- Verify network connectivity and latency

#### 5. Port Already in Use
```
Port 30080 is already in use
```

**Solution:**
- Check what's using the port: `netstat -ano | findstr 30080`
- Kill the process or use a different port
- Update configuration if using a different port

### Log Analysis

#### Enable debug logging:
```powershell
./mvnw test -Dtest="UserRegistrationAndAuthE2ETest" -Dspring.profiles.active=e2e -Dlogging.level.com.selimhorri.app.e2e=DEBUG
```

#### Check specific logs:
- **Test execution logs**: Console output during test runs
- **RestAssured logs**: HTTP request/response details
- **Service logs**: `kubectl logs deployment/<service-name>`

## Advanced Configuration

### Custom API Gateway URL
```powershell
# Using environment variable
$env:E2E_API_GATEWAY_BASE_URL = "http://my-cluster:30080"
.\run-e2e-tests.ps1

# Using Maven property
./mvnw test -Dtest="E2ETestSuite" -Dspring.profiles.active=e2e -De2e.api-gateway.base-url=http://my-cluster:30080
```

### Running in CI/CD
```yaml
# Example GitHub Actions step
- name: Run E2E Tests
  run: |
    ./mvnw test -Dtest="com.selimhorri.app.e2e.flows.*" -Dspring.profiles.active=e2e
  env:
    E2E_API_GATEWAY_BASE_URL: http://localhost:30080
```

### Database Cleanup
If manual database cleanup is needed:
```powershell
# Connect to your database and run:
# DELETE FROM users WHERE email LIKE '%@test.com';
# DELETE FROM products WHERE sku LIKE 'TEST-%';
# DELETE FROM categories WHERE category_title LIKE '%Test%';
# ... etc for other test data
```

## Performance Considerations

### Resource Requirements
- **Memory**: At least 8GB RAM for full cluster
- **CPU**: 4+ cores recommended
- **Disk**: 20GB+ free space
- **Network**: Low latency connection to cluster

### Optimization Tips
1. Run tests sequentially (already configured)
2. Use local Kubernetes cluster for faster network
3. Allocate sufficient resources to pods
4. Enable test data cleanup after each test
5. Use health checks to verify readiness

## Support and Documentation

- **Main README**: `src/test/java/com/selimhorri/app/e2e/README.md`
- **Test class documentation**: Javadoc in individual test files
- **Configuration reference**: `application-e2e.yml`
- **Troubleshooting**: This document and test logs

For additional support, check:
1. Service logs: `kubectl logs deployment/<service-name>`
2. Test execution logs in console output
3. RestAssured request/response details in debug mode
4. Kubernetes cluster status: `kubectl get all`
