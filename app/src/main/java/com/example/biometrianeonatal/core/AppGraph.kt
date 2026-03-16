package com.example.biometrianeonatal.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.biometrianeonatal.features.babies.BabiesListScreen
import com.example.biometrianeonatal.features.babies.BabyRegistrationScreen
import com.example.biometrianeonatal.features.babies.BabySummaryScreen
import com.example.biometrianeonatal.features.babies.GuardiansScreen
import com.example.biometrianeonatal.features.biometric.BiometricSessionScreen
import com.example.biometrianeonatal.features.biometric.CaptureBiometricScreen
import com.example.biometrianeonatal.features.biometric.CaptureReviewScreen
import com.example.biometrianeonatal.features.biometric.BiometricViewModel
import com.example.biometrianeonatal.features.dashboard.DashboardScreen
import com.example.biometrianeonatal.features.history.HistoryDetailScreen
import com.example.biometrianeonatal.features.history.HistoryScreen
import com.example.biometrianeonatal.features.login.LoginScreen
import com.example.biometrianeonatal.features.sync.SyncScreen
import com.example.biometrianeonatal.core.security.AccessPolicy
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.example.biometrianeonatal.di.AppGraphDependencies
import com.example.biometrianeonatal.ui.theme.BiometriaNeonatalTheme

/**
 * Objeto centralizador das rotas e helpers usados pela navegacao do aplicativo.
 */
object AppRoutes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard/{userId}"
    const val BABIES = "babies/{userId}"
    const val BABY_SUMMARY = "baby-summary/{userId}/{babyId}"
    const val BABY_FORM_CREATE = "baby-form-create/{userId}"
    const val BABY_FORM_EDIT = "baby-form-edit/{userId}/{babyId}"
    const val GUARDIANS = "guardians/{userId}/{babyId}"
    const val GUARDIANS_MANAGE = "guardians-manage/{userId}/{babyId}"
    const val SESSION = "session/{userId}/{babyId}"
    const val CAPTURE = "capture/{userId}/{babyId}/{sessionId}"
    const val REVIEW = "review/{userId}/{babyId}/{sessionId}"
    const val HISTORY = "history/{userId}"
    const val HISTORY_DETAIL = "history-detail/{userId}/{sessionId}"
    const val SYNC = "sync/{userId}"

    fun dashboard(userId: String) = "dashboard/$userId"
    fun babies(userId: String) = "babies/$userId"
    fun babySummary(userId: String, babyId: String) = "baby-summary/$userId/$babyId"
    fun babyFormCreate(userId: String) = "baby-form-create/$userId"
    fun babyFormEdit(userId: String, babyId: String) = "baby-form-edit/$userId/$babyId"
    fun guardians(userId: String, babyId: String) = "guardians/$userId/$babyId"
    fun guardiansManage(userId: String, babyId: String) = "guardians-manage/$userId/$babyId"
    fun session(userId: String, babyId: String) = "session/$userId/$babyId"
    fun capture(userId: String, babyId: String, sessionId: String) = "capture/$userId/$babyId/$sessionId"
    fun review(userId: String, babyId: String, sessionId: String) = "review/$userId/$babyId/$sessionId"
    fun history(userId: String) = "history/$userId"
    fun historyDetail(userId: String, sessionId: String) = "history-detail/$userId/$sessionId"
    fun sync(userId: String) = "sync/$userId"
}

/**
 * Composable raiz que conecta sessao autenticada, tema visual e grafo de navegacao.
 */
