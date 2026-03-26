def call(Map config = [:]) {
    pipeline {
        agent any
        environment {
            DOCKER_HUB_CREDS = 'docker-hub-creds'
            GIT_CREDS_ID     = 'github-creds' // Jenkins mein GitHub credentials ka ID
            IMAGE_NAME       = "${config.imageName}"
            MANIFEST_REPO    = "Hrishikesh2901/missing-person-portal" // Aapka repo path
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
            stage('Update Manifest & ArgoCD Sync') {
                steps {
                    script {
                        // Ye step aapke deployment.yaml mein naya image tag update karega
                        withCredentials([usernamePassword(credentialsId: "${env.GIT_CREDS_ID}", passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                            sh """
                                git config user.email "hrishikeshpatil@example.com"
                                git config user.name "Hrishikesh Patil"
                                # deployment.yaml mein image tag update karna (using sed)
                                sed -i 's|image: ${env.IMAGE_NAME}:.*|image: ${env.IMAGE_NAME}:${env.BUILD_NUMBER}|g' k8s/deployment.yaml
                                git add k8s/deployment.yaml
                                git commit -m "Update image tag to ${env.BUILD_NUMBER} [skip ci]"
                                git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${env.MANIFEST_REPO}.git main
                            """
                        }
                    }
                }
            }
        }
    }
}