package com.rork.cookscope.data

import com.rork.cookscope.data.model.CookStep
import com.rork.cookscope.data.model.Cuisine
import com.rork.cookscope.data.model.Difficulty
import com.rork.cookscope.data.model.Ingredient
import com.rork.cookscope.data.model.IngredientCategory
import com.rork.cookscope.data.model.Recipe

/**
 * Curated seed recipe library — represents the AI-generated recipe collection.
 * Uses high-quality food photography from Unsplash.
 */
object RecipeData {

    // Dynamic store for AI-generated recipes
    private val dynamicRecipes = mutableMapOf<String, Recipe>()

    // Cache for recipes fetched from recipeapi.io
    private val apiRecipes = mutableMapOf<String, Recipe>()

    fun addRecipe(recipe: Recipe) {
        dynamicRecipes[recipe.id] = recipe
    }

    /**
     * Cache recipes fetched from the recipe API.
     */
    fun setApiRecipes(recipes: List<Recipe>) {
        recipes.forEach { recipe ->
            apiRecipes[recipe.id] = recipe
        }
    }

    /**
     * Add or update a single API recipe in the cache.
     */
    fun addApiRecipe(recipe: Recipe) {
        apiRecipes[recipe.id] = recipe
    }

    /**
     * All recipes from the API cache.
     */
    val apiCached: List<Recipe> get() = apiRecipes.values.toList()

    val cuisines: List<Cuisine> = listOf(
        Cuisine("Italian", "\uD83C\uDDE9\uD83C\uDDF0", "Pasta, risotto & rustic classics"),
        Cuisine("Japanese", "\uD83C\uDDEF\uD83C\uDDF5", "Sushi, ramen & refined techniques"),
        Cuisine("Mexican", "\uD83C\uDDF2\uD83C\uDDFD", "Bold, vibrant & chili-forward"),
        Cuisine("Mediterranean", "\uD83C\uDFD7️", "Olive oil, herbs & sun-kissed veg"),
        Cuisine("Indian", "\uD83C\uDDEE\uD83C\uDDF3", "Layered spices & rich curries"),
        Cuisine("Thai", "\uD83C\uDDF9\uD83C\uDDED", "Balance of sweet, sour & heat"),
        Cuisine("Korean", "\uD83C\uDDF0\uD83C\uDDF7", "Fermented depth & BBQ culture"),
        Cuisine("American", "\uD83C\uDDFA\uD83C\uDDF8", "Comfort food & backyard classics"),
    )

