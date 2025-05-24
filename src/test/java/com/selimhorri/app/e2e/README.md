# E2E Tests for Ecommerce Microservices

This directory contains end-to-end (E2E) tests for the complete ecommerce microservices application. The tests are designed to run against services deployed in Kubernetes.

## Overview

The E2E test suite includes 5 comprehensive test flows that verify complete workflows across multiple microservices:

1. **User Registration and Authentication Flow** - Tests user account creation, login, profile management, and address management
2. **Product and Category Management Flow** - Tests category/product creation, browsing, and favorites integration
3. **Complete Purchase Flow** - Tests the entire checkout process including cart, order, payment, and shipping
4. **Order History and Management Flow** - Tests order tracking, status updates, and order management
5. **Favorites Management Flow** - Tests adding/removing favorites and managing favorite lists

## Prerequisites

### 1. Kubernetes Cluster
- Ensure your Kubernetes cluster is running
- All microservices must be deployed and accessible
- API Gateway should be exposed on NodePort 30080

### 2. Dependencies
The following dependencies are automatically included in the Maven configuration:
- RestAssured for API testing
- JUnit 5 for test framework
- Spring Boot Test for integration testing
- Awaitility for async operations

### 3. Network Access
- Ensure the API Gateway is accessible at `http://localhost:30080`
- Verify all microservices are healthy and responding

## Configuration

### Application Configuration
Tests use the `application-e2e.yml` configuration file located in `src/test/resources/`. Key configurations:

```yaml
e2e:
  api-gateway:
    host: localhost
    port: 30080
    base-url: http://localhost:30080
  test:
    timeout:
      default: 30
      authentication: 10
      api-calls: 15
    retry:
      max-attempts: 3
      delay-seconds: 2
```

### Microservice Endpoints
Tests connect to microservices through the API Gateway using these base paths:
- User Service: `/user-service/api`
- Product Service: `/product-service/api`
- Order Service: `/order-service/api`
- Payment Service: `/payment-service/api`
- Shipping Service: `/shipping-service/api`
- Favourite Service: `/favourite-service/api`

## Running the Tests

### Option 1: Run All E2E Tests
```powershell
# From the root project directory
./mvnw test -Dtest="com.selimhorri.app.e2e.flows.*" -Dspring.profiles.active=e2e
```

### Option 2: Run Individual Test Classes
```powershell
# User Registration and Authentication Flow
./mvnw test -Dtest="UserRegistrationAndAuthE2ETest" -Dspring.profiles.active=e2e

# Product and Category Management Flow
./mvnw test -Dtest="ProductAndCategoryManagementE2ETest" -Dspring.profiles.active=e2e

# Complete Purchase Flow
./mvnw test -Dtest="CompletePurchaseFlowE2ETest" -Dspring.profiles.active=e2e

# Order History and Management Flow
./mvnw test -Dtest="OrderHistoryAndManagementE2ETest" -Dspring.profiles.active=e2e

# Favorites Management Flow
./mvnw test -Dtest="FavoritesManagementE2ETest" -Dspring.profiles.active=e2e
```

### Option 3: Use the PowerShell Script
```powershell
# Run the provided script
.\run-e2e-tests.ps1
```

## Test Structure

### Base Configuration
- `E2ETestConfig`: Base configuration for RestAssured and API Gateway connection
- `BaseE2ETest`: Abstract base class providing common functionality for all tests
- `E2ETestProperties`: Configuration properties for E2E testing

### Test Flows
Each test class follows a consistent pattern:
1. **Setup**: Create test users and authentication tokens
2. **Data Preparation**: Create necessary categories, products, etc.
3. **Test Execution**: Execute the specific workflow steps
4. **Verification**: Verify results across multiple microservices
5. **Cleanup**: Remove test data to avoid interference

### Test Ordering
Tests use `@Order` annotations to ensure proper execution sequence within each test class.

## Debugging and Troubleshooting

### Common Issues

1. **Connection Refused Errors**
   - Verify Kubernetes cluster is running
   - Check API Gateway service is exposed on port 30080
   - Ensure all microservices are healthy

2. **Authentication Failures**
   - Verify user-service is responding
   - Check JWT token generation and validation
   - Review authentication endpoints

3. **Test Data Conflicts**
   - Tests include cleanup procedures
   - Unique identifiers are used for test data
   - Manual cleanup may be needed if tests fail

### Logging
- E2E tests use SLF4J logging with DEBUG level
- Check console output for detailed test execution logs
- RestAssured requests/responses are logged at INFO level

### Health Checks
Before running tests, verify microservice health:
```powershell
# Check API Gateway health
curl http://localhost:30080/actuator/health

# Check individual service health through gateway
curl http://localhost:30080/user-service/actuator/health
curl http://localhost:30080/product-service/actuator/health
# ... etc for other services
```

## Test Data Management

### Automatic Cleanup
- Tests automatically clean up created data after execution
- Cleanup occurs even if tests fail (with some exceptions)
- Test data uses unique identifiers to avoid conflicts

### Manual Cleanup
If manual cleanup is needed:
1. Check for test users with email pattern `*@test.com`
2. Remove test categories with "Test" in the name
3. Clear test orders and favorites
4. Reset product inventory if needed

## Extending the Tests

### Adding New Test Cases
1. Create a new test class extending `BaseE2ETest`
2. Use `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`
3. Follow the established pattern: setup → execute → verify → cleanup
4. Add appropriate logging and error handling

### Modifying Existing Tests
1. Maintain test isolation
2. Update cleanup procedures when adding new test data
3. Preserve test ordering with `@Order` annotations
4. Update documentation as needed

## Continuous Integration

These tests are designed to run in CI/CD pipelines:
- Tests use configurable timeouts and retry mechanisms
- Parallel execution is disabled to avoid data conflicts
- Comprehensive logging for troubleshooting
- Clean exit codes for build systems

## Support

For issues or questions regarding the E2E tests:
1. Check the test logs for detailed error information
2. Verify Kubernetes cluster and service health
3. Review the configuration in `application-e2e.yml`
4. Check network connectivity to the API Gateway
