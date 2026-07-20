package cookscopeai.com.data

import cookscopeai.com.data.model.CookStep
import cookscopeai.com.data.model.Cuisine
import cookscopeai.com.data.model.Difficulty
import cookscopeai.com.data.model.Ingredient
import cookscopeai.com.data.model.IngredientCategory
import cookscopeai.com.data.model.Recipe

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
        Cuisine("Italian", "\uD83C\uDDEE\uD83C\uDDF9", "Pasta, risotto & rustic classics"),
        Cuisine("Japanese", "\uD83C\uDDEF\uD83C\uDDF5", "Sushi, ramen & refined techniques"),
        Cuisine("Mexican", "\uD83C\uDDF2\uD83C\uDDFD", "Bold, vibrant & chili-forward"),
        Cuisine("Mediterranean", "\uD83C\uDFD7️", "Olive oil, herbs & sun-kissed veg"),
        Cuisine("Indian", "\uD83C\uDDEE\uD83C\uDDF3", "Layered spices & rich curries"),
        Cuisine("Thai", "\uD83C\uDDF9\uD83C\uDDED", "Balance of sweet, sour & heat"),
        Cuisine("Korean", "\uD83C\uDDF0\uD83C\uDDF7", "Fermented depth & BBQ culture"),
        Cuisine("American", "\uD83C\uDDFA\uD83C\uDDF8", "Comfort food & backyard classics"),
        Cuisine("Chinese", "\uD83C\uDDE8\uD83C\uDDF3", "Wok hei, dumplings & regional staples"),
        Cuisine("French", "\uD83C\uDDEB\uD83C\uDDF7", "Butter, wine & elegant sauces"),
        Cuisine("Vietnamese", "\uD83C\uDDFB\uD83C\uDDF3", "Fresh herbs, broths & bright flavors"),
        Cuisine("Spanish", "\uD83C\uDDEA\uD83C\uDDF8", "Paella, tapas & smoky paprika"),
        Cuisine("Moroccan", "\uD83C\uDDF2\uD83C\uDDE6", "Tagines, preserved lemon & spice"),
        Cuisine("Greek", "\uD83C\uDDEC\uD83C\uDDF7", "Feta, olives & sunshine bowls"),
        Cuisine("Brazilian", "\uD83C\uDDE7\uD83C\uDDF7", "Feijoada, moqueca & tropical fire"),
        Cuisine("Caribbean", "\uD83C\uDDF0\uD83C\uDDFC", "Jerk, plantains & island spice"),
        Cuisine("Turkish", "\uD83C\uDDF9\uD83C\uDDF7", "Kebabs, mezze & aromatic rice"),
        Cuisine("Lebanese", "\uD83C\uDDF1\uD83C\uDDE7", "Shawarma, tabbouleh & silky hummus"),
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
        Recipe(
            id = "r9",
            title = "Kung Pao Chicken",
            description = "Sichuan classic: tender chicken, dried chilies, and peanuts in a glossy, tangy, slightly numbing sauce. Faster than takeout.",
            cuisine = "Chinese",
            difficulty = Difficulty.MEDIUM,
            cookTimeMin = 25,
            servings = 3,
            caloriesPerServing = 420,
            proteinG = 32,
            carbsG = 18,
            fatG = 26,
            imageUrl = "https://images.unsplash.com/photo-1525755662778-949d4484cf9e?w=800&q=80",
            tags = listOf("Spicy", "Quick", "Stir-Fry"),
            ingredients = listOf(
                Ingredient("Chicken thigh, cubed", "450g", IngredientCategory.PROTEIN),
                Ingredient("Dried red chilies", "12", IngredientCategory.SPICE),
                Ingredient("Sichuan peppercorns", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Roasted peanuts", "80g", IngredientCategory.PANTRY),
                Ingredient("Scallions", "4", IngredientCategory.PRODUCE),
                Ingredient("Soy sauce", "3 tbsp", IngredientCategory.PANTRY),
                Ingredient("Chinkiang black vinegar", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Shaoxing wine", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Cornstarch", "1 tbsp", IngredientCategory.PANTRY),
                Ingredient("Sugar", "1 tsp", IngredientCategory.PANTRY),
                Ingredient("Garlic & ginger", "2 tbsp minced", IngredientCategory.PRODUCE),
                Ingredient("Neutral oil", "3 tbsp", IngredientCategory.PANTRY),
            ),
            steps = listOf(
                CookStep(1, "Marinate chicken", "Toss chicken with soy sauce, Shaoxing wine, and cornstarch. Let sit 15 minutes.", 15, technique = "Velveting", tip = "Cornstarch creates a silky coating that keeps the chicken juicy."),
                CookStep(2, "Make the sauce", "Whisk soy sauce, vinegar, sugar, and a splash of water. Set aside.", 2),
                CookStep(3, "Stir-fry", "Heat oil until smoking. Add chilies and Sichuan peppercorns, stir 15 seconds. Add chicken in a single layer; sear without moving 1 minute, then toss.", 4, technique = "Wok", tip = "High heat and minimal movement create the wok-seared crust."),
                CookStep(4, "Combine", "Add garlic, ginger, and scallion whites. Stir 30 seconds. Pour in sauce and toss until glossy. Add peanuts and scallion greens.", 2, technique = "Toss"),
                CookStep(5, "Serve", "Plate immediately over steamed rice.", 1),
            ),
            tips = listOf(
                "Adjust dried chilies to your heat tolerance.",
                "Sichuan peppercorn creates a gentle numbing buzz — don't skip it.",
                "Use chicken thigh, not breast, for stir-fries.",
            ),
            rating = 4.7f,
            reviewCount = 987,
            authorName = "Chef Li Wei",
        ),
        Recipe(
            id = "r10",
            title = "Coq au Vin",
            description = "Burgundian comfort: chicken braised in red wine with bacon, mushrooms, and pearl onions until the sauce is silky and rich.",
            cuisine = "French",
            difficulty = Difficulty.HARD,
            cookTimeMin = 150,
            servings = 6,
            caloriesPerServing = 580,
            proteinG = 42,
            carbsG = 18,
            fatG = 32,
            imageUrl = "https://images.unsplash.com/photo-1600891964092-4316c288032e?w=800&q=80",
            tags = listOf("Braise", "Weekend", "Dinner Party"),
            ingredients = listOf(
                Ingredient("Chicken legs & thighs", "1.5kg", IngredientCategory.PROTEIN),
                Ingredient("Bacon lardons", "200g", IngredientCategory.PROTEIN),
                Ingredient("Red wine (Burgundy)", "750ml", IngredientCategory.PANTRY),
                Ingredient("Chicken stock", "500ml", IngredientCategory.PANTRY),
                Ingredient("Pearl onions", "300g", IngredientCategory.PRODUCE),
                Ingredient("Cremini mushrooms", "300g", IngredientCategory.PRODUCE),
                Ingredient("Carrots", "3", IngredientCategory.PRODUCE),
                Ingredient("Garlic", "6 cloves", IngredientCategory.PRODUCE),
                Ingredient("Fresh thyme", "4 sprigs", IngredientCategory.PRODUCE),
                Ingredient("Bay leaves", "2", IngredientCategory.PANTRY),
                Ingredient("Butter", "40g", IngredientCategory.DAIRY),
                Ingredient("Flour", "2 tbsp", IngredientCategory.PANTRY),
            ),
            steps = listOf(
                CookStep(1, "Brown bacon", "Render lardons in a Dutch oven until crisp. Remove, leaving fat.", 6, technique = "Render"),
                CookStep(2, "Sear chicken", "Season and brown chicken in batches in the bacon fat. Set aside.", 10, technique = "Sear", tip = "Deep brown skin = deep flavor."),
                CookStep(3, "Sauté aromatics", "Sauté carrots and onions in the same pot 5 minutes. Add garlic and thyme for 1 minute.", 6),
                CookStep(4, "Deglaze & braise", "Sprinkle flour and stir 1 minute. Deglaze with wine and stock. Return chicken and bacon. Simmer covered 90 minutes.", 90, technique = "Braise", tip = "Use a wine you'd drink; cheap wine makes the sauce harsh."),
                CookStep(5, "Finish mushrooms & onions", "Sauté mushrooms and pearl onions in butter until golden. Add to the pot for the final 20 minutes.", 20, technique = "Sauté"),
                CookStep(6, "Reduce & serve", "Remove chicken. Reduce sauce until it coats a spoon. Spoon over chicken with parsley.", 8, technique = "Reduce"),
            ),
            tips = listOf(
                "Better the next day — the wine mellows and the sauce thickens.",
                "Serve with crusty bread or mashed potatoes to catch every drop.",
                "Don't rush the browning; it's the foundation of the dish.",
            ),
            rating = 4.8f,
            reviewCount = 756,
            authorName = "Chef Amélie Dubois",
            isPremium = true,
        ),
        Recipe(
            id = "r11",
            title = "Beef Pho",
            description = "Vietnamese noodle soup: slow-simmered bone broth scented with star anise, cinnamon, and cloves, poured over rice noodles and rare beef.",
            cuisine = "Vietnamese",
            difficulty = Difficulty.HARD,
            cookTimeMin = 300,
            servings = 6,
            caloriesPerServing = 480,
            proteinG = 34,
            carbsG = 52,
            fatG = 14,
            imageUrl = "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=800&q=80",
            tags = listOf("Soup", "Broth", "Weekend Project"),
            ingredients = listOf(
                Ingredient("Beef bones", "1.5kg", IngredientCategory.PROTEIN),
                Ingredient("Rice noodles (banh pho)", "400g", IngredientCategory.GRAIN),
                Ingredient("Beef sirloin, thinly sliced", "300g", IngredientCategory.PROTEIN),
                Ingredient("Star anise", "4", IngredientCategory.SPICE),
                Ingredient("Cinnamon stick", "2", IngredientCategory.SPICE),
                Ingredient("Whole cloves", "6", IngredientCategory.SPICE),
                Ingredient("Black cardamom", "2", IngredientCategory.SPICE),
                Ingredient("Fish sauce", "3 tbsp", IngredientCategory.PANTRY),
                Ingredient("Rock sugar", "30g", IngredientCategory.PANTRY),
                Ingredient("Ginger, charred", "80g", IngredientCategory.PRODUCE),
                Ingredient("Yellow onion, charred", "1", IngredientCategory.PRODUCE),
                Ingredient("Fresh garnishes", "bean sprouts, lime, basil, cilantro, jalapeño", IngredientCategory.PRODUCE),
            ),
            steps = listOf(
                CookStep(1, "Char aromatics", "Char ginger and onion over open flame or under broiler until blackened. Scrape off charred bits.", 10, technique = "Char", tip = "Charring adds the smoky depth that defines pho."),
                CookStep(2, "Simmer broth", "Cover bones with cold water, bring to a boil, then dump and rinse. Cover with fresh water, add charred aromatics and spices. Simmer 4 hours, skimming often.", 240, technique = "Simmer", tip = "A clear broth comes from a gentle simmer and frequent skimming."),
                CookStep(3, "Season broth", "Add fish sauce and rock sugar. Taste and adjust until savory-sweet-balanced.", 5, technique = "Season"),
                CookStep(4, "Prepare bowls", "Cook noodles per package. Divide among bowls. Top with raw beef slices; the hot broth will cook them.", 10),
                CookStep(5, "Serve", "Ladle boiling broth over beef and noodles. Serve with garnishes on the side.", 5),
            ),
            tips = listOf(
                "The broth is everything — don't shortcut the simmer time.",
                "Freeze extra broth; it reheats beautifully.",
                "Partially freeze beef for 20 minutes to slice it paper-thin.",
            ),
            rating = 4.9f,
            reviewCount = 1432,
            authorName = "Chef Linh Nguyen",
            isPremium = true,
        ),
        Recipe(
            id = "r12",
            title = "Seafood Paella",
            description = "Spanish rice dish from Valencia: saffron-scented bomba rice, prawns, mussels, and squid cooked in a wide paella pan until the socarrat forms.",
            cuisine = "Spanish",
            difficulty = Difficulty.MEDIUM,
            cookTimeMin = 55,
            servings = 6,
            caloriesPerServing = 520,
            proteinG = 38,
            carbsG = 58,
            fatG = 16,
            imageUrl = "https://images.unsplash.com/photo-1534080564583-6be75777b70a?w=800&q=80",
            tags = listOf("Seafood", "One-Pan", "Dinner Party"),
            ingredients = listOf(
                Ingredient("Bomba rice", "400g", IngredientCategory.GRAIN),
                Ingredient("Prawns, shell-on", "300g", IngredientCategory.PROTEIN),
                Ingredient("Mussels, cleaned", "400g", IngredientCategory.PROTEIN),
                Ingredient("Squid rings", "200g", IngredientCategory.PROTEIN),
                Ingredient("Chicken thighs", "300g", IngredientCategory.PROTEIN),
                Ingredient("Fish stock", "1L", IngredientCategory.PANTRY),
                Ingredient("Saffron threads", "1 pinch", IngredientCategory.SPICE),
                Ingredient("Tomato puree", "100g", IngredientCategory.PANTRY),
                Ingredient("Garlic", "4 cloves", IngredientCategory.PRODUCE),
                Ingredient("Spanish paprika (pimentón)", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Olive oil", "60ml", IngredientCategory.PANTRY),
                Ingredient("Lemon", "1", IngredientCategory.PRODUCE),
            ),
            steps = listOf(
                CookStep(1, "Infuse stock", "Steep saffron in warm fish stock for 15 minutes.", 15),
                CookStep(2, "Sear proteins", "Season chicken and sear in oil until golden. Remove. Sear prawns 1 minute per side; remove. Lightly sear squid; remove.", 8, technique = "Sear"),
                CookStep(3, "Build sofrito", "In the same pan, sauté garlic and tomato puree with paprika until deep red and thick, 5 minutes.", 5, technique = "Sofrito"),
                CookStep(4, "Add rice & stock", "Add rice and stir to coat. Pour in hot stock. Arrange chicken, squid, and mussels in the rice. Do not stir after this point.", 3, technique = "Arrange", tip = "Stirring releases starch and makes the rice creamy instead of distinct."),
                CookStep(5, "Cook undisturbed", "Simmer 18-20 minutes until liquid is absorbed and rice is just tender. Add prawns in the last 5 minutes.", 20, technique = "Socarrat", tip = "Listen for gentle crackling at the end — that's the coveted crispy bottom."),
                CookStep(6, "Rest & serve", "Cover with foil and rest 5 minutes. Serve with lemon wedges.", 5),
            ),
            tips = listOf(
                "Use a wide, shallow pan so the rice cooks evenly in a thin layer.",
                "Bomba rice absorbs about 3x its volume in liquid.",
                "Socarrat is the caramelized rice crust — prized, not burnt.",
            ),
            rating = 4.8f,
            reviewCount = 1104,
            authorName = "Chef Mateo García",
        ),
        Recipe(
            id = "r13",
            title = "Lamb Tagine with Apricots",
            description = "Moroccan slow-cooked lamb with sweet apricots, warm spices, and preserved lemon. Tender, fragrant, and deeply comforting.",
            cuisine = "Moroccan",
            difficulty = Difficulty.MEDIUM,
            cookTimeMin = 180,
            servings = 6,
            caloriesPerServing = 560,
            proteinG = 40,
            carbsG = 42,
            fatG = 26,
            imageUrl = "https://images.unsplash.com/photo-1511690656952-34342d5c2895?w=800&q=80",
            tags = listOf("Braise", "Sweet & Savory", "Make Ahead"),
            ingredients = listOf(
                Ingredient("Lamb shoulder, cubed", "1kg", IngredientCategory.PROTEIN),
                Ingredient("Dried apricots", "200g", IngredientCategory.PRODUCE),
                Ingredient("Preserved lemon", "1", IngredientCategory.PANTRY),
                Ingredient("Green olives", "100g", IngredientCategory.PANTRY),
                Ingredient("Canned chickpeas", "400g", IngredientCategory.PANTRY),
                Ingredient("Onion, sliced", "2", IngredientCategory.PRODUCE),
                Ingredient("Garlic", "4 cloves", IngredientCategory.PRODUCE),
                Ingredient("Fresh ginger", "2 tbsp grated", IngredientCategory.PRODUCE),
                Ingredient("Ras el hanout", "2 tbsp", IngredientCategory.SPICE),
                Ingredient("Ground cinnamon", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Saffron threads", "1 pinch", IngredientCategory.SPICE),
                Ingredient("Vegetable stock", "500ml", IngredientCategory.PANTRY),
            ),
            steps = listOf(
                CookStep(1, "Sear lamb", "Brown lamb cubes in batches. Remove and set aside.", 10, technique = "Sear"),
                CookStep(2, "Sauté aromatics", "Cook onions until soft and golden. Add garlic, ginger, ras el hanout, cinnamon, and saffron. Toast 1 minute.", 6, technique = "Bloom spices"),
                CookStep(3, "Braise", "Return lamb, add stock and apricots. Cover and simmer 2 hours until fork-tender.", 120, technique = "Braise", tip = "Tagine is traditionally cooked low and slow — the meat should fall apart."),
                CookStep(4, "Add chickpeas & olives", "Stir in chickpeas, olives, and preserved lemon rind. Simmer uncovered 15 minutes to thicken.", 15),
                CookStep(5, "Serve", "Garnish with fresh cilantro and toasted almonds. Serve with couscous.", 5),
            ),
            tips = listOf(
                "Ras el hanout blends vary; find one with rose petals for authenticity.",
                "Preserved lemon rind is edible and tangy — discard the pith.",
                "Make a day ahead; the spices bloom overnight.",
            ),
            rating = 4.8f,
            reviewCount = 876,
            authorName = "Chef Fatima El Amrani",
            isPremium = true,
        ),
        Recipe(
            id = "r14",
            title = "Moussaka",
            description = "Greek baked casserole of layered eggplant, spiced ground lamb, and creamy béchamel. Golden, comforting, and made for sharing.",
            cuisine = "Greek",
            difficulty = Difficulty.MEDIUM,
            cookTimeMin = 120,
            servings = 8,
            caloriesPerServing = 490,
            proteinG = 28,
            carbsG = 24,
            fatG = 32,
            imageUrl = "https://images.unsplash.com/photo-1574484284002-952d92456975?w=800&q=80",
            tags = listOf("Baked", "Comfort", "Family"),
            ingredients = listOf(
                Ingredient("Globe eggplants", "3 large", IngredientCategory.PRODUCE),
                Ingredient("Ground lamb", "700g", IngredientCategory.PROTEIN),
                Ingredient("Canned crushed tomatoes", "800g", IngredientCategory.PANTRY),
                Ingredient("Red onion, diced", "1", IngredientCategory.PRODUCE),
                Ingredient("Garlic", "4 cloves", IngredientCategory.PRODUCE),
                Ingredient("Ground cinnamon", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Dried oregano", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Red wine", "120ml", IngredientCategory.PANTRY),
                Ingredient("Béchamel sauce", "500ml", IngredientCategory.DAIRY),
                Ingredient("Grated parmesan", "80g", IngredientCategory.DAIRY),
                Ingredient("Egg", "1", IngredientCategory.PROTEIN),
                Ingredient("Olive oil", "for brushing", IngredientCategory.PANTRY),
            ),
            steps = listOf(
                CookStep(1, "Salt eggplant", "Slice eggplants lengthwise, salt, and rest 30 minutes to draw out bitterness. Rinse and pat dry.", 30, technique = "Degorge", tip = "Salting prevents the eggplant from absorbing too much oil."),
                CookStep(2, "Fry eggplant", "Brush slices with oil and pan-fry until golden on both sides. Drain on paper towels.", 12, technique = "Pan-fry"),
                CookStep(3, "Make meat sauce", "Sauté onion and garlic. Add lamb, brown, then add tomatoes, wine, cinnamon, and oregano. Simmer 30 minutes until thick.", 35, technique = "Simmer"),
                CookStep(4, "Make béchamel", "Warm milk, make a roux with butter and flour, whisk in milk until thick. Stir in parmesan and egg yolk off heat.", 10, technique = "Béchamel"),
                CookStep(5, "Layer & bake", "Layer eggplant, meat sauce, eggplant, béchamel in a baking dish. Bake at 180°C / 350°F for 45 minutes until golden and bubbling.", 45, technique = "Bake"),
                CookStep(6, "Rest", "Rest 15 minutes before slicing so layers hold together.", 15),
            ),
            tips = listOf(
                "Moussaka is even better the next day.",
                "For a lighter version, roast the eggplant instead of frying.",
                "Let it rest before cutting for clean slices.",
            ),
            rating = 4.7f,
            reviewCount = 923,
            authorName = "Chef Katerina Papadopoulos",
        ),
        Recipe(
            id = "r15",
            title = "Moqueca Baiana",
            description = "Brazilian seafood stew: coconut milk, palm oil, peppers, and fresh fish slowly simmered until every spoonful tastes like the coast of Bahia.",
            cuisine = "Brazilian",
            difficulty = Difficulty.MEDIUM,
            cookTimeMin = 45,
            servings = 4,
            caloriesPerServing = 460,
            proteinG = 36,
            carbsG = 14,
            fatG = 30,
            imageUrl = "https://images.unsplash.com/photo-1604329760661-e71dc83f8f26?w=800&q=80",
            tags = listOf("Seafood", "Stew", "Coconut"),
            ingredients = listOf(
                Ingredient("Firm white fish, cubed", "600g", IngredientCategory.PROTEIN),
                Ingredient("Large prawns", "300g", IngredientCategory.PROTEIN),
                Ingredient("Coconut milk", "400ml", IngredientCategory.PANTRY),
                Ingredient("Dendê palm oil", "2 tbsp", IngredientCategory.PANTRY),
                Ingredient("Bell peppers, sliced", "2", IngredientCategory.PRODUCE),
                Ingredient("Tomatoes, sliced", "3", IngredientCategory.PRODUCE),
                Ingredient("Onion, sliced", "1", IngredientCategory.PRODUCE),
                Ingredient("Garlic", "4 cloves", IngredientCategory.PRODUCE),
                Ingredient("Fresh cilantro", "1 bunch", IngredientCategory.PRODUCE),
                Ingredient("Lime, juiced", "2", IngredientCategory.PRODUCE),
                Ingredient("Fish stock", "200ml", IngredientCategory.PANTRY),
                Ingredient("Piri piri or hot sauce", "to taste", IngredientCategory.SPICE),
            ),
            steps = listOf(
                CookStep(1, "Marinate seafood", "Toss fish and prawns with lime juice, garlic, salt, and pepper. Marinate 20 minutes.", 20, technique = "Marinate"),
                CookStep(2, "Layer vegetables", "In a heavy pot, layer onions, peppers, and tomatoes. Drizzle with palm oil.", 3, technique = "Layer"),
                CookStep(3, "Add seafood & liquid", "Place fish and prawns on top. Add coconut milk and fish stock. Cover and simmer gently 20 minutes without stirring.", 20, technique = "Stew", tip = "Gentle simmering keeps the fish intact and tender."),
                CookStep(4, "Finish", "Garnish generously with cilantro and a drizzle of dendê oil. Serve with white rice and farofa.", 2),
            ),
            tips = listOf(
                "Dendê oil gives the signature color and earthy flavor; annatto oil is a substitute.",
                "Use a firm fish like sea bass, halibut, or snapper.",
                "Don't stir once the stew is simmering — the layers are part of the presentation.",
            ),
            rating = 4.7f,
            reviewCount = 654,
            authorName = "Chef Camila Souza",
        ),
        Recipe(
            id = "r16",
            title = "Jerk Chicken with Rice & Peas",
            description = "Jamaican classic: chicken marinated in fiery jerk seasoning, slow-grilled until smoky, served with coconut rice and peas.",
            cuisine = "Caribbean",
            difficulty = Difficulty.MEDIUM,
            cookTimeMin = 90,
            servings = 4,
            caloriesPerServing = 540,
            proteinG = 42,
            carbsG = 48,
            fatG = 20,
            imageUrl = "https://images.unsplash.com/photo-1596797038530-2c107229654b?w=800&q=80",
            tags = listOf("Spicy", "Grill", "Island"),
            ingredients = listOf(
                Ingredient("Chicken legs, skin-on", "4", IngredientCategory.PROTEIN),
                Ingredient("Scotch bonnet peppers", "2", IngredientCategory.PRODUCE),
                Ingredient("Fresh thyme", "6 sprigs", IngredientCategory.PRODUCE),
                Ingredient("Green onions", "6", IngredientCategory.PRODUCE),
                Ingredient("Garlic", "6 cloves", IngredientCategory.PRODUCE),
                Ingredient("Ginger", "2 tbsp grated", IngredientCategory.PRODUCE),
                Ingredient("Allspice berries", "1 tbsp", IngredientCategory.SPICE),
                Ingredient("Soy sauce", "3 tbsp", IngredientCategory.PANTRY),
                Ingredient("Brown sugar", "2 tbsp", IngredientCategory.PANTRY),
                Ingredient("Lime juice", "3 tbsp", IngredientCategory.PRODUCE),
                Ingredient("Rice & peas", "1.5 cups rice + 1 can kidney beans", IngredientCategory.GRAIN),
                Ingredient("Coconut milk", "400ml", IngredientCategory.PANTRY),
            ),
            steps = listOf(
                CookStep(1, "Make jerk marinade", "Blend scotch bonnets, thyme, green onions, garlic, ginger, allspice, soy sauce, brown sugar, and lime juice until smooth.", 5, technique = "Blend"),
                CookStep(2, "Marinate chicken", "Score chicken and coat in marinade. Marinate at least 4 hours, preferably overnight.", 240, technique = "Marinate", tip = "The longer the marinade, the deeper the jerk flavor."),
                CookStep(3, "Cook rice & peas", "Simmer rice, coconut milk, kidney beans, and thyme in water until tender and fragrant, about 20 minutes.", 20, technique = "Simmer"),
                CookStep(4, "Grill chicken", "Grill chicken over medium heat, turning often, until charred and cooked through, about 30 minutes. Baste with leftover marinade.", 30, technique = "Grill", tip = "Low and slow with frequent turning prevents burning from the sugar."),
                CookStep(5, "Serve", "Rest chicken 5 minutes. Serve with rice & peas and extra lime.", 5),
            ),
            tips = listOf(
                "Scotch bonnets are very hot; remove seeds or use habaneros as substitute.",
                "Allspice is non-negotiable for authentic jerk flavor.",
                "Char is good; blackened sugar is part of the flavor.",
            ),
            rating = 4.8f,
            reviewCount = 1112,
            authorName = "Chef Marlon Brown",
            isPremium = true,
        ),
        Recipe(
            id = "r17",
            title = "Turkish Lamb Kebabs",
            description = "Adana-style spiced ground lamb skewers grilled over charcoal, served with warm flatbread, sumac onions, and yogurt.",
            cuisine = "Turkish",
            difficulty = Difficulty.MEDIUM,
            cookTimeMin = 40,
            servings = 4,
            caloriesPerServing = 510,
            proteinG = 34,
            carbsG = 28,
            fatG = 30,
            imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=800&q=80",
            tags = listOf("Grill", "Spicy", "Street Food"),
            ingredients = listOf(
                Ingredient("Ground lamb", "700g", IngredientCategory.PROTEIN),
                Ingredient("Lamb fat or beef fat", "100g, minced", IngredientCategory.PROTEIN),
                Ingredient("Red bell pepper, finely diced", "1", IngredientCategory.PRODUCE),
                Ingredient("Red onion, grated", "1", IngredientCategory.PRODUCE),
                Ingredient("Garlic", "4 cloves", IngredientCategory.PRODUCE),
                Ingredient("Cumin", "1.5 tsp", IngredientCategory.SPICE),
                Ingredient("Sumac", "1.5 tsp", IngredientCategory.SPICE),
                Ingredient("Paprika", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Red chili flakes", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Parsley, minced", "1/2 cup", IngredientCategory.PRODUCE),
                Ingredient("Flatbread", "4", IngredientCategory.GRAIN),
                Ingredient("Yogurt", "200g", IngredientCategory.DAIRY),
            ),
            steps = listOf(
                CookStep(1, "Prepare meat", "Mix lamb, fat, bell pepper, onion, garlic, spices, and parsley. Knead until tacky and well-combined, about 5 minutes.", 10, technique = "Knead", tip = "Kneading binds the proteins so the kebab holds its shape on the skewer."),
                CookStep(2, "Rest", "Cover and refrigerate the mixture 30 minutes to firm up.", 30),
                CookStep(3, "Shape skewers", "Wet hands and mold the meat onto flat metal skewers, pressing firmly into a long sausage shape.", 5, technique = "Skewer"),
                CookStep(4, "Grill", "Grill over high heat, turning once, until charred and cooked through, about 8-10 minutes total.", 10, technique = "Grill", tip = "Don't move them too much; let a crust form before turning."),
                CookStep(5, "Serve", "Slide off skewers onto warm flatbread with sumac onions and a drizzle of yogurt.", 2),
            ),
            tips = listOf(
                "Use wide, flat metal skewers — the meat grips better.",
                "Don't skip the fat; it keeps the kebab juicy and binds the meat.",
                "Sumac onions: thinly slice red onion, toss with sumac and parsley.",
            ),
            rating = 4.7f,
            reviewCount = 834,
            authorName = "Chef Emre Yilmaz",
        ),
        Recipe(
            id = "r18",
            title = "Chicken Shawarma Bowl",
            description = "Lebanese street food made at home: marinated chicken roasted on a spit-style pan, served over hummus with pickled vegetables and tahini.",
            cuisine = "Lebanese",
            difficulty = Difficulty.EASY,
            cookTimeMin = 45,
            servings = 4,
            caloriesPerServing = 480,
            proteinG = 38,
            carbsG = 26,
            fatG = 24,
            imageUrl = "https://images.unsplash.com/photo-1540914124281-342587941389?w=800&q=80",
            tags = listOf("Quick", "High Protein", "Meal Prep"),
            ingredients = listOf(
                Ingredient("Chicken thighs, sliced", "700g", IngredientCategory.PROTEIN),
                Ingredient("Garlic", "6 cloves", IngredientCategory.PRODUCE),
                Ingredient("Lemon juice", "4 tbsp", IngredientCategory.PRODUCE),
                Ingredient("Olive oil", "3 tbsp", IngredientCategory.PANTRY),
                Ingredient("Ground cumin", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Ground coriander", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Paprika", "1 tsp", IngredientCategory.SPICE),
                Ingredient("Turmeric", "1/2 tsp", IngredientCategory.SPICE),
                Ingredient("Ground cinnamon", "1/2 tsp", IngredientCategory.SPICE),
                Ingredient("Cayenne", "1/2 tsp", IngredientCategory.SPICE),
                Ingredient("Hummus", "300g", IngredientCategory.PANTRY),
                Ingredient("Tahini sauce", "120ml", IngredientCategory.PANTRY),
                Ingredient("Pickled turnips", "100g", IngredientCategory.PANTRY),
                Ingredient("Pita bread", "4", IngredientCategory.GRAIN),
            ),
            steps = listOf(
                CookStep(1, "Marinate chicken", "Whisk garlic, lemon, oil, and spices. Coat chicken and marinate 30 minutes (or overnight).", 30, technique = "Marinate"),
                CookStep(2, "Sear chicken", "Cook chicken in a hot skillet in batches until deeply browned and cooked through, about 8 minutes.", 8, technique = "Sear", tip = "High heat creates the shawarma-style crispy edges."),
                CookStep(3, "Assemble bowls", "Spread hummus in each bowl. Top with chicken, pickled turnips, fresh tomatoes, and cucumber.", 4, technique = "Assemble"),
                CookStep(4, "Finish", "Drizzle with tahini sauce and serve with warm pita.", 2),
            ),
            tips = listOf(
                "Slice chicken thinly against the grain for the best texture.",
                "Make extra marinade; it's excellent as a dressing for the veggies.",
                "For authenticity, add a pinch of allspice and cardamom to the marinade.",
            ),
            rating = 4.8f,
            reviewCount = 1420,
            authorName = "Chef Layla Haddad",
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

    /**
     * Returns recipes ranked by how many of [ingredients] appear in each
     * recipe's ingredient list (case-insensitive partial match). Recipes with
     * no matches are excluded. Works against both seed recipes and the
     * recipeapi.io cache.
     */
    fun findByIngredients(ingredients: List<String>, limit: Int = 6): List<Recipe> {
        if (ingredients.isEmpty()) return emptyList()
        val normalized = ingredients
            .map { it.lowercase().trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (normalized.isEmpty()) return emptyList()
        return all.mapNotNull { recipe ->
            var score = 0
            recipe.ingredients.forEach { ing ->
                val name = ing.name.lowercase()
                if (normalized.any { det -> name.contains(det) || det.contains(name) }) {
                    score += 1
                }
            }
            if (score == 0) null else recipe to score
        }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}
