package id.temankontrol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import id.temankontrol.app.domain.repository.PreferencesRepository
import id.temankontrol.app.navigation.AppNavHost
import id.temankontrol.app.ui.theme.TemanKontrolTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isNoticeAccepted by preferencesRepository.noticeAccepted.collectAsStateWithLifecycle(initialValue = false)

            TemanKontrolTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(isNoticeAccepted = isNoticeAccepted)
                }
            }
        }
    }
}
