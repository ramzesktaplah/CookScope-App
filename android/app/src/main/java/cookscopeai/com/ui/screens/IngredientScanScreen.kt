package cookscopeai.com.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import cookscopeai.com.data.IngredientScanService
import cookscopeai.com.data.NotificationService
import cookscopeai.com.data.RecipeData
import cookscopeai.com.data.RecipeGenerationService
import cookscopeai.com.data.ScanUsageManager
import cookscopeai.com.data.SubscriptionManager
import cookscopeai.com.data.model.DetectedIngredient
import cookscopeai.com.data.model.IngredientScanResult
import cookscopeai.com.data.model.Recipe
import cookscopeai.com.ui.components.RecipeCard
import cookscopeai.com.ui.theme.GoldDeep
import cookscopeai.com.ui.theme.GoldLight
import cookscopeai.com.ui.theme.GoldPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

/**
 * Smart Ingredient Scanner — AI-powered raw ingredient detection.
 *
 * Users can capture a photo with the camera OR pick an existing image from
 * their gallery. AI vision identifies the raw food items, then the screen
 * suggests matching recipes from the local library and offers to generate a
 * brand-new AI recipe from the detected ingredients.
 *
 * Shares the same 5-hour AI-vision scan window as [DishScanScreen]
 * (ScanUsageManager): free 10 / premium 50 per 5-hour window, refill at 0.
 */
@Composable
fun IngredientScanScreen(
    onBack: () -> Unit,
    onRecipeClick: (String) -> Unit,
    onUpgrade: () -> Unit = onBack,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanService = remember { IngredientScanService() }
    val generationService = remember { RecipeGenerationService() }

    val isPremium by SubscriptionManager.isPremium.collectAsState()
    val scanUsed by ScanUsageManager.used.collectAsState()
    val scanRemaining by ScanUsageManager.remaining.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<IngredientScanResult?>(null) }
    val detectedIngredients = remember { mutableStateListOf<DetectedIngredient>() }
    var suggestedRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var isGenerating by remember { mutableStateOf(false) }
    var generateError by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var limitReached by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val copied = withContext(Dispatchers.IO) {
                    copyUriToCacheFile(context, uri)
                }
                if (copied != null) {
                    capturedImagePath = copied
                } else {
                    errorMessage = "Could not load that image. Try another one."
                }
            }
        }
    }

    fun resetScan() {
        capturedImagePath = null
        scanResult = null
        detectedIngredients.clear()
        suggestedRecipes = emptyList()
        errorMessage = null
        isAnalyzing = false
        isGenerating = false
        generateError = null
        limitReached = false
    }

    fun analyzeImage(path: String) {
        if (!ScanUsageManager.canScan()) {
            limitReached = true
            return
        }
        scope.launch {
            isAnalyzing = true
            errorMessage = null
            val result = withContext(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeFile(path)
                val resized = resizeBitmap(bitmap, 1024)
                val base64 = bitmapToBase64(resized)
                scanService.detectIngredients(base64)
            }
            isAnalyzing = false
            result.fold(
                onSuccess = { res ->
                    scanResult = res
                    detectedIngredients.clear()
                    detectedIngredients.addAll(res.ingredients)
                    suggestedRecipes = RecipeData.findByIngredients(
                        res.ingredients.map { it.name },
                        limit = 6,
                    )
                    ScanUsageManager.increment()
                    NotificationService.recordScanComplete()
                    NotificationService.showPostScanSuggestion(null)
                    NotificationService.markActive()
                },
                onFailure = { errorMessage = "Could not identify ingredients. Please try a clearer photo." }
            )
        }
    }

    fun generateAiRecipe() {
        if (isGenerating || detectedIngredients.isEmpty()) return
        scope.launch {
            isGenerating = true
            generateError = null
            val result = withContext(Dispatchers.IO) {
                generationService.generateRecipe(
                    ingredients = detectedIngredients.map { it.name },
                    cuisine = "Any Cuisine",
                    dishType = "Any Dish",
                    skillLevel = "Home Cook",
                    dietaryMode = "Non-Vegan",
                    isPremium = isPremium,
                )
            }
            isGenerating = false
            result.fold(
                onSuccess = { recipe ->
                    RecipeData.addRecipe(recipe)
                    onRecipeClick(recipe.id)
                },
                onFailure = { generateError = "Recipe generation failed. Please try again." }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            scanResult != null -> {
                ScanResultsView(
                    result = scanResult!!,
                    imagePath = capturedImagePath,
                    detectedIngredients = detectedIngredients,
                    suggestedRecipes = suggestedRecipes,
                    isGenerating = isGenerating,
                    generateError = generateError,
                    onRemoveIngredient = { ing ->
                        detectedIngredients.remove(ing)
                        suggestedRecipes = RecipeData.findByIngredients(
                            detectedIngredients.map { it.name },
                            limit = 6,
                        )
                    },
                    onRecipeClick = onRecipeClick,
                    onGenerateAi = { generateAiRecipe() },
                    onScanAgain = { resetScan() },
                    onBack = onBack,
                )
            }

            isGenerating -> {
                GeneratingRecipeOverlay(
                    ingredients = detectedIngredients.map { it.name },
                    onBack = onBack,
                )
            }

            isAnalyzing -> {
                AnalyzingView(imagePath = capturedImagePath)

                IconButton(
                    onClick = { resetScan() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(44.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.White,
                    )
                }
            }

            errorMessage != null -> {
                ErrorView(
                    message = errorMessage!!,
                    onRetry = { resetScan() },
                    onBack = onBack,
                )
            }

            capturedImagePath != null -> {
                CapturedPreviewView(
                    imagePath = capturedImagePath!!,
                    onRetake = { resetScan() },
                    onAnalyze = { analyzeImage(capturedImagePath!!) },
                    onBack = onBack,
                )
            }

            limitReached -> {
                ScanLimitView(
                    isPremium = isPremium,
                    used = scanUsed,
                    onBack = onBack,
                    onUpgrade = onUpgrade,
                )
            }

            hasCameraPermission -> {
                CameraView(
                    onCapture = { path -> capturedImagePath = path },
                    onPickFromGallery = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onBack = onBack,
                )
            }

            else -> {
                PermissionDeniedView(
                    onBack = onBack,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onPickFromGallery = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                )
            }
        }

        // Floating usage chip (camera or permission-denied views only).
        if (capturedImagePath == null && scanResult == null && !isAnalyzing && errorMessage == null && !limitReached) {
            ScanUsageChip(
                remaining = scanRemaining,
                limit = ScanUsageManager.currentLimit(),
                isPremium = isPremium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp),
            )
        }
    }
}