@Composable
fun BiometriaNeonatalApp(
    dependencies: AppGraphDependencies,
) {
    // Controla toda a pilha de telas Compose do app.
    val navController = rememberNavController()
    // Observa a sessão atual para decidir automaticamente entre login e áreas autenticadas.
    val currentSession by dependencies.observeCurrentSessionUseCase().collectAsStateWithLifecycle(initialValue = null)
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(currentSession?.id, currentRoute) {
        val currentUserId = currentSession?.id
        when {
            // Se a sessão já existe e ainda estamos no login, redireciona para o dashboard do usuário logado.
            currentUserId != null && currentRoute == AppRoutes.LOGIN -> {
                navController.navigate(AppRoutes.dashboard(currentUserId)) {
                    popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            }

            // Se a sessão expirou ou foi encerrada, qualquer tela protegida volta para o login.
            currentUserId == null && currentRoute != null && currentRoute != AppRoutes.LOGIN -> {
                navController.navigate(AppRoutes.LOGIN) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    BiometriaNeonatalTheme {
        NavHost(
            navController = navController,
            startDestination = AppRoutes.LOGIN,
        ) {
            // A primeira rota sempre é o login; a sessão observada acima decide se o usuário permanece aqui.
            composable(AppRoutes.LOGIN) {
                LoginScreen()
            }
            composable(
                route = AppRoutes.DASHBOARD,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                DashboardScreen(
                    onOpenBabies = { navController.navigate(AppRoutes.babies(userId)) },
                    onOpenHistory = { navController.navigate(AppRoutes.history(userId)) },
                    onNewBaby = { navController.navigate(AppRoutes.babyFormCreate(userId)) },
                    onOpenSync = { navController.navigate(AppRoutes.sync(userId)) },
                )
            }
            composable(
                route = AppRoutes.HISTORY,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canViewHistory(user?.role)) {
                    HistoryScreen(
                        onBack = { navController.popBackStack() },
                        onOpenSessionDetail = { sessionId ->
                            navController.navigate(AppRoutes.historyDetail(userId, sessionId))
                        },
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Histórico restrito",
                        message = "Seu perfil não pode visualizar o histórico de coletas do dispositivo.",
                        actionLabel = "Voltar ao dashboard",
                        onAction = { navController.navigate(AppRoutes.dashboard(userId)) },
                    )
                }
            }
            composable(
                route = AppRoutes.HISTORY_DETAIL,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("sessionId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canViewHistory(user?.role)) {
                    HistoryDetailScreen(
                        onBack = { navController.popBackStack() },
                        onContinueCollection = { babyId, sessionId ->
                            navController.navigate(AppRoutes.capture(userId, babyId, sessionId)) {
                                launchSingleTop = true
                            }
                        },
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Detalhe restrito",
                        message = "Seu perfil não pode visualizar o detalhe das coletas locais.",
                        actionLabel = "Voltar ao histórico",
                        onAction = { navController.navigate(AppRoutes.history(userId)) },
                    )
                }
            }
            composable(
                route = AppRoutes.BABIES,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canAccessReadOnlyBabies(user?.role)) {
                    BabiesListScreen(
                        userRole = user?.role,
                        onBack = { navController.popBackStack() },
                        onNewBaby = { navController.navigate(AppRoutes.babyFormCreate(userId)) },
                        onViewSummary = { babyId -> navController.navigate(AppRoutes.babySummary(userId, babyId)) },
                        onCollect = { babyId -> navController.navigate(AppRoutes.session(userId, babyId)) },
                        onEditBaby = { babyId -> navController.navigate(AppRoutes.babyFormEdit(userId, babyId)) },
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Acesso restrito",
                        message = "Seu perfil não pode acessar os cadastros de recém-nascidos.",
                        actionLabel = "Voltar ao dashboard",
                        onAction = { navController.navigate(AppRoutes.dashboard(userId)) },
                    )
                }
            }
            composable(
                route = AppRoutes.BABY_SUMMARY,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("babyId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val babyId = backStackEntry.arguments?.getString("babyId").orEmpty()
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canViewBabySummary(user?.role)) {
                    BabySummaryScreen(
                        userRole = user?.role,
                        onBack = { navController.popBackStack() },
                        onManageGuardians = { targetBabyId ->
                            navController.navigate(AppRoutes.guardiansManage(userId, targetBabyId)) {
                                launchSingleTop = true
                            }
                        },
                        onStartCollection = { targetBabyId ->
                            navController.navigate(AppRoutes.session(userId, targetBabyId)) {
                                launchSingleTop = true
                            }
                        },
                        onResumeCollection = { targetBabyId, sessionId ->
                            navController.navigate(AppRoutes.capture(userId, targetBabyId, sessionId)) {
                                launchSingleTop = true
                            }
                        },
                        onOpenLatestSession = { sessionId ->
                            navController.navigate(AppRoutes.historyDetail(userId, sessionId)) {
                                launchSingleTop = true
                            }
                        },
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Resumo indisponível",
                        message = "Seu perfil não pode visualizar o resumo deste cadastro.",
                        actionLabel = "Voltar à lista",
                        onAction = { navController.navigate(AppRoutes.babies(userId)) },
                    )
                }
            }
            composable(
                route = AppRoutes.BABY_FORM_CREATE,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canCreateOrEditBaby(user?.role)) {
                    BabyRegistrationScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { babyId -> navController.navigate(AppRoutes.guardians(userId, babyId)) },
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Cadastro indisponível",
                        message = "Seu perfil não pode criar cadastros de recém-nascidos.",
                        actionLabel = "Voltar à lista",
                        onAction = { navController.navigate(AppRoutes.babies(userId)) },
                    )
                }
            }
            composable(
                route = AppRoutes.BABY_FORM_EDIT,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("babyId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val babyId = backStackEntry.arguments?.getString("babyId").orEmpty()
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canCreateOrEditBaby(user?.role)) {
                    BabyRegistrationScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                        onManageGuardians = { navController.navigate(AppRoutes.guardiansManage(userId, babyId)) },
                        onDelete = { navController.popBackStack() },
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Edição indisponível",
                        message = "Seu perfil não pode editar cadastros de recém-nascidos.",
                        actionLabel = "Voltar à lista",
                        onAction = { navController.navigate(AppRoutes.babies(userId)) },
                    )
                }
            }
            composable(
                route = AppRoutes.GUARDIANS,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("babyId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val babyId = backStackEntry.arguments?.getString("babyId").orEmpty()
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canManageGuardians(user?.role)) {
                    GuardiansScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.navigate(AppRoutes.session(userId, babyId)) },
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Responsáveis restritos",
                        message = "Seu perfil não pode acessar os dados dos responsáveis.",
                        actionLabel = "Voltar à lista",
                        onAction = { navController.navigate(AppRoutes.babies(userId)) },
                    )
                }
            }
            composable(
                route = AppRoutes.GUARDIANS_MANAGE,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("babyId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val babyId = backStackEntry.arguments?.getString("babyId").orEmpty()
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canManageGuardians(user?.role)) {
                    GuardiansScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Responsáveis restritos",
                        message = "Seu perfil não pode editar os responsáveis do recém-nascido.",
                        actionLabel = "Voltar à lista",
                        onAction = { navController.navigate(AppRoutes.babies(userId)) },
                    )
                }
            }
            composable(
                route = AppRoutes.SESSION,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("babyId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val babyId = backStackEntry.arguments?.getString("babyId").orEmpty()
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canCollectBiometrics(user?.role)) {
                    BiometricSessionScreen(
                        onBack = { navController.popBackStack() },
                        onSessionStarted = { sessionId ->
                            navController.navigate(AppRoutes.capture(userId, babyId, sessionId))
                        },
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Coleta indisponível",
                        message = "Seu perfil não pode iniciar sessões de coleta biométrica.",
                        actionLabel = "Voltar à lista",
                        onAction = { navController.navigate(AppRoutes.babies(userId)) },
                    )
                }
            }
            composable(
                route = AppRoutes.CAPTURE,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("babyId") { type = NavType.StringType },
                    navArgument("sessionId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val babyId = backStackEntry.arguments?.getString("babyId").orEmpty()
                val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
                val biometricViewModel: BiometricViewModel = hiltViewModel(backStackEntry)
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canCollectBiometrics(user?.role)) {
                    CaptureBiometricScreen(
                        viewModel = biometricViewModel,
                        onBack = { navController.popBackStack() },
                        onReview = { navController.navigate(AppRoutes.review(userId, babyId, sessionId)) },
                        onFinish = {
                            navController.navigate(AppRoutes.babies(userId)) {
                                popUpTo(AppRoutes.babies(userId)) { inclusive = true }
                            }
                        },
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Captura indisponível",
                        message = "Seu perfil não pode executar capturas biométricas.",
                        actionLabel = "Voltar à lista",
                        onAction = { navController.navigate(AppRoutes.babies(userId)) },
                    )
                }
            }
            composable(
                route = AppRoutes.REVIEW,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("babyId") { type = NavType.StringType },
                    navArgument("sessionId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val babyId = backStackEntry.arguments?.getString("babyId").orEmpty()
                val sessionId = backStackEntry.arguments?.getString("sessionId").orEmpty()
                val captureBackStackEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AppRoutes.capture(userId, babyId, sessionId))
                }
                val biometricViewModel: BiometricViewModel = hiltViewModel(captureBackStackEntry)
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canCollectBiometrics(user?.role)) {
                    CaptureReviewScreen(
                        viewModel = biometricViewModel,
                        onBack = { navController.popBackStack() },
                        onContinueCollection = { navController.popBackStack() },
                        onSessionCompleted = {
                            navController.navigate(AppRoutes.babies(userId)) {
                                popUpTo(AppRoutes.babies(userId)) { inclusive = true }
                            }
                        },
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Revisão indisponível",
                        message = "Seu perfil não pode revisar capturas biométricas.",
                        actionLabel = "Voltar à lista",
                        onAction = { navController.navigate(AppRoutes.babies(userId)) },
                    )
                }
            }
            composable(
                route = AppRoutes.SYNC,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                val user by dependencies.authRepository.observeUser(userId).collectAsStateWithLifecycle(initialValue = null)
                if (AccessPolicy.canSync(user?.role)) {
                    SyncScreen(
                        onBack = { navController.popBackStack() },
                        onDone = { navController.navigate(AppRoutes.dashboard(userId)) }
                    )
                } else {
                    AccessDeniedScreen(
                        title = "Sincronização restrita",
                        message = "Seu perfil não pode disparar sincronização manual com a nuvem.",
                        actionLabel = "Voltar ao dashboard",
                        onAction = { navController.navigate(AppRoutes.dashboard(userId)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessDeniedScreen(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Scaffold {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(it)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = message,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

