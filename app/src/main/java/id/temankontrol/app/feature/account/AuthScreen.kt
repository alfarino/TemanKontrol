package id.temankontrol.app.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.temankontrol.app.core.designsystem.component.AppScaffold
import id.temankontrol.app.core.designsystem.component.AppTopBar
import id.temankontrol.app.data.remote.TemanKontrolApiClient
import id.temankontrol.app.ui.theme.HeroTealEndDark
import id.temankontrol.app.ui.theme.HeroTealEndLight
import id.temankontrol.app.ui.theme.HeroTealStartDark
import id.temankontrol.app.ui.theme.HeroTealStartLight

private enum class AuthMode {
    LOGIN,
    REGISTER,
    RESET_PASSWORD
}

@Composable
fun AuthScreen(
    onNavigateBack: () -> Unit,
    onAuthenticated: () -> Unit,
    onContinueGuest: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuthContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onClearFeedback = viewModel::clearFeedback,
        onLogin = { email, password, url ->
            viewModel.login(email, password, url, onSuccess = onAuthenticated)
        },
        onRegister = { name, email, password, accepted, url ->
            viewModel.register(name, email, password, accepted, url, onSuccess = onAuthenticated)
        },
        onRequestOtp = { email, url, onSent ->
            viewModel.requestOtp(email, url, onDispatched = onSent)
        },
        onResetPassword = { email, otp, newPass, url, onDone ->
            viewModel.confirmResetPassword(email, otp, newPass, url, onSuccess = onDone)
        },
        onContinueGuest = onContinueGuest
    )
}

@Composable
fun AuthContent(
    uiState: AccountUiState,
    onNavigateBack: () -> Unit,
    onClearFeedback: () -> Unit,
    onLogin: (email: String, password: String, serverUrl: String) -> Unit,
    onRegister: (displayName: String, email: String, password: String, privacyAccepted: Boolean, serverUrl: String) -> Unit,
    onRequestOtp: (email: String, serverUrl: String, onSent: (String) -> Unit) -> Unit,
    onResetPassword: (email: String, otp: String, newPassword: String, serverUrl: String, onDone: () -> Unit) -> Unit,
    onContinueGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var privacyAccepted by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showServerField by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf(TemanKontrolApiClient.DEFAULT_BASE_URL) }
    var otpInfoMessage by remember { mutableStateOf<String?>(null) }

    val isDark = isSystemInDarkTheme()
    val gradientStart = if (isDark) HeroTealStartDark else HeroTealStartLight
    val gradientEnd = if (isDark) HeroTealEndDark else HeroTealEndLight

    AppScaffold(
        topBar = {
            AppTopBar(
                title = when (mode) {
                    AuthMode.LOGIN -> "Masuk ke Akun"
                    AuthMode.REGISTER -> "Daftar Akun Baru"
                    AuthMode.RESET_PASSWORD -> "Reset Kata Sandi"
                },
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Cloud Sync Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Pemulihan & Sinkronisasi Akun",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Tautkan catatan kunjungan, pertanyaan, foto & VN Anda ke server pribadi unand.tech agar data otomatis pulih saat berganti HP atau pindah ke iPhone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }

            // Mode Tabs: Masuk / Daftar
            if (mode != AuthMode.RESET_PASSWORD) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = mode == AuthMode.LOGIN,
                        onClick = {
                            mode = AuthMode.LOGIN
                            onClearFeedback()
                        },
                        label = { Text("Masuk") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mode == AuthMode.REGISTER,
                        onClick = {
                            mode = AuthMode.REGISTER
                            onClearFeedback()
                        },
                        label = { Text("Daftar Baru") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (uiState.statusMessage != null || otpInfoMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.statusMessage ?: otpInfoMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (mode == AuthMode.REGISTER) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nama Lengkap / Panggilan") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("contoh@unand.tech") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (mode == AuthMode.RESET_PASSWORD) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        label = { Text("Kode OTP (6 digit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        enabled = !uiState.isBusy && email.isNotBlank(),
                        onClick = {
                            onRequestOtp(email, serverUrl) { msg -> otpInfoMessage = msg }
                        }
                    ) {
                        Text("Kirim OTP")
                    }
                }
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(if (mode == AuthMode.RESET_PASSWORD) "Kata Sandi Baru (min. 8 karakter)" else "Kata Sandi")
                },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (mode == AuthMode.REGISTER) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = privacyAccepted,
                        onCheckedChange = { privacyAccepted = it }
                    )
                    Text(
                        text = "Saya menyetujui Kebijakan Privasi (temankontrol.unand.tech/privacy) untuk menyimpan & menyinkronkan catatan saya secara aman.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Optional custom server URL toggle
            TextButton(onClick = { showServerField = !showServerField }) {
                Text(if (showServerField) "Sembunyikan Alamat Server" else "Server: api.temankontrol.unand.tech (Ubah?)")
            }
            if (showServerField) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("URL Server API") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.isBusy) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.busyMessage ?: "Memproses…",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Button(
                enabled = !uiState.isBusy,
                onClick = {
                    when (mode) {
                        AuthMode.LOGIN -> onLogin(email, password, serverUrl)
                        AuthMode.REGISTER -> onRegister(displayName, email, password, privacyAccepted, serverUrl)
                        AuthMode.RESET_PASSWORD -> onResetPassword(email, otpCode, password, serverUrl) {
                            mode = AuthMode.LOGIN
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = when (mode) {
                        AuthMode.LOGIN -> "Masuk & Pulihkan Data"
                        AuthMode.REGISTER -> "Daftar & Amankan Data Lokal"
                        AuthMode.RESET_PASSWORD -> "Simpan Kata Sandi Baru"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            if (mode == AuthMode.LOGIN) {
                TextButton(
                    onClick = {
                        mode = AuthMode.RESET_PASSWORD
                        onClearFeedback()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Lupa kata sandi?")
                }
            } else if (mode == AuthMode.RESET_PASSWORD) {
                TextButton(
                    onClick = {
                        mode = AuthMode.LOGIN
                        onClearFeedback()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Kembali ke Masuk")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            OutlinedButton(
                onClick = onContinueGuest,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Lewati, gunakan secara lokal dulu")
            }
        }
    }
}
