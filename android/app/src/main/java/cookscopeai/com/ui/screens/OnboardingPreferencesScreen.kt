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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.unit.dp
import cookscopeai.com.data.UserManager
import cookscopeai.com.ui.theme.GoldDeep
import cookscopeai.com.ui.theme.GoldPrimary
import cookscopeai.com.ui.theme.LightOnboardingTheme

/**
 * Onboarding Preferences screen — dietary tags (multi-select), skill
 * level (single-select), favorite cuisines (multi-select), allergies
 * (multi-select). Selections are saved via UserManager and become the
 * defaults on the Create Recipe screen and for AI recipe generation.
 *
 * Always renders in light mode.
 */
@Composable
fun OnboardingPreferencesScreen(
    onContinue: () -> Unit,
) {
    LightOnboardingTheme {
        val skillOptions = listOf("Beginner", "Amateur", "Intermediate", "Home Cook", "Professional", "Chef-Level")
        val dietaryOptions = listOf(
            "Vegetarian", "Vegan", "Gluten-Free", "Keto", "Paleo", "Pescatarian",
            "Low-Carb", "Dairy-Free", "Nut-Free", "Halal", "Kosher",
        )
        val cuisineOptions = listOf(
            "Italian", "Japanese", "Mexican", "Mediterranean", "Indian", "Thai",
            "Korean", "American", "Chinese", "French", "Vietnamese", "Spanish",
            "Moroccan", "Greek", "Brazilian", "Caribbean", "Turkish", "Lebanese",
            "Middle Eastern", "German",
        )
        val allergyOptions = listOf(
            "Peanuts", "Shellfish", "Dairy", "Eggs", "Soy", "Wheat", "Fish", "Tree Nuts",
        )

        var selectedSkill by remember { mutableStateOf("Beginner") }
        var selectedDietary by remember { mutableStateOf(setOf<String>()) }
        var selectedCuisines by remember { mutableStateOf(setOf<String>()) }
        var selectedAllergies by remember { mutableStateOf(setOf<String>()) }

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
                    "Your cooking preferences",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "We'll use these as defaults when generating recipes for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(28.dp))

                // Skill Level (single-select)
                PreferenceSectionTitle("Skill Level")
                Spacer(Modifier.height(10.dp))
                PreferenceOptionsGrid(
                    options = skillOptions,
                    selected = setOf(selectedSkill),
                    onToggle = { selectedSkill = it },
                    singleSelect = true,
                )

                Spacer(Modifier.height(24.dp))

                // Dietary Restrictions (multi-select)
                PreferenceSectionTitle("Dietary Restrictions")
                Spacer(Modifier.height(10.dp))
                PreferenceOptionsGrid(
                    options = dietaryOptions,
                    selected = selectedDietary,
                    onToggle = { tag ->
                        selectedDietary = togglePreferenceSet(selectedDietary, tag)
                    },
                )

                Spacer(Modifier.height(24.dp))

                // Favorite Cuisines (multi-select)
                PreferenceSectionTitle("Favorite Cuisines")
                Spacer(Modifier.height(10.dp))
                PreferenceOptionsGrid(
                    options = cuisineOptions,
                    selected = selectedCuisines,
                    onToggle = { c ->
                        selectedCuisines = togglePreferenceSet(selectedCuisines, c)
                    },
                )

                Spacer(Modifier.height(24.dp))

                // Allergies (multi-select)
                PreferenceSectionTitle("Allergies")
                Spacer(Modifier.height(10.dp))
                PreferenceOptionsGrid(
                    options = allergyOptions,
                    selected = selectedAllergies,
                    onToggle = { a ->
                        selectedAllergies = togglePreferenceSet(selectedAllergies, a)
                    },
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        UserManager.updatePreferences(
                            UserManager.UserPreferences(
                                dietaryTags = selectedDietary.toList(),
                                skillLevel = selectedSkill,
                                favoriteCuisines = selectedCuisines.toList(),
                                allergies = selectedAllergies.toList(),
                            )
                        )
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
internal fun PreferenceSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = GoldDeep,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun PreferenceOptionsGrid(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    singleSelect: Boolean = false,
) {
    Column {
        options.forEach { option ->
            val isSelected = option in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) GoldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) GoldPrimary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onToggle(option) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onBackground,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

internal fun togglePreferenceSet(set: Set<String>, item: String): Set<String> {
    val mutable = set.toMutableSet()
    if (item in mutable) mutable.remove(item) else mutable.add(item)
    return mutable
}
