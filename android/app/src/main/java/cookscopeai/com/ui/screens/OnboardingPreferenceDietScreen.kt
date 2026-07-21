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
 * Onboarding preference step 2: dietary restrictions (multi-select).
 */
@Composable
fun OnboardingPreferenceDietScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val draft by UserManager.draftPreferences.collectAsState()
    val options = listOf(
        "Vegetarian", "Vegan", "Gluten-Free", "Keto", "Paleo", "Pescatarian",
        "Low-Carb", "Dairy-Free", "Nut-Free", "Halal", "Kosher",
    )
    val selected = draft.dietaryTags.toSet()

    OnboardingScaffold(
        progress = 0.4f,
        onContinue = onContinue,
        onBack = onBack,
    ) {
        SpeechBubble(
            text = "Do you follow any dietary preferences?",
            highlighted = "dietary preferences",
        )
        Spacer(Modifier.height(8.dp))
        PreferenceHint("Select all that apply. You can change these later in Settings.")
        Spacer(Modifier.height(24.dp))
        PreferenceChipGrid(
            options = options,
            selected = selected,
            onToggle = { tag ->
                val updated = if (tag in selected) selected - tag else selected + tag
                UserManager.updateDraftPreferences(
                    draft.copy(dietaryTags = updated.toList())
                )
            },
        )
    }
}
