# Visão de Arquitetura

## 1. Resumo

O aplicativo `Biometria Neonatal` foi organizado como um app Android modularizado por camadas e por funcionalidade, com forte foco em:

- operação offline-first;
- segurança local dos dados;
- separação de responsabilidades;
- suporte a captura biométrica em múltiplas fontes.

---

## 2. Organização em pacotes

## `core/`
Infraestrutura compartilhada.

### Principais responsabilidades
- `AppGraph.kt`: navegação principal e orquestração das rotas.
- `AppInitializer.kt`: bootstrap do app, seed local e agendamento de sync.
- `database/`: entidades Room, DAOs, migrations e criação do banco.
- `designsystem/`: componentes Compose reutilizáveis.
- `security/`: criptografia, sessão, artefatos protegidos, política de acesso.
- `sensors/`: abstrações e implementações de captura biométrica.
- `sync/`: agendamento e coordenação de sincronização.
- `network/`: configuração de Retrofit/OkHttp.
- `config/`: runtime config e flags de ambiente.

## `data/`
Implementações concretas dos contratos.

### Principais responsabilidades
- `repository/OfflineFirstBiometriaRepository.kt`: repositório principal do sistema.
- `remote/`: APIs, DTOs, data sources remotos e fallbacks.
- `sync/SyncPayloadAssembler.kt`: monta payloads de sincronização.
- `seed/DatabaseSeeder.kt`: massa inicial para demo offline.

## `domain/`
Camada de regras e contratos.

### Principais responsabilidades
- `model/`: modelos de domínio usados pela apresentação.
- `repository/Repositories.kt`: contratos dos repositórios.
- `usecase/`: casos de uso por domínio funcional.

## `features/`
Camada de apresentação por funcionalidade.

### Features existentes
- `login`
- `dashboard`
- `babies`
- `biometric`
- `history`
- `sync`

---

## 3. Padrões arquiteturais adotados

## MVVM
Cada feature principal usa:
- `Screen` para UI Compose;
- `ViewModel` para estado e ações.

## Clean Architecture (pragmática)
Separação em:
- UI / ViewModel
- UseCase
- Repository
- Infraestrutura local/remota

## Offline-first
Os dados são priorizados localmente e sincronizados depois.

Fluxo resumido:

```text
UI -> ViewModel -> UseCase -> Repository -> Banco local
                                    \-> fila de sync -> backend
```

---

## 4. Navegação

A navegação é centralizada em `core/AppGraph.kt`.

### Principais rotas
- `login`
- `dashboard/{userId}`
- `babies/{userId}`
- `baby-summary/{userId}/{babyId}`
- `baby-form-create/{userId}`
- `baby-form-edit/{userId}/{babyId}`
- `guardians/{userId}/{babyId}`
- `guardians-manage/{userId}/{babyId}`
- `session/{userId}/{babyId}`
- `capture/{userId}/{babyId}/{sessionId}`
- `review/{userId}/{babyId}/{sessionId}`
- `history/{userId}`
- `history-detail/{userId}/{sessionId}`
- `sync/{userId}`

Além da navegação, o `AppGraph` aplica políticas de acesso usando `AccessPolicy`.

---

## 5. Persistência e segurança

## Banco local
O banco é definido em `core/database/AppDatabase.kt`.

### Entidades principais
- `HospitalEntity`
- `UserEntity`
- `BabyEntity`
- `GuardianEntity`
- `BiometricSessionEntity`
- `FingerprintEntity`
- `SyncQueueEntity`
- `AuditLogEntity`

## Proteção de dados
- banco criptografado com SQLCipher;
- dados sensíveis criptografados por `LocalCryptoService`;
- artefatos biométricos protegidos via `EncryptedArtifactStore`;
- sessão persistida em `SessionStore`.

---

## 6. Coleta biométrica

A camada de sensores abstrai o hardware real.

### Fontes suportadas
- demo local
- câmera do tablet
- sensor USB

### Componentes principais
- `SensorCapturePort.kt`
- `AdaptiveSensorCapturePort.kt`
- `FakeSensorAdapter.kt`
- `tabletcamera/`
- `usb/`
- `nativeprocessing/`

A ViewModel biométrica concentra:
- contexto da sessão;
- status do sensor;
- dedo ativo;
- captura pendente;
- progresso da sessão;
- aceitação/descartar captura.

---

## 7. Sincronização

A sincronização é tratada em camadas:

- `SyncPayloadAssembler`: monta o lote local
- `SyncRemoteDataSource`: envia ao backend
- `SyncCoordinator`: coordena leitura local e atualização de status
- `SyncWorkScheduler`: agenda execução em background

---

## 8. Modo demo offline

Quando `OFFLINE_DEMO_MODE` está ativo:
- o app pode usar massa local;
- o banco é populado via `DatabaseSeeder`;
- o fluxo pode operar mesmo sem backend real.
