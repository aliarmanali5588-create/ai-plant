package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.PlantAnalysisResult
import com.example.ui.PlantCareViewModel
import com.example.ui.Screen
import com.example.ui.components.*
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantCareApp(
    viewModel: PlantCareViewModel,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val progressText by viewModel.loadingProgressText.collectAsState()

    // Setup Back Navigation handling for sub-screens
    if (currentScreen != Screen.Home) {
        BackHandler {
            if (!viewModel.navigateBack()) {
                viewModel.resetToHome()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Official Claymorphism Logo Badge with full height and width
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    ambientColor = ClayShadowDark,
                                    spotColor = ClayShadowDark
                                )
                                .clip(RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.claymorphism),
                                contentDescription = "PlantCare AI Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Column {
                            // Heading: Black on white screen, White on black screen
                            Text(
                                text = "PlantCare AI",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 20.sp
                            )
                            // Subheading: Black on white screen, White on black screen
                            Text(
                                text = "Organic Clay Health Assistant",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    // Theme toggle button: allows switching between White screen (Black text) and Black screen (White text)
                    ClayIconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to Light Screen" else "Switch to Dark Screen",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (currentScreen != Screen.Home) {
                        ClayIconButton(
                            onClick = { viewModel.resetToHome() },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("nav_home_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Navigate to Home",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        onImageUri = { uri, isFromCamera ->
                            viewModel.handleImageSelected(uri, isFromCamera)
                        },
                        onPermissionDenied = { message ->
                            viewModel.navigateTo(Screen.Error(message) {
                                viewModel.resetToHome()
                            })
                        }
                    )
                }
                is Screen.ImagePreview -> {
                    ImagePreviewScreen(
                        imageUri = screen.imageUri,
                        onAnalyze = { viewModel.startAnalysis(context, screen.imageUri) },
                        onCancel = { viewModel.resetToHome() }
                    )
                }
                is Screen.Analyzing -> {
                    AnalysisLoadingScreen(
                        progressText = progressText
                    )
                }
                is Screen.Result -> {
                    ResultScreen(
                        result = screen,
                        onReset = { viewModel.resetToHome() },
                        onFetchAdvisory = { res, uri -> viewModel.fetchAdvisory(res, uri) }
                    )
                }
                is Screen.Error -> {
                    ErrorScreen(
                        message = screen.message,
                        onRetry = screen.retryAction
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onImageUri: (String, Boolean) -> Unit,
    onPermissionDenied: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Persistent temp URI state for camera captures
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                cameraTempUri?.let { uri ->
                    onImageUri(uri.toString(), true)
                }
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onImageUri(uri.toString(), false)
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = createTempImageUri(context)
                cameraTempUri = uri
                cameraLauncher.launch(uri)
            } else {
                onPermissionDenied("Camera access is required to scan a plant.")
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Hero Clay Showcase Card with Full Official Logo
        ClayCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            elevation = 7.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_home_hero),
                        contentDescription = "Greenhouse plants banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Vignette gradient for dark contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.70f)
                                    ),
                                    startY = 90f
                                )
                            )
                    )

                    // Prominent Clay Logo Card in the Hero section (Full height & width)
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .shadow(8.dp, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.claymorphism),
                                contentDescription = "PlantCare AI Official Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Column {
                            Surface(
                                color = ClayPrimary,
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "AI VISION ENGINE",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // On dark surface: White text
                            Text(
                                text = "Smart Plant Diagnosis",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Heading: Black on white screen, White on black screen
                    Text(
                        text = "Keep Your Plants Healthy",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    // Subheading: Black on white screen, White on black screen
                    Text(
                        text = "Take a photo of a leaf and let PlantCare AI help you understand what might be affecting your plant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // Action Buttons with Tactile Claymorphism
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Primary Clay Button (Scan a Plant)
            ClayButton(
                onClick = {
                    val permissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    )
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        val uri = createTempImageUri(context)
                        cameraTempUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_plant_button"),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Scan icon",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Scan a Plant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            // Secondary Clay Button (Choose from Gallery)
            ClaySecondaryButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_button"),
                shape = RoundedCornerShape(26.dp),
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery icon",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Choose from Gallery",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Helpful Tip Card with Official Logo Badge (Full Height & Width)
        ClayCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            elevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(3.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.claymorphism),
                        contentDescription = "PlantCare AI Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tip Heading: Black on white screen, White on black screen
                    Text(
                        text = "For better results",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    // Tip Subheading: Black on white screen, White on black screen
                    Text(
                        text = "Take a clear photo of the affected leaf in good lighting, keeping the camera steady and focused on the symptoms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ImagePreviewScreen(
    imageUri: String,
    onAnalyze: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Official Logo with Full Height & Width
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(3.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.claymorphism),
                    contentDescription = "PlantCare AI Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Column {
                // Heading: Black on white screen, White on black screen
                Text(
                    text = "Review Your Photo",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Subheading: Black on white screen, White on black screen
                Text(
                    text = "Verify leaf symptoms before AI diagnosis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Rounded Clay Image Container (32dp radius)
        ClayCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            elevation = 6.dp
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Selected leaf image preview",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Action Buttons with Clay Styling
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ClayButton(
                onClick = onAnalyze,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analyze_plant_button"),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Analyze icon",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Analyze Plant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            ClaySecondaryButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("choose_another_button"),
                shape = RoundedCornerShape(26.dp),
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                Text(
                    text = "Choose Another Photo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun AnalysisLoadingScreen(
    progressText: String
) {
    // Gentle Organic Claymorphism Pulsing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "clay_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "clay_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Official Clay Logo with Full Height & Width and Organic 3D Pulse
        Box(
            modifier = Modifier
                .scale(scale)
                .size(190.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(48.dp),
                    ambientColor = ClayAIGlow.copy(alpha = 0.5f),
                    spotColor = ClayPrimary.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(48.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.claymorphism),
                contentDescription = "Pulsing Claymorphism Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Heading: Black on white screen, White on black screen
        Text(
            text = "Analyzing Your Plant",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subheading: Black on white screen, White on black screen
        Text(
            text = "We're examining the image for signs of plant health issues.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Clay Progress Stage Pill
        ClayCard(
            shape = RoundedCornerShape(50.dp),
            elevation = 5.dp,
            modifier = Modifier.testTag("analysis_progress_card")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 3.dp,
                    color = ClayPrimary
                )
                // Stage Progress Text: Black on white screen, White on black screen
                Text(
                    text = progressText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun ResultScreen(
    result: Screen.Result,
    onReset: () -> Unit,
    onFetchAdvisory: (PlantAnalysisResult, String) -> Unit
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (result.advisory == null && !result.isAdvisoryLoading && result.advisoryError == null) {
            onFetchAdvisory(result.result, result.imageUri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Diagnosis Summary Clay Card
        ClayCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            elevation = 7.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Brand Header with Official Logo (Full Height & Width)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(3.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.claymorphism),
                            contentDescription = "PlantCare AI Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Text(
                        text = "PlantCare AI Diagnosis",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Analyzed Leaf Thumbnail with Clay Framing
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .shadow(4.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                    ) {
                        AsyncImage(
                            model = result.imageUri,
                            contentDescription = "Analyzed leaf thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Plant Species Clay Tag
                        val isDarkTag = MaterialTheme.colorScheme.onBackground == Color.White || MaterialTheme.colorScheme.background == Color(0xFF000000)
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(50.dp))
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isDarkTag) Color(0xFF242424) else Color(0xFFF0FDF4))
                                .border(1.dp, if (isDarkTag) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🌱 ${result.result.plant}",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 12.sp
                            )
                        }

                        // Disease Diagnosis Heading: Black on white screen, White on black screen
                        Text(
                            text = result.result.disease,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (result.result.isLowConfidence) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = "Confidence indicator",
                                tint = if (result.result.isLowConfidence) Color(0xFFC2410C) else ClayPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            // Confidence Subheading: Black on white screen, White on black screen
                            Text(
                                text = if (result.result.isLowConfidence) {
                                    "${(result.result.confidence * 100).toInt()}% (Likely diagnosis)"
                                } else {
                                    "${(result.result.confidence * 100).toInt()}% Confidence"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Inset Clay Confidence Bar
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ClayProgressBar(
                        progress = result.result.confidence,
                        fillColor = if (result.result.isLowConfidence) Color(0xFFF97316) else ClayPrimary
                    )
                }
            }
        }

        // Low Confidence Warning Alert
        if (result.result.isLowConfidence) {
            val isDark = MaterialTheme.colorScheme.onBackground == Color.White || MaterialTheme.colorScheme.background == Color(0xFF000000)
            ClayCard(
                shape = RoundedCornerShape(26.dp),
                backgroundColor = if (isDark) Color(0xFF3E2005) else Color(0xFFFEF3C7),
                elevation = 4.dp,
                border = BorderStroke(1.5.dp, if (isDark) Color(0xFFF59E0B) else Color(0xFFFCD34D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("low_confidence_banner")
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning icon",
                            tint = if (isDark) Color.White else Color(0xFF92400E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "⚠️ Low confidence",
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color.Black,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "The image may not be clear enough for a reliable plant disease prediction. Try taking a closer photo in good lighting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.White else Color.Black,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Top Predictions Section
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Heading: Black on white screen, White on black screen
            Text(
                text = "Alternative Possibilities",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            for ((index, prediction) in result.result.predictions.withIndex()) {
                val medal = when (index) {
                    0 -> "🥇"
                    1 -> "🥈"
                    else -> "🥉"
                }
                ClayCard(
                    shape = RoundedCornerShape(20.dp),
                    elevation = if (index == 0) 5.dp else 2.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = medal, fontSize = 20.sp)
                            // Item name Heading: Black on white screen, White on black screen
                            Text(
                                text = prediction.name,
                                fontWeight = if (index == 0) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 15.sp
                            )
                        }
                        // Percentage Subheading: Black on white screen, White on black screen
                        Text(
                            text = "${(prediction.confidence * 100).toInt()}%",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // AI Advisory Section — Styled with ClayAICard & Official Logo Badge
        if (result.isAdvisoryLoading) {
            ClayCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "🌿 Preparing your plant-care guidance...",
                    modifier = Modifier.padding(22.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )
            }
        } else if (result.advisoryError != null) {
            val isDark = MaterialTheme.colorScheme.onBackground == Color.White || MaterialTheme.colorScheme.background == Color(0xFF000000)
            ClayCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (isDark) Color(0xFF381414) else Color(0xFFFEE2E2)
            ) {
                Text(
                    text = "🌿 Plant analysis completed.\n\nDetailed AI guidance is temporarily unavailable.\n\n${result.advisoryError}",
                    modifier = Modifier.padding(22.dp),
                    color = if (isDark) Color.White else Color.Black
                )
            }
        } else if (result.advisory != null) {
            val advisory = result.advisory
            ClayAICard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Official Logo Badge with Full Height & Width
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(3.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.claymorphism),
                                contentDescription = "PlantCare AI Advisory Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Column {
                            // Heading: Black on white screen, White on black screen
                            Text(
                                text = "AI Care Advisory",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            // Subheading: Black on white screen, White on black screen
                            Text(
                                text = "Powered by Groq LLM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                        thickness = 1.dp
                    )

                    // What is it?
                    Text(
                        text = "What is it?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = advisory.what_is_it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Common symptoms
                    Text(
                        text = "Common symptoms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    advisory.symptoms.forEach {
                        Text(
                            text = "• $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // What to do
                    Text(
                        text = "What to do",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    advisory.what_to_do.forEach {
                        Text(
                            text = "• $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Prevention
                    Text(
                        text = "Prevention",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    advisory.prevention.forEach {
                        Text(
                            text = "• $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // When to seek expert help
                    Text(
                        text = "When to seek expert help",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = advisory.when_to_seek_expert_help,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Caution
                    Text(
                        text = "Caution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = advisory.caution,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons with Clay Styling
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ClayButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_another_button"),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Scan another icon",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Scan Another Plant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Clay Logo Badge with Error Indicator (Full Height & Width)
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = ClayShadowDark, spotColor = ClayShadowDark)
                .clip(RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.claymorphism),
                contentDescription = "PlantCare AI Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Heading: Black on white screen, White on black screen
        Text(
            text = "An Error Occurred",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subheading: Black on white screen, White on black screen
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(36.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            ClayButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("error_retry_button"),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = "Try Again",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            if (message.contains("permission", ignoreCase = true) || message.contains("access", ignoreCase = true)) {
                ClaySecondaryButton(
                    onClick = { openAppSettings(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_settings_button"),
                    shape = RoundedCornerShape(26.dp),
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings icon",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open App Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

// Securely create a temporary file in the application's cache directory and get its URI
private fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile("plant_scan_", ".jpg", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

// Deep link shortcut helper to open Settings details of the current app
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallbackIntent)
    }
}
