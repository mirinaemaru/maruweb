pipeline {
    agent any

    environment {
        // Environment variables from Jenkins credentials
        GOOGLE_CLIENT_ID = credentials('google-client-id')
        GOOGLE_CLIENT_SECRET = credentials('google-client-secret')
        CALENDAR_ENCRYPTION_KEY = credentials('calendar-encryption-key')

        // Build settings
        MAVEN_OPTS = '-Xmx1024m'
        APP_NAME = 'maruweb'
        DEPLOY_PORT = '9080'
    }

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code from GitHub...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building application...'
                sh './mvnw clean package -DskipTests -Dspring.profiles.active=prod'
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                sh './mvnw test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Stop Previous Instance') {
            steps {
                echo 'Stopping previous instance...'
                sh '''
                    PID=$(lsof -ti:${DEPLOY_PORT}) || true
                    if [ -n "$PID" ]; then
                        echo "Killing process on port ${DEPLOY_PORT}: $PID"
                        kill -9 $PID
                        sleep 3
                    else
                        echo "No process running on port ${DEPLOY_PORT}"
                    fi
                '''
            }
        }

        stage('Deploy') {
            steps {
                echo 'Deploying application...'
                sh '''
                    # Create deploy directory if not exists
                    mkdir -p /opt/maruweb

                    # Copy JAR file
                    cp target/todo-0.0.1-SNAPSHOT.jar /opt/maruweb/${APP_NAME}.jar

                    # Start application in background
                    nohup java -jar \
                        -Dspring.profiles.active=prod \
                        -DGOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID} \
                        -DGOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET} \
                        -DCALENDAR_ENCRYPTION_KEY=${CALENDAR_ENCRYPTION_KEY} \
                        /opt/maruweb/${APP_NAME}.jar > /opt/maruweb/application.log 2>&1 &

                    echo $! > /opt/maruweb/app.pid
                    sleep 5
                '''
            }
        }

        stage('Health Check') {
            steps {
                echo 'Checking application health...'
                sh '''
                    for i in {1..30}; do
                        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${DEPLOY_PORT}/ || echo "000")
                        if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "302" ]; then
                            echo "Application is running (HTTP $HTTP_CODE)"
                            exit 0
                        fi
                        echo "Waiting for application to start... ($i/30)"
                        sleep 2
                    done
                    echo "Application failed to start"
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            echo 'Deployment successful!'
            emailext (
                subject: "SUCCESS: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                body: "Deployment completed successfully.\n\nURL: http://localhost:${DEPLOY_PORT}",
                recipientProviders: [developers()]
            )
        }
        failure {
            echo 'Deployment failed!'
            emailext (
                subject: "FAILED: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                body: "Deployment failed. Check console output for details.",
                recipientProviders: [developers()]
            )
        }
        always {
            cleanWs()
        }
    }
}