@Composable
private fun ScanUsageChip(
    remaining: Int,
    limit: Int,
    isPremium: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "$remaining / $limit scans left",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        if (!isPremium) {
            Spacer(Modifier.width(6.dp))
            Text(
                "· Free",
                style = MaterialTheme.typography.labelSmall,
                color = GoldPrimary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CameraView(
    onCapture: (String) -> Unit,
    onPickFromGallery: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    LaunchedEffect(Unit) {
        val cameraProvider = awaitCameraProvider(context)
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
            )
        } catch (_: Exception) {
            // Camera init failed
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(44.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Text(
                "Scan Ingredients",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = onPickFromGallery,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(44.dp),
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Pick from gallery",
                    tint = Color.White,
                )
            }
        }

        // Capture button + gallery button row
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onPickFromGallery() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(Modifier.width(24.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .clickable {
                        val photoFile = File(
                            context.cacheDir,
                            "ingredient_scan_${System.currentTimeMillis()}.jpg"
                        )
                        val outputOptions = ImageCapture.OutputFileOptions
                            .Builder(photoFile)
                            .build()

                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val savedPath = output.savedUri?.path ?: photoFile.absolutePath
                                    onCapture(savedPath)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    // Capture failed
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }
            }

            Spacer(Modifier.width(24.dp))

            Spacer(Modifier.size(56.dp))
        }

        Text(
            "Point at raw ingredients — tap to capture or pick from gallery",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
                .padding(horizontal = 32.dp),
        )
    }
}

@Composable
private fun CapturedPreviewView(
    imagePath: String,
    onRetake: () -> Unit,
    onAnalyze: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = "Captured ingredients",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(44.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(
                icon = Icons.Default.Refresh,
                label = "Retake",
                onClick = onRetake,
                tint = Color.White,
            )
            ActionButton(
                icon = Icons.Default.CheckCircle,
                label = "Detect",
                onClick = onAnalyze,
                tint = GoldPrimary,
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun AnalyzingView(imagePath: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (imagePath != null) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
            }
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(
                color = GoldPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Identifying ingredients...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "CookScope AI is scanning your photo for\nraw food items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GeneratingRecipeOverlay(
    ingredients: List<String>,
    onBack: () -> Unit,
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
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(44.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cancel",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = GoldPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(20.dp))
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    steps[step],
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Generating an AI recipe from your ingredients",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ingredients.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    ingredients.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = null,
            tint = Color(0xFFE53935),
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Scan Failed",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GoldPrimary)
                .clickable { onRetry() }
                .padding(horizontal = 24.dp, vertical = 14.dp),
        ) {
            Text(
                "Try Again",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Go Back",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onBack() },
        )
    }
}

@Composable
private fun ScanLimitView(
    isPremium: Boolean,
    used: Int,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
) {
    // Live countdown — recompute every second so the timer ticks down.
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    val timeRemaining = remember(tick) { ScanUsageManager.formatTimeRemaining() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Scan Limit Reached",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isPremium) {
                "You've used all 50 scans this 5-hour window. Scans refill in $timeRemaining."
            } else {
                "You've used all 10 free scans this 5-hour window. Scans refill in $timeRemaining. Upgrade to Premium for 50 scans / 5 hrs."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(GoldLight, GoldPrimary, GoldDeep)
                        )
                    )
                    .clickable { onUpgrade() }
                    .padding(horizontal = 28.dp, vertical = 14.dp),
            ) {
                Text(
                    "Upgrade to Premium",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(
            "Go Back",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onBack() },
        )
    }
}

@Composable
private fun PermissionDeniedView(
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Camera Access Needed",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "CookScope can use your camera to scan raw ingredients, or you can pick a photo from your gallery.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GoldPrimary)
                .clickable { onRequestPermission() }
                .padding(horizontal = 24.dp, vertical = 14.dp),
        ) {
            Text(
                "Grant Camera Access",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onPickFromGallery() }
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Pick from Gallery",
                style = MaterialTheme.typography.titleSmall,
                color = GoldPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Go Back",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onBack() },
        )
    }
}

