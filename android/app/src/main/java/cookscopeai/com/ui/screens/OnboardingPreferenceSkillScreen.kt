package cookscopeai.com.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cookscopeai.com.data.UserManager

/**
 * Onboarding preference step 1: cooking skill level (single-select).
 */
@Composable
fun OnboardingPreferenceSkillScreen(
    onContinue: () -> Unit,
) {
    val draft by UserManager.draftPreferences.collectAsState()
    val options = listOf(
        "Beginner",
        "Amateur",
        "Intermediate",
        "Home Cook",
        "Professional",
        "Chef-Level",
    )
    val selectedSkill = draft.skillLevel

    OnboardingScaffold(
        progress = 0.2f,
        onContinue = onContinue,
        continueEnabled = selectedSkill.isNotBlank(),
    ) {
        SpeechBubble(
            text = "How would you describe your cooking skill?",
            highlighted = "cooking skill",
        )
        Spacer(Modifier.height(24.dp))
        options.forEach { option ->
            PreferenceOptionCard(
                label = option,
                isSelected = option == selectedSkill,
                onClick = {
                    UserManager.updateDraftPreferences(
                        draft.copy(skillLevel = option)
                    )
                },
            )
        }
    }
}
