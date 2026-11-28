package id.temankontrol.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import id.temankontrol.app.data.local.DatabaseSeeder
import id.temankontrol.app.data.reminder.ReminderNotificationHelper
import id.temankontrol.app.data.sync.CloudAutoSyncCoordinator
import id.temankontrol.app.domain.model.AppLockSettings
import id.temankontrol.app.domain.repository.PreferencesRepository
import id.temankontrol.app.feature.lock.AppLockOverlay
import id.temankontrol.app.navigation.AppNavHost
import id.temankontrol.app.ui.theme.TemanKontrolTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    @Inject
    lateinit var cloudAutoSyncCoordinator: CloudAutoSyncCoordinator

    private var initialReady by mutableStateOf(false)
    private var initialNoticeAccepted by mutableStateOf(false)
    private var isLocked by mutableStateOf(false)
    private var currentLockSettings by mutableStateOf(AppLockSettings())
    private var lastBackgroundAtMillis: Long = 0L
    private var deepLinkConsultationId by mutableStateOf<String?>(null)
    private var deepLinkOpenBackup by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        handleDeepLinkIntent(intent)

        splashScreen.setKeepOnScreenCondition { !initialReady }

        lifecycleScope.launch {
            initialNoticeAccepted = preferencesRepository.noticeAccepted.first()
            val lockSettings = preferencesRepository.appLockSettings.first()
            currentLockSettings = lockSettings
            if (lockSettings.enabled) {
                if (canAuthenticateDevice()) {
                    isLocked = true
                } else {
                    preferencesRepository.updateAppLockSettings { it.copy(enabled = false) }
                }
            }
            initialReady = true
            databaseSeeder.seedSampleDataIfEmpty()
        }

        lifecycleScope.launch {
            preferencesRepository.appLockSettings.collect { settings ->
                currentLockSettings = settings
                if (settings.enabled && settings.secureScreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            val isNoticeAccepted by preferencesRepository.noticeAccepted
                .collectAsStateWithLifecycle(initialValue = initialNoticeAccepted)
            var showBrandedIntro by remember { mutableStateOf(true) }

            LaunchedEffect(initialReady) {
                if (initialReady) {
                    delay(650L)
                    showBrandedIntro = false
                }
            }

            if (initialReady) {
                TemanKontrolTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        when {
                            showBrandedIntro -> BrandedSplashScreen()
                            isLocked -> AppLockOverlay(
                                onUnlockClick = { showBiometricPrompt() }
                            )
                            else -> AppNavHost(
                                isNoticeAccepted = isNoticeAccepted,
                                deepLinkConsultationId = deepLinkConsultationId,
                                deepLinkOpenBackup = deepLinkOpenBackup,
                                onDeepLinkConsumed = {
                                    deepLinkConsultationId = null
                                    deepLinkOpenBackup = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: android.content.Intent?) {
        val cId = intent?.getStringExtra(ReminderNotificationHelper.EXTRA_NAV_CONSULTATION_ID)
        val openBackup = intent?.getBooleanExtra(ReminderNotificationHelper.EXTRA_NAV_OPEN_BACKUP, false) == true
        if (!cId.isNullOrBlank()) {
            deepLinkConsultationId = cId
        } else if (openBackup) {
            deepLinkOpenBackup = true
        }
    }

    override fun onStop() {
        super.onStop()
        lastBackgroundAtMillis = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        cloudAutoSyncCoordinator.onAppForeground()
        if (!initialReady) return
        if (currentLockSettings.enabled) {
            if (!canAuthenticateDevice()) {
                isLocked = false
                lifecycleScope.launch {
                    preferencesRepository.updateAppLockSettings { it.copy(enabled = false) }
                }
                return
            }
            if (!isLocked && lastBackgroundAtMillis > 0L) {
                val elapsed = System.currentTimeMillis() - lastBackgroundAtMillis
                val timeoutMs = currentLockSettings.timeout.minutes * 60_000L
                if (elapsed >= timeoutMs) {
                    isLocked = true
                }
            }
        }
    }

    private fun canAuthenticateDevice(): Boolean {
        val result = BiometricManager.from(this)
            .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        if (!canAuthenticateDevice()) {
            isLocked = false
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isLocked = false
                    lastBackgroundAtMillis = 0L
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Teman Kontrol")
            .setSubtitle("Gunakan sidik jari atau kunci layar HP Anda")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(promptInfo)
    }
}

@Composable
private fun BrandedSplashScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0E8489), Color(0xFF0B6E72), Color(0xFF064A4E))
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_splash_logo),
                contentDescription = "Logo Teman Kontrol",
                modifier = Modifier.size(136.dp)
            )
            Text(
                text = "Teman Kontrol",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "Pendamping Konsultasi Kesehatan",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}
