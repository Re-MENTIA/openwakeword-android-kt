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
    
    // Permission launcher
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
    
    // Check permission on start
    LaunchedEffect(Unit) {
        hasPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == 
                       android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    // Start wake word detection when permission is granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val models = listOf(
                WakeWordModel(
                    name = "Hello World",
                    modelPath = "hello_world.onnx",
                    threshold = 0.5f
                )
            )
            
            val engine = WakeWordEngine(
                context = context,
                models = models
            )
            
            onEngineReady(engine)
            
            // Collect detection events
            launch {
                engine.detections.collect { detection ->
                    currentScore = detection.score
                    isDetected = true
                    
                    Toast.makeText(
                        context,
                        "${detection.model.name} detected! (${String.format("%.2f", detection.score)})",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Reset detection state after animation
                    delay(2000)
                    isDetected = false
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
            // Header
            HeaderSection()
            
            // Main visualization
            DetectionVisualization(
                isListening = isListening,
                isDetected = isDetected
            )
            
            // Status display
            StatusSection(
                status = status,
                isListening = isListening
            )
            
            // Wake word hint
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
        // Pulse ring when detected
        if (isDetected) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = 0.1f))
            )
        }
        
        // Center circle with status
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
                text = "\"Hello World\"",
                style = MaterialTheme.typography.headlineMedium,
                color = AccentGreen,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "to trigger detection",
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
        }
    }
}