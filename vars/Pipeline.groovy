def call(Map config = [:]) {
    pipeline {
        agent any
        
        environment {
            SCANNER_HOME = tool 'SonarScanner'
            DOCKER_IMAGE = "${config.imageName}:${env.BUILD_NUMBER}"
        }

        stages {
            stage('Static Code Analysis') {
                steps {
                    script {
                        withSonarQubeEnv('SonarQube-Server') {
                            sh "${SCANNER_HOME}/bin/sonar-scanner -Dsonar.projectKey=${config.projectKey}"
                        }
                    }
                }
            }

            stage('Quality Gate Check') {
                steps {
                    timeout(time: 1, unit: 'HOURS') {
                        // This will fail the build if SonarQube score is < 85%
                        waitForQualityGate abortPipeline: true
                    }
                }
            }

            stage('Security Scan - Trivy') {
                steps {
                    // Scanning the source code before building the image
                    sh "trivy fs . --severity HIGH,CRITICAL --exit-code 1"
                }
            }

            stage('Build & Push Docker Image') {
                steps {
                    script {
                        sh "docker build -t ${DOCKER_IMAGE} ."
                        // sh "docker push ${DOCKER_IMAGE}"
                    }
                }
            }

            stage('GitOps Sync - ArgoCD') {
                steps {
                    script {
                        // Updating the Helm Chart values.yaml with the new image tag
                        sh "sed -i 's|repository:.*|repository: ${config.imageName}|' charts/values.yaml"
                        sh "sed -i 's|tag:.*|tag: ${env.BUILD_NUMBER}|' charts/values.yaml"
                        
                        echo "ArgoCD will now automatically sync the changes to the Kubernetes cluster."
                    }
                }
            }
        }
        
        post {
            always {
                echo "Monitoring metrics are available on the Grafana Dashboard."
            }
        }
    }
}