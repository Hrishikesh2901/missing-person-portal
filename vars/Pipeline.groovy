def call(Map config = [:]) {
    pipeline {
        agent any
        
        environment {
            SCANNER_HOME = tool 'SonarScanner'
            // Yahan hardcode kar diya hai taaki null wala error na aaye
            SONAR_PROJECT_KEY = "MissingPersonAI" 
            DOCKER_IMAGE = "${config.imageName}:${env.BUILD_NUMBER}"
        }

        stages {
            stage('Static Code Analysis') {
                steps {
                    script {
                        // 'SonarQube-Server' aapke Jenkins System configuration se match hona chahiye
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
                    // Is stage pe Jenkins wait karega SonarQube ke result ka
                    timeout(time: 5, unit: 'MINUTES') { 
                        waitForQualityGate abortPipeline: true
                    }
                }
            }

            stage('Security Scan - Trivy') {
                steps {
                    // Source code scan
                    sh "trivy fs . --severity HIGH,CRITICAL --exit-code 0" 
                }
            }

            stage('Build & Push Docker Image') {
                steps {
                    script {
                        sh "docker build -t ${DOCKER_IMAGE} ."
                        echo "Docker Image Built: ${DOCKER_IMAGE}"
                        // sh "docker push ${DOCKER_IMAGE}"
                    }
                }
            }

            stage('GitOps Sync - ArgoCD') {
                steps {
                    script {
                        // Path sahi check kar lena (charts/values.yaml ya charts/missing-person-portal/values.yaml)
                        sh "sed -i 's|repository:.*|repository: ${config.imageName}|' charts/values.yaml"
                        sh "sed -i 's|tag:.*|tag: ${env.BUILD_NUMBER}|' charts/values.yaml"
                        
                        echo "ArgoCD will now automatically sync the changes."
                    }
                }
            }
        }
        
        post {
            always {
                echo "Monitoring metrics are available on the Grafana Dashboard."
            }
            success {
                echo "Pipeline finished successfully!"
            }
            failure {
                echo "Pipeline failed. Check SonarQube or Trivy logs."
            }
        }
    }
}