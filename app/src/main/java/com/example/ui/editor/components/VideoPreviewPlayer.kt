package com.example.ui.editor.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun VideoPreviewPlayer(
    clips: List<VideoClip>,
    currentPositionMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    colorGrade: ColorGradeSettings,
    textOverlays: List<TextOverlay>,
    motionGraphics: List<MotionGraphic>,
    transitions: List<TransitionEffect>,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentClip = remember(clips, currentPositionMs) {
        var accumulated = 0L
        var found: VideoClip? = null
        for (c in clips) {
            val dur = c.durationMs
            if (currentPositionMs in accumulated until (accumulated + dur)) {
                found = c
                break
            }
            accumulated += dur
        }
        found ?: clips.firstOrNull()
    }

    // Pulse animation for glow and HUD elements
    val infiniteTransition = rememberInfiniteTransition(label = "player_effects")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Playhead ticker when playing
    LaunchedEffect(isPlaying, totalDurationMs) {
        if (isPlaying) {
            while (true) {
                delay(33) // ~30 fps update
                val nextPos = currentPositionMs + 33
                if (nextPos >= totalDurationMs) {
                    onSeek(0L)
                    onTogglePlay()
                    break
                } else {
                    onSeek(nextPos)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(VibeBackground)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onTogglePlay() }
            .testTag("video_preview_box"),
        contentAlignment = Alignment.Center
    ) {
        // Base Video / Poster rendering
        if (currentClip != null) {
            if (currentClip.uri.startsWith("android.resource") || currentClip.uri.contains("drawable")) {
                AsyncImage(
                    model = currentClip.uri,
                    contentDescription = "Video Frame Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // ExoPlayer surface
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Empty state placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VibeSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Video Loaded\nImport a clip or select a template",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Live Color Grading and Vignette Overlay (Drawn on Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawColorGradeEffects(colorGrade, size)
        }

        // Live Motion Graphics Overlay
        val activeGraphics = motionGraphics.filter {
            currentPositionMs in it.startMs..it.endMs
        }
        for (mg in activeGraphics) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = when (mg.type) {
                    MotionGraphicType.LOWER_THIRD -> Alignment.BottomStart
                    MotionGraphicType.HUD_TAG -> Alignment.TopCenter
                    MotionGraphicType.KINETIC_TITLE -> Alignment.Center
                    MotionGraphicType.CALLOUT -> Alignment.CenterEnd
                    MotionGraphicType.GLOW_PULSE -> Alignment.TopStart
                    MotionGraphicType.LIGHT_LEAK -> Alignment.Center
                }
            ) {
                MotionGraphicRenderer(mg = mg, pulseAlpha = pulseAlpha)
            }
        }

        // Live Subtitles / Captions Overlay
        val activeCaptions = textOverlays.filter {
            currentPositionMs in it.startMs..it.endMs
        }
        for (caption in activeCaptions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                CaptionRenderer(caption = caption, positionMs = currentPositionMs)
            }
        }

        // Center Play / Pause Indicator
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .testTag("play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = EmeraldLight,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Bottom OSD (On-Screen Display): Timecode & Resolution
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatTime(currentPositionMs)} / ${formatTime(totalDurationMs)}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${colorGrade.preset.name.replace('_', ' ')}",
                    color = EmeraldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawColorGradeEffects(grade: ColorGradeSettings, canvasSize: Size) {
    // 1. Color tint filter / atmosphere
    val tintColor = when (grade.preset) {
        ColorPreset.CINEMATIC_TEAL_ORANGE -> Color(0xFF00B4D8).copy(alpha = 0.12f)
        ColorPreset.MOODY_DARK -> Color(0xFF1E1B4B).copy(alpha = 0.18f)
        ColorPreset.WARM_FILM -> Color(0xFFF59E0B).copy(alpha = 0.12f)
        ColorPreset.COOL_BLUE -> Color(0xFF3B82F6).copy(alpha = 0.14f)
        ColorPreset.VIBRANT -> Color(0xFF10B981).copy(alpha = 0.10f)
        ColorPreset.RETRO_GLITCH -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    if (tintColor != Color.Transparent) {
        drawRect(color = tintColor, blendMode = BlendMode.Overlay)
    }

    // 2. Vignette effect
    if (grade.vignette > 0f) {
        val radius = canvasSize.minDimension * (1.2f - (grade.vignette * 0.4f))
        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = grade.vignette.coerceIn(0f, 0.85f))),
                center = center,
                radius = radius
            )
        )
    }

    // 3. Exposure / brightness compensation
    if (grade.exposure > 0.05f) {
        drawRect(color = Color.White.copy(alpha = (grade.exposure * 0.2f).coerceIn(0f, 0.3f)), blendMode = BlendMode.Screen)
    } else if (grade.exposure < -0.05f) {
        drawRect(color = Color.Black.copy(alpha = (-grade.exposure * 0.3f).coerceIn(0f, 0.5f)))
    }
}

@Composable
fun MotionGraphicRenderer(mg: MotionGraphic, pulseAlpha: Float) {
    when (mg.type) {
        MotionGraphicType.LOWER_THIRD -> {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(26.dp)
                        .background(EmeraldLight)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = mg.primaryText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (mg.secondaryText.isNotBlank()) {
                        Text(
                            text = mg.secondaryText,
                            color = VioletLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
        MotionGraphicType.HUD_TAG -> {
            Box(
                modifier = Modifier
                    .background(Color(0xFF0B192C).copy(alpha = pulseAlpha * 0.9f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "[ ${mg.primaryText} ]",
                    color = EmeraldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }
        MotionGraphicType.KINETIC_TITLE -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = mg.primaryText,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall
                )
                if (mg.secondaryText.isNotBlank()) {
                    Text(
                        text = mg.secondaryText,
                        color = EmeraldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        MotionGraphicType.GLOW_PULSE -> {
            Text(
                text = "⚡ ${mg.primaryText}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(VioletDark.copy(alpha = pulseAlpha), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        else -> {
            Text(
                text = mg.primaryText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun CaptionRenderer(caption: TextOverlay, positionMs: Long) {
    val bgColor = try {
        caption.bgColorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Transparent
    } catch (_: Exception) {
        Color.Black.copy(alpha = 0.6f)
    }

    val textColor = try {
        Color(android.graphics.Color.parseColor(caption.textColorHex))
    } catch (_: Exception) {
        Color.White
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        if (caption.animationStyle == CaptionAnimation.KARAOKE) {
            // Words animation: highlight active word
            val words = caption.text.split(" ")
            val totalCaptionDuration = (caption.endMs - caption.startMs).coerceAtLeast(1L)
            val progress = ((positionMs - caption.startMs).toFloat() / totalCaptionDuration).coerceIn(0f, 1f)
            val activeWordIndex = (progress * words.size).toInt().coerceIn(0, words.size - 1)

            Row(horizontalArrangement = Arrangement.Center) {
                words.forEachIndexed { idx, word ->
                    val isHighlighted = idx <= activeWordIndex
                    Text(
                        text = "$word ",
                        color = if (isHighlighted) EmeraldLight else Color.White.copy(alpha = 0.6f),
                        fontSize = caption.fontSizeSp.sp,
                        fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Normal,
                        textAlign = if (caption.isUrdu) TextAlign.Right else TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = caption.text,
                color = textColor,
                fontSize = caption.fontSizeSp.sp,
                fontWeight = FontWeight.Bold,
                textAlign = if (caption.isUrdu) TextAlign.Right else TextAlign.Center
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}
