# Responsabilidades por Arquivo e Módulo

Este documento resume o papel dos arquivos mais importantes do sistema.

---

## Raiz do projeto

### `build.gradle.kts`
Configuração global do Gradle.

### `app/build.gradle.kts`
Configuração do módulo Android principal, plugins, build types, Room schema e dependências.

### `gradle/libs.versions.toml`
Catálogo central de versões e aliases de dependências.

### `README.md`
Documentação principal do projeto.

### `ARCHITECTURE_OVERVIEW.md`
Resumo arquitetural do app.

### `FILE_RESPONSIBILITIES.md`
Mapa técnico das responsabilidades por pacote e arquivo.

---

## Aplicação

### `BiometriaNeonatalApplication.kt`
Classe `Application` com bootstrap do Hilt e chamada do inicializador do app.

### `MainActivity.kt`
Ponto de entrada Android; injeta dependências de navegação e renderiza a árvore Compose.

---

## `core/`

### `core/AppGraph.kt`
Define todas as rotas da aplicação, a navegação e as restrições de acesso entre telas.

### `core/AppInitializer.kt`
Executa seed local em modo demo e agenda sincronização automática quando há backend configurado.

---

## `core/database/`

### `AppDatabase.kt`
Fonte de verdade do banco local: entidades, DAOs, Room database e bootstrap com SQLCipher.

### `DatabaseMigrations.kt`
Contém todas as migrations do banco entre versões.

### Responsabilidade geral da pasta
- schema local;
- queries de acesso;
- suporte a evolução do banco.

---

## `core/designsystem/`

### `AppComponents.kt`
Componentes reutilizáveis de formulário, dropdowns, badges e cartões.

### `SignaturePad.kt`
Componente de assinatura digital desenhada na tela e exportada em Base64.

### Responsabilidade geral da pasta
Padronizar UI e reduzir duplicação de componentes Compose.

---

## `core/security/`

### `AccessPolicy.kt`
Regras de autorização por perfil de usuário.

### `AuthSessionManager.kt`
Gerencia tokens e sessão autenticada.

### `EncryptedArtifactStore.kt`
Protege artefatos sensíveis como imagens biométricas.

### `LocalCryptoService.kt`
Criptografia/descriptografia de campos sensíveis.

### `SessionStore.kt`
Persistência local da sessão do usuário.

---

## `core/sensors/`

### `SensorCapturePort.kt`
Contrato principal de captura biométrica.

### `AdaptiveSensorCapturePort.kt`
Orquestra a escolha da implementação de captura adequada.

### `FakeSensorAdapter.kt`
Implementação simulada para modo demo/offline.

### Subpastas
- `tabletcamera/`: integração com câmera do tablet
- `usb/`: integração com dispositivos USB
- `native/` / `nativeprocessing/`: processamento nativo ou abstrações relacionadas

---

## `core/sync/`

### `SyncCoordinator.kt`
Orquestra a sincronização local-remoto.

### `SyncWorkScheduler.kt`
Agenda rotinas de sincronização em background.

### `FakeSyncAdapter.kt`
Fallback/simulação de sincronização para modo demo.

---

## `data/repository/`

### `OfflineFirstBiometriaRepository.kt`
Principal implementação dos repositórios do sistema.

Responsabilidades:
- autenticação e sessão local;
- leitura/escrita de recém-nascidos e responsáveis;
- criação de sessões biométricas;
- aceite/descarte de capturas;
- leitura de histórico;
- sincronização dos dados pendentes.

É o centro do comportamento offline-first do app.

---

## `data/remote/`

### `AuthApiService.kt`
Contrato Retrofit para autenticação.

### `AuthDtos.kt`
DTOs de autenticação.

### `AuthRemoteDataSource.kt`
Contrato de acesso remoto de autenticação.

### `SyncApiService.kt`
Contrato Retrofit para sincronização.

### `SyncDtos.kt`
DTOs do payload de sincronização.

### `SyncRemoteDataSource.kt`
Contrato para envio/recebimento de sync.

