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
        }

        stage('Ensure Git Branch') {
            steps {
                script {
                    echo "🔄 Changing to master branch"
                    bat 'git checkout master'
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
                    
                    try {
                        // Intenta obtener el último tag, si no existe usa v1.0.0
                        def versionResult = bat(script: "git tag -l --sort=-version:refname | head -n 1", returnStdout: true).trim()
                        env.VERSION = versionResult ?: "v1.0.0"
                        
                        if (!env.VERSION || env.VERSION.isEmpty()) {
                            env.VERSION = "v1.0.0"
                            echo "⚠️ No tags found, using default version: ${env.VERSION}"
                        } else {
                            echo "📌 Found latest version: ${env.VERSION}"
                        }
                        
                    } catch (Exception e) {
                        env.VERSION = "v1.0.0"
                        echo "⚠️ Error getting version, using default: ${env.VERSION}"
                    }

                    try {
                        // Obtiene los commits recientes (últimos 10)
                        def gitLog = bat(script: "git log --oneline -10", returnStdout: true).trim()
                        if (!gitLog || gitLog.isEmpty()) {
                            gitLog = "No recent commits found"
                        }
                        
                        // Detecta archivos modificados en los últimos commits
                        def filesChanged = bat(script: "git diff --name-only HEAD~5..HEAD", returnStdout: true).trim()
                        if (!filesChanged || filesChanged.isEmpty()) {
                            filesChanged = "No files changed in recent commits"
                        }

                        // Fecha actual
                        def now = new Date().format("yyyy-MM-dd HH:mm:ss")

                        // Release notes automático
                        def notes = """
=== RELEASE NOTES ===
Version: ${env.VERSION}
Date: ${now}
Environment: ${ENV}
Namespace: ${NAMESPACE}
Jenkins Build: #${env.BUILD_NUMBER}

Recent Changes:
${filesChanged}

Recent Commits:
${gitLog}

Build Information:
- Job: ${env.JOB_NAME}
- Build Number: ${env.BUILD_NUMBER}
- Build URL: ${env.BUILD_URL ?: 'N/A'}
=======================
"""

                        // Crear el directorio release-notes si no existe
                        bat 'if not exist "release-notes" mkdir "release-notes"'
                        
                        // Generar nombre único del archivo con timestamp
                        def timestamp = new Date().format("yyyyMMdd-HHmmss")
                        def fileName = "release-notes/release-notes-${env.VERSION}-${timestamp}.txt"
                        
                        writeFile file: fileName, text: notes
                        archiveArtifacts artifacts: fileName
                        
                        echo "📋 Release Notes generated:"
                        echo notes
                        
                        // Opcional: Guardar en el repositorio Git solo si queremos commitear
                        try {
                            bat """
                            git add ${fileName}
                            git commit -m "📋 Release notes for ${env.VERSION} - Build #${env.BUILD_NUMBER}" || echo "Nothing to commit"
                            git push origin HEAD:master || echo "Failed to push, continuing..."
                            """
                            echo "✅ Release notes saved to repository: ${fileName}"
                        } catch (Exception gitError) {
                            echo "⚠️ Could not commit release notes to Git: ${gitError.message}"
                            echo "✅ Release notes still archived as build artifact"
                        }
                        
                    } catch (Exception e) {
                        echo "⚠️ Error generating detailed release notes: ${e.message}"
                        // Generar release notes básicas como fallback
                        def basicNotes = """
=== BASIC RELEASE NOTES ===
Version: ${env.VERSION}
Date: ${new Date().format("yyyy-MM-dd HH:mm:ss")}
Environment: ${ENV}
Build: #${env.BUILD_NUMBER}
Status: Deployment completed
============================
"""
                        def timestamp = new Date().format("yyyyMMdd-HHmmss")
                        def fileName = "release-notes/basic-release-notes-${timestamp}.txt"
                        writeFile file: fileName, text: basicNotes
                        archiveArtifacts artifacts: fileName
                        echo "📋 Basic release notes generated as fallback"
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
