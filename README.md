<div align="center">

# NaHora! — Backend

**API REST e servidor WebSocket do marketplace que conecta clientes a profissionais autônomos em tempo real.**

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis)](https://redis.io/)
[![License](https://img.shields.io/badge/license-Acad%C3%AAmico-lightgrey?style=flat-square)](#-licença)

</div>

---

## 📖 Sobre o projeto

O **NaHora!** é um marketplace de serviços essenciais (eletricistas, encanadores, pintores, faxineiras, jardineiros e marceneiros) que resolve dois problemas do mercado informal brasileiro:

- 👤 **Para o cliente** — encontrar profissionais confiáveis em minutos, sem depender de indicação
- 🔨 **Para o profissional** — visibilidade no mercado digital e demanda constante na sua região

Este repositório contém o **backend** da aplicação. O app mobile fica em [`nahora-frontend`](https://github.com/nahora/nahora-frontend).

---

## ✨ Principais funcionalidades

- 🔐 **Autenticação segura** com JWT, refresh token e validação por OTP
- 📍 **Geolocalização** nativa via PostGIS para busca de profissionais por raio
- 💬 **Chat em tempo real** com WebSocket (STOMP) por pedido
- 💳 **Pagamento com escrow** via Pagar.me (Pix e cartão), liberado só após confirmação
- 🔔 **Notificações push** via Firebase Cloud Messaging
- 🛡️ **Sistema de moderação** para denúncias, suspensões e disputas
- ⭐ **Avaliação bilateral** entre cliente e profissional
- 🤖 **Filtro de IA no chat** que bloqueia troca de contatos antes da contratação

---

## 🛠️ Stack tecnológica

| Categoria | Tecnologia |
|---|---|
| **Linguagem** | Java 21 com Virtual Threads (Project Loom) |
| **Framework** | Spring Boot 3.3 |
| **Segurança** | Spring Security + JJWT |
| **Tempo real** | Spring WebSocket (STOMP + SockJS) |
| **Persistência** | Spring Data JPA + Hibernate Spatial |
| **Banco de dados** | PostgreSQL 16 + extensão PostGIS |
| **Migrações** | Flyway |
| **Cache & sessões** | Redis 7 |
| **Push** | Firebase Admin SDK |
| **Pagamentos** | Pagar.me (Pix e cartão de crédito) |
| **Storage** | AWS S3 ou Cloudflare R2 |
| **Documentação** | Springdoc OpenAPI (Swagger UI) |
| **Build** | Maven |

---

## 📋 Pré-requisitos

Antes de começar, garanta que você tem instalado:

- ☕ **Java 21** ou superior — `java -version`
- 📦 **Maven 3.9+** — `mvn -version`
- 🐳 **Docker** e **Docker Compose** — `docker -v`

---

## 🚀 Começando

### 1️⃣ Clone o repositório

```bash
git clone https://github.com/nahora/nahora-backend.git
cd nahora-backend
```

### 2️⃣ Suba a infraestrutura local

```bash
docker compose up -d
```

> 💡 Sobe **PostgreSQL com PostGIS** e **Redis** prontos para uso, na porta padrão.

### 3️⃣ Configure as variáveis de ambiente

```bash
cp .env.example .env
```

Edite o arquivo `.env` conforme a tabela [Variáveis de ambiente](#-variáveis-de-ambiente).

### 4️⃣ Execute a aplicação

```bash
./mvnw spring-boot:run
```

✅ A API ficará disponível em **http://localhost:8080**
📚 Swagger UI em **http://localhost:8080/swagger-ui.html**

---

## 🔧 Variáveis de ambiente

<details>
<summary><strong>Banco de dados e cache</strong></summary>

| Variável | Descrição | Exemplo |
|---|---|---|
| `DB_URL` | URL de conexão do PostgreSQL | `jdbc:postgresql://localhost:5432/nahora` |
| `DB_USERNAME` | Usuário do banco | `nahora` |
| `DB_PASSWORD` | Senha do banco | `senha_local` |
| `REDIS_HOST` | Host do Redis | `localhost` |
| `REDIS_PORT` | Porta do Redis | `6379` |

</details>

<details>
<summary><strong>Autenticação (JWT)</strong></summary>

| Variável | Descrição | Exemplo |
|---|---|---|
| `JWT_SECRET` | Chave secreta para assinar tokens (mín. 256 bits) | `chave_super_secreta_aqui` |
| `JWT_EXPIRATION_MS` | Validade do access token | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | Validade do refresh token | `604800000` (7 dias) |

</details>

<details>
<summary><strong>Integrações externas</strong></summary>

| Variável | Descrição | Exemplo |
|---|---|---|
| `FIREBASE_CREDENTIALS_PATH` | Caminho do JSON do Firebase | `./firebase-credentials.json` |
| `PAGARME_API_KEY` | Chave da API do Pagar.me | `sk_test_...` |
| `STORAGE_BUCKET` | Nome do bucket | `nahora-uploads` |
| `STORAGE_ACCESS_KEY` | Access key (S3 ou R2) | — |
| `STORAGE_SECRET_KEY` | Secret key | — |
| `STORAGE_ENDPOINT` | Endpoint (apenas R2) | `https://<id>.r2.cloudflarestorage.com` |

</details>

<details>
<summary><strong>CORS e segurança</strong></summary>

| Variável | Descrição | Exemplo |
|---|---|---|
| `CORS_ALLOWED_ORIGINS` | Origens permitidas (separadas por vírgula) | `http://localhost:3000,https://nahora.app` |

</details>

---

## 🏗️ Arquitetura

O backend segue o padrão **MVC clássico em camadas**. Cada camada tem uma responsabilidade clara e só se comunica com a camada imediatamente abaixo.

### Fluxo de uma requisição

```
📱 App mobile
       │
       ▼
🛡️ Security Filter Chain
   CorsFilter → JwtAuthFilter → ExceptionFilter
       │
       ▼
🎯 Controller — recebe e valida a entrada
       │
       ▼
⚙️ Service — executa a lógica de negócio
       │
       ▼
💾 Repository — persiste e consulta o banco
       │
       ▼
🗄️ PostgreSQL + Redis
```

### Responsabilidade de cada camada

| Camada | Responsabilidade | O que NÃO faz |
|---|---|---|
| **🛡️ Security** | Valida tokens JWT, libera CORS, padroniza erros 401/403 | Não conhece regras do domínio |
| **🎯 Controller** | Recebe HTTP, valida payload com `@Valid`, chama o service | Não contém regras de negócio |
| **⚙️ Service** | Regras do domínio, validações, máquinas de estado, transações | Não fala diretamente com HTTP |
| **💾 Repository** | Consultas e persistência via `JpaRepository` | Não contém lógica de negócio |
| **📦 Model** | Entidades JPA mapeadas para tabelas | Não contém comportamento externo |

---

## 📂 Estrutura do projeto

```
src/main/java/com/nahora/
│
├── 🎯 controllers/           # Camada de entrada (HTTP e WebSocket)
│   ├── AuthController.java
│   ├── UserController.java
│   ├── OrderController.java
│   ├── ProposalController.java
│   ├── ChatController.java
│   ├── PaymentController.java
│   └── ModerationController.java
│
├── ⚙️ services/              # Regras de negócio
│   ├── AuthService.java
│   ├── JwtService.java
│   ├── UserService.java
│   ├── OrderService.java
│   ├── ProposalService.java
│   ├── ChatService.java
│   ├── PaymentService.java
│   ├── NotificationService.java
│   └── ModerationService.java
│
├── 💾 repositories/          # Acesso ao banco (JpaRepository)
│   ├── UserRepository.java
│   ├── OrderRepository.java
│   ├── ProposalRepository.java
│   ├── ChatRepository.java
│   ├── MessageRepository.java
│   └── PaymentRepository.java
│
├── 📦 model/                 # Entidades JPA
│   ├── User.java
│   ├── Professional.java
│   ├── Order.java
│   ├── Proposal.java
│   ├── Chat.java
│   ├── Message.java
│   └── Payment.java
│
├── 📨 dto/                   # Objetos de transporte
│   ├── request/
│   └── response/
│
├── 🛡️ security/              # Spring Security
│   ├── SecurityConfig.java
│   ├── JwtAuthFilter.java
│   └── ExceptionFilter.java
│
├── ⚙️ config/                # Configurações da aplicação
│   ├── WebConfig.java        # CORS
│   ├── WebSocketConfig.java  # STOMP
│   ├── RedisConfig.java
│   └── OpenApiConfig.java
│
├── ❌ exceptions/            # Tratamento global de erros
│   └── GlobalExceptionHandler.java
│
└── NaHoraApplication.java
```

---

## 🧩 Módulos principais

<table>
<tr>
<td width="50%" valign="top">

### 🔐 Autenticação
**Classes:** `AuthController`, `AuthService`, `JwtService`

Cadastro de clientes e profissionais, validação de telefone via OTP por SMS, login com e-mail e senha, emissão de access token JWT e refresh token armazenado no Redis com TTL automático.

</td>
<td width="50%" valign="top">

### 📋 Pedidos
**Classes:** `OrderController`, `OrderService`

Ciclo de vida do pedido com máquina de estados: `OPEN → IN_PROGRESS → AWAITING_VALIDATION → COMPLETED`. Notifica profissionais da categoria e região via PostGIS quando um pedido é aberto.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📝 Propostas
**Classes:** `ProposalController`, `ProposalService`

Envio, edição, aceite e recusa de propostas. Ao aceitar, fecha os outros chats do pedido, dispara push para o profissional e cria o registro de pagamento pendente.

</td>
<td width="50%" valign="top">

### 💬 Chat
**Classes:** `ChatController`, `ChatService`

WebSocket com STOMP. Cada proposta ativa abre um canal em `/topic/chat/{chatId}`. Histórico persistido no PostgreSQL. Canal fica somente leitura quando o pedido é concluído.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 💳 Pagamentos
**Classes:** `PaymentController`, `PaymentService`

Pagar.me com escrow para Pix e cartão. Valor capturado no pagamento e liberado só após confirmação. Em caso de disputa, fica retido até a moderação resolver (prazo de 48h).

</td>
<td width="50%" valign="top">

### 🔔 Notificações
**Classes:** `NotificationService`

Push via FCM em eventos importantes: nova proposta, proposta aceita, mensagem no chat, pagamento liberado, disputa aberta.

</td>
</tr>
<tr>
<td colspan="2" valign="top">

### 🛡️ Moderação
**Classes:** `ModerationController`, `ModerationService`

Sistema de denúncias e disputas. Permite suspender ou banir usuários, mediar disputas de serviço e liberar ou estornar pagamentos retidos.

</td>
</tr>
</table>

---

## 📚 Documentação da API

Com a aplicação rodando, a documentação interativa está em:

🔗 **http://localhost:8080/swagger-ui.html**

Todos os endpoints estão documentados com exemplos de request, response e códigos de erro. Para testar endpoints autenticados:

1. Faça login via `POST /api/v1/auth/login`
2. Copie o `accessToken` da resposta
3. Clique em **Authorize** no Swagger e cole o token
4. Pronto — todos os endpoints autenticados ficam liberados

---

## 🧪 Testes

```bash
# Rodar todos os testes
./mvnw test

# Apenas testes unitários (rápidos)
./mvnw test -Dgroups="unit"

# Apenas testes de integração (requer Docker)
./mvnw test -Dgroups="integration"

# Cobertura de testes
./mvnw test jacoco:report
# Relatório em target/site/jacoco/index.html
```

---

## 🔄 Versionamento da API

Todos os endpoints seguem o prefixo `/api/v1/`.

> ⚠️ Mudanças que quebram compatibilidade com o frontend criam um novo prefixo `/api/v2/`, mantendo a versão anterior ativa durante o período de migração combinado com o time do mobile.

---

## 🤝 Como contribuir

A ordem natural de implementar uma feature nova no MVC é de baixo para cima:

```
1. 📦 model         → defina a entidade JPA
2. 💾 repository    → crie a interface JpaRepository
3. ⚙️ service       → implemente as regras de negócio
4. 🎯 controller    → exponha o endpoint REST
5. 🧪 testes        → cubra os casos principais
```

### Fluxo de Pull Request

1. Crie uma branch a partir de `develop`:
   ```bash
   git checkout -b feature/nome-da-feature
   ```
2. Implemente seguindo a ordem acima
3. Abra um PR para `develop` referenciando o caso de uso ou história de usuário (ex: `UC-04`, `UH-01`)
4. Após revisão e aprovação, o merge é feito em `develop`
5. O merge em `main` aciona o deploy automático em produção

---

## 👥 Time

| Função | Responsável |
|---|---|
| **** |  |
| **** |  |
| **** |  |
| **** |  |
| **** |  |

---

## 📄 Licença

Projeto acadêmico — **NaHora!** © 2025. Todos os direitos reservados.

---

<div align="center">

**Feito com ☕ e muita energia em Recife/PE**

</div>
