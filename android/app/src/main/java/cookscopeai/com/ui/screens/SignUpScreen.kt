package cookscopeai.com.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cookscopeai.com.data.AuthManager
import cookscopeai.com.data.AuthResult
import cookscopeai.com.ui.theme.BlackBg
import cookscopeai.com.ui.theme.BlackSurface
import cookscopeai.com.ui.theme.GoldDeep
import cookscopeai.com.ui.theme.GoldPrimary
import kotlinx.coroutines.launch

/**
 * Sign Up screen — username, email, password, and Google sign-in button.
 * On success, navigates to the Verify Email screen.
 * Always rendered on a dark backdrop (auth flow).
 */
@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignUpSuccess: (email: String) -> Unit,
    onGoogleSuccess: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isFormValid = username.isNotBlank() &&
        email.isNotBlank() &&
        password.length >= 6 &&
        email.contains("@")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GoldPrimary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Create your account",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Join CookScope and start cooking smarter with AI.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0B0B0),
            )

            Spacer(Modifier.height(28.dp))

            // Username
            AuthField(
                label = "Username",
                value = username,
                onValueChange = { username = it },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary) },
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text,
            )
            Spacer(Modifier.height(14.dp))

            // Email
            AuthField(
                label = "Email",
                value = email,
                onValueChange = { email = it },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = GoldPrimary) },
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Email,
            )
            Spacer(Modifier.height(14.dp))

            // Password
            AuthField(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary) },
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFFB0B0B0),
                        )
                    }
                },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Use at least 6 characters.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF808080),
            )

            Spacer(Modifier.height(24.dp))

            // Sign up button
            Button(
                onClick = {
                    keyboard?.hide()
                    if (!isFormValid) {
                        error = when {
                            username.isBlank() -> "Please enter a username."
                            email.isBlank() || !email.contains("@") -> "Please enter a valid email."
                            password.length < 6 -> "Password must be at least 6 characters."
                            else -> "Please fill in all fields."
                        }
                        return@Button
                    }
                    error = null
                    isLoading = true
                    scope.launch {
                        val result = AuthManager.signUp(username, email, password)
                        isLoading = false
                        when (result) {
                            is AuthResult.Success -> onSignUpSuccess(email)
                            is AuthResult.Error -> error = result.message
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black,
                ),
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Text(
                        "Sign Up",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF2A2A2A),
                )
                Text(
                    "  or  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF808080),
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF2A2A2A),
                )
            }

            Spacer(Modifier.height(20.dp))

            // Google sign-in
            GoogleSignInButton(
                label = "Sign up with Google",
                isLoading = isGoogleLoading,
                onClick = {
                    if (isGoogleLoading) return@GoogleSignInButton
                    isGoogleLoading = true
                    error = null
                    scope.launch {
                        val result = AuthManager.signInWithGoogle(context)
                        isGoogleLoading = false
                        when (result) {
                            is AuthResult.Success -> onGoogleSuccess()
                            is AuthResult.Error -> error = result.message
                        }
                    }
                },
            )

            // Error
            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE53935),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(28.dp))

            // Already have account? Sign in
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB0B0B0),
                )
                Text(
                    "Sign in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onBack() },
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: @Composable () -> Unit,
    imeAction: ImeAction = ImeAction.Next,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFFB0B0B0)) },
        singleLine = true,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        visualTransformation = visualTransformation,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = BlackSurface,
            unfocusedContainerColor = BlackSurface,
            focusedIndicatorColor = GoldPrimary,
            unfocusedIndicatorColor = Color(0xFF2A2A2A),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = GoldPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun GoogleSignInButton(
    label: String,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color(0xFF5F6368),
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Google "G" logo (multi-color, drawn as text for simplicity)
                Text(
                    "G",
                    color = Color(0xFF4285F4),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    label,
                    color = Color(0xFF1F1F1F),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
