def call(Map config = [:]) {
    pipeline {
        agent any

        environment {
            // Using credentials from Jenkins Global Store
            DOCKER_HUB_CREDS = 'docker-hub-creds'
            SONAR_SERVER     = 'sonar-server'
            IMAGE_NAME       = "${config.imageName}"
        }

        stages {
            stage('Clean Workspace') {
                steps {
                    // Cleaning old files to ensure a fresh start
                    cleanWs()
                }
            }

            stage('Checkout Source') {
                steps {
                    // Pulling code from GitHub
                    checkout scm
                }
            }

            stage('Trivy Security Scan') {
                steps {
                    // Scanning code files for vulnerabilities before building
                    sh "trivy fs . > trivy_report.txt"
                }
            }

            stage('SonarQube Analysis') {
                steps {
                    // Sending code to SonarQube for Quality Gate check
                    withSonarQubeEnv("${env.SONAR_SERVER}") {
                        sh "sonar-scanner -Dsonar.projectKey=missing-person-portal"
                    }
                }
            }

            stage('Docker Build') {
                steps {
                    // Building the Docker image with the build number as tag
                    sh "docker build -t ${env.IMAGE_NAME}:${env.BUILD_NUMBER} ."
                    sh "docker tag ${env.IMAGE_NAME}:${env.BUILD_NUMBER} ${env.IMAGE_NAME}:latest"
                }
            }

            stage('Docker Hub Push') {
                steps {
                    // Pushing the image to Docker Hub using credentials
                    script {
                        withDockerRegistry(credentialsId: "${env.DOCKER_HUB_CREDS}", url: 'https://index.docker.io/v1/') {
                            sh "docker push ${env.IMAGE_NAME}:${env.BUILD_NUMBER}"
                            sh "docker push ${env.IMAGE_NAME}:latest"
                        }
                    }
                }
            }
        }

        post {
            success {
                echo "Deployment Successful for ${env.IMAGE_NAME}"
            }
            failure {
                echo "Pipeline Failed! Please check logs for errors."
            }
        }
    }
}