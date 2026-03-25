def call(Map config = [:]) {
    pipeline {
        agent any
        
        environment {
            SCANNER_HOME = tool 'SonarScanner'
            SONAR_PROJECT_KEY = "MissingPersonAI" 
            DOCKER_IMAGE = "${config.imageName}"
            HELM_VALUES_PATH = "charts/missing-person-portal/values.yaml" 
        }

        stages {
            stage('Static Code Analysis') {
                steps {
                    script {
                        withSonarQubeEnv('SonarQube-Server') {
                            sh """
                                ${SCANNER_HOME}/bin/sonar-scanner \
                                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                -Dsonar.projectName=${SONAR_PROJECT_KEY} \
                                -Dsonar.sources=. \
                                -Dsonar.python.version=3
                            """
                        }
                    }
                }
            }

            stage('Quality Gate Check') {
                steps {
                    timeout(time: 5, unit: 'MINUTES') { 
                        waitForQualityGate abortPipeline: true
                    }
                }
            }

            stage('Security Scan - Trivy') {
                steps {
                    sh "trivy fs . --severity HIGH,CRITICAL --exit-code 0" 
                }
            }

            stage('Build & Push Docker Image') {
                steps {
                    script {
                        sh "docker build -t ${DOCKER_IMAGE}:${env.BUILD_NUMBER} ."
                        sh "docker tag ${DOCKER_IMAGE}:${env.BUILD_NUMBER} ${DOCKER_IMAGE}:latest"
                        
                        withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                            sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
                            sh "docker push ${DOCKER_IMAGE}:${env.BUILD_NUMBER}"
                            sh "docker push ${DOCKER_IMAGE}:latest"
                        }
                    }
                }
            }

            stage('GitOps Sync - ArgoCD') {
                steps {
                    script {
                        // 1. Helm values update
                        sh "sed -i 's|repository:.*|repository: ${DOCKER_IMAGE}|' ${HELM_VALUES_PATH}"
                        sh "sed -i 's|tag:.*|tag: ${env.BUILD_NUMBER}|' ${HELM_VALUES_PATH}"
                        
                        echo "Updated ${HELM_VALUES_PATH} with Tag: ${env.BUILD_NUMBER}"
                        
                        // 2. Git Push logic with your exact URL
                        withCredentials([usernamePassword(credentialsId: 'github-creds', passwordVariable: 'GIT_PASS', usernameVariable: 'GIT_USER')]) {
                            sh "git config user.email 'hrishikeshpatil@example.com'"
                            sh "git config user.name 'Hrishikesh Patil'"
                            sh "git add ${HELM_VALUES_PATH}"
                            sh "git commit -m 'Update image tag to ${env.BUILD_NUMBER} [skip ci]'"
                            
                            // Exact URL updated here
                            sh "git push https://${GIT_USER}:${GIT_PASS}@github.com/Hrishikesh2901/missing-person-portal.git HEAD:main"
                        }
                    }
                }
            }
        }
        
        post {
            success {
                echo "Bhai, Build Green hai! ArgoCD sync check karo."
            }
            failure {
                echo "Kuch toh gadbad hai Daya! Logs check karo."
            }
        }
    }
}