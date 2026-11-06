package com.example.ui.editor.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.engine.ExportProgress
import com.example.model.ExportConfig
import com.example.ui.theme.*
import java.io.File

@Composable
fun ExportDialog(
    exportProgress: ExportProgress?,
    onStartExport: (ExportConfig) -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var selectedRes by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableStateOf(30) }
    var isH265 by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (exportProgress == null || exportProgress.isComplete || exportProgress.error != null) {
                onDismiss()
            }
        },
        containerColor = VibeSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = null, tint = EmeraldLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXPORT VIDEO (MP4)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (exportProgress == null) {
                    // Export Settings Form
                    Text("RESOLUTION", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("720p", "1080p", "4K").forEach { res ->
                            FilterChip(
                                selected = selectedRes == res,
                                onClick = { selectedRes = res },
                                label = { Text(res, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("FRAME RATE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(24, 30, 60).forEach { fps ->
                            FilterChip(
                                selected = selectedFps == fps,
                                onClick = { selectedFps = fps },
                                label = { Text("${fps} fps", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("VIDEO CODEC", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isH265,
                            onClick = { isH265 = false },
                            label = { Text("H.264 (Universal)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = isH265,
                            onClick = { isH265 = true },
                            label = { Text("H.265 (HEVC)", fontSize = 11.sp) }
                        )
                    }
                } else if (exportProgress.isComplete && exportProgress.outputFile != null) {
                    // Export Succeeded
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = EmeraldLight,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Render Successfully Finished!",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Saved to:\n${exportProgress.outputFile.name}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { shareExportedVideo(context, exportProgress.outputFile) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share / Save Video", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (exportProgress.error != null) {
                    // Export Failed
                    Text(
                        text = "Error during export: ${exportProgress.error}",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
                    )
                } else {
                    // Progress Bar
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { exportProgress.percentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = EmeraldLight,
                            trackColor = VibeBorder,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${exportProgress.percentage}% — ${exportProgress.stage}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (exportProgress == null) {
                Button(
                    onClick = {
                        val (w, h) = when (selectedRes) {
                            "720p" -> 1280 to 720
                            "4K" -> 3840 to 2160
                            else -> 1920 to 1080
                        }
                        onStartExport(
                            ExportConfig(
                                resolutionLabel = selectedRes,
                                width = w,
                                height = h,
                                fps = selectedFps,
                                isH265 = isH265,
                                bitrateKbps = if (selectedRes == "4K") 25000 else 8000
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("start_export_button")
                ) {
                    Text("Render MP4", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else if (exportProgress.isComplete || exportProgress.error != null) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = TextSecondary)
                }
            } else {
                TextButton(onClick = onCancelExport) {
                    Text("Cancel", color = Color(0xFFEF4444))
                }
            }
        },
        dismissButton = {
            if (exportProgress == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    )
}

private fun shareExportedVideo(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Finished Video"))
    } catch (e: Exception) {
        // Fallback standard share
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_TEXT, "Exported with VIBE CUT AI: ${file.absolutePath}")
        }
        context.startActivity(Intent.createChooser(intent, "Share Video"))
    }
}