### `RetrofitAuthRemoteDataSource.kt`
Implementação Retrofit da autenticação.

### `RetrofitSyncRemoteDataSource.kt`
Implementação Retrofit da sincronização.

### `FallbackSyncRemoteDataSource.kt`
Fallback para cenários offline/demo.

### `LocalFallbackAuthRemoteDataSource.kt`
Fallback local de autenticação.

---

## `data/sync/`

### `SyncPayloadAssembler.kt`
Lê dados locais pendentes e monta o lote de sincronização para envio.

---

## `data/seed/`

### `DatabaseSeeder.kt`
Popula o banco com dados iniciais quando o app roda em modo demo offline.

---

## `domain/model/`

### `AppModels.kt`
Modelos de domínio consumidos pelas features e casos de uso.

Exemplos:
- `BabyDraft`
- `GuardianDraft`
- `BabyProfileSummary`
- `SessionCaptureProgress`
- `SensorRuntimeInfo`

---

## `domain/repository/`

### `Repositories.kt`
Define os contratos dos repositórios:
- `AuthRepository`
- `DashboardRepository`
- `HistoryRepository`
- `BabyRepository`
- `BiometricRepository`
- `SyncRepository`

---

## `domain/usecase/`

### Responsabilidade geral
Encapsular operações de negócio chamadas pelas ViewModels.

Exemplos:
- login/logout
- observar dashboards
- salvar bebê
- salvar responsáveis
- iniciar sessão biométrica
- gerar preview
- aceitar captura
- rodar sincronização

---

## `di/`

### `AppModules.kt`
Fornece dependências do app via Hilt.

### `AppGraphDependencies.kt`
Agrupa dependências usadas pela camada de navegação principal.

---

## `features/login/`

### `LoginScreen.kt`
Tela de autenticação.

### `LoginViewModel.kt`
Estado e ações do login.

---

## `features/dashboard/`

### `DashboardScreen.kt`
Tela inicial após login com atalhos e indicadores.

### `DashboardViewModel.kt`
Fornece métricas resumidas da operação.

---

## `features/babies/`

### `BabiesScreens.kt`
Concentra as telas relacionadas a recém-nascidos e responsáveis.

Inclui:
- lista de bebês;
- resumo do bebê;
- cadastro/edição do bebê;
- gestão de responsáveis;
- formulário de responsável.

### `BabiesViewModels.kt`
ViewModels das telas de bebês e responsáveis.

### `BabiesFormValidation.kt`
Funções utilitárias de validação e formatação para bebês e responsáveis.

---

## `features/biometric/`

### `BiometricScreens.kt`
Telas de sessão, captura e revisão biométrica.

### `BiometricViewModel.kt`
Estado central do fluxo biométrico.

### `CameraXPreview.kt`
Integração visual com preview usando CameraX.

---

## `features/history/`

### `HistoryScreen.kt`
Lista de sessões históricas.

### `HistoryViewModel.kt`
Estado e carregamento da lista de histórico.

### `HistoryDetailScreen.kt`
Detalhe de uma sessão específica.

### `HistoryDetailViewModel.kt`
Estado do detalhe e abertura de artefatos da sessão.

---

## `features/sync/`

### `SyncScreen.kt`
Tela operacional de sincronização.

### `SyncViewModel.kt`
Estado da sincronização e execução manual.

---

## `ui/`

### Responsabilidade geral
Temas e elementos visuais globais do aplicativo.

---

## Fluxo macro entre módulos

```text
Login -> Dashboard -> Bebês -> Resumo -> Responsáveis -> Sessão -> Captura -> Revisão
                                      \-> Histórico
                                      \-> Sync
```

---

## Arquivos mais críticos para manutenção

Se for preciso entender rapidamente o sistema, comece por:

1. `core/AppGraph.kt`
2. `data/repository/OfflineFirstBiometriaRepository.kt`
3. `core/database/AppDatabase.kt`
4. `di/AppModules.kt`
5. `features/babies/BabiesScreens.kt`
6. `features/babies/BabiesViewModels.kt`
7. `features/biometric/BiometricViewModel.kt`

