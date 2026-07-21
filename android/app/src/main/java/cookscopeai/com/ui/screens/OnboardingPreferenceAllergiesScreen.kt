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
 * Onboarding preference step 3: allergies (multi-select).
 */
@Composable
fun OnboardingPreferenceAllergiesScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val draft by UserManager.draftPreferences.collectAsState()
    val options = listOf(
        "Peanuts", "Shellfish", "Dairy", "Eggs", "Soy", "Wheat", "Fish", "Tree Nuts",
    )
    val selected = draft.allergies.toSet()

    OnboardingScaffold(
        progress = 0.6f,
        onContinue = onContinue,
        onBack = onBack,
    ) {
        SpeechBubble(
            text = "Any allergies we should know about?",
            highlighted = "allergies",
        )
        Spacer(Modifier.height(8.dp))
        PreferenceHint("This helps us keep your recipes safe. Skip if none apply.")
        Spacer(Modifier.height(24.dp))
        PreferenceChipGrid(
            options = options,
            selected = selected,
            onToggle = { allergy ->
                val updated = if (allergy in selected) selected - allergy else selected + allergy
                UserManager.updateDraftPreferences(
                    draft.copy(allergies = updated.toList())
                )
            },
        )
    }
}
