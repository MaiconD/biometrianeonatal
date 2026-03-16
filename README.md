# Biometria Neonatal

Aplicativo Android para cadastro de recém-nascidos, gestão de responsáveis e coleta biométrica neonatal com operação **offline-first**, armazenamento local seguro e sincronização posterior com backend.

## Objetivo do sistema

O projeto foi estruturado para apoiar fluxos hospitalares de:

- autenticação de profissionais de saúde;
- cadastro e edição de recém-nascidos;
- cadastro de responsáveis;
- captura biométrica neonatal;
- revisão local das coletas;
- consulta de histórico;
- sincronização de dados locais com um serviço remoto.

O app também possui suporte a **modo demo offline**, permitindo execução local com massa de dados e sensores simulados.

---

## Stack tecnológica

### Plataforma

- **Android nativo**
- **Kotlin**
- **Jetpack Compose** para UI
- **Navigation Compose** para navegação
- **Hilt** para injeção de dependência

### Persistência e offline

- **Room** como banco local
- **SQLCipher** para banco criptografado
- **Android Keystore** para serviços de criptografia
- fila local de sincronização via tabela `sync_queue`

### Rede e integração

- **Retrofit**
- **OkHttp**
- DTOs para autenticação e sincronização

### Biometria e captura

- abstração de sensores em `core/sensors`
- suporte a:
  - demo local
  - câmera do tablet
  - sensores USB
- **CameraX** para preview/captura quando aplicável

### Concorrência

- **Kotlin Coroutines**
- **Flow / StateFlow**

---

## Requisitos técnicos atuais

- **compileSdk / targetSdk / minSdk: 36**
- **Java 11**
- **KSP** para geração de código do Room e Hilt

---

## Arquitetura

O projeto segue uma combinação prática de:

- **MVVM**
- **Clean Architecture**
- organização **feature-first**
- separação em camadas de `core`, `data`, `domain` e `features`

### Camadas

#### `features/`
Contém a camada de apresentação por funcionalidade:
- telas Compose
- ViewModels
- validações específicas de formulário

#### `domain/`
Contém:
- modelos de domínio
- contratos de repositório
- casos de uso

#### `data/`
Contém:
- implementação dos repositórios
- integração remota
- montagem de payload de sync
- seeding local

#### `core/`
Contém infraestrutura transversal:
- navegação (`AppGraph`)
- banco (`AppDatabase`)
- segurança
- sensores
- sync
- componentes visuais reutilizáveis

---

## Fluxos principais do sistema

### 1. Login

Fluxo:
1. usuário informa email, senha e hospital;
2. `LoginViewModel` chama o caso de uso de login;
3. sessão é persistida localmente;
4. navegação segue para dashboard.

Arquivos principais:
- `app/src/main/java/com/example/biometrianeonatal/features/login/LoginScreen.kt`
- `app/src/main/java/com/example/biometrianeonatal/features/login/LoginViewModel.kt`

### 2. Dashboard

Exibe indicadores operacionais do dispositivo, atalhos para módulos e status resumido.

Arquivos principais:
- `features/dashboard/DashboardScreen.kt`
- `features/dashboard/DashboardViewModel.kt`

### 3. Cadastro de recém-nascido

Permite criar e editar o cadastro clínico inicial.

Campos principais:
- unidade hospitalar
- nome
- data e hora de nascimento
- sexo
- peso
- altura
- prontuário
- observações

Validações:
- obrigatoriedade de nome, data e hora
- erros exibidos diretamente nos campos
- mensagem geral no corpo do formulário

Arquivos principais:
- `features/babies/BabiesScreens.kt`
- `features/babies/BabiesViewModels.kt`
- `features/babies/BabiesFormValidation.kt`

### 4. Gestão de responsáveis

Fluxo atual:
1. lista responsáveis já cadastrados;
2. permite adicionar, editar e remover;
3. assinatura é capturada na própria tela;
4. dados são salvos localmente no perfil do bebê.

Arquivos principais:
- `features/babies/BabiesScreens.kt`
- `features/babies/BabiesViewModels.kt`
- `core/designsystem/SignaturePad.kt`

### 5. Sessão de coleta biométrica

Fluxo:
1. usuário escolhe a fonte de captura;
2. sistema apresenta status do equipamento;
3. sessão biométrica é criada;
4. captura segue para seleção de dedos e revisão.

