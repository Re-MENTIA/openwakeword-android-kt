package com.rementia.openwakeword

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.rementia.openwakeword.lib.WakeWordEngine
import com.rementia.openwakeword.lib.model.WakeWordModel
import com.rementia.openwakeword.lib.model.DetectionMode
import com.rementia.openwakeword.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern wake word detection demo with sleek black UI design.
 */
class MainActivity : ComponentActivity() {
    
    private var wakeWordEngine: WakeWordEngine? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            OpenWakeWordTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WakeWordDetectionScreen(
                        onEngineReady = { engine ->
                            wakeWordEngine = engine
                        }
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        wakeWordEngine?.release()
    }
}

@Composable
fun WakeWordDetectionScreen(
    onEngineReady: (WakeWordEngine) -> Unit
) {
    val context = LocalContext.current
    
    var hasPermission by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var currentScore by remember { mutableStateOf(0f) }
    var isDetected by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Initializing...") }
    var realtimeScore by remember { mutableStateOf(0f) }
    var threshold by remember { mutableStateOf(0.03f) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                "Microphone permission is required for wake word detection",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    LaunchedEffect(Unit) {
        hasPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == 
                       android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val models = listOf(
                WakeWordModel(
                    name = "Tomori",
                    modelPath = "tomori.onnx",
                    threshold = threshold
                )
            )
            
            val engine = WakeWordEngine(
                context = context,
                models = models,
                detectionMode = DetectionMode.SINGLE_BEST,
                detectionCooldownMs = 5000L
            )
            
            onEngineReady(engine)
            
            launch {
                engine.detections.collect { detection ->
                    currentScore = detection.score
                    isDetected = true
                    
                    Toast.makeText(
                        context,
                        "${detection.model.name} detected! (${String.format("%.2f", detection.score)})",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    delay(2000)
                    isDetected = false
                }
            }
            
            launch {
                engine.scores.collect { score ->
                    realtimeScore = score.score
                }
            }
            
            engine.start()
            isListening = true
            status = "Listening..."
        } else {
            status = "Permission required"
            isListening = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HeaderSection()
            
            DetectionVisualization(
                isListening = isListening,
                isDetected = isDetected
            )
            
            RealtimeScoreDisplay(
                score = realtimeScore,
                threshold = threshold
            )
            
            StatusSection(
                status = status,
                isListening = isListening
            )
            
            WakeWordHint()
        }
    }
}

@Composable
fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 32.dp)
    ) {
        Text(
            text = "OpenWakeWord",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Light
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Demo",
            style = MaterialTheme.typography.titleMedium,
            color = AccentGreen,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DetectionVisualization(
    isListening: Boolean,
    isDetected: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val detectionScale by animateFloatAsState(
        targetValue = if (isDetected) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "detection_scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isDetected) AccentGreen.copy(alpha = 0.2f) else SurfaceDark,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "background_color"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(300.dp)
    ) {
        if (isDetected) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.1f))
            )
        }
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(detectionScale)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            CrossfadeContent(
                targetState = when {
                    !isListening -> "offline"
                    isDetected -> "detected"
                    else -> "listening"
                },
                label = "icon_crossfade"
            ) { state ->
                when (state) {
                    "offline" -> Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Offline",
                        modifier = Modifier.size(48.dp),
                        tint = TextDim
                    )
                    "listening" -> Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Listening",
                        modifier = Modifier.size(48.dp),
                        tint = TextSecondary
                    )
                    "detected" -> Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Detected",
                        modifier = Modifier.size(56.dp),
                        tint = AccentGreen
                    )
                }
            }
        }
    }
}

@Composable
fun <T> CrossfadeContent(
    targetState: T,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(300),
    label: String = "Crossfade",
    content: @Composable (T) -> Unit
) {
    Crossfade(
        targetState = targetState,
        modifier = modifier,
        animationSpec = animationSpec,
        label = label
    ) { state ->
        content(state)
    }
}


@Composable
fun StatusSection(
    status: String,
    isListening: Boolean
) {
    Text(
        text = status,
        style = MaterialTheme.typography.titleLarge,
        color = if (isListening) AccentGreen else TextSecondary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 32.dp)
    )
}

@Composable
fun RealtimeScoreDisplay(
    score: Float,
    threshold: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Real-time Inference",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Score display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Score",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                    Text(
                        text = String.format("%.5f", score),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (score > threshold) AccentGreen else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Threshold",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                    Text(
                        text = String.format("%.5f", threshold),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            var barWidth by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MediumGray)
                    .onSizeChanged { size ->
                        with(density) {
                            barWidth = size.width.toDp()
                        }
                    }
            ) {
                val progress = (score / 0.1f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            if (score > threshold) AccentGreen else TextSecondary
                        )
                )
                
                // Threshold indicator
                val thresholdPosition = (threshold / 0.1f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .offset(x = barWidth * thresholdPosition)
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
fun WakeWordHint() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MediumGray.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Say",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDim
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "\"Tomori\"",
                style = MaterialTheme.typography.headlineMedium,
                color = AccentGreen,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "to trigger detection (SINGLE_BEST mode)",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
        }
    }
}