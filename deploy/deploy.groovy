// Entrega o artefacto (artifact/app.jar) a um ambiente da VM por SSH e reinicia o serviço.
// Requer no Jenkins: plugin SSH Agent, credencial 'jenkins-deploy-in-server-ssh'
// e as variáveis globais DEMO_SSH_TARGET e DEMO_SSH_PORT.
// Utilização num Jenkinsfile: load('deploy/deploy.groovy').run('dev')
def run(String environment) {
    if (!(environment in ['dev', 'uat', 'prod'])) {
        error('Ambiente desconhecido.')
    }
    sshagent(credentials: ['jenkins-deploy-in-server-ssh']) {
        withEnv(["DEPLOY_ENV=${environment}"]) {
            sh '''
                set -eu
                test -s artifact/app.jar
                scp -o BatchMode=yes -P "$DEMO_SSH_PORT" artifact/app.jar "$DEMO_SSH_TARGET:/opt/jenkins-cicd-env-demo/$DEPLOY_ENV/app.jar.new"
                ssh -T -o BatchMode=yes -p "$DEMO_SSH_PORT" "$DEMO_SSH_TARGET" "sh -s -- '$DEPLOY_ENV'" <<'REMOTE'
set -eu
name=$1
cd "/opt/jenkins-cicd-env-demo/$name"
test -s app.jar.new
sudo -n /usr/bin/systemctl stop "jenkins-cicd-env-demo@$name.service"
mv app.jar.new app.jar
chmod 644 app.jar
sudo -n /usr/bin/systemctl start "jenkins-cicd-env-demo@$name.service"
REMOTE
                # Verificação mínima: o serviço continua ativo passados alguns segundos
                sleep 10
                ssh -n -T -o BatchMode=yes -p "$DEMO_SSH_PORT" "$DEMO_SSH_TARGET" "/usr/bin/systemctl is-active --quiet jenkins-cicd-env-demo@$DEPLOY_ENV.service"
            '''
        }
    }
}

return this
