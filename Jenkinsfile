pipeline {
    agent any
    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Integration Test') {
            steps {
                sh 'mvn test-compile failsafe:integration-test failsafe:verify'
            }
        }

        stage('Mutation Test') {
            steps {
                sh 'mvn test-compile org.pitest:pitest-maven:mutationCoverage'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
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
