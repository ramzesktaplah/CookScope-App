package cookscopeai.com.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import cookscopeai.com.data.DishScanService
import cookscopeai.com.data.model.DishAnalysis
import cookscopeai.com.ui.theme.BlackBg
import cookscopeai.com.ui.theme.BlackCard
import cookscopeai.com.ui.theme.BlackSurface
import cookscopeai.com.ui.theme.GoldDeep
import cookscopeai.com.ui.theme.GoldLight
import cookscopeai.com.ui.theme.GoldPrimary
import cookscopeai.com.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

/**
 * Scan Your Dish — AI-powered dish analyzer.
 * Opens camera, captures a photo, sends to AI vision for analysis,
 * and displays nutrition, ingredients, health score, and dietary info.
 */
@Composable
fun DishScanScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanService = remember { DishScanService() }

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

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var capturedImagePath by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<DishAnalysis?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun resetScan() {
        capturedImagePath = null
        analysisResult = null
        errorMessage = null
        isAnalyzing = false
    }

    fun analyzeImage(path: String) {
        scope.launch {
            isAnalyzing = true
            errorMessage = null
            val result = withContext(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeFile(path)
                val resized = resizeBitmap(bitmap, 1024)
                val base64 = bitmapToBase64(resized)
                scanService.analyzeDish(base64)
            }
            isAnalyzing = false
            result.fold(
                onSuccess = { analysisResult = it },
                onFailure = { errorMessage = it.message ?: "Analysis failed" }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBg),
    ) {
        when {
            analysisResult != null -> {
                AnalysisResultView(
                    analysis = analysisResult!!,
                    imagePath = capturedImagePath,
                    onBack = onBack,
                    onScanAgain = { resetScan() },
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

            hasCameraPermission -> {
                CameraView(
                    onCapture = { path ->
                        capturedImagePath = path
                    },
                    onBack = onBack,
                )
            }

            else -> {
                PermissionDeniedView(
                    onBack = onBack,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraView(
    onCapture: (String) -> Unit,
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
                "Scan Your Dish",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.size(44.dp))
        }

        // Capture button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .clickable {
                    val photoFile = File(
                        context.cacheDir,
                        "dish_scan_${System.currentTimeMillis()}.jpg"
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

        // Hint text
        Text(
            "Point at a dish and tap to capture",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp),
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
            contentDescription = "Captured dish",
            contentScale = ContentScale.Fit,
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
        }

        // Bottom action bar
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
                label = "Analyze",
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
                "Analyzing your dish...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "CookScope AI is identifying ingredients,\ncalories & nutrition",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
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
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
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
            color = TextSecondary,
            modifier = Modifier.clickable { onBack() },
        )
    }
}

@Composable
private fun AnalysisResultView(
    analysis: DishAnalysis,
    imagePath: String?,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackBg),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Photo header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(BlackSurface),
            ) {
                if (imagePath != null) {
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = analysis.dishName,
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
                                1f to BlackBg,
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
                    Text(
                        analysis.dishName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        analysis.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                    )
                }
            }
        }

        // Health Score
        item {
            HealthScoreCard(analysis.healthScore, analysis.healthNotes)
        }

        // Calorie highlight
        item {
            CalorieHighlightCard(analysis.calories, analysis.mealType, analysis.portionSize)
        }

        // Nutrition breakdown
        item {
            SectionTitle("Nutrition Breakdown")
            NutritionGrid(analysis)
        }

        // Ingredients
        if (analysis.ingredients.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                SectionTitle("Detected Ingredients")
            }
            items(analysis.ingredients) { ingredient ->
                IngredientRow(ingredient.name, ingredient.amount)
            }
        }

        // Dietary tags
        if (analysis.dietaryTags.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                SectionTitle("Dietary Info")
                TagRow(analysis.dietaryTags, GoldPrimary.copy(alpha = 0.15f), GoldPrimary)
            }
        }

        // Allergens
        if (analysis.allergens.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Allergens",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                TagRow(analysis.allergens, Color(0xFFE53935).copy(alpha = 0.15f), Color(0xFFE53935))
            }
        }

        // Scan again button
        item {
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(GoldLight, GoldPrimary, GoldDeep)
                        )
                    )
                    .clickable { onScanAgain() },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Scan Another Dish",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthScoreCard(score: Int, notes: String) {
    val scoreColor = when {
        score >= 70 -> Color(0xFF4CAF50)
        score >= 40 -> GoldPrimary
        else -> Color(0xFFE53935)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(BlackCard)
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(
                    "Health Score",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$score / 100",
                    style = MaterialTheme.typography.headlineMedium,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$score",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (notes.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                notes,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun CalorieHighlightCard(calories: Int, mealType: String, portionSize: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(GoldLight.copy(alpha = 0.15f), GoldPrimary.copy(alpha = 0.08f))
                )
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "$calories",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (mealType.isNotBlank()) {
                Text(
                    mealType.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (portionSize.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    portionSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun NutritionGrid(analysis: DishAnalysis) {
    val nutrients = listOf(
        "Protein" to "${analysis.proteinG}g",
        "Carbs" to "${analysis.carbsG}g",
        "Fat" to "${analysis.fatG}g",
        "Fiber" to "${analysis.fiberG}g",
        "Sugar" to "${analysis.sugarG}g",
        "Sodium" to "${analysis.sodiumMg}mg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        nutrients.take(3).forEach { (label, value) ->
            NutrientCell(label, value, Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        nutrients.drop(3).forEach { (label, value) ->
            NutrientCell(label, value, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NutrientCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BlackCard)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun IngredientRow(name: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BlackCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
        Text(
            amount,
            style = MaterialTheme.typography.bodySmall,
            color = GoldPrimary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagRow(tags: List<String>, bgColor: Color, textColor: Color) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Text(
                tag,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun PermissionDeniedView(
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
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
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "CookScope needs camera access to scan and analyze your dishes",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
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
        Text(
            "Go Back",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.clickable { onBack() },
        )
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
