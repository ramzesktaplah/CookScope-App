package cookscopeai.com.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cookscopeai.com.R
import cookscopeai.com.ui.theme.GoldPrimary
import cookscopeai.com.utilities.openUrl

private const val TERMS_URL = "https://cookscope.ai/terms"
private const val PRIVACY_URL = "https://cookscope.ai/privacy"

/**
 * Welcome screen — the first screen shown to unauthenticated users.
 * Phantom-style layout: centered hero, bold headline, clean CTAs, and
 * legal links. Uses the regular app theme (light or dark) so it respects
 * the user's system preference.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(80.dp))

            // Brand mark at top center
            Image(
                painter = painterResource(R.drawable.ic_brand_logo),
                contentDescription = "CookScope",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp),
            )

            Spacer(Modifier.height(48.dp))

            // Hero image — large centered logo
            Image(
                painter = painterResource(R.drawable.ic_brand_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(200.dp),
            )

            Spacer(Modifier.height(40.dp))

            // Subtle social-proof tagline
            Text(
                "Your AI culinary companion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            // Bold headline
            Text(
                "Your home for cooking, recipes, and more",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            // Legal footer with clickable links
            val annotated = buildAnnotatedString {
                append("By continuing you agree to our ")
                pushStringAnnotation(tag = "terms", annotation = TERMS_URL)
                withStyle(
                    SpanStyle(
                        color = GoldPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                ) {
                    append("Terms & Conditions")
                }
                pop()
                append(" and ")
                pushStringAnnotation(tag = "privacy", annotation = PRIVACY_URL)
                withStyle(
                    SpanStyle(
                        color = GoldPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                ) {
                    append("Privacy Policy")
                }
                pop()
                append(".")
            }
            Text(
                text = annotated,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier
                    .clickable {
                        annotated.getStringAnnotations("terms", 0, annotated.length)
                            .firstOrNull()?.let { openUrl(context, it.item) }
                        annotated.getStringAnnotations("privacy", 0, annotated.length)
                            .firstOrNull()?.let { openUrl(context, it.item) }
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )

            Spacer(Modifier.height(8.dp))

            // Primary CTA
            Button(
                onClick = onGetStarted,
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
                    "Get Started",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Secondary sign-in link
            Text(
                "I already have an account",
                style = MaterialTheme.typography.titleMedium,
                color = GoldPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onSignIn)
                    .padding(vertical = 8.dp),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
