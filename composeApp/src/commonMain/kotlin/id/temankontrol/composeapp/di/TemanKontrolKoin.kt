package id.temankontrol.composeapp.di

import id.temankontrol.composeapp.data.KmpAppStore
import id.temankontrol.composeapp.data.SupabaseKmpApiClient
import id.temankontrol.composeapp.platform.*
import id.temankontrol.shared.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MultiplatformApiGateway(
    engine: HttpClientEngine = createHttpClientEngine(),
    val baseUrl: String = SupabaseKmpApiClient.DEFAULT_BASE_URL
) {
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    suspend fun health(): HealthResponse = client.get("${baseUrl.trimEnd('/')}/health").body()

    suspend fun pullSync(accessToken: String, sinceMillis: Long): SyncPullResponse =
        client.get("${baseUrl.trimEnd('/')}/v1/sync/pull?since=$sinceMillis") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()
}

val sharedKmpModule = module {
    single { createPlatformAlarmScheduler() }
    single { createPlatformMediaAdapter() }
    single { createPlatformBiometricAuth() }
    single { createPlatformPdfExporter() }
    single { createPlatformLocalStore() }
    single { createPlatformMediaActions() }
    single { createPlatformShareBridge() }
    single { SupabaseKmpApiClient(engine = createHttpClientEngine()) }
    single {
        KmpAppStore(
            localStore = get(),
            apiClient = get(),
            alarmScheduler = get(),
            pdfExporter = get()
        )
    }
    single { MultiplatformApiGateway() }
}

fun initKoin(extraDeclaration: KoinApplication.() -> Unit = {}): KoinApplication = startKoin {
    extraDeclaration()
    modules(sharedKmpModule)
}
