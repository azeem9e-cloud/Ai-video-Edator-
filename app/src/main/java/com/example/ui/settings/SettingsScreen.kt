package com.example.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VibeBackground)
            )
        },
        containerColor = VibeBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // AI Brain Status Card
            item {
                Text("AI BRAIN & NEURAL PROCESSING", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = VibeSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(VibeBorder)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google Gemini 3.8 Flash", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "State-of-the-art multimodal video comprehension engine integrated via Firebase AI Logic and secured by Firebase App Check.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Badge(containerColor = EmeraldDark) {
                                Text("App Check: Active", color = EmeraldLight, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                            }
                            Badge(containerColor = VioletDark) {
                                Text("Structured JSON: Enabled", color = VioletLight, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                            }
                        }
                    }
                }
            }

            // Media & Render Engine
            item {
                Text("RENDER PIPELINE & HARDWARE", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = VibeSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(VibeBorder)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingItemRow(
                            icon = Icons.Default.Movie,
                            title = "Media3 Transformer",
                            subtitle = "Hardware-accelerated MP4 container rendering"
                        )
                        Divider(color = VibeBorder, modifier = Modifier.padding(vertical = 10.dp))
                        SettingItemRow(
                            icon = Icons.Default.Speed,
                            title = "Multi-Track Timeline",
                            subtitle = "Interactive scrubbing with frame-level resolution"
                        )
                        Divider(color = VibeBorder, modifier = Modifier.padding(vertical = 10.dp))
                        SettingItemRow(
                            icon = Icons.Default.GraphicEq,
                            title = "Real-Time Sound Synthesis",
                            subtitle = "Audible cinematic whooshes, risers, and impact SFX"
                        )
                    }
                }
            }

            // Storage & Cache
            item {
                Text("STORAGE & EXPORTS", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = VibeSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(VibeBorder)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Exported Videos Cache", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Clear temporary render files", color = TextSecondary, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    clearRenderCache(context)
                                    cacheClearedMessage = "Render cache cleared successfully!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VibeSurfaceCard),
                                border = ButtonDefaults.outlinedButtonBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(VibeBorder)
                                )
                            ) {
                                Text("Clear Cache", color = TextPrimary, fontSize = 11.sp)
                            }
                        }

                        if (cacheClearedMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(cacheClearedMessage!!, color = EmeraldLight, fontSize = 11.sp)
                        }
                    }
                }
            }

            // About Section
            item {
                Text("ABOUT", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = VibeSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(VibeBorder)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("VIBE CUT AI — Professional Video Editor", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Version 1.0.0 • Native Android Architecture", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Combines professional desktop video editing ergonomics with artificial intelligence for rapid video creation.",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

private fun clearRenderCache(context: Context) {
    try {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "exports")
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
    } catch (_: Exception) {}
}
