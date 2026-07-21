package cookscopeai.com.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cookscopeai.com.data.UserManager
import cookscopeai.com.ui.theme.GoldPrimary

/**
 * In-app Preferences screen. Lets users update their dietary tags, skill
 * level, favorite cuisines, and allergies. Values are seeded from
 * [UserManager.preferences] and written back to it on save.
 *
 * Uses the same multi-select options as onboarding so users can edit
 * their choices consistently.
 */
@Composable
fun PreferencesScreen(onBack: () -> Unit) {
    val saved by UserManager.preferences.collectAsState()
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

    var selectedSkill by remember(saved) { mutableStateOf(saved.skillLevel) }
    var selectedDietary by remember(saved) { mutableStateOf(saved.dietaryTags.toSet()) }
    var selectedCuisines by remember(saved) { mutableStateOf(saved.favoriteCuisines.toSet()) }
    var selectedAllergies by remember(saved) { mutableStateOf(saved.allergies.toSet()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 4.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                "Preferences",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Update your cooking preferences",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            PreferenceSectionTitle("Skill Level")
            Spacer(Modifier.height(10.dp))
            skillOptions.forEach { option ->
                PreferenceOptionCard(
                    label = option,
                    isSelected = option == selectedSkill,
                    onClick = { selectedSkill = option },
                )
            }

            Spacer(Modifier.height(24.dp))

            PreferenceSectionTitle("Dietary Restrictions")
            Spacer(Modifier.height(10.dp))
            PreferenceChipGrid(
                options = dietaryOptions,
                selected = selectedDietary,
                onToggle = { tag ->
                    selectedDietary = if (tag in selectedDietary) selectedDietary - tag else selectedDietary + tag
                },
            )

            Spacer(Modifier.height(24.dp))

            PreferenceSectionTitle("Favorite Cuisines")
            Spacer(Modifier.height(10.dp))
            PreferenceChipGrid(
                options = cuisineOptions,
                selected = selectedCuisines,
                onToggle = { cuisine ->
                    selectedCuisines = if (cuisine in selectedCuisines) selectedCuisines - cuisine else selectedCuisines + cuisine
                },
            )

            Spacer(Modifier.height(24.dp))

            PreferenceSectionTitle("Allergies")
            Spacer(Modifier.height(10.dp))
            PreferenceChipGrid(
                options = allergyOptions,
                selected = selectedAllergies,
                onToggle = { allergy ->
                    selectedAllergies = if (allergy in selectedAllergies) selectedAllergies - allergy else selectedAllergies + allergy
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
                    onBack()
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
                    "Save Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
