package com.example.biometrianeonatal.di

import android.content.Context
import com.example.biometrianeonatal.BuildConfig
import com.example.biometrianeonatal.core.config.AppRuntimeConfig
import com.example.biometrianeonatal.core.database.AppDatabase
import com.example.biometrianeonatal.core.database.DatabasePassphraseProvider
import com.example.biometrianeonatal.core.network.NetworkConfig
import com.example.biometrianeonatal.core.security.AndroidEncryptedArtifactStore
import com.example.biometrianeonatal.core.security.AuthSessionManager
import com.example.biometrianeonatal.core.security.AuthTokenInterceptor
import com.example.biometrianeonatal.core.security.AndroidKeystoreCryptoService
import com.example.biometrianeonatal.core.security.EncryptedArtifactStore
import com.example.biometrianeonatal.core.security.LocalCryptoService
import com.example.biometrianeonatal.core.security.SecureSessionStore
import com.example.biometrianeonatal.core.security.SessionStore
import com.example.biometrianeonatal.core.sensors.AdaptiveSensorCapturePort
import com.example.biometrianeonatal.core.sensors.nativeprocessing.NativeImageProcessor
import com.example.biometrianeonatal.core.sensors.nativeprocessing.NoOpNativeImageProcessor
import com.example.biometrianeonatal.core.sensors.FakeSensorAdapter
import com.example.biometrianeonatal.core.sensors.SensorCapturePort
import com.example.biometrianeonatal.core.sensors.usb.AndroidUsbSensorDiscovery
import com.example.biometrianeonatal.core.sensors.usb.UsbSensorDiscovery
import com.example.biometrianeonatal.core.sync.FakeSyncAdapter
import com.example.biometrianeonatal.core.sync.SyncCoordinator
import com.example.biometrianeonatal.core.sync.SyncWorkScheduler
import com.example.biometrianeonatal.data.remote.AuthApiService
import com.example.biometrianeonatal.data.remote.AuthRemoteDataSource
import com.example.biometrianeonatal.data.remote.FallbackAuthRemoteDataSource
import com.example.biometrianeonatal.data.remote.FallbackSyncRemoteDataSource
import com.example.biometrianeonatal.data.remote.LocalFallbackAuthRemoteDataSource
import com.example.biometrianeonatal.data.remote.RetrofitAuthRemoteDataSource
import com.example.biometrianeonatal.data.remote.RetrofitSyncRemoteDataSource
import com.example.biometrianeonatal.data.remote.SyncApiService
import com.example.biometrianeonatal.data.remote.SyncRemoteDataSource
import com.example.biometrianeonatal.data.repository.OfflineFirstBiometriaRepository
import com.example.biometrianeonatal.data.sync.SyncPayloadAssembler
import com.example.biometrianeonatal.domain.repository.AuthRepository
import com.example.biometrianeonatal.domain.repository.BabyRepository
import com.example.biometrianeonatal.domain.repository.BiometricRepository
import com.example.biometrianeonatal.domain.repository.DashboardRepository
import com.example.biometrianeonatal.domain.repository.HistoryRepository
import com.example.biometrianeonatal.domain.repository.SyncRepository
import com.example.biometrianeonatal.domain.usecase.auth.LoginUseCase
import com.example.biometrianeonatal.domain.usecase.auth.LogoutUseCase
import com.example.biometrianeonatal.domain.usecase.auth.ObserveCurrentSessionUseCase
import com.example.biometrianeonatal.domain.usecase.auth.ObserveHospitalsUseCase
import com.example.biometrianeonatal.domain.usecase.auth.ObserveUserUseCase
import com.example.biometrianeonatal.domain.usecase.babies.DeleteBabyUseCase
import com.example.biometrianeonatal.domain.usecase.babies.ObserveBabiesUseCase
import com.example.biometrianeonatal.domain.usecase.babies.ObserveBabyUseCase
import com.example.biometrianeonatal.domain.usecase.babies.ObserveBabySummaryUseCase
import com.example.biometrianeonatal.domain.usecase.babies.ObserveGuardiansUseCase
import com.example.biometrianeonatal.domain.usecase.babies.SaveBabyUseCase
import com.example.biometrianeonatal.domain.usecase.babies.SaveGuardiansUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.AcceptCaptureUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.DiscardCaptureUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.GenerateCapturePreviewUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.OpenPendingCaptureUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.ObservePendingCaptureUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.RequestUsbPermissionUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.ObserveSensorRuntimeUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.ObserveSessionProgressUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.ObserveSessionContextUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.SelectCaptureSourceUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.SelectUsbDeviceUseCase
import com.example.biometrianeonatal.domain.usecase.biometric.StartBiometricSessionUseCase
import com.example.biometrianeonatal.domain.usecase.dashboard.ObserveDashboardSummaryUseCase
import com.example.biometrianeonatal.domain.usecase.history.ObserveSessionDetailUseCase
import com.example.biometrianeonatal.domain.usecase.history.OpenSessionCaptureUseCase
import com.example.biometrianeonatal.domain.usecase.history.ObserveSessionHistoryUseCase
import com.example.biometrianeonatal.domain.usecase.sync.ObserveImmediateSyncWorkUseCase
import com.example.biometrianeonatal.domain.usecase.sync.ObservePendingSyncItemsUseCase
import com.example.biometrianeonatal.domain.usecase.sync.RunSyncNowUseCase
import com.example.biometrianeonatal.domain.usecase.sync.ScheduleImmediateSyncUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Named
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Modulo Hilt responsavel por montar as dependencias compartilhadas da aplicacao.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModules {

    @Provides
    @Singleton
    fun provideAppRuntimeConfig(): AppRuntimeConfig = AppRuntimeConfig(
        environmentName = BuildConfig.APP_ENV_NAME,
        remoteBaseUrl = BuildConfig.REMOTE_BASE_URL,
        offlineDemoMode = BuildConfig.OFFLINE_DEMO_MODE,
        isDebugBuild = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        databasePassphraseProvider: DatabasePassphraseProvider,
        appRuntimeConfig: AppRuntimeConfig,
    ): AppDatabase = AppDatabase.build(context, databasePassphraseProvider, appRuntimeConfig)

    @Provides
    @Singleton
    fun provideLocalCryptoService(): LocalCryptoService = AndroidKeystoreCryptoService()

    @Provides
    @Singleton
    fun provideEncryptedArtifactStore(
        @ApplicationContext context: Context,
    ): EncryptedArtifactStore = AndroidEncryptedArtifactStore(context)

    @Provides
    @Singleton
    fun provideSessionStore(
        @ApplicationContext context: Context,
    ): SessionStore = SecureSessionStore(context)

    @Provides
    @Singleton
    fun provideAuthSessionManager(
        sessionStore: SessionStore,
        authRemoteDataSource: AuthRemoteDataSource,
    ): AuthSessionManager = AuthSessionManager(sessionStore, authRemoteDataSource)

    @Provides
    @Singleton
    fun provideAuthTokenInterceptor(
        authSessionManager: AuthSessionManager,
    ): AuthTokenInterceptor = AuthTokenInterceptor(authSessionManager)

    @Provides
    @Singleton
    @Named("unauthenticatedOkHttpClient")
    fun provideUnauthenticatedOkHttpClient(
        appRuntimeConfig: AppRuntimeConfig,
    ): OkHttpClient = NetworkConfig.createOkHttpClient(appRuntimeConfig)

    @Provides
    @Singleton
    @Named("authenticatedOkHttpClient")
    fun provideAuthenticatedOkHttpClient(
        appRuntimeConfig: AppRuntimeConfig,
        authTokenInterceptor: AuthTokenInterceptor,
    ): OkHttpClient = NetworkConfig.createOkHttpClient(
        appRuntimeConfig = appRuntimeConfig,
        authInterceptor = authTokenInterceptor,
    )

    @Provides
    @Singleton
    @Named("unauthenticatedRetrofit")
    fun provideUnauthenticatedRetrofit(
        appRuntimeConfig: AppRuntimeConfig,
        @Named("unauthenticatedOkHttpClient") okHttpClient: OkHttpClient,
    ): Retrofit = NetworkConfig.createRetrofit(
        baseUrl = appRuntimeConfig.remoteBaseUrl,
        okHttpClient = okHttpClient,
    )

    @Provides
    @Singleton
    @Named("authenticatedRetrofit")
    fun provideAuthenticatedRetrofit(
        appRuntimeConfig: AppRuntimeConfig,
        @Named("authenticatedOkHttpClient") okHttpClient: OkHttpClient,
    ): Retrofit = NetworkConfig.createRetrofit(
        baseUrl = appRuntimeConfig.remoteBaseUrl,
        okHttpClient = okHttpClient,
    )

    @Provides
    @Singleton
    fun provideAuthApiService(
        @Named("unauthenticatedRetrofit") retrofit: Retrofit,
    ): AuthApiService = retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideRetrofitAuthRemoteDataSource(
        appRuntimeConfig: AppRuntimeConfig,
        authApiService: AuthApiService,
    ): RetrofitAuthRemoteDataSource = RetrofitAuthRemoteDataSource(
        appRuntimeConfig = appRuntimeConfig,
        authApiService = authApiService,
    )

    @Provides
    @Singleton
    fun provideLocalFallbackAuthRemoteDataSource(
        database: AppDatabase,
    ): LocalFallbackAuthRemoteDataSource = LocalFallbackAuthRemoteDataSource(database)

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(
        appRuntimeConfig: AppRuntimeConfig,
        primary: RetrofitAuthRemoteDataSource,
        fallback: LocalFallbackAuthRemoteDataSource,
    ): AuthRemoteDataSource = FallbackAuthRemoteDataSource(
        appRuntimeConfig = appRuntimeConfig,
        primary = primary,
        fallback = fallback,
    )

    @Provides
    @Singleton
    fun provideSyncApiService(
        @Named("authenticatedRetrofit") retrofit: Retrofit,
    ): SyncApiService = retrofit.create(SyncApiService::class.java)

    @Provides
    @Singleton
    fun provideRetrofitSyncRemoteDataSource(
        syncApiService: SyncApiService,
    ): RetrofitSyncRemoteDataSource = RetrofitSyncRemoteDataSource(syncApiService)

    @Provides
    @Singleton
    fun provideFakeSyncAdapter(): FakeSyncAdapter = FakeSyncAdapter()

    @Provides
    @Singleton
    fun provideSyncRemoteDataSource(
        appRuntimeConfig: AppRuntimeConfig,
        primary: RetrofitSyncRemoteDataSource,
        fallback: FakeSyncAdapter,
    ): SyncRemoteDataSource = FallbackSyncRemoteDataSource(
        appRuntimeConfig = appRuntimeConfig,
        primary = primary,
        fallback = fallback,
    )

    @Provides
    @Singleton
    fun provideSyncPayloadAssembler(
        database: AppDatabase,
        localCryptoService: LocalCryptoService,
    ): SyncPayloadAssembler = SyncPayloadAssembler(database, localCryptoService)

    @Provides
    @Singleton
    fun provideSyncCoordinator(
        database: AppDatabase,
        payloadAssembler: SyncPayloadAssembler,
        remoteDataSource: SyncRemoteDataSource,
    ): SyncCoordinator = SyncCoordinator(
        database = database,
        payloadAssembler = payloadAssembler,
        remoteDataSource = remoteDataSource,
    )

    @Provides
    @Singleton
    fun provideFakeSensorAdapter(): FakeSensorAdapter = FakeSensorAdapter()

    @Provides
    @Singleton
    fun provideSensorCapturePort(
        adaptiveSensorCapturePort: AdaptiveSensorCapturePort,
    ): SensorCapturePort = adaptiveSensorCapturePort

    @Provides
    @Singleton
    fun provideUsbSensorDiscovery(
        androidUsbSensorDiscovery: AndroidUsbSensorDiscovery,
    ): UsbSensorDiscovery = androidUsbSensorDiscovery

    @Provides
    @Singleton
    fun provideNativeImageProcessor(): NativeImageProcessor = NoOpNativeImageProcessor()

    @Provides
    @Singleton
    fun provideSyncWorkScheduler(
        @ApplicationContext context: Context,
    ): SyncWorkScheduler = SyncWorkScheduler(context)

    @Provides
    @Singleton
    fun provideOfflineFirstBiometriaRepository(
        database: AppDatabase,
        localCryptoService: LocalCryptoService,
        encryptedArtifactStore: EncryptedArtifactStore,
        sessionStore: SessionStore,
        authRemoteDataSource: AuthRemoteDataSource,
        syncCoordinator: SyncCoordinator,
        sensorAdapter: SensorCapturePort,
    ): OfflineFirstBiometriaRepository = OfflineFirstBiometriaRepository(
        database = database,
        localCryptoService = localCryptoService,
        encryptedArtifactStore = encryptedArtifactStore,
        sessionStore = sessionStore,
        authRemoteDataSource = authRemoteDataSource,
        syncCoordinator = syncCoordinator,
        sensorAdapter = sensorAdapter,
    )

    @Provides
    @Singleton
    fun provideAuthRepository(
        repository: OfflineFirstBiometriaRepository,
    ): AuthRepository = repository

    @Provides
    @Singleton
    fun provideDashboardRepository(
        repository: OfflineFirstBiometriaRepository,
    ): DashboardRepository = repository

    @Provides
    @Singleton
    fun provideHistoryRepository(
        repository: OfflineFirstBiometriaRepository,
    ): HistoryRepository = repository

    @Provides
    @Singleton
    fun provideBabyRepository(
        repository: OfflineFirstBiometriaRepository,
    ): BabyRepository = repository

    @Provides
    @Singleton
    fun provideBiometricRepository(
        repository: OfflineFirstBiometriaRepository,
    ): BiometricRepository = repository

    @Provides
    @Singleton
    fun provideSyncRepository(
        repository: OfflineFirstBiometriaRepository,
    ): SyncRepository = repository

    @Provides
    @Singleton
    fun provideLoginUseCase(
        authRepository: AuthRepository,
    ): LoginUseCase = LoginUseCase(authRepository)

    @Provides
    @Singleton
    fun provideObserveCurrentSessionUseCase(
        authRepository: AuthRepository,
    ): ObserveCurrentSessionUseCase = ObserveCurrentSessionUseCase(authRepository)

    @Provides
    @Singleton
    fun provideObserveHospitalsUseCase(
        authRepository: AuthRepository,
    ): ObserveHospitalsUseCase = ObserveHospitalsUseCase(authRepository)

    @Provides
    @Singleton
    fun provideObserveUserUseCase(
        authRepository: AuthRepository,
    ): ObserveUserUseCase = ObserveUserUseCase(authRepository)

    @Provides
    @Singleton
    fun provideLogoutUseCase(
        authRepository: AuthRepository,
    ): LogoutUseCase = LogoutUseCase(authRepository)

    @Provides
    @Singleton
    fun provideObserveDashboardSummaryUseCase(
        dashboardRepository: DashboardRepository,
    ): ObserveDashboardSummaryUseCase = ObserveDashboardSummaryUseCase(dashboardRepository)

    @Provides
    @Singleton
    fun provideObserveSessionHistoryUseCase(
        historyRepository: HistoryRepository,
    ): ObserveSessionHistoryUseCase = ObserveSessionHistoryUseCase(historyRepository)

    @Provides
    @Singleton
    fun provideObserveSessionDetailUseCase(
        historyRepository: HistoryRepository,
    ): ObserveSessionDetailUseCase = ObserveSessionDetailUseCase(historyRepository)

    @Provides
    @Singleton
    fun provideOpenSessionCaptureUseCase(
        historyRepository: HistoryRepository,
    ): OpenSessionCaptureUseCase = OpenSessionCaptureUseCase(historyRepository)

    @Provides
    @Singleton
    fun provideObserveBabiesUseCase(
        babyRepository: BabyRepository,
    ): ObserveBabiesUseCase = ObserveBabiesUseCase(babyRepository)

    @Provides
    @Singleton
    fun provideObserveBabyUseCase(
        babyRepository: BabyRepository,
    ): ObserveBabyUseCase = ObserveBabyUseCase(babyRepository)

    @Provides
    @Singleton
    fun provideObserveBabySummaryUseCase(
        babyRepository: BabyRepository,
    ): ObserveBabySummaryUseCase = ObserveBabySummaryUseCase(babyRepository)

    @Provides
    @Singleton
    fun provideSaveBabyUseCase(
        babyRepository: BabyRepository,
    ): SaveBabyUseCase = SaveBabyUseCase(babyRepository)

    @Provides
    @Singleton
    fun provideDeleteBabyUseCase(
        babyRepository: BabyRepository,
    ): DeleteBabyUseCase = DeleteBabyUseCase(babyRepository)

    @Provides
    @Singleton
    fun provideObserveGuardiansUseCase(
        babyRepository: BabyRepository,
    ): ObserveGuardiansUseCase = ObserveGuardiansUseCase(babyRepository)


    @Provides
    @Singleton
    fun provideSaveGuardiansUseCase(
        babyRepository: BabyRepository,
    ): SaveGuardiansUseCase = SaveGuardiansUseCase(babyRepository)

    @Provides
    @Singleton
    fun provideObserveSessionContextUseCase(
        biometricRepository: BiometricRepository,
    ): ObserveSessionContextUseCase = ObserveSessionContextUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideObserveSensorRuntimeUseCase(
        biometricRepository: BiometricRepository,
    ): ObserveSensorRuntimeUseCase = ObserveSensorRuntimeUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideObserveSessionProgressUseCase(
        biometricRepository: BiometricRepository,
    ): ObserveSessionProgressUseCase = ObserveSessionProgressUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideSelectCaptureSourceUseCase(
        biometricRepository: BiometricRepository,
    ): SelectCaptureSourceUseCase = SelectCaptureSourceUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideRequestUsbPermissionUseCase(
        biometricRepository: BiometricRepository,
    ): RequestUsbPermissionUseCase = RequestUsbPermissionUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideSelectUsbDeviceUseCase(
        biometricRepository: BiometricRepository,
    ): SelectUsbDeviceUseCase = SelectUsbDeviceUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideOpenPendingCaptureUseCase(
        biometricRepository: BiometricRepository,
    ): OpenPendingCaptureUseCase = OpenPendingCaptureUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideObservePendingCaptureUseCase(
        biometricRepository: BiometricRepository,
    ): ObservePendingCaptureUseCase = ObservePendingCaptureUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideStartBiometricSessionUseCase(
        biometricRepository: BiometricRepository,
    ): StartBiometricSessionUseCase = StartBiometricSessionUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideGenerateCapturePreviewUseCase(
        biometricRepository: BiometricRepository,
    ): GenerateCapturePreviewUseCase = GenerateCapturePreviewUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideAcceptCaptureUseCase(
        biometricRepository: BiometricRepository,
    ): AcceptCaptureUseCase = AcceptCaptureUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideDiscardCaptureUseCase(
        biometricRepository: BiometricRepository,
    ): DiscardCaptureUseCase = DiscardCaptureUseCase(biometricRepository)

    @Provides
    @Singleton
    fun provideObservePendingSyncItemsUseCase(
        syncRepository: SyncRepository,
    ): ObservePendingSyncItemsUseCase = ObservePendingSyncItemsUseCase(syncRepository)

    @Provides
    @Singleton
    fun provideScheduleImmediateSyncUseCase(
        syncWorkScheduler: SyncWorkScheduler,
    ): ScheduleImmediateSyncUseCase = ScheduleImmediateSyncUseCase(syncWorkScheduler)

    @Provides
    @Singleton
    fun provideObserveImmediateSyncWorkUseCase(
        syncWorkScheduler: SyncWorkScheduler,
    ): ObserveImmediateSyncWorkUseCase = ObserveImmediateSyncWorkUseCase(syncWorkScheduler)

    @Provides
    @Singleton
    fun provideRunSyncNowUseCase(
        syncRepository: SyncRepository,
    ): RunSyncNowUseCase = RunSyncNowUseCase(syncRepository)
}

