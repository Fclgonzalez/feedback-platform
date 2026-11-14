-- Usuário da aplicação com privilégios mínimos.
-- A senha NÃO fica neste arquivo: é passada pelo psql com  -v app_pwd="..."
-- Pode ser executado várias vezes (cria o usuário se não existir e sempre atualiza a senha).

SELECT format('CREATE ROLE feedback_app LOGIN PASSWORD %L', :'app_pwd')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'feedback_app')
\gexec

ALTER ROLE feedback_app WITH PASSWORD :'app_pwd';

GRANT CONNECT ON DATABASE feedback TO feedback_app;
GRANT USAGE ON SCHEMA public TO feedback_app;

-- A aplicação só grava e lê avaliações: não altera estrutura, não apaga, não atualiza.
GRANT SELECT, INSERT ON avaliacao TO feedback_app;
GRANT USAGE, SELECT ON SEQUENCE avaliacao_id_seq TO feedback_app;
