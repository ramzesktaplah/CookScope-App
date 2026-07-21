package cookscopeai.com.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cookscopeai.com.data.AuthManager
import cookscopeai.com.data.AuthResult
import cookscopeai.com.data.UserManager
import cookscopeai.com.ui.theme.GoldPrimary
import kotlinx.coroutines.launch

/**
 * Verify Email screen — shown after sign up. The user must confirm their
 * email via the Supabase link flow. The deep link returns to the app and
 * the session becomes Authenticated automatically; this screen then moves
 * to the paywall.
 *
 * Dialog behavior:
 *  - "Yes" (I've confirmed): if the session is not yet authenticated, show a
 *    message asking the user to confirm and stay on this screen. Once
 *    authenticated, the app proceeds automatically.
 *  - "No" (Cancel session): show a warning that preferences will not be
 *    saved. If the user confirms, discard the draft preferences and return
 *    to the Welcome screen. If they decline, keep them on this screen.
 *
 * Uses the regular app theme (light or dark).
 */
@Composable
fun VerifyEmailScreen(
    email: String,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onCancelSession: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isAuthenticated by AuthManager.isAuthenticatedFlow.collectAsState()
    var isResending by remember { mutableStateOf(false) }
    var resendMessage by remember { mutableStateOf<String?>(null) }
    var showNotConfirmedDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Auto-advance once the email link confirms the session.
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            UserManager.commitDraftPreferences()
            onContinue()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Check your inbox",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "We sent a confirmation link to",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                email,
                style = MaterialTheme.typography.titleMedium,
                color = GoldPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Tap the link in the email to confirm your account. " +
                    "You'll return here automatically once confirmed, and then we'll sign you in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    if (!AuthManager.isAuthenticated) {
                        showNotConfirmedDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    "Yes, I've confirmed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = { showDiscardDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GoldPrimary,
                ),
            ) {
                Text(
                    "No, cancel session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Resend
            OutlinedButton(
                onClick = {
                    if (isResending) return@OutlinedButton
                    isResending = true
                    resendMessage = null
                    scope.launch {
                        val result = AuthManager.resendConfirmation(email)
                        isResending = false
                        resendMessage = when (result) {
                            is AuthResult.Success -> "Confirmation email resent. Check your inbox."
                            is AuthResult.Error -> result.message
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GoldPrimary,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = GoldPrimary.copy(alpha = 0.6f),
                ),
                enabled = !isResending,
            ) {
                if (isResending) {
                    CircularProgressIndicator(
                        color = GoldPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        "Resend email",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (resendMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    resendMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (resendMessage!!.startsWith("Confirmation")) GoldPrimary else Color(0xFFE53935),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Didn't receive an email? Check your spam folder, or wait a minute and resend.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showNotConfirmedDialog) {
        AlertDialog(
            onDismissRequest = { showNotConfirmedDialog = false },
            confirmButton = {
                TextButton(onClick = { showNotConfirmedDialog = false }) {
                    Text("OK", color = GoldPrimary)
                }
            },
            title = {
                Text(
                    "Please confirm your email",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    "We haven't detected a confirmed email yet. Please tap the link in your inbox. " +
                        "Don't close this screen; once confirmed, you'll be signed in automatically."
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        UserManager.clearDraftPreferences()
                        onCancelSession()
                    }
                ) {
                    Text("Yes, discard", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("No, keep me here", color = GoldPrimary)
                }
            },
            title = {
                Text(
                    "Your preferences won't be saved",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    "If you cancel now, the taste profile you just built won't be saved. " +
                        "Are you sure you want to go back?"
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
