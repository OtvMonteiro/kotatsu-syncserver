# Instalação e migração do servidor de sincronização Futon

Este guia instala a variante do Kotatsu Sync Server ajustada para responder ao Futon 9.8.2. A instância atual usa MariaDB e publica o serviço na porta 8081; numa instalação nova, use uma porta livre e ajuste os exemplos abaixo.

## Compatibilidade com Futon

O Futon 9.8.2 rejeita alguns campos de manga presentes no JSON do Kotatsu Sync Server. Esta variante omite `manga.nsfw` e `manga.tags[].pinned` das respostas de favoritos e histórico, preserva `favourites[].pinned` e responde aos POSTs com apenas o delta que o cliente ainda não possui. Os GETs continuam devolvendo o conjunto completo.

Há também uma correção necessária no Futon: ao persistir dados recebidos, o app precisa gravar `nsfw=false` e `pinned=false` nas colunas locais obrigatórias. Portanto, **o servidor sozinho não corrige o Futon 9.8.2 sem essa alteração no app**. Instale a versão Futon que contém a correção nos dois dispositivos. O APK de debug tem identificador `io.github.landwarderer.futon.debug`, separado do app release, e não está incluído neste repositório.

## Requisitos

- Linux com Docker Engine e o plugin Docker Compose v2 (`docker compose`);
- usuário com acesso ao daemon Docker;
- acesso de rede dos dispositivos à máquina servidora;
- Git e OpenSSL.

## Instalação nova

Clone o fork e crie o arquivo local de configuração:

```sh
git clone --branch futon https://github.com/OtvMonteiro/kotatsu-syncserver.git
cd kotatsu-syncserver
cp .env.example .env
```

Edite `.env` antes de iniciar os containers. Substitua o `JWT_SECRET=secret` de exemplo e defina segredos longos e exclusivos para `JWT_SECRET`, `DATABASE_PASSWORD` e `DATABASE_ROOT_PASSWORD`. Gere valores aleatórios, por exemplo com `openssl rand -hex 32`. Nunca publique nem compartilhe o `.env`.

Defina também:

- `SERVER_PORT`: porta externa do serviço; o padrão é `8080`. Use outra porta, como `8081`, se necessário;
- `BASE_URL`: endereço que os clientes usarão, incluindo protocolo e porta, por exemplo `http://192.168.1.20:8081` numa rede local;
- `ALLOW_NEW_REGISTER=true` durante a criação da conta. Depois que os dois dispositivos estiverem configurados, use `false` para impedir novos cadastros.

Inicie os serviços e confira o estado:

```sh
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 server
docker compose logs --tail=100 db
```

Teste localmente usando a porta externa escolhida em `SERVER_PORT` — o shell não lê automaticamente os valores do arquivo `.env`:

```sh
curl -fsS http://127.0.0.1:8080/
```

Se configurou `SERVER_PORT=8081`, use `http://127.0.0.1:8081/`. O resultado esperado é `Alive`. No Futon, configure `http://IP-DA-MAQUINA:PORTA`, por exemplo `http://192.168.1.20:8081`. Não use `localhost` ou `127.0.0.1` no telefone: esses endereços apontam para o próprio aparelho.

Libere no firewall somente a porta do servidor para a rede necessária e bloqueie o acesso externo à porta do banco `3306`. Em hospedagem pública, prefira HTTPS por meio de um proxy reverso.

Os dados do MariaDB ficam no volume Docker `kotatsu-db` por padrão. Para acompanhar os logs do servidor continuamente, use `docker compose logs -f server`.

## Levar contas e dados para outra máquina

Uma instalação nova começa sem contas, favoritos ou histórico. Para migrar os dados, faça um dump no servidor de origem e transfira-o por um canal protegido. Guarde o arquivo fora do repositório:

```sh
docker compose exec -T db sh -c 'mariadb-dump --user="$MARIADB_USER" --password="$MARIADB_PASSWORD" "$MARIADB_DATABASE"' > /caminho/seguro/kotatsu-sync.sql
```

Na máquina de destino, clone a branch `futon` ou atualize o checkout existente. Configure o `.env` com o endereço e as credenciais do destino, como na seção anterior:

Se o banco antigo do destino pode ser descartado, execute o próximo comando **somente na máquina de destino**, dentro da pasta do Compose. Ele apaga os volumes e os dados antigos desse projeto; não o execute na origem:

```sh
docker compose down -v
```

Em seguida, importe o dump no banco novo:

```sh
docker compose up -d db
docker compose exec -T db sh -c 'mariadb --user="$MARIADB_USER" --password="$MARIADB_PASSWORD" "$MARIADB_DATABASE"' < /caminho/seguro/kotatsu-sync.sql
docker compose up -d --build server
docker compose ps
```

Mantenha o mesmo `JWT_SECRET` para que tokens já emitidos continuem válidos. Atualize `BASE_URL` para o novo endereço e `SERVER_PORT` para uma porta livre. As credenciais do banco configuradas no `.env` novo devem corresponder ao banco inicializado nessa máquina.

Confirme o serviço com `curl` na porta escolhida e teste login e sincronização nos dois dispositivos antes de desligar a instalação antiga. Não é necessário copiar diretamente o volume Docker.

## Atualizar o servidor

Dentro do clone, baixe as alterações do fork e reconstrua o container do servidor:

```sh
git pull --ff-only
docker compose up -d --build server
docker compose ps
```

Para parar os serviços sem apagar os dados, use `docker compose down`. **Não use `docker compose down -v`** durante a operação normal: `-v` remove o volume do banco.

## Usar MySQL em vez de MariaDB

O padrão usa MariaDB 11.8. Para a variante MySQL fornecida pelo projeto, use os dois arquivos Compose em todos os comandos do projeto:

```sh
docker compose -f docker-compose.yaml -f docker-compose.mysql.yaml up -d --build
```

## Diagnóstico rápido

- `docker compose ps`: confirma se banco e servidor estão ativos e saudáveis;
- `docker compose logs --tail=100 server`: mostra erros do servidor;
- `docker compose logs --tail=100 db`: mostra erros de inicialização do banco;
- `curl http://127.0.0.1:PORTA/`: verifica a resposta `Alive` na porta externa configurada;
- se funciona na máquina servidora, mas não no telefone, confira o IP usado no app, a rede Wi-Fi e o firewall.

Os testes da integração Futon verificam que os campos incompatíveis são removidos sem apagar `favourites[].pinned`, e que respostas POST incluem somente registros ausentes ou diferentes do pedido.
