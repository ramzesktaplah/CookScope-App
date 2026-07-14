package com.rork.cookscope.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rork.cookscope.data.RecipeData
import com.rork.cookscope.data.RecipeGenerationService
import com.rork.cookscope.ui.components.SmoothInput
import com.rork.cookscope.ui.theme.BlackBorder
import com.rork.cookscope.ui.theme.BlackCard
import com.rork.cookscope.ui.theme.GoldPrimary
import com.rork.cookscope.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreateRecipeScreen(onRecipeGenerated: (String) -> Unit) {
    val ingredientList = remember { mutableStateListOf<String>() }
    var selectedCuisine by remember { mutableStateOf("Any Cuisine") }
    var selectedDishType by remember { mutableStateOf("Any Dish") }
    var selectedSkill by remember { mutableStateOf("Amateur") }
    var selectedMode by remember { mutableStateOf("Non-Vegan") }
    var isGenerating by remember { mutableStateOf(false) }
    var isPremium by remember { mutableStateOf(false) }
    var suggestionSeed by remember { mutableStateOf(0) }
    var generateError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val generationService = remember { RecipeGenerationService() }

    val allSuggestedIngredients = listOf(
        "Shrimp", "Cod", "Lamb", "Dill", "White Wine", "Leeks", "Egg", "Rice",
        "Basil", "Chocolate", "Chicken Breast", "Wild Mushroom", "Garlic", "Onion",
        "Tomato", "Spinach", "Avocado", "Pasta", "Cream", "Soy Sauce", "Honey",
        "Ginger", "Lemon", "Cilantro", "Thyme", "Rosemary", "Parmesan", "Bacon",
        "Ground Beef", "Pork Belly", "Tofu", "Chickpeas", "Coconut Milk", "Bell Pepper",
    )

    val suggestedIngredients = remember(suggestionSeed) {
        allSuggestedIngredients.shuffled().take(10)
    }

    val cuisines = listOf(
        "Any Cuisine", "Italian", "Japanese", "Mexican", "Mediterranean", "Indian", "Thai",
        "Korean", "American", "Chinese", "French", "Vietnamese", "Middle Eastern",
        "Brazilian", "Spanish", "Greek", "Moroccan", "Turkish", "Caribbean", "German",
    )
    val dishTypes = listOf(
        "Any Dish", "Main Course", "Appetizer", "Dessert", "Breakfast", "Salad", "Soup",
        "Snack", "Side Dish", "Beverage", "Bread", "Sandwich", "Pizza", "Pasta Dish",
        "Curry", "Stew", "Stir-Fry", "Grill/BBQ", "Seafood Plate", "Bowl",
    )
    val skills = listOf("Amateur", "Intermediate", "Professional", "Home Cook", "Beginner", "Chef-Level")
    val modes = listOf(
        "Non-Vegan", "Vegan", "Vegetarian", "Gluten-Free", "Keto", "Paleo", "Low-Carb",
        "Dairy-Free", "Nut-Free", "Pescatarian", "Halal", "Kosher", "High-Protein", "Whole30",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Available ingredients — first item, no header above it
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Spacer(Modifier.height(48.dp))
                SectionLabel("Available Ingredients", "Separate ingredients by commas or a line")
                Spacer(Modifier.height(12.dp))
                SmoothInput(
                    onSend = { input ->
                        input.split(",", "\n")
                            .map { it.trim() }
                            .filter { it.isNotBlank() && it !in ingredientList }
                            .forEach { ingredientList.add(it) }
                    },
                    placeholder = "eg. chicken breast, wild mushroom etc.",
                    singleLine = true,
                    imeAction = ImeAction.Done,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Click any ingredient to add it to your list",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    IconButton(
                        onClick = { suggestionSeed++ },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh suggestions",
                            tint = GoldPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                SuggestedIngredientsFlow(
                    ingredients = suggestedIngredients,
                    onAdd = { ingredient ->
                        if (ingredient !in ingredientList) {
                            ingredientList.add(ingredient)
                        }
                    },
                )
                if (ingredientList.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ingredientList) { ingredient ->
                            IngredientChip(
                                text = ingredient,
                                onRemove = { ingredientList.remove(ingredient) },
                            )
                        }
                    }
                }
            }
        }

        // Cuisine + Dish Type dropdowns
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DropdownField(
                    label = "Cuisine Culture",
                    options = cuisines,
                    selected = selectedCuisine,
                    onSelect = { selectedCuisine = it },
                    modifier = Modifier.weight(1f),
                )
                DropdownField(
                    label = "Dish Type",
                    options = dishTypes,
                    selected = selectedDishType,
                    onSelect = { selectedDishType = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Skill + Mode dropdowns
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DropdownField(
                    label = "Recipe Skill Level",
                    options = skills,
                    selected = selectedSkill,
                    onSelect = { selectedSkill = it },
                    modifier = Modifier.weight(1f),
                )
                DropdownField(
                    label = "Mode",
                    options = modes,
                    selected = selectedMode,
                    onSelect = { selectedMode = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Generate buttons
        item {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Premium generate button
                Button(
                    onClick = {
                        if (!isGenerating && ingredientList.isNotEmpty()) {
                            isGenerating = true
                            isPremium = true
                            generateError = null
                            scope.launch {
                                val result = generationService.generateRecipe(
                                    ingredients = ingredientList.toList(),
                                    cuisine = selectedCuisine,
                                    dishType = selectedDishType,
                                    skillLevel = selectedSkill,
                                    dietaryMode = selectedMode,
                                    isPremium = true,
                                )
                                isGenerating = false
                                result.fold(
                                    onSuccess = { recipe ->
                                        RecipeData.addRecipe(recipe)
                                        onRecipeGenerated(recipe.id)
                                    },
                                    onFailure = { generateError = it.message ?: "Generation failed" }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.Black,
                    ),
                    enabled = !isGenerating && ingredientList.isNotEmpty(),
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Generate Premium Recipe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    "AI-crafted restaurant quality recipe with pro techniques",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Amateur generate button — label changes based on selected skill level
                Button(
                    onClick = {
                        if (!isGenerating && ingredientList.isNotEmpty()) {
                            isGenerating = true
                            isPremium = false
                            generateError = null
                            scope.launch {
                                val result = generationService.generateRecipe(
                                    ingredients = ingredientList.toList(),
                                    cuisine = selectedCuisine,
                                    dishType = selectedDishType,
                                    skillLevel = selectedSkill,
                                    dietaryMode = selectedMode,
                                    isPremium = false,
                                )
                                isGenerating = false
                                result.fold(
                                    onSuccess = { recipe ->
                                        RecipeData.addRecipe(recipe)
                                        onRecipeGenerated(recipe.id)
                                    },
                                    onFailure = { generateError = it.message ?: "Generation failed" }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = GoldPrimary,
                    ),
                    enabled = !isGenerating && ingredientList.isNotEmpty(),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = GoldPrimary,
                    ),
                ) {
                    Text(
                        "Generate $selectedSkill Recipe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                // Error message
                if (generateError != null) {
                    Text(
                        generateError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE53935),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Empty ingredients hint
                if (ingredientList.isEmpty() && !isGenerating) {
                    Text(
                        "Add at least one ingredient to generate a recipe",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
        }

        // Generating overlay
        if (isGenerating) {
            item {
                GeneratingOverlay(
                    ingredients = ingredientList.toList(),
                    cuisine = selectedCuisine,
                    skill = selectedSkill,
                    premium = isPremium,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
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
            color = TextSecondary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestedIngredientsFlow(
    ingredients: List<String>,
    onAdd: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ingredients.forEach { ingredient ->
            SuggestedChip(
                text = ingredient,
                onClick = { onAdd(ingredient) },
            )
        }
    }
}

@Composable
private fun SuggestedChip(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BlackCard)
            .border(1.dp, BlackBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun IngredientChip(text: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GoldPrimary.copy(alpha = 0.15f))
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = GoldPrimary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(4.dp))
        androidx.compose.material3.IconButton(
            onClick = onRemove,
            modifier = Modifier.size(20.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = GoldPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = BlackCard,
                unfocusedContainerColor = BlackCard,
                focusedIndicatorColor = GoldPrimary,
                unfocusedIndicatorColor = BlackBorder,
                focusedLabelColor = GoldPrimary,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(BlackCard)
                .heightIn(max = 260.dp),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (option == selected) GoldPrimary else Color.White,
                            fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    colors = MenuItemColors(),
                )
            }
        }
    }
}

@Composable
private fun MenuItemColors() = androidx.compose.material3.MenuItemColors(
    textColor = Color.White,
    leadingIconColor = Color.White,
    trailingIconColor = Color.White,
    disabledTextColor = TextSecondary,
    disabledLeadingIconColor = TextSecondary,
    disabledTrailingIconColor = TextSecondary,
)

@Composable
private fun GeneratingOverlay(
    ingredients: List<String>,
    cuisine: String,
    skill: String,
    premium: Boolean,
) {
    var step by remember { mutableStateOf(0) }
    val steps = listOf(
        "Analyzing your ingredients...",
        "Selecting flavor pairings...",
        "Crafting the recipe...",
        "Adding cooking techniques...",
        "Finalizing your recipe...",
    )

    LaunchedEffect(Unit) {
        while (step < steps.size - 1) {
            delay(800)
            step++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = GoldPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(20.dp))
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    steps[step],
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (premium) "Premium • $cuisine • $skill" else "$skill • $cuisine",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ingredients.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    ingredients.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
