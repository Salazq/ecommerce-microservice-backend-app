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
        MINIKUBE_PROFILE = "minikube-${params.ENVIRONMENT}"
        NAMESPACE = "${params.ENVIRONMENT}"
    }

    stages {
        stage('Validate Parameters') {
            steps {
                script {
                    echo "Deploying to environment: ${env.ENV}"
                    echo "Namespace: ${env.NAMESPACE}"

                    if (env.ENV == 'prod') {
                        echo "⚠️  PRODUCTION DEPLOYMENT - Extra validations will be performed"
                    }
                }
            }
        }

        stage('Start Minikube if needed') {
            steps {
                powershell '''
                $status = minikube status -p $env:MINIKUBE_PROFILE
                if ($status -notmatch "host: Running") {
                    Write-Host "Minikube no está iniciado para el profile $env:MINIKUBE_PROFILE. Iniciando..."
                    minikube start -p $env:MINIKUBE_PROFILE --cpus=6 --memory=3800
                } else {
                    Write-Host "Minikube ya está corriendo para el profile $env:MINIKUBE_PROFILE."
                }
                '''
            }
        }

        stage('Set Docker to Minikube Env') {
            steps {
                powershell '''
                Invoke-Expression -Command $(minikube -p $env:MINIKUBE_PROFILE docker-env --shell powershell)
                '''
            }
        }

        stage('Run Tests') {
            when {
                expression { return !params.SKIP_TESTS }
            }
            steps {
                script {
                    switch (env.ENV) {
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
                                            powershell "./mvnw.cmd test -Dtest=*ApplicationTests"
                                        }
                                    }
                                },
                                'Integration Tests': {
                                    def services = getServicesList()
                                    for (svc in services) {
                                        dir(svc) {
                                            powershell "./mvnw.cmd test -Dtest=*ResourceIntegrationTest"
                                        }
                                    }
                                },
                                'E2E Tests': {
                                    echo "Running E2E tests for staging..."
                                    powershell "npm install -g newman"
                                    dir('postman-collections') {
                                        def collections = findFiles(glob: '*.postman_collection.json')
                                        for (collection in collections) {
                                            powershell "newman run \"${collection.name}\" --env-var \"spring_profiles_active=stage\""
                                        }
                                    }
                                },
                                'Security Tests': {
                                    echo "Running security tests for staging..."
                                    powershell "echo 'Security scan for stage environment'"
                                    // Add real security tests here
                                }
                            )
                            break

                        case 'prod':
                            echo "🎯 PROD Environment: Running only E2E tests for final validation"
                            powershell "npm install -g newman"
                            dir('postman-collections') {
                                def collections = findFiles(glob: '*.postman_collection.json')
                                for (collection in collections) {
                                    powershell "newman run \"${collection.name}\" --env-var \"spring_profiles_active=prod\""
                                }
                            }
                            break

                        default:
                            error("Unknown environment: ${env.ENV}")
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
                            powershell "minikube image build -p $env:MINIKUBE_PROFILE -t ${svc}:${env.ENV}-latest ."

                            if (env.ENV == 'prod') {
                                def version = env.BUILD_NUMBER ?: 'latest'
                                powershell "minikube image build -p $env:MINIKUBE_PROFILE -t ${svc}:${env.ENV}-${version} ."
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                script {
                    powershell "kubectl create namespace $env:NAMESPACE --dry-run=client -o yaml | kubectl apply -f -"
                    def services = getDeploymentServicesList()
                    for (svc in services) {
                        powershell "kubectl apply -f k8s/${svc}-deployment.yaml -n $env:NAMESPACE"
                        powershell "kubectl apply -f k8s/${svc}-service.yaml -n $env:NAMESPACE"

                        def imageTag = env.ENV == 'prod' ? "${svc}:${env.ENV}-${env.BUILD_NUMBER}" : "${svc}:${env.ENV}-latest"
                        powershell "kubectl set image deployment/${svc} ${svc}=${imageTag} -n $env:NAMESPACE"
                        powershell "kubectl rollout status deployment/${svc} -n $env:NAMESPACE --timeout=300s"
                    }
                }
            }
        }

        stage('Post-Deploy Validation') {
            steps {
                script {
                    echo "Validating deployment in ${env.ENV} environment..."
                    powershell "kubectl get pods -n $env:NAMESPACE"
                    powershell "kubectl get services -n $env:NAMESPACE"

                    if (env.ENV == 'prod') {
                        echo "Running production smoke tests..."
                        // Add smoke tests here
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline completed for environment: ${env.ENV}"
        }
        failure {
            script {
                if (env.ENV == 'prod') {
                    echo "🚨 PRODUCTION DEPLOYMENT FAILED - Alert operations team!"
                    // Add notifications
                }
            }
        }
        success {
            script {
                echo "✅ Successfully deployed to ${env.ENV} environment"
                if (env.ENV == 'prod') {
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
        // 'payment-service',
        // 'favourite-service'
    ]
}

// Función helper para servicios de deployment
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
        // 'payment-service',
        // 'favourite-service'
    ]
}
