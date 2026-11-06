package com.example.ui.settings

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ApiKeySettingsManager
import com.example.ui.editor.components.ApiKeyDialog
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apiKeyManager = remember { ApiKeySettingsManager(context) }
    var currentAiConfig by remember { mutableStateOf(apiKeyManager.loadConfig()) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & API Keys", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            // Custom API Key Configuration Card
            item {
                Text("CUSTOM FREE API KEY (GEMINI / OPENAI / AI STUDIO)", color = EmeraldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = VibeSurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(EmeraldLight)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (currentAiConfig.apiKey.isNotBlank()) {
                                            "Active: ${currentAiConfig.provider.displayName}"
                                        } else {
                                            "Using Default Built-in AI"
                                        },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Model: ${currentAiConfig.modelName}",
                                        color = VioletLight,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { showApiKeyDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (currentAiConfig.apiKey.isNotBlank()) "Edit Key" else "Insert API Key",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Paste any free API Key from Google AI Studio (Gemini), OpenAI, Groq, or DeepSeek. VIBE CUT AI will generate cuts, transitions, motion graphics, and color grades directly using your key.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

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
                            Text("Multi-Provider AI Video Engine", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Supports custom Google AI Studio / Gemini keys, OpenAI / Groq keys, and Firebase AI Logic with App Check.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Badge(containerColor = EmeraldDark) {
                                Text("Custom API Key Ready", color = EmeraldLight, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
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
                            title = "Media3 Transformer & ExoPlayer",
                            subtitle = "Hardware-accelerated video playback & MP4 rendering"
                        )
                        HorizontalDivider(color = VibeBorder, modifier = Modifier.padding(vertical = 10.dp))
                        SettingItemRow(
                            icon = Icons.Default.Speed,
                            title = "Multi-Track Interactive Timeline",
                            subtitle = "Direct clip selection, speed control (0.5x-2x), split & trim"
                        )
                        HorizontalDivider(color = VibeBorder, modifier = Modifier.padding(vertical = 10.dp))
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
        }

        if (showApiKeyDialog) {
            ApiKeyDialog(
                initialConfig = currentAiConfig,
                onSave = { newConfig ->
                    apiKeyManager.saveConfig(newConfig)
                    currentAiConfig = newConfig
                    showApiKeyDialog = false
                },
                onDismiss = { showApiKeyDialog = false }
            )
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