@Composable
private fun ScanResultsView(
    result: IngredientScanResult,
    imagePath: String?,
    detectedIngredients: List<DetectedIngredient>,
    suggestedRecipes: List<Recipe>,
    isGenerating: Boolean,
    generateError: String?,
    onRemoveIngredient: (DetectedIngredient) -> Unit,
    onRecipeClick: (String) -> Unit,
    onGenerateAi: () -> Unit,
    onScanAgain: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Photo header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                if (imagePath != null) {
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = "Scanned ingredients",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background,
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(44.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = onScanAgain,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(44.dp),
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Scan again",
                            tint = Color.White,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Kitchen,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Detected Ingredients",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${detectedIngredients.size} item${if (detectedIngredients.size == 1) "" else "s"} found · tap a chip to remove",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Notes from AI
        if (result.notes.isNotBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GoldPrimary.copy(alpha = 0.10f))
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            result.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // Ingredient chips (horizontally scrollable, tappable to remove)
        if (detectedIngredients.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(detectedIngredients) { ing ->
                        DetectedIngredientChip(
                            ingredient = ing,
                            onRemove = { onRemoveIngredient(ing) },
                        )
                    }
                }
            }
        }

        // No ingredients detected
        if (detectedIngredients.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "No ingredients detected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Try a clearer photo with better lighting, or scan again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Suggested recipes
        if (suggestedRecipes.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Suggested Recipes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Text(
                    "From your library — matching the detected ingredients",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            items(suggestedRecipes) { recipe ->
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    RecipeCard(
                        recipe = recipe,
                        onClick = { onRecipeClick(recipe.id) },
                        cardHeight = 220,
                    )
                }
            }
        }

        // Generate AI recipe button
        if (detectedIngredients.isNotEmpty()) {
            item {
                Spacer(Modifier.height(28.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(GoldLight, GoldPrimary, GoldDeep)
                            )
                        )
                        .clickable(enabled = !isGenerating) { onGenerateAi() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Generate AI Recipe",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                if (generateError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        generateError,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE53935),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Scan again button
        item {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onScanAgain() },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Scan Again",
                        style = MaterialTheme.typography.titleSmall,
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetectedIngredientChip(
    ingredient: DetectedIngredient,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ingredient.confidence >= 0.8f) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            ingredient.name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(20.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// --- Helpers ---

private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val ratio = width.toFloat() / height.toFloat()
    val (targetW, targetH) = if (width > height) {
        maxDimension to (maxDimension / ratio).toInt()
    } else {
        (maxDimension * ratio).toInt() to maxDimension
    }
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    var quality = 85
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    while (stream.size() > 2_500_000 && quality > 40) {
        quality -= 15
        stream.reset()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    }
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

private suspend fun awaitCameraProvider(context: android.content.Context): ProcessCameraProvider {
    return suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { cont.resume(future.get()) },
            ContextCompat.getMainExecutor(context)
        )
    }
}

private fun copyUriToCacheFile(
    context: android.content.Context,
    uri: Uri,
): String? {
    return try {
        val outFile = File(
            context.cacheDir,
            "ingredient_pick_${System.currentTimeMillis()}.jpg"
        )
        context.contentResolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        outFile.absolutePath
    } catch (_: Exception) {
        null
    }
}
