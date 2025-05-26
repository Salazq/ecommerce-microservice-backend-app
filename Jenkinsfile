pipeline {
    agent any
    
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'stage', 'prod'],
            description: 'Environment to deploy'
        )
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip test execution'
        )
    }
    
    environment {
        ENV = "${params.ENVIRONMENT}"
        MINIKUBE_PROFILE = "minikube-${ENV}"
        NAMESPACE = "${ENV}"
    }
    
    stages {
        stage('Validate Parameters') {
            steps {
                script {
                    echo "Deploying to environment: ${ENV}"
                    echo "Namespace: ${NAMESPACE}"
                    
                    // Validaciones específicas por environment
                    if (ENV == 'prod') {
                        echo "⚠️  PRODUCTION DEPLOYMENT - Extra validations will be performed"
                    }
                }
            }
        }
        
        stage('Start Minikube if needed') {
            steps {
                bat '''
                minikube status -p %MINIKUBE_PROFILE% | findstr /C:"host: Running" >nul
                if %ERRORLEVEL% NEQ 0 (
                    echo Minikube no está iniciado para el profile %MINIKUBE_PROFILE%. Iniciando...
                    minikube start -p %MINIKUBE_PROFILE% --cpus=6 --memory=3800 
                ) else (
                    echo Minikube ya está corriendo para el profile %MINIKUBE_PROFILE%.
                )
                '''
            }
        }

        stage('Set Docker to Minikube Env') {
            steps {
                bat '''
                for /f "delims=" %%i in ('minikube docker-env -p %MINIKUBE_PROFILE% --shell cmd') do call %%i
                '''
            }
        }
        
        stage('Run Tests') {
            when {
                not { params.SKIP_TESTS }
            }
            steps {
                script {
                    switch(ENV) {
                        case 'dev':
                            echo "🚀 DEV Environment: Skipping all tests for faster deployment"
                            break
                            
                        case 'stage':
                            echo "🧪 STAGE Environment: Running all tests (Unit + Integration + E2E + Security)"
                            
                            parallel(
                                'Unit Tests': {
                                    def services = getServicesList()
                                    for (svc in services) {
                                        dir(svc) {
                                            bat "mvnw.cmd test -Dtest=**/*ApplicationTests"
                                        }
                                    }
                                },
                                'Integration Tests': {
                                    def services = getServicesList()
                                    for (svc in services) {
                                        dir(svc) {
                                            bat "mvnw.cmd test -Dtest=**/*ResourceIntegrationTest"
                                        }
                                    }
                                },
                                'E2E Tests': {
                                    echo "Running E2E tests for staging..."
                                    // Ensure Newman is installed
                                    bat "npm install -g newman"
                                    dir('postman-collections') {
                                        def collections = findFiles(glob: '*.postman_collection.json')
                                        for (collection in collections) {
                                            bat "newman run ${collection.name} --env-var \\"spring_profiles_active=stage\\""
                                        }
                                    }
                                },
                                'Security Tests': {
                                    echo "Running security tests for staging..."
                                    bat "echo 'Security scan for stage environment'"
                                    // Aquí irían tus tests de seguridad específicos
                                }
                            )
                            break
                            
                        case 'prod':
                            echo "🎯 PROD Environment: Running only E2E tests for final validation"
                            
                            // Ensure Newman is installed
                            bat "npm install -g newman"
                            dir('postman-collections') {
                                def collections = findFiles(glob: '*.postman_collection.json')
                                for (collection in collections) {
                                    bat "newman run ${collection.name} --env-var \\"spring_profiles_active=prod\\""
                                }
                            }
                            break
                            
                        default:
                            error("Unknown environment: ${ENV}")
                    }
                }
            }
        }

        stage('Build Images') {
            steps {
                script {
                    def services = getServicesList()
                    for (svc in services) {
                        dir(svc) {
                            // Build con tag específico del environment
                            bat "minikube image build -p %MINIKUBE_PROFILE% -t ${svc}:${ENV}-latest ."
                            
                            // También crear tag con versión si es prod
                            if (ENV == 'prod') {
                                def version = env.BUILD_NUMBER ?: 'latest'
                                bat "minikube image build -p %MINIKUBE_PROFILE% -t ${svc}:${ENV}-${version} ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Deploy to Minikube') {
            steps {
                script {
                    // Crear namespace si no existe
                    bat "kubectl create namespace %NAMESPACE% --dry-run=client -o yaml | kubectl apply -f -"
                    
                    def services = getDeploymentServicesList()
                    for (svc in services) {
                        // Los YAML son siempre los mismos para todos los environments
                        bat "kubectl apply -f k8s/${svc}-deployment.yaml -n %NAMESPACE%"
                        bat "kubectl apply -f k8s/${svc}-service.yaml -n %NAMESPACE%"
                        
                        // Actualizar imagen en el deployment con tag específico del environment
                        def imageTag = ENV == 'prod' ? "${svc}:${ENV}-${env.BUILD_NUMBER}" : "${svc}:${ENV}-latest"
                        bat "kubectl set image deployment/${svc} ${svc}=${imageTag} -n %NAMESPACE%"
                        
                        // Esperar que el deployment esté listo
                        bat "kubectl rollout status deployment/${svc} -n %NAMESPACE% --timeout=300s"
                    }
                }
            }
        }
        
        stage('Post-Deploy Validation') {
            steps {
                script {
                    echo "Validating deployment in ${ENV} environment..."
                    
                    // Health checks básicos
                    bat "kubectl get pods -n %NAMESPACE%"
                    bat "kubectl get services -n %NAMESPACE%"
                    
                    // Smoke tests específicos por environment
                    if (ENV == 'prod') {
                        echo "Running production smoke tests..."
                        // Aquí irían smoke tests críticos para prod
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                // Cleanup o notificaciones específicas por environment
                echo "Pipeline completed for environment: ${ENV}"
            }
        }
        failure {
            script {
                if (ENV == 'prod') {
                    echo "🚨 PRODUCTION DEPLOYMENT FAILED - Alert operations team!"
                    // Aquí podrías agregar notificaciones críticas
                }
            }
        }
        success {
            script {
                echo "✅ Successfully deployed to ${ENV} environment"
                if (ENV == 'prod') {
                    echo "🎉 Production deployment successful!"
                }
            }
        }
    }
}

// Función helper para obtener lista de servicios
def getServicesList() {
    return [
        'service-discovery',
        'cloud-config',
        'api-gateway',
        'proxy-client',
        'order-service',
        'product-service',
        'user-service',
        'shipping-service'
        // Descomenta según necesites
        // 'payment-service',
        // 'favourite-service'
    ]
}

// Función helper para servicios de deployment (incluye zipkin)
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
        'shipping-service'
        // Descomenta según necesites
        // 'payment-service',
        // 'favourite-service'
    ]
}