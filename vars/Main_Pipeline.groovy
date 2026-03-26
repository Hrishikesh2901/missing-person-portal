def call(Map config = [:]) {
    pipeline {
        agent any
        environment {
            DOCKER_HUB_CREDS = 'docker-hub-creds'
            IMAGE_NAME = "${config.imageName}"
        }
        stages {
            stage('Fresh Start') {
                steps {
                    cleanWs()
                    checkout scm
                }
            }
            stage('Security Scan') {
                steps {
                    // Forcefully scanning files
                    sh "trivy fs . > trivy_report.txt"
                }
            }
            stage('Docker Build & Push') {
                steps {
                    script {
                        sh "docker build -t ${env.IMAGE_NAME}:${env.BUILD_NUMBER} ."
                        withDockerRegistry(credentialsId: "${env.DOCKER_HUB_CREDS}") {
                            sh "docker push ${env.IMAGE_NAME}:${env.BUILD_NUMBER}"
                            sh "docker tag ${env.IMAGE_NAME}:${env.BUILD_NUMBER} ${env.IMAGE_NAME}:latest"
                            sh "docker push ${env.IMAGE_NAME}:latest"
                        }
                    }
                }
            }
        }
    }
}