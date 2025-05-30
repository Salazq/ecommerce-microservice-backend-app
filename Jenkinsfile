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
        }        stage('Run Tests') {
            when {
                expression { return ENV == 'stage' && !params.SKIP_TESTS }
            }
            steps {
                script {

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
                        }
                    )
                    
                    echo " Archiving unit and integration test results..."
                    archiveArtifacts artifacts: 'user-service/target/surefire-reports/*.xml, user-service/target/surefire-reports/*.txt', allowEmptyArchive: true
                    
                }                
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
                        echo " Starting kubectl port-forward in loop until tests are done..."
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
                        echo "📊 Archiving E2E test results..."
                        archiveArtifacts artifacts: 'newman-results/*.json, newman-results/*.html, postman-collections/*.json', allowEmptyArchive: true
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
                        echo " Starting kubectl port-forward for load testing..."
                        bat 'powershell -ExecutionPolicy Bypass -File forward.ps1'
                    }
                }

                stage('Locust Load Tests') {
                    steps {
                        echo " Waiting for port-forward to be ready..."
                        sleep(time: 10, unit: 'SECONDS')                        
                        echo "Running Locust load tests..."
                        bat '''
                        powershell -ExecutionPolicy Bypass -File run-locust.ps1
                        echo done > done.flag
                        '''

                        echo " Load test completed - archiving results..."
                        archiveArtifacts artifacts: 'load-testing/resultados-carga/*.csv, load-testing/resultados-estres/*.csv', allowEmptyArchive: true
                    }                }
            }
        }

        stage('Ensure Git Branch') {
            when {
                expression { return ENV == 'prod' }
            }
            steps {
                script {
                    echo " Updating and changing to master branch"
                    bat '''
                    git fetch origin
                    git checkout master
                    git pull origin master
                    '''
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
                        
                        bat '''
                        @echo off
                        git describe --tags --always --abbrev=0 2>nul > temp_version.txt || echo v1.0.0 > temp_version.txt
                        git log --oneline -3 > temp_commits.txt
                        git diff --name-only HEAD~2..HEAD 2>nul > temp_files.txt || echo "No changes" > temp_files.txt
                        '''
                        
                        
                        env.VERSION = readFile('temp_version.txt').trim()
                        def gitLog = readFile('temp_commits.txt').trim()
                        def filesChanged = readFile('temp_files.txt').trim()
                        
                       
                        bat 'del temp_version.txt temp_commits.txt temp_files.txt 2>nul'
                        
                        if (!env.VERSION || env.VERSION.isEmpty()) {
                            env.VERSION = "v1.0.0"
                        }
                        
                        echo "📌 Found version: ${env.VERSION}"

                        // Fecha actual
                        def now = new Date().format("yyyy-MM-dd HH:mm:ss")

                        // Release notes simplificado
                        def notes = """=== RELEASE NOTES ===
                            Version: ${env.VERSION}
                            Date: ${now}
                            Environment: ${ENV}
                            Build: #${env.BUILD_NUMBER}

                            Recent Changes:
                            ${filesChanged}

                            Recent Commits:
                            ${gitLog}

                            Job: ${env.JOB_NAME}
                            Build URL: ${env.BUILD_URL ?: 'N/A'}
======================="""

                        // Crear directorio y archivo
                        bat 'if not exist "release-notes" mkdir "release-notes"'
                        
                        def timestamp = new Date().format("yyyyMMdd-HHmmss")
                        def safeVersion = env.VERSION.replaceAll("[^a-zA-Z0-9.-]", "")
                        def fileName = "release-notes/release-${safeVersion}-${timestamp}.txt"
                        
                        writeFile file: fileName, text: notes
                        archiveArtifacts artifacts: fileName
                        
                        echo " Release Notes:"
                        echo notes
                        
                       
                        try {
                            bat """
                            git add ${fileName}
                            git commit -m "Release notes ${env.VERSION} - Build #${env.BUILD_NUMBER}"
                            git pull origin master --rebase --quiet
                            git push origin HEAD:master --quiet
                            """
                            echo "Release notes committed to repository"
                        } catch (Exception gitError) {
                            echo "⚠️ Could not commit to Git (archived as artifact)"
                        }
                        
                    } catch (Exception e) {
                        echo "⚠️ Error generating release notes: ${e.message}"
                        
                        
                        def basicNotes = """=== RELEASE NOTES ===
Version: Build-${env.BUILD_NUMBER}
Date: ${new Date().format("yyyy-MM-dd HH:mm:ss")}
Environment: ${ENV}
Status: Deployment completed
============================"""
                        
                        def timestamp = new Date().format("yyyyMMdd-HHmmss")
                        def fileName = "release-notes/basic-${timestamp}.txt"
                        writeFile file: fileName, text: basicNotes
                        archiveArtifacts artifacts: fileName
                        echo " Basic release notes generated"
                    }
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
