// Pipeline 1 de 3: CI e Deploy Dev. Arranca automaticamente em cada commit.
// O pacote produzido fica arquivado e versionado neste job (build number + fingerprint).
// No fim, promove o mesmo build para UAT.
pipeline {
    agent any
    tools {
        jdk 'JDK17'
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Build') {
            steps {
                echo 'Building the project....'
                sh 'sh mvnw clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'sh mvnw test'
            }
        }

        stage('Integration Test') {
            steps {
                sh 'sh mvnw test-compile failsafe:integration-test failsafe:verify'
            }
        }

        stage('Mutation Test') {
            steps {
                sh 'sh mvnw test-compile org.pitest:pitest-maven:mutationCoverage'
            }
        }

        stage('Package') {
            steps {
                sh 'sh mvnw package -DskipTests'
            }
        }

        stage('Archive') {
            steps {
                sh '''
                    set -eu
                    mkdir -p artifact
                    jar=$(ls target/psoft-g1-*.jar)
                    cp "$jar" artifact/app.jar
                    printf 'GIT_COMMIT=%s\\nBUILD_NUMBER=%s\\nARTIFACT=%s\\n' "$(git rev-parse HEAD)" "$BUILD_NUMBER" "$(basename "$jar")" > artifact/release.properties
                '''
                // Cada build guarda a sua cópia; o fingerprint identifica o ficheiro entre jobs
                archiveArtifacts artifacts: 'artifact/*', fingerprint: true
            }
        }

        stage('Deploy Dev') {
            steps {
                script { load('deploy/deploy.groovy').run('dev') }
            }
        }

        stage('Trigger UAT') {
            steps {
                // Dispara o job de UAT com o número deste build, sem esperar pelo seu fim
                build job: 'GitHub-ODSOFT-UAT', wait: false,
                      parameters: [string(name: 'CI_BUILD', value: env.BUILD_NUMBER)]
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml' // Publish Unit Results
            junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml' // Publish Integration Results
            archiveArtifacts artifacts: 'target/pit-reports/**', allowEmptyArchive: true // Publish Mutation Results
        }
    }
}
