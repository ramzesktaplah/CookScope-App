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
 * Onboarding preference step 4: favorite cuisines (multi-select).
 */
@Composable
fun OnboardingPreferenceCuisinesScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val draft by UserManager.draftPreferences.collectAsState()
    val options = listOf(
        "Italian", "Japanese", "Mexican", "Mediterranean", "Indian", "Thai",
        "Korean", "American", "Chinese", "French", "Vietnamese", "Spanish",
        "Moroccan", "Greek", "Brazilian", "Caribbean", "Turkish", "Lebanese",
        "Middle Eastern", "German",
    )
    val selected = draft.favoriteCuisines.toSet()

    OnboardingScaffold(
        progress = 0.8f,
        onContinue = onContinue,
        onBack = onBack,
    ) {
        SpeechBubble(
            text = "What cuisines do you love most?",
            highlighted = "cuisines",
        )
        Spacer(Modifier.height(8.dp))
        PreferenceHint("We'll use these to personalize recipe suggestions.")
        Spacer(Modifier.height(24.dp))
        PreferenceChipGrid(
            options = options,
            selected = selected,
            onToggle = { cuisine ->
                val updated = if (cuisine in selected) selected - cuisine else selected + cuisine
                UserManager.updateDraftPreferences(
                    draft.copy(favoriteCuisines = updated.toList())
                )
            },
        )
    }
}
