def call(Map config = [:]) {
    pipeline {
        agent any
        environment {
            DOCKER_IMAGE = "${config.dockerImage}"
            GIT_REPO_URL = "${config.gitRepo}"
            IMAGE_TAG    = "${env.BUILD_NUMBER}"
        }
        stages {
            stage('Checkout') {
                steps {
                    git branch: 'main', url: "https://${env.GIT_REPO_URL}"
                }
            }
            stage('Build and Tag') {
                steps {
                    // Building the image for the Missing Person Portal
                    sh "docker build -t ${env.DOCKER_IMAGE}:${env.IMAGE_TAG} ."
                    sh "docker tag ${env.DOCKER_IMAGE}:${env.IMAGE_TAG} ${env.DOCKER_IMAGE}:latest"
                }
            }
            stage('Push to Registry') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                        sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
                        sh "docker push ${env.DOCKER_IMAGE}:${env.IMAGE_TAG}"
                        sh "docker push ${env.DOCKER_IMAGE}:latest"
                    }
                }
            }
            stage('Update Manifests') {
                steps {
                    // Update the image tag in Helm values for ArgoCD to detect
                    sh "sed -i 's/tag: .*/tag: \"${env.IMAGE_TAG}\"/' charts/missing-person-portal/values.yaml"
                }
            }
            stage('GitOps Push') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'github-creds', passwordVariable: 'GIT_PASS', usernameVariable: 'GIT_USER')]) {
                        sh "git config user.email 'jenkins-bot@example.com'"
                        sh "git config user.name 'Jenkins CI'"
                        sh "git add charts/missing-person-portal/values.yaml"
                        sh "git commit -m 'chore: update image tag to ${env.IMAGE_TAG} [skip ci]'"
                        sh "git push https://${GIT_USER}:${GIT_PASS}@${env.GIT_REPO_URL} main"
                    }
                }
            }
        }
    }
}