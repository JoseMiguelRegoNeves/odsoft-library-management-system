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
    validateSwagger(environment)
}

// Valida que o Swagger do ambiente responde com HTTP 200 (pelo URL público do gateway).
// Requer as variáveis globais DEMO_DEV_URL, DEMO_UAT_URL e DEMO_PROD_URL, sem barra final.
def validateSwagger(String environment) {
    // O acesso explícito evita o getAt dinâmico bloqueado pelo sandbox Jenkins.
    def baseUrl
    switch (environment) {
        case 'dev':
            baseUrl = env.DEMO_DEV_URL
            break
        case 'uat':
            baseUrl = env.DEMO_UAT_URL
            break
        case 'prod':
            baseUrl = env.DEMO_PROD_URL
            break
    }
    if (!baseUrl || !(baseUrl ==~ /https?:\/\/[a-zA-Z0-9.:\/-]+/)) {
        error("Configurar a variável global DEMO_${environment.toUpperCase()}_URL no Jenkins.")
    }
    withEnv(["SWAGGER_URL=${baseUrl.replaceAll('/$', '')}/swagger-ui/index.html"]) {
        sh '''
            set -eu
            attempt=1
            while [ "$attempt" -le 30 ]; do
                status=$(curl --silent --output /dev/null --write-out '%{http_code}' --connect-timeout 3 --max-time 5 "$SWAGGER_URL" || true)
                if [ "$status" = 200 ]; then
                    echo "Swagger OK (HTTP 200): $SWAGGER_URL"
                    exit 0
                fi
                echo "A aguardar HTTP 200 em $SWAGGER_URL (tentativa $attempt de 30, estado: $status)"
                sleep 3
                attempt=$((attempt + 1))
            done
            echo "Falhou: $SWAGGER_URL não devolveu HTTP 200." >&2
            exit 1
        '''
    }
}

return this