    val recipes: List<Recipe> = listOf(
        Recipe(
            id = "r1",
            title = "Wild Mushroom Risotto with Truffle",
            description = "Creamy carnaroli risotto layered with porcini, shiitake, and a drizzle of black truffle oil. A restaurant-level dish that teaches you the art of patient stirring.",
            cuisine = "Italian",
            difficulty = Difficulty.MEDIUM,
            cookTimeMin = 45,
            servings = 4,
            caloriesPerServing = 520,
            proteinG = 14,
            carbsG = 62,
            fatG = 22,
            imageUrl = "https://images.unsplash.com/photo-1476124369496-e5bda7f4d1a1?w=800&q=80",
            tags = listOf("Vegetarian", "Comfort", "Date Night"),
            ingredients = listOf(
                Ingredient("Carnaroli rice", "1.5 cups", IngredientCategory.GRAIN),
                Ingredient("Mixed wild mushrooms", "300g", IngredientCategory.PRODUCE),
                Ingredient("Dried porcini", "20g", IngredientCategory.PANTRY),
                Ingredient("Vegetable stock", "1.2L, warm", IngredientCategory.PANTRY),
                Ingredient("Shallots, fine dice", "2", IngredientCategory.PRODUCE),
                Ingredient("Dry white wine", "120ml", IngredientCategory.PANTRY),
                Ingredient("Parmesan, grated", "60g", IngredientCategory.DAIRY),
                Ingredient("Butter", "40g", IngredientCategory.DAIRY),
                Ingredient("Black truffle oil", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Fresh thyme", "2 sprigs", IngredientCategory.PRODUCE),
            ),
            steps = listOf(
                CookStep(1, "Bloom the porcini", "Soak dried porcini in 150ml hot water for 15 minutes. Strain and reserve both the mushrooms and the soaking liquid.", 15, technique = "Bloom", tip = "The soaking liquid is liquid gold — don't discard it."),
                CookStep(2, "Sear the mushrooms", "Sear fresh mushrooms in a dry hot pan until they release their moisture and brown deeply, about 6 minutes. Season with salt. Set aside.", 6, technique = "Sear", tip = "Crowding the pan steams instead of sears — work in batches if needed."),
                CookStep(3, "Build the soffritto", "Sweat shallots in butter over medium-low heat until translucent and fragrant, about 5 minutes. Add garlic and thyme for 30 seconds.", 5, technique = "Sweat"),
                CookStep(4, "Toast the rice", "Add carnaroli rice to the soffritto. Toast, stirring constantly, until edges turn translucent and rice smells nutty, about 2 minutes.", 2, technique = "Toast", tip = "Toasting seals the starch grain and prevents gumminess."),
                CookStep(5, "Deglaze with wine", "Pour in white wine and stir until fully absorbed. The alcohol cooks off and leaves bright acidity.", 3, technique = "Deglaze"),
                CookStep(6, "Stock & stir", "Add hot stock one ladle at a time, stirring almost continuously. Wait until each ladle is absorbed before adding the next. Incorporate porcini liquid after the third ladle.", 20, technique = "Mantecatura", tip = "The starch released by stirring creates the signature creamy texture — no cream needed."),
                CookStep(7, "Finish & rest", "Off heat, fold in seared mushrooms, parmesan, remaining butter, and a drizzle of truffle oil. Cover and rest 2 minutes. Plate and finish with more truffle oil and thyme.", 4, technique = "Mantecatura"),
            ),
            tips = listOf(
                "Carnaroli holds its shape better than arborio — worth seeking out.",
                "Risotto should flow like lava when plated — never stiff.",
                "Always taste for salt at the end; stock varies wildly in salinity.",
            ),
            rating = 4.9f,
            reviewCount = 1284,
            authorName = "Chef Lucia Romano",
            isPremium = true,
        ),
        Recipe(
            id = "r2",
            title = "Miso-Glazed Salmon with Charred Greens",
            description = "A Weeknight showstopper: caramelized miso-glazed salmon over charred bok choy with a yuzu-kosho aioli. On the table in 25 minutes.",
            cuisine = "Japanese",
            difficulty = Difficulty.EASY,
            cookTimeMin = 25,
            servings = 2,
            caloriesPerServing = 410,
            proteinG = 34,
            carbsG = 12,
            fatG = 24,
            imageUrl = "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=800&q=80",
            tags = listOf("Pescatarian", "High Protein", "30 Min"),
            ingredients = listOf(
                Ingredient("Salmon fillets, skin-on", "2 x 180g", IngredientCategory.PROTEIN),
                Ingredient("White miso paste", "3 tbsp", IngredientCategory.PANTRY),
                Ingredient("Mirin", "2 tbsp", IngredientCategory.PANTRY),
                Ingredient("Sake", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Brown sugar", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Baby bok choy", "4 heads", IngredientCategory.PRODUCE),
                Ingredient("Yuzu kosho", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Mayonnaise", "2 tbsp", IngredientCategory.PANTRY),
                Ingredient("Sesame oil", "1 tsp", IngredientCategory.PANTRY),
                Ingredient("Toasted sesame seeds", "for garnish", IngredientCategory.PANTRY),
            ),
            steps = listOf(
                CookStep(1, "Make the glaze", "Whisk miso, mirin, sake, and brown sugar until smooth and glossy. Reserve 1 tbsp for the greens.", 3),
                CookStep(2, "Marinate salmon", "Brush salmon fillets with the glaze. Let sit 10 minutes while you prep the greens.", 10, technique = "Marinate", tip = "Don't marinate longer than 20 minutes — miso can start to cure the fish."),
                CookStep(3, "Char the greens", "Sear bok choy cut-side down in a hot pan with a little oil until deeply charred, 3 minutes. Flip, add a splash of water, cover, and steam 2 minutes.", 5, technique = "Char"),
                CookStep(4, "Glaze the salmon", "Sear salmon skin-side down in a non-stick pan over medium-high heat, 4 minutes for crispy skin. Flip, reduce heat, brush with more glaze, and cook 3 minutes more.", 7, technique = "Pan-sear", tip = "Press the fillet flat for the first 30 seconds to ensure even skin contact."),
                CookStep(5, "Yuzu aioli", "Mix mayo, yuzu kosho, and sesame oil. Plate greens, top with salmon, drizzle aioli, and scatter sesame seeds.", 2),
            ),
            tips = listOf(
                "Ask for center-cut salmon fillets of equal thickness for even cooking.",
                "Yuzu kosho is a citrus-chili paste — find it at Japanese grocers or substitute lime zest + a pinch of chili flakes.",
                "The skin should shatter like glass when done — medium-high heat is the sweet spot.",
            ),
            rating = 4.8f,
            reviewCount = 892,
            authorName = "Chef Kenji Tanaka",
        ),
        Recipe(
            id = "r3",
            title = "Slow-Braised Short Rib Tacos",
            description = "Red-wine-braised short ribs shreddable with a fork, tucked into charred corn tortillas with pickled red onion and cilantro-lime crema.",
            cuisine = "Mexican",
            difficulty = Difficulty.HARD,
            cookTimeMin = 240,
            servings = 6,
            caloriesPerServing = 580,
            proteinG = 38,
            carbsG = 28,
            fatG = 36,
            imageUrl = "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=800&q=80",
            tags = listOf("Crowd Pleaser", "Weekend Project", "Make Ahead"),
            ingredients = listOf(
                Ingredient("Bone-in short ribs", "1.5kg", IngredientCategory.PROTEIN),
                Ingredient("Dried guajillo chilies", "3", IngredientCategory.SPICE),
                Ingredient("Dried ancho chilies", "2", IngredientCategory.SPICE),
                Ingredient("Red wine", "750ml", IngredientCategory.PANTRY),
                Ingredient("Beef stock", "500ml", IngredientCategory.PANTRY),
                Ingredient("Yellow onions, rough chop", "2", IngredientCategory.PRODUCE),
                Ingredient("Garlic", "8 cloves", IngredientCategory.PRODUCE),
                Ingredient("Tomato paste", "3 tbsp", IngredientCategory.PANTRY),
                Ingredient("Corn tortillas", "18", IngredientCategory.GRAIN),
                Ingredient("Red onion, thinly sliced", "1", IngredientCategory.PRODUCE),
                Ingredient("Lime", "3", IngredientCategory.PRODUCE),
                Ingredient("Cilantro", "1 bunch", IngredientCategory.PRODUCE),
                Ingredient("Mexican crema", "150ml", IngredientCategory.DAIRY),
                Ingredient("Cumin seeds", "2 tsp", IngredientCategory.SPICE),
            ),
            steps = listOf(
                CookStep(1, "Rehydrate chilies", "Toast dried chilies in a dry pan 30 seconds per side. Cover with hot water and soak 15 minutes until soft.", 15, technique = "Toast & soak"),
                CookStep(2, "Sear the ribs", "Season ribs generously. Sear in a Dutch oven over high heat until deeply browned on all sides, 3 minutes per side. Work in batches.", 12, technique = "Sear", tip = "Browning = flavor. Don't rush this — aim for a dark mahogany crust."),
                CookStep(3, "Build the braising liquid", "Sauté onions in the fond until softened. Add garlic, cumin, and tomato paste; cook 2 minutes. Deglaze with wine, scraping the bottom.", 8, technique = "Deglaze", tip = "The browned bits on the pan are concentrated umami — get them all up."),
                CookStep(4, "Blend & strain sauce", "Blend rehydrated chilies with 200ml soaking liquid until smooth. Strain into the pot. Add beef stock. Return ribs.", 5),
                CookStep(5, "Braise low & slow", "Cover and braise in a 150°C / 300°F oven for 3 hours, or until the meat falls off the bone. The connective tissue must fully break down.", 180, technique = "Braise", tip = "If the liquid reduces too much, add water. The meat should be half-submerged."),
                CookStep(6, "Shred & reduce", "Remove ribs and shred the meat, discarding bones and fat. Strain the braising liquid and simmer on the stove until it coats a spoon. Toss meat with the reduced sauce.", 15, technique = "Reduce"),
                CookStep(7, "Pickled onion & crema", "Quick-pickle red onion in lime juice and salt for 10 minutes. Mix crema with lime zest and a pinch of salt.", 10),
                CookStep(8, "Char tortillas & build", "Char tortillas directly over the flame until blistered. Pile on shredded rib, pickled onion, cilantro, and a drizzle of crema. Serve with lime wedges.", 5, technique = "Char", tip = "Stack charred tortillas in a clean towel to keep them warm and pliable."),
            ),
            tips = listOf(
                "Braise the day before — the flavor deepens overnight and the fat is easier to remove when chilled.",
                "Don't skip the tortilla charring — it transforms the texture from doughy to smoky.",
                "Save leftover braising liquid; it freezes beautifully for future tacos or ramen bowls.",
            ),
            rating = 4.9f,
            reviewCount = 2105,
            authorName = "Chef Maria Santos",
            isPremium = true,
        ),
        Recipe(
            id = "r4",
            title = "Charred Eggplant with Tahini & Pomegranate",
            description = "Smoky whole-roasted eggplant draped in lemon-garlic tahini, jeweled with pomegranate seeds and toasted pine nuts. A Levantine classic.",
            cuisine = "Mediterranean",
            difficulty = Difficulty.EASY,
            cookTimeMin = 35,
            servings = 4,
            caloriesPerServing = 280,
            proteinG = 8,
            carbsG = 22,
            fatG = 18,
            imageUrl = "https://images.unsplash.com/photo-1625937329935-287441889b58?w=800&q=80",
            tags = listOf("Vegan", "Gluten-Free", "Light"),
            ingredients = listOf(
                Ingredient("Globe eggplants", "2 large", IngredientCategory.PRODUCE),
                Ingredient("Tahini paste", "4 tbsp", IngredientCategory.PANTRY),
                Ingredient("Lemon, juiced", "1", IngredientCategory.PRODUCE),
                Ingredient("Garlic, minced", "2 cloves", IngredientCategory.PRODUCE),
                Ingredient("Pomegranate seeds", "80g", IngredientCategory.PRODUCE),
                Ingredient("Pine nuts, toasted", "30g", IngredientCategory.PANTRY),
                Ingredient("Fresh parsley", "1 bunch", IngredientCategory.PRODUCE),
                Ingredient("Olive oil", "4 tbsp", IngredientCategory.PANTRY),
                Ingredient("Smoked paprika", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Sumac", "1 tsp", IngredientCategory.SPICE),
            ),
            steps = listOf(
                CookStep(1, "Char the eggplant", "Prick eggplants all over with a fork. Char directly over flame or under broiler, turning occasionally, until skin is blackened and flesh collapses, 20-25 minutes.", 25, technique = "Char", tip = "The skin must be fully black — that's where the smoky flavor lives."),
                CookStep(2, "Steam & peel", "Place charred eggplants in a covered bowl for 5 minutes. This loosens the skin. Peel carefully, keeping the flesh intact.", 5),
                CookStep(3, "Whisk the tahini", "Whisk tahini with lemon juice, garlic, and 3 tbsp water until silky and pourable. Season with salt.", 3, technique = "Emulsify", tip = "Tahini seizes when you add liquid — keep whisking; it will come back smooth."),
                CookStep(4, "Plate & finish", "Lay eggplant on a platter. Drizzle with olive oil and tahini sauce. Scatter pomegranate, pine nuts, parsley, smoked paprika, and sumac.", 2),
            ),
            tips = listOf(
                "Use large globe eggplants — the slim Japanese variety don't yield enough flesh.",
                "Toast pine nuts in a dry pan until just golden; they go from perfect to burnt in seconds.",
                "Pomegranate seeds can be frozen — spread on a tray, then bag once solid.",
            ),
            rating = 4.7f,
            reviewCount = 645,
            authorName = "Chef Yara Kassem",
        ),
        Recipe(
            id = "r5",
            title = "Butter Chicken with Saffron Rice",
            description = "North-Indian classic: tandoori-spiced chicken simmered in a velvety tomato-cream gravy with fenugreek and toasted spices. Served over fragrant saffron basmati.",
            cuisine = "Indian",
            difficulty = Difficulty.MEDIUM,
            cookTimeMin = 60,
            servings = 4,
            caloriesPerServing = 640,
            proteinG = 36,
            carbsG = 58,
            fatG = 28,
            imageUrl = "https://images.unsplash.com/photo-1603894584373-5d82c93bd819?w=800&q=80",
            tags = listOf("Crowd Pleaser", "Comfort", "Family"),
            ingredients = listOf(
                Ingredient("Boneless chicken thighs", "800g", IngredientCategory.PROTEIN),
                Ingredient("Greek yogurt", "150g", IngredientCategory.DAIRY),
                Ingredient("Garam masala", "2 tsp", IngredientCategory.SPICE),
                Ingredient("Ground turmeric", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Kashmiri chili powder", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Ginger-garlic paste", "3 tbsp", IngredientCategory.PANTRY),
                Ingredient("Tomato puree", "500g", IngredientCategory.PANTRY),
                Ingredient("Heavy cream", "200ml", IngredientCategory.DAIRY),
                Ingredient("Dried fenugreek leaves (kasuri methi)", "1 tbsp", IngredientCategory.SPICE),
                Ingredient("Butter", "50g", IngredientCategory.DAIRY),
                Ingredient("Basmati rice", "2 cups", IngredientCategory.GRAIN),
                Ingredient("Saffron threads", "1 pinch", IngredientCategory.SPICE),
                Ingredient("Fresh cilantro", "1 bunch", IngredientCategory.PRODUCE),
                Ingredient("Green cardamom pods", "4", IngredientCategory.SPICE),
                Ingredient("Cinnamon stick", "1", IngredientCategory.SPICE),
            ),
            steps = listOf(
                CookStep(1, "Marinate the chicken", "Combine yogurt, half the ginger-garlic paste, 1 tsp garam masala, turmeric, and chili powder. Coat chicken thighs and marinate at least 30 minutes (overnight is best).", 30, technique = "Marinate", tip = "Kashmiri chili gives vivid red color without much heat — substitute paprika + a pinch of cayenne if needed."),
                CookStep(2, "Cook the rice", "Rinse basmati until water runs clear. Steep saffron in 2 tbsp warm milk. Toast cardamom and cinnamon in ghee, add rice and 3 cups water. Cook 15 minutes, fold in saffron milk, rest covered 10 minutes.", 25, technique = "Steamed rice", tip = "Rinsing removes surface starch for distinct, fluffy grains."),
                CookStep(3, "Sear the chicken", "Sear marinated chicken in a hot pan or grill until charred edges form, 4-5 minutes. Don't cook through — it finishes in the sauce.", 5, technique = "Sear"),
                CookStep(4, "Build the gravy", "Melt butter, sauté remaining ginger-garlic paste 1 minute. Add tomato puree and simmer 10 minutes until it darkens and oil separates.", 10, technique = "Bhunao", tip = "When oil separates from the tomato, the gravy base is ready — this is the key to authentic flavor."),
                CookStep(5, "Finish the curry", "Blend the gravy smooth. Return to pan with cream and chicken. Simmer 8 minutes until chicken is cooked through. Crush fenugreek between palms and stir in. Season.", 8, technique = "Simmer", tip = "Crushing kasuri methi releases its aroma — never skip this step."),
                CookStep(6, "Plate", "Spoon butter chicken over saffron rice. Garnish with a swirl of cream, chopped cilantro, and a knob of butter.", 2),
            ),
            tips = listOf(
                "Thighs beat breasts here — they stay juicy in the gravy.",
                "Kasuri methi is the secret ingredient — find it at Indian grocers or online.",
                "For a lighter version, use evaporated milk instead of cream; the texture stays silky.",
            ),
            rating = 4.8f,
            reviewCount = 1678,
            authorName = "Chef Priya Anand",
        ),
        Recipe(
            id = "r6",
            title = "Pad See Ew with Crispy Pork",
            description = "Bangkok street-food icon: wide rice noodles charred in a wok with Chinese broccoli, crispy pork belly, and a deep soy-garlic sauce. Smoky wok hei guaranteed.",
            cuisine = "Thai",
            difficulty = Difficulty.MEDIUM,
            cookTimeMin = 30,
            servings = 2,
            caloriesPerServing = 560,
            proteinG = 28,
            carbsG = 65,
            fatG = 22,
            imageUrl = "https://images.unsplash.com/photo-1559314809-0d155014e29e?w=800&q=80",
            tags = listOf("Street Food", "Quick", "One-Pan"),
            ingredients = listOf(
                Ingredient("Wide rice noodles", "300g", IngredientCategory.GRAIN),
                Ingredient("Pork belly, sliced", "200g", IngredientCategory.PROTEIN),
                Ingredient("Chinese broccoli (gai lan)", "1 bunch", IngredientCategory.PRODUCE),
                Ingredient("Eggs", "2", IngredientCategory.PROTEIN),
                Ingredient("Garlic, smashed", "4 cloves", IngredientCategory.PRODUCE),
                Ingredient("Dark soy sauce", "2 tbsp", IngredientCategory.PANTRY),
                Ingredient("Light soy sauce", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Oyster sauce", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Palm sugar", "1 tsp", IngredientCategory.PANTRY),
                Ingredient("White pepper", "1/2 tsp", IngredientCategory.SPICE),
                Ingredient("Neutral oil", "3 tbsp", IngredientCategory.PANTRY),
            ),
            steps = listOf(
                CookStep(1, "Prep noodles", "Separate rice noodles gently with your hands. If refrigerated, briefly soak in warm water 2 minutes to soften.", 5, tip = "Cold noodles from the fridge won't char — bring them to room temp first."),
                CookStep(2, "Crisp the pork", "Render pork belly in a hot wok with 1 tbsp oil until edges are crispy and golden, 5 minutes. Remove and set aside.", 5, technique = "Render", tip = "Let the pork sit undisturbed before stirring — that's how you get the crust."),
                CookStep(3, "Blister the broccoli", "Add broccoli stems to the wok, char hard for 2 minutes. Add leaves and wilt 30 seconds. Set aside with pork.", 3, technique = "Char"),
                CookStep(4, "Fry the noodles", "Get the wok smoking hot. Add oil and garlic, stir 10 seconds. Add noodles and spread in a single layer. Let sit 30 seconds without stirring to develop char, then toss.", 4, technique = "Wok hei", tip = "Patience here = smoky flavor. Resist the urge to stir constantly."),
                CookStep(5, "Sauce & combine", "Push noodles aside, crack eggs into the wok, scramble 20 seconds. Add dark soy, light soy, oyster sauce, and palm sugar. Toss everything together with pork and broccoli until evenly coated.", 3, technique = "Toss"),
                CookStep(6, "Plate", "Plate immediately, top with white pepper. Serve with chili vinegar on the side.", 1, tip = "Pad see ew must be served hot from the wok — texture degrades fast."),
            ),
            tips = listOf(
                "Wok hei (smoky flavor) comes from a screaming hot wok and minimal stirring.",
                "Dark soy is thicker and less salty than light soy — both are essential.",
                "Substitute broccoli rabe if gai lan isn't available.",
            ),
            rating = 4.7f,
            reviewCount = 543,
            authorName = "Chef Apinya Suriya",
        ),
        Recipe(
            id = "r7",
            title = "Korean Braised Tofu (Dubu Jorim)",
            description = "Soy-braised tofu blocks with a glossy savory-sweet glaze, sesame, and scallions. The most satisfying 20-minute banchan you'll ever make.",
            cuisine = "Korean",
            difficulty = Difficulty.EASY,
            cookTimeMin = 20,
            servings = 4,
            caloriesPerServing = 220,
            proteinG = 16,
            carbsG = 8,
            fatG = 14,
            imageUrl = "https://images.unsplash.com/photo-1546548970-71785318a17b?w=800&q=80",
            tags = listOf("Vegan", "Gluten-Free", "Quick"),
            ingredients = listOf(
                Ingredient("Firm tofu", "2 blocks (400g)", IngredientCategory.PROTEIN),
                Ingredient("Soy sauce", "3 tbsp", IngredientCategory.PANTRY),
                Ingredient("Gochugaru (Korean chili flakes)", "1 tbsp", IngredientCategory.SPICE),
                Ingredient("Sugar or honey", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Sesame oil", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Toasted sesame seeds", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Garlic, minced", "2 cloves", IngredientCategory.PRODUCE),
                Ingredient("Scallions, sliced", "3", IngredientCategory.PRODUCE),
                Ingredient("Neutral oil", "2 tbsp", IngredientCategory.PANTRY),
            ),
            steps = listOf(
                CookStep(1, "Press & slice tofu", "Press tofu 10 minutes to remove excess water. Slice into 1cm planks.", 10, technique = "Press", tip = "Drier tofu browns better and absorbs more sauce."),
                CookStep(2, "Pan-fry the tofu", "Heat oil in a pan. Fry tofu planks over medium heat until golden on both sides, 3 minutes per side.", 6, technique = "Pan-fry"),
                CookStep(3, "Braise", "Mix soy sauce, sugar, gochugaru, garlic, and 1/4 cup water. Pour over tofu. Simmer and spoon sauce over tofu until it reduces to a glossy glaze, 4 minutes.", 4, technique = "Braise", tip = "Baste continuously — the sauce thickens as it reduces."),
                CookStep(4, "Finish", "Drizzle sesame oil, scatter sesame seeds and scallions. Serve warm or at room temperature.", 1),
            ),
            tips = listOf(
                "Gochugaru has a smoky, fruity heat — adjust to taste.",
                "Leftovers are great cold, rolled in lettuce wraps with rice.",
                "Use tamari instead of soy sauce to keep this strictly gluten-free.",
            ),
            rating = 4.6f,
            reviewCount = 312,
            authorName = "Chef Min-jun Park",
        ),
        Recipe(
            id = "r8",
            title = "Smash Burger with Special Sauce",
            description = "The platonic ideal of a burger: thin crispy-smashed patties, melted American cheese, toasted brioche, and a tangy special sauce. Backyard legend.",
            cuisine = "American",
            difficulty = Difficulty.EASY,
            cookTimeMin = 25,
            servings = 4,
            caloriesPerServing = 620,
            proteinG = 30,
            carbsG = 38,
            fatG = 38,
            imageUrl = "https://images.unsplash.com/photo-1550317138-10000687a72b?w=800&q=80",
            tags = listOf("Comfort", "Grill", "Family"),
            ingredients = listOf(
                Ingredient("Ground chuck (20% fat)", "600g", IngredientCategory.PROTEIN),
                Ingredient("Brioche buns", "4", IngredientCategory.GRAIN),
                Ingredient("American cheese slices", "8", IngredientCategory.DAIRY),
                Ingredient("Dill pickles", "16 slices", IngredientCategory.PRODUCE),
                Ingredient("Yellow onion, thinly sliced", "1", IngredientCategory.PRODUCE),
                Ingredient("Iceberg lettuce", "1 head", IngredientCategory.PRODUCE),
                Ingredient("Mayonnaise", "4 tbsp", IngredientCategory.PANTRY),
                Ingredient("Ketchup", "2 tbsp", IngredientCategory.PANTRY),
                Ingredient("Yellow mustard", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Pickle relish", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Paprika", "1/2 tsp", IngredientCategory.SPICE),
                Ingredient("Garlic powder", "1/2 tsp", IngredientCategory.SPICE),
            ),
            steps = listOf(
                CookStep(1, "Mix the special sauce", "Combine mayo, ketchup, mustard, relish, paprika, and garlic powder. Chill while you cook.", 3, tip = "This is essentially Big Mac sauce. Make extra — it's addictive on everything."),
                CookStep(2, "Portion & shape", "Gently form ground chuck into 8 loose 75g balls. Don't pack them tight.", 5, tip = "Loose balls = jagged crispy edges when smashed. Overworking makes tough patties."),
                CookStep(3, "Toast the buns", "Toast brioche buns cut-side down in butter until golden. Set aside.", 3),
                CookStep(4, "Smash the patties", "Heat a cast-iron pan or griddle until smoking. Place a ball, top with parchment, and smash flat with a heavy spatula. Season with salt and pepper. Cook 2 minutes until edges are crispy.", 2, technique = "Smash", tip = "Smash within 10 seconds of contact — once the crust forms, it won't flatten."),
                CookStep(5, "Flip & cheese", "Flip, top with 2 cheese slices, and cook 1 more minute. The cheese should be fully melted.", 1),
                CookStep(6, "Build the burgers", "Spread special sauce on both bun halves. Bottom: lettuce, onion, pickles. Top with patty. Crown it.", 2, tip = "Layering pickles under the patty keeps the bottom bun from getting soggy."),
            ),
            tips = listOf(
                "Use a flat top or cast iron — you need direct metal contact for the crust.",
                "20% fat is non-negotiable. Lean meat makes dry, dense patties.",
                "Let the meat come to room temp before cooking for even browning.",
            ),
            rating = 4.9f,
            reviewCount = 3287,
            authorName = "Chef Bobby Reeves",
        ),
    )

    fun byCuisine(cuisine: String): List<Recipe> {
        val all = apiRecipes.values + recipes
        return all.filter { it.cuisine.equals(cuisine, ignoreCase = true) }
            .distinctBy { it.id }
    }

    fun byId(id: String): Recipe? =
        apiRecipes[id] ?: dynamicRecipes[id] ?: recipes.find { it.id == id }

    /**
     * Featured recipes — prefer API recipes, fall back to seeds.
     */
    val featured: List<Recipe>
        get() = if (apiRecipes.isNotEmpty()) apiRecipes.values.take(3) else recipes.take(3)

    /**
     * Trending recipes — prefer API recipes, fall back to seeds.
     */
    val trending: List<Recipe>
        get() = if (apiRecipes.isNotEmpty()) {
            apiRecipes.values.toList()
        } else {
            recipes.sortedByDescending { it.reviewCount }
        }

    /**
     * All recipes — API cache + seeds combined.
     */
    val all: List<Recipe>
        get() = (apiRecipes.values + recipes + dynamicRecipes.values).distinctBy { it.id }

    fun search(query: String): List<Recipe> {
        if (query.isBlank()) return all
        val q = query.lowercase()
        return all.filter {
            it.title.lowercase().contains(q) ||
                it.cuisine.lowercase().contains(q) ||
                it.tags.any { tag -> tag.lowercase().contains(q) } ||
                it.ingredients.any { ing -> ing.name.lowercase().contains(q) }
        }
    }
}
