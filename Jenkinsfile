// Jenkins Pipeline para eCommerce Microservices
// Configurado para usar imágenes pre-construidas de Docker Hub (salazq/*)
// Forzar parámetros visibles para Jenkins
properties([
    parameters([
        choice(name: 'ENVIRONMENT', choices: ['dev', 'stage', 'prod'], description: 'Environment to deploy'),
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip test execution')
    ])
])

pipeline {
    agent any    
    
    environment {
        ENV = "${params.ENVIRONMENT}"
        MINIKUBE_PROFILE = "minikube"
        NAMESPACE = "default"
    }

    stages {
        stage('Validate Parameters') {
            steps {
                script {
                    echo "Deploying to environment: ${ENV}"
                    echo "Namespace: ${NAMESPACE}"
                    if (ENV == 'prod') {
                        echo "⚠️  PRODUCTION DEPLOYMENT - Extra validations will be performed"
                    }                }
            }
        }

        stage('Start Minikube if needed') {
            steps {
                bat """
                minikube status -p %MINIKUBE_PROFILE% | findstr /C:"host: Running" >nul
                if %ERRORLEVEL% NEQ 0 (
                    echo Minikube no está iniciado para el profile %MINIKUBE_PROFILE%. Iniciando...
                    minikube start -p %MINIKUBE_PROFILE% --cpus=6 --memory=3800
                ) else (
                    echo Minikube ya está corriendo para el profile %MINIKUBE_PROFILE%.
                )
                """
            }
        }

        stage('Set Docker to Minikube Env') {
            steps {
                bat """
                for /f "delims=" %%i in ('minikube -p %MINIKUBE_PROFILE% docker-env --shell cmd') do call %%i
                """
            }
        }

        stage('Run Tests') {
            when {
                expression { return !params.SKIP_TESTS }
            }
            steps {
                script {
                    switch (ENV) {
                        case 'dev':
                            echo "🚀 DEV Environment: Skipping all tests for faster deployment"
                            break

                        case 'stage':
                            echo "🧪 STAGE Environment: Running unit and integration tests"

                            parallel(
                                'Unit Tests': {
                                    echo "Running unit tests only for user-service..."
                                    dir('user-service') {
                                        bat "mvnw.cmd test -Dtest=*ApplicationTests*"
                                    }
                                },
                                'Integration Tests': {
                                    echo "Running integration tests only for user-service..."
                                    dir('user-service') {
                                        bat "mvnw.cmd test -Dtest=*ResourceIntegrationTest"
                                    }
                                }                            )
                            break

                        case 'prod':
                            echo "🎯 PROD Environment: E2E tests will run after deployment"
                            break
                        default:
                            error("Unknown environment: ${ENV}")
                    }                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                script {
                    echo "Deploying to default namespace in Minikube..."

                    def services = getDeploymentServicesList()
                    for (svc in services) {
                        bat "kubectl delete pods -l app=${svc} -n %NAMESPACE% --ignore-not-found"

                        bat "kubectl apply -f k8s/${svc}-deployment.yaml -n %NAMESPACE%"
                        bat "kubectl apply -f k8s/${svc}-service.yaml -n %NAMESPACE%"
                   }
                }
            }
        }        
        
  
        stage('E2E Tests') {
            when {
                expression { return ENV == 'stage' || ENV == 'prod' }
            }
            steps {
                script {
                    echo "🎯 Running E2E tests after services are deployed..."

                    // Esperar un poco para que los servicios se estabilicen
                    echo "Waiting for services to stabilize..."
                    sleep(time: 420, unit: 'SECONDS')

                    echo "🌐 Starting kubectl port-forward for Eureka in background..."

                    // Iniciar port-forward como proceso en segundo plano
                    def portForwardCmd = """
                        powershell -Command "Start-Process -WindowStyle Hidden powershell -ArgumentList 'kubectl port-forward service/proxy-client 8080:8080' -PassThru | Select-Object -ExpandProperty Id > portforward.pid"
                    """
                    bat portForwardCmd

                    // Esperar a que el túnel esté disponible
                    sleep(time: 5, unit: 'SECONDS')

                    // Ejecutar los tests E2E
                    echo "🧪 Running E2E tests using run-all-tests.ps1..."
                    bat 'powershell -ExecutionPolicy Bypass -File run-all-tests.ps1'

                    // Finalizar el túnel (leer el PID y matarlo)
                    echo "🛑 Killing port-forward process..."
                    bat 'for /f %%i in (portforward.pid) do taskkill /PID %%i /F'

                    // Limpiar archivo temporal
                    bat 'del portforward.pid'
                }
            }
        }
    }

    post {
        always {
            script {
                echo "Pipeline completed for environment: ${ENV}"
            }
        }
        failure {
            script {
                if (ENV == 'prod') {
                    echo "🚨 PRODUCTION DEPLOYMENT FAILED - Alert operations team!"
                }
            }
        }
        success {
            script {
                echo "✅ Successfully deployed to ${ENV}"
                if (ENV == 'prod') {
                    echo "🎉 Production deployment successful!"
                }
            }
        }
    }
}

// Helpers fuera del bloque principal
def getServicesList() {
    return [
        'service-discovery',
        'cloud-config',
        'api-gateway',
        'proxy-client',
        'order-service',
        'product-service',
        'user-service',
        'shipping-service'/*,
        'payment-service',
        'favourite-service' */
    ]
}

def getDeploymentServicesList() {
    return [
        'zipkin',
        'service-discovery',
        'cloud-config',
        'api-gateway',
        'proxy-client',
        'order-service',
        'product-service',
        'user-service',
        'shipping-service'/*,
        'payment-service',
        'favourite-service'*/
    ]
}
