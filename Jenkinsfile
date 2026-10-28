pipeline {
    agent any
    tools {
        jdk 'JDK17'
    }

    stages {
        stage('Build') {
            steps {
                echo 'Building the project...'
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
    }

    post {
        always {
            junit 'target/surefire-reports/*.xml' // Publish Unit Results
            junit allowEmptyResults: true, testResults: 'target/failsafe-reports/*.xml' // Publish Integration Results
            archiveArtifacts artifacts: 'target/pit-reports/**', allowEmptyArchive: true // Publish Mutation Results
        }
    }
}
