package cookscopeai.com.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cookscopeai.com.data.UserManager
import cookscopeai.com.ui.theme.GoldPrimary

@Composable
fun PreferencesScreen(onBack: () -> Unit) {
    val profile by UserManager.profile.collectAsState()

    var skillLevel by remember { mutableStateOf("Intermediate") }
    var dietaryTag by remember { mutableStateOf("None") }
    var favoriteCuisine by remember { mutableStateOf("Italian") }
    var allergies by remember { mutableStateOf("None") }

    val skillOptions = listOf("Beginner", "Amateur", "Intermediate", "Home Cook", "Professional", "Chef-Level")
    val dietaryOptions = listOf("None", "Vegetarian", "Vegan", "Gluten-Free", "Keto", "Paleo", "Pescatarian", "Low-Carb", "Dairy-Free", "Nut-Free", "Halal", "Kosher")
    val cuisineOptions = listOf("Italian", "Japanese", "Mexican", "Mediterranean", "Indian", "Thai", "Korean", "American", "Chinese", "French", "Vietnamese", "Spanish", "Moroccan", "Greek", "Brazilian", "Caribbean", "Turkish", "Lebanese", "Middle Eastern", "German")
    val allergyOptions = listOf("None", "Peanuts", "Shellfish", "Dairy", "Eggs", "Soy", "Wheat", "Fish", "Tree Nuts")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Top bar
        item {
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
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PreferenceSelector(
                    title = "Skill Level",
                    subtitle = "Your cooking expertise",
                    options = skillOptions,
                    selected = skillLevel,
                    onSelect = { skillLevel = it },
                )
                PreferenceSelector(
                    title = "Dietary Restrictions",
                    subtitle = "Foods you avoid",
                    options = dietaryOptions,
                    selected = dietaryTag,
                    onSelect = { dietaryTag = it },
                )
                PreferenceSelector(
                    title = "Favorite Cuisine",
                    subtitle = "Your preferred cooking style",
                    options = cuisineOptions,
                    selected = favoriteCuisine,
                    onSelect = { favoriteCuisine = it },
                )
                PreferenceSelector(
                    title = "Allergies",
                    subtitle = "Ingredients to avoid",
                    options = allergyOptions,
                    selected = allergies,
                    onSelect = { allergies = it },
                )
            }
        }
    }
}

@Composable
private fun PreferenceSelector(
    title: String,
    subtitle: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (option == selected) GoldPrimary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = if (option == selected) 1.5.dp else 0.dp,
                        color = if (option == selected) GoldPrimary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (option == selected) GoldPrimary else MaterialTheme.colorScheme.onBackground,
                    fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (option == selected) {
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
