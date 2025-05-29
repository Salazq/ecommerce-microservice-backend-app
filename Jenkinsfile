// Jenkins Pipeline para eCommerce Microservices
// Configurado para usar imágenes pre-construidas de Docker Hub (salazq/*)
// Forzar parámetros visibles para Jenkins
properties([
    parameters([
        choice(name: 'ENVIRONMENT', choices: ['dev', 'stage', 'prod'], description: 'Environment to deploy'),
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip test execution'),
        booleanParam(name: 'SKIP_DEPLOYMENT', defaultValue: false, description: 'Skip deployment to Minikube')
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
                }
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
        
        stage('Start Minikube if needed') {
            when {
                expression { return !params.SKIP_DEPLOYMENT }
            }
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
            when {
                expression { return !params.SKIP_DEPLOYMENT }
            }
            steps {
                bat """
                for /f "delims=" %%i in ('minikube -p %MINIKUBE_PROFILE% docker-env --shell cmd') do call %%i
                """
            }
        }        
        
        stage('Deploy to Minikube') {
            when {
                expression { return !params.SKIP_DEPLOYMENT }
            }
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
                expression { return (ENV == 'stage' || ENV == 'prod') && !params.SKIP_TESTS && !params.SKIP_DEPLOYMENT }
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
                expression { return ENV == 'stage' && !params.SKIP_TESTS && !params.SKIP_DEPLOYMENT }
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
                    }                }
            }
        }        stage('Ensure Git Branch') {
            when {
                expression { return ENV == 'prod' }
            }
            steps {
                script {
                    echo "🔄 Changing to master branch"
                    bat 'git checkout master'
                    echo "🔄 Pulling latest changes"
                    bat 'git pull origin master'
                }
            }
        }

        stage('Generate Release Notes') {
            when {
                expression { return ENV == 'prod' }
            }
            steps {
                script {                      
                    echo "📝 Generating release notes for production deployment..."
                    
                    // Intenta obtener el último tag, si no existe usa v1.0.0 como versión inicial
                    def lastTagResult = bat(script: "git describe --tags --abbrev=0 2>nul", returnStdout: true, returnStatus: true)
                    def gitLog
                    def servicesChanged
                    
                    if (lastTagResult.status == 0) {
                        env.VERSION = lastTagResult.stdout.trim()
                        // Obtiene los commits desde ese tag
                        gitLog = bat(script: "git log ${env.VERSION}..HEAD --pretty=format:\"- %h %an %s (%cd)\" --date=short 2>nul || echo \"No commits found\"", returnStdout: true).trim()
                        // Detecta carpetas/microservicios afectados desde el último tag
                        servicesChanged = bat(script: "git diff --name-only ${env.VERSION}..HEAD 2>nul | for /f \"delims=/\" %%i in ('more') do @echo %%i | sort | findstr /v \"^\$\" 2>nul || echo \"No changes detected\"", returnStdout: true).trim()
                    } else {
                        env.VERSION = "v1.0.0-build${env.BUILD_NUMBER}"
                        // Si no hay tags, obtiene los últimos 10 commits
                        gitLog = bat(script: "git log --oneline -10 --pretty=format:\"- %h %an %s (%cd)\" --date=short 2>nul || echo \"No commits found\"", returnStdout: true).trim()
                        // Detecta todas las carpetas modificadas en el repositorio
                        servicesChanged = bat(script: "dir /b /ad 2>nul | findstr -v \"^\\.\" | findstr -v \"target\" | findstr -v \"__pycache__\" || echo \"No services detected\"", returnStdout: true).trim()
                    }

                    // Fecha actual
                    def now = new Date().format("yyyy-MM-dd HH:mm:ss")                    // Release notes automático
                    def notes = """
                        === RELEASE NOTES ===
                        Version: ${env.VERSION}
                        Fecha: ${now}
                        Environment: ${ENV}
                        Namespace: ${NAMESPACE}

                        Servicios modificados:
                        ${servicesChanged}

                        Commits recientes:
                        ${gitLog}

                        Build ejecutado por: ${env.BUILD_USER ?: 'jenkins'}
                        Pipeline Job: ${env.JOB_NAME}
                        Build Number: ${env.BUILD_NUMBER}
                        =======================
                        """                    // Crear el directorio release-notes si no existe
                    bat 'if not exist "release-notes" mkdir "release-notes"'
                    
                    // Generar nombre único del archivo con timestamp
                    def timestamp = new Date().format("yyyyMMdd-HHmmss")
                    def fileName = "release-notes/release-notes-${env.VERSION}-${timestamp}.txt"
                    
                    writeFile file: fileName, text: notes
                    archiveArtifacts artifacts: fileName
                    
                    echo "📋 Release Notes generadas:"
                    echo notes
                      // Guardar en el repositorio Git
                    bat """
                    git add ${fileName}
                    git commit -m "📋 Release notes for ${env.VERSION} - Build #${env.BUILD_NUMBER}"
                    git push origin HEAD:master
                    """
                    
                    echo "✅ Release notes guardadas en el repositorio: ${fileName}"
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
