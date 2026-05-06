## Descrição

<!-- O que foi implementado e por quê? Seja direto. -->

## Referências

- **Issue mãe:** nahora-app#___
- **Issue backend:** Closes #___
- **Caso de Uso:** UC-___
- **Onda:** ___

## Tipo de mudança

- [ ] Nova feature (adiciona funcionalidade sem quebrar existente)
- [ ] Bugfix (corrige um problema sem alterar funcionalidade)
- [ ] Refatoração (melhora código sem alterar comportamento)
- [ ] Migration (altera schema do banco de dados)
- [ ] Configuração / infraestrutura

## Camadas alteradas

- [ ] `model/` — entidades JPA
- [ ] `repositories/` — acesso a dados
- [ ] `services/` — regras de negócio
- [ ] `controllers/` — endpoints REST / WebSocket
- [ ] `security/` — autenticação / autorização
- [ ] `config/` — configurações da aplicação
- [ ] `dto/` — objetos de request / response
- [ ] `db/migration/` — scripts Flyway

## Checklist de qualidade

### Obrigatório
- [ ] Código compila sem erros (`./mvnw compile`)
- [ ] Todos os testes passam (`./mvnw test`)
- [ ] Não há imports não utilizados
- [ ] Segui a estrutura MVC (lógica no Service, não no Controller)

### Se criou endpoint novo
- [ ] Endpoint documentado no Swagger com `@Operation`
- [ ] Validação de entrada com `@Valid`
- [ ] Retorna status HTTP correto (201 para criação, 204 para delete, etc.)
- [ ] Testei no Postman / Swagger UI

### Se criou migration
- [ ] Migration é incremental (V{N}__descricao.sql)
- [ ] Testei com banco limpo (docker compose down -v && docker compose up -d)
- [ ] Migration é reversível ou tem plano de rollback

### Se alterou segurança
- [ ] Endpoints protegidos exigem JWT válido
- [ ] Regras de autorização estão no SecurityConfig
- [ ] Não expus dados sensíveis na resposta (senha, tokens internos)

## Screenshots / Evidências

<!-- Resposta do Swagger, curl, logs — o que ajudar o revisor -->

## Observações para o revisor

<!-- Algo que precisa de atenção especial? Decisão técnica controversa? -->
