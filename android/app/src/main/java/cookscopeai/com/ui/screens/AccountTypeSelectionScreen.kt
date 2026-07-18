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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cookscopeai.com.data.UserManager
import cookscopeai.com.data.model.UserRole
import cookscopeai.com.ui.theme.GoldDeep
import cookscopeai.com.ui.theme.GoldPrimary
import cookscopeai.com.ui.theme.LightOnboardingTheme

/**
 * Account Type selection — Chef/Teacher or Learner.
 *
 * The choice is permanent: after the user taps Continue, UserManager
 * stores the role, locks it (role_locked = true), and pushes it to the
 * profiles table. The role selector in EditProfile will be read-only
 * after this.
 *
 * Always renders in light mode.
 */
@Composable
fun AccountTypeSelectionScreen(
    onContinue: () -> Unit,
) {
    LightOnboardingTheme {
        var selectedRole by remember { mutableStateOf<UserRole?>(null) }

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
            ) {
                Spacer(Modifier.height(56.dp))

                Text(
                    "Choose your account type",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "This will personalize your CookScope experience.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(32.dp))

                // Chef / Teacher
                RoleCard(
                    title = "Chef / Teacher",
                    description = "Post cooking content, share recipes, and receive Stars from learners.",
                    icon = Icons.Default.Restaurant,
                    isSelected = selectedRole == UserRole.CHEF_TEACHER,
                    onClick = { selectedRole = UserRole.CHEF_TEACHER },
                )

                Spacer(Modifier.height(14.dp))

                // Learner
                RoleCard(
                    title = "Learner",
                    description = "Watch, learn, give Stars to chefs you love, and discover new recipes.",
                    icon = Icons.Default.Book,
                    isSelected = selectedRole == UserRole.LEARNER,
                    onClick = { selectedRole = UserRole.LEARNER },
                )

                Spacer(Modifier.height(24.dp))

                // Permanent note
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = GoldDeep,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "This choice is permanent and cannot be changed later.",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldDeep,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        val role = selectedRole ?: return@Button
                        UserManager.updateProfile(role = role)
                        UserManager.setRoleLocked()
                        onContinue()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black,
                    ),
                    enabled = selectedRole != null,
                ) {
                    Text(
                        "Continue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSelected) GoldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) GoldPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