Arquivos principais:
- `features/biometric/BiometricScreens.kt`
- `features/biometric/BiometricViewModel.kt`
- `core/sensors/*`

### 6. Captura e revisão biométrica

Fluxo:
1. usuário seleciona o dedo;
2. captura é executada;
3. preview local seguro pode ser aberto;
4. captura pode ser aceita ou descartada;
5. progresso da sessão é atualizado.

### 7. Histórico

Permite consultar sessões anteriores e o detalhe de cada captura armazenada localmente.

Arquivos principais:
- `features/history/HistoryScreen.kt`
- `features/history/HistoryViewModel.kt`
- `features/history/HistoryDetailScreen.kt`
- `features/history/HistoryDetailViewModel.kt`

### 8. Sincronização

Responsável por enviar os dados locais pendentes ao backend quando disponível.

Arquivos principais:
- `features/sync/SyncScreen.kt`
- `features/sync/SyncViewModel.kt`
- `data/sync/SyncPayloadAssembler.kt`
- `core/sync/SyncCoordinator.kt`

---

## Banco de dados local

Entidades principais:

- `hospitals`
- `app_users`
- `babies`
- `guardians`
- `biometric_sessions`
- `fingerprints`
- `sync_queue`
- `audit_logs`

### Destaques

- dados sensíveis são criptografados antes da persistência;
- responsáveis armazenam `signatureBase64` para assinatura digital capturada na tela;
- imagens/capturas usam armazenamento seguro via artefatos criptografados;
- migrations são mantidas em `DatabaseMigrations.kt`;
- schemas do Room são gerados em `app/schemas/`.

Arquivos principais:
- `core/database/AppDatabase.kt`
- `core/database/DatabaseMigrations.kt`

---

## Segurança

O projeto possui mecanismos locais de proteção para ambiente hospitalar:

- controle de acesso por perfil em `AccessPolicy.kt`;
- banco criptografado com SQLCipher;
- uso de serviços de criptografia local;
- armazenamento seguro de artefatos biométricos;
- trilha de auditoria em `audit_logs`.

Arquivos principais:
- `core/security/AccessPolicy.kt`
- `core/security/LocalCryptoService.kt`
- `core/security/EncryptedArtifactStore.kt`
- `core/security/SessionStore.kt`
- `core/security/AuthSessionManager.kt`

---

## Navegação

A navegação central do app está em:

- `app/src/main/java/com/example/biometrianeonatal/core/AppGraph.kt`

Rotas principais:
- login
- dashboard
- babies
- baby-summary
- baby-form-create
- baby-form-edit
- guardians / guardians-manage
- session
- capture
- review
- history / history-detail
- sync

---

## Estrutura resumida

```text
app/src/main/java/com/example/biometrianeonatal/
├── core/
├── data/
├── di/
├── domain/
├── features/
├── ui/
├── BiometriaNeonatalApplication.kt
└── MainActivity.kt
```

Documentação complementar criada na raiz:
- `ARCHITECTURE_OVERVIEW.md`
- `FILE_RESPONSIBILITIES.md`

---

## Como executar localmente

### Build debug

```powershell
./gradlew.bat :app:assembleDebug
```

### Compilar Kotlin do app

```powershell
./gradlew.bat :app:compileDebugKotlin
```

### Rodar testes instrumentados/migração (quando configurado)

```powershell
./gradlew.bat :app:connectedDebugAndroidTest
```

---

## Configuração de ambiente

Propriedades relevantes:

- `REMOTE_BASE_URL`
- `DEBUG_OFFLINE_DEMO_MODE`
- `RELEASE_OFFLINE_DEMO_MODE`

No modo demo:
- o app faz seed local automático;
- sensores e sync podem operar em modo simulado.

---

## Política de documentação neste repositório

A pasta `docs/` foi definida para uso local e será ignorada pelo Git.
A documentação oficial rastreada do projeto fica na raiz do repositório.

---

## Publicação no Git

O projeto pode ser inicializado localmente com:

```powershell
git init -b main
git add .
git commit -m "docs: add project documentation and repository setup"
```

Para publicar em um remoto:

```powershell
git remote add origin <URL_DO_REPOSITORIO>
git push -u origin main
```

> Observação: o push remoto depende da URL do repositório destino.

