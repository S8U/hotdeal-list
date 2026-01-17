pipeline {
    agent any

    environment {
        PROJECT_NAME = 'hotdeal-list'
    }

    parameters {
        string(name: 'DOCKER_REGISTRY', defaultValue: '', description: 'Docker Registry URL (예: registry.example.com:8082)'))
        string(name: 'DEPLOY_HOST', defaultValue: '', description: '배포 서버 SSH 호스트')
        string(name: 'DEPLOY_USER', defaultValue: 'deploy', description: '배포 서버 SSH 사용자')
        string(name: 'DEPLOY_PATH', defaultValue: '/opt/hotdeal-list', description: '배포 경로')
    }

    stages {
        stage('Prepare') {
            steps {
                script {
                    env.IMAGE_TAG = "${BUILD_NUMBER}"
                    env.BACKEND_IMAGE = "${params.DOCKER_REGISTRY}/${PROJECT_NAME}-backend"
                    env.FRONTEND_IMAGE = "${params.DOCKER_REGISTRY}/${PROJECT_NAME}-frontend"
                    env.NGINX_IMAGE = "${params.DOCKER_REGISTRY}/${PROJECT_NAME}-nginx"
                }
            }
        }

        stage('Build Images') {
            parallel {
                stage('Backend') {
                    steps {
                        dir('backend') {
                            sh "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} -t ${BACKEND_IMAGE}:latest ."
                        }
                    }
                }
                stage('Frontend') {
                    steps {
                        dir('frontend') {
                            sh "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} -t ${FRONTEND_IMAGE}:latest ."
                        }
                    }
                }
                stage('Nginx') {
                    steps {
                        dir('nginx') {
                            sh "docker build -t ${NGINX_IMAGE}:${IMAGE_TAG} -t ${NGINX_IMAGE}:latest ."
                        }
                    }
                }
            }
        }

        stage('Push to Registry') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${PROJECT_NAME}-docker-registry", usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASS')]) {
                    sh "echo \$REGISTRY_PASS | docker login ${params.DOCKER_REGISTRY} -u \$REGISTRY_USER --password-stdin"
                    sh "docker push ${BACKEND_IMAGE}:${IMAGE_TAG}"
                    sh "docker push ${BACKEND_IMAGE}:latest"
                    sh "docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}"
                    sh "docker push ${FRONTEND_IMAGE}:latest"
                    sh "docker push ${NGINX_IMAGE}:${IMAGE_TAG}"
                    sh "docker push ${NGINX_IMAGE}:latest"
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: "${PROJECT_NAME}-docker-registry", usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASS'),
                    sshUserPrivateKey(credentialsId: "${PROJECT_NAME}-deploy-ssh-key", keyFileVariable: 'SSH_KEY'),
                    file(credentialsId: "${PROJECT_NAME}-env-backend", variable: 'ENV_BACKEND'),
                    file(credentialsId: "${PROJECT_NAME}-env-frontend", variable: 'ENV_FRONTEND')
                ]) {
                    sh """
                        scp -i \$SSH_KEY -o StrictHostKeyChecking=no \$ENV_BACKEND ${params.DEPLOY_USER}@${params.DEPLOY_HOST}:${params.DEPLOY_PATH}/backend/.env
                        scp -i \$SSH_KEY -o StrictHostKeyChecking=no \$ENV_FRONTEND ${params.DEPLOY_USER}@${params.DEPLOY_HOST}:${params.DEPLOY_PATH}/frontend/.env
                        scp -i \$SSH_KEY -o StrictHostKeyChecking=no docker-compose.yml ${params.DEPLOY_USER}@${params.DEPLOY_HOST}:${params.DEPLOY_PATH}/
                    """
                    sh """
                        ssh -i \$SSH_KEY -o StrictHostKeyChecking=no ${params.DEPLOY_USER}@${params.DEPLOY_HOST} \\
                        "cd ${params.DEPLOY_PATH} && \\
                         echo \$REGISTRY_PASS | docker login ${params.DOCKER_REGISTRY} -u \$REGISTRY_USER --password-stdin && \\
                         export DOCKER_REGISTRY=${params.DOCKER_REGISTRY} && \\
                         docker compose pull && \\
                         docker compose up -d && \\
                         docker logout ${params.DOCKER_REGISTRY}"
                    """
                }
            }
        }
    }

    post {
        always {
            sh "docker logout ${params.DOCKER_REGISTRY} || true"
        }
    }
}
