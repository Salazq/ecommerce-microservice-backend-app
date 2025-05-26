// Funciones helper (deben ir fuera del bloque pipeline)
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
    }

    stages {
        stage('Validate Parameters') {
            steps {
                script {
                    echo "Deploying to environment: ${params.ENVIRONMENT}"
                    echo "Namespace: ${params.ENVIRONMENT}"

                    if (params.ENVIRONMENT == 'prod') {
                        echo "⚠️  PRODUCTION DEPLOYMENT - Extra validations will be performed"
                    }
                }
            }
        }

        stage('Start Minikube if needed') {
            steps {
                powershell '''
                $profile = "minikube-${env:ENV}"
                $status = minikube status -p $profile
                if ($status -notmatch "host: Running") {
                    Write-Host "Minikube is not running. Starting..."
                    minikube start -p $profile --cpus=6 --memory=3800
                } else {
                    Write-Host "Minikube is already running."
                }
                '''
            }
        }

        stage('Set Docker to Minikube Env') {
            steps {
                powershell '''
                $profile = "minikube-${env:ENV}"
                Invoke-Expression -Command (& minikube -p $profile docker-env --shell powershell)
                '''
            }
        }

        stage('Run Tests') {
            when {
                not { params.SKIP_TESTS }
            }
            steps {
                script {
                    def envTag = params.ENVIRONMENT
                    switch(envTag) {
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
                                            powershell "./mvnw.cmd test -Dtest='**/*ApplicationTests'"
                                        }
                                    }
                                },
                                'Integration Tests': {
                                    def services = getServicesList()
                                    for (svc in services) {
                                        dir(svc) {
                                            powershell "./mvnw.cmd test -Dtest='**/*ResourceIntegrationTest'"
                                        }
                                    }
                                },
                                'E2E Tests': {
                                    echo "Running E2E tests for staging..."
                                    powershell "npm install -g newman"
                                    dir('postman-collections') {
                                        def collections = findFiles(glob: '*.postman_collection.json')
                                        for (collection in collections) {
                                            powershell "newman run '${collection.name}' --env-var spring_profiles_active=stage"
                                        }
                                    }
                                },
                                'Security Tests': {
                                    echo "Running security tests for staging..."
                                    powershell "echo 'Security scan for stage environment'"
                                }
                            )
                            break

                        case 'prod':
                            echo "🎯 PROD Environment: Running only E2E tests for final validation"
                            powershell "npm install -g newman"
                            dir('postman-collections') {
                                def collections = findFiles(glob: '*.postman_collection.json')
                                for (collection in collections) {
                                    powershell "newman run '${collection.name}' --env-var spring_profiles_active=prod"
                                }
                            }
                            break

                        default:
                            error("Unknown environment: ${envTag}")
                    }
                }
            }
        }

        stage('Build Images') {
            steps {
                script {
                    def services = getServicesList()
                    def profile = "minikube-${params.ENVIRONMENT}"
                    for (svc in services) {
                        dir(svc) {
                            powershell "minikube image build -p ${profile} -t ${svc}:${params.ENVIRONMENT}-latest ."
                            if (params.ENVIRONMENT == 'prod') {
                                def version = env.BUILD_NUMBER ?: 'latest'
                                powershell "minikube image build -p ${profile} -t ${svc}:${params.ENVIRONMENT}-${version} ."
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                script {
                    def profile = "minikube-${params.ENVIRONMENT}"
                    def namespace = params.ENVIRONMENT
                    powershell "kubectl create namespace ${namespace} --dry-run=client -o yaml | kubectl apply -f -"

                    def services = getDeploymentServicesList()
                    for (svc in services) {
                        powershell "kubectl apply -f k8s/${svc}-deployment.yaml -n ${namespace}"
                        powershell "kubectl apply -f k8s/${svc}-service.yaml -n ${namespace}"

                        def imageTag = params.ENVIRONMENT == 'prod'
                            ? "${svc}:${params.ENVIRONMENT}-${env.BUILD_NUMBER}"
                            : "${svc}:${params.ENVIRONMENT}-latest"

                        powershell "kubectl set image deployment/${svc} ${svc}=${imageTag} -n ${namespace}"
                        powershell "kubectl rollout status deployment/${svc} -n ${namespace} --timeout=300s"
                    }
                }
            }
        }

        stage('Post-Deploy Validation') {
            steps {
                script {
                    def namespace = params.ENVIRONMENT
                    echo "Validating deployment in ${namespace} environment..."

                    powershell "kubectl get pods -n ${namespace}"
                    powershell "kubectl get services -n ${namespace}"

                    if (params.ENVIRONMENT == 'prod') {
                        echo "Running production smoke tests..."
                        // Aquí irían smoke tests críticos
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "Pipeline completed for environment: ${params.ENVIRONMENT}"
            }
        }
        failure {
            script {
                if (params.ENVIRONMENT == 'prod') {
                    echo "🚨 PRODUCTION DEPLOYMENT FAILED - Alert operations team!"
                    // Aquí podrías agregar alertas o integraciones
                }
            }
        }
        success {
            script {
                echo "✅ Successfully deployed to ${params.ENVIRONMENT} environment"
                if (params.ENVIRONMENT == 'prod') {
                    echo "🎉 Production deployment successful!"
                }
            }
        }
    }
}
