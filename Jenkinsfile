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
              stage('Run E2E with Forwarding') {
            when {
                expression { return ENV == 'stage' || ENV == 'prod' }
            }
            parallel {
                stage('Port Forward') {
                    steps {
                        sleep(time: 350, unit: 'SECONDS')
                        echo "📡 Starting kubectl port-forward in loop until tests are done..."
                        bat 'powershell -ExecutionPolicy Bypass -File forward.ps1'
                    }
                }

                stage('E2E Tests') {
                    steps {
                        echo "⏳ Waiting a bit for port-forward to initialize..."
                        sleep(time: 360, unit: 'SECONDS')

                        echo "🎯 Running E2E tests..."
                        bat '''
                        powershell -ExecutionPolicy Bypass -File run-all-tests.ps1
                        echo done > done.flag
                        '''
                    }
                }
            }
        }

        stage('Load Testing with Forwarding') {
            when {
                expression { return ENV == 'stage'}
            }
            parallel {
                stage('Port Forward for Load Testing') {
                    steps {
                        echo "📡 Starting kubectl port-forward for load testing..."
                        bat 'powershell -ExecutionPolicy Bypass -File forward.ps1'
                    }
                }

                stage('Locust Load Tests') {
                    steps {
                        echo "⏳ Waiting for port-forward to be ready..."
                        sleep(time: 10, unit: 'SECONDS')

                        echo "🚀 Running Locust load tests..."
                        bat '''
                        powershell -ExecutionPolicy Bypass -File run-locust.ps1
                        echo done > done.flag
                        '''

                        echo "📊 Load test completed - archiving results..."
                        archiveArtifacts artifacts: 'load-testing/load_test_report_*.csv', allowEmptyArchive: true
                    }
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
