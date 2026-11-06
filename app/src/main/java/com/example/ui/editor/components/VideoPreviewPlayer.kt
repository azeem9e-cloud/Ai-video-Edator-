package com.example.ui.editor.components

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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.sin

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

    // Keep latest state references for the smooth playback loop so it NEVER resets or glitches!
    val currentPosState by rememberUpdatedState(currentPositionMs)
    val totalDurState by rememberUpdatedState(totalDurationMs)
    val onSeekState by rememberUpdatedState(onSeek)
    val onTogglePlayState by rememberUpdatedState(onTogglePlay)

    // Determine active clip and relative offset inside that clip
    val activeClipInfo = remember(clips, currentPositionMs) {
        var accumulated = 0L
        var foundClip: VideoClip? = clips.firstOrNull()
        var clipStartInTimeline = 0L
        for (c in clips) {
            val dur = c.durationMs
            if (currentPositionMs in accumulated until (accumulated + dur)) {
                foundClip = c
                clipStartInTimeline = accumulated
                break
            }
            accumulated += dur
        }
        Pair(foundClip, (currentPositionMs - clipStartInTimeline).coerceAtLeast(0L))
    }

    val currentClip = activeClipInfo.first
    val offsetInClipMs = activeClipInfo.second

    // Check if there is an active transition on this clip (first 600ms of clip)
    val activeTransition = remember(transitions, currentClip?.id, offsetInClipMs) {
        if (currentClip != null && offsetInClipMs <= 650L) {
            transitions.find { it.clipId == currentClip.id }
        } else null
    }
    val transitionProgress = if (activeTransition != null) {
        (offsetInClipMs / 650f).coerceIn(0f, 1f)
    } else 1f

    // Real ExoPlayer instance for user-imported videos
    val isRealVideoUri = currentClip != null &&
            !currentClip.uri.startsWith("android.resource") &&
            !currentClip.uri.contains("drawable")

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Update ExoPlayer media item when clip URI changes
    LaunchedEffect(currentClip?.uri, isRealVideoUri) {
        if (isRealVideoUri && currentClip != null) {
            try {
                val mediaItem = MediaItem.fromUri(Uri.parse(currentClip.uri))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            } catch (_: Exception) {}
        }
    }

    // Sync Play/Pause, Speed, Volume with ExoPlayer
    LaunchedEffect(isPlaying, currentClip?.speed, currentClip?.volume, isRealVideoUri) {
        if (isRealVideoUri && currentClip != null) {
            exoPlayer.playbackParameters = PlaybackParameters(currentClip.speed.coerceIn(0.25f, 3.0f))
            exoPlayer.volume = currentClip.volume.coerceIn(0f, 1f)
            if (isPlaying) {
                exoPlayer.play()
            } else {
                exoPlayer.pause()
            }
        } else {
            exoPlayer.pause()
        }
    }

    // Smooth 30fps Playhead Ticker (Fixed: Uses rememberUpdatedState so it actually advances smoothly!)
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(33L)
                val nextPos = currentPosState + 33L
                val maxDur = totalDurState.coerceAtLeast(1000L)
                if (nextPos >= maxDur) {
                    onSeekState(0L)
                    if (isRealVideoUri) {
                        exoPlayer.seekTo(currentClip?.startTrimMs ?: 0L)
                    }
                    onTogglePlayState()
                    break
                } else {
                    onSeekState(nextPos)
                }
            }
        }
    }

    // Pulse animation for glow and HUD elements
    val infiniteTransition = rememberInfiniteTransition(label = "player_effects")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Dynamic camera motion / transition transform calculations
    val dynamicScale = remember(currentPositionMs, activeTransition, transitionProgress, isPlaying) {
        val baseMotion = if (isPlaying) {
            1.0f + 0.06f * sin((currentPositionMs % 4000L) / 4000f * Math.PI.toFloat())
        } else 1.0f

        when (activeTransition?.type) {
            TransitionType.ZOOM_IN -> baseMotion * (1.35f - 0.35f * transitionProgress)
            TransitionType.GLITCH -> if ((currentPositionMs / 60) % 2L == 0L) 1.08f else 1.0f
            else -> baseMotion
        }
    }

    val dynamicTranslationX = remember(activeTransition, transitionProgress) {
        when (activeTransition?.type) {
            TransitionType.SLIDE_LEFT -> (1f - transitionProgress) * 300f
            TransitionType.SLIDE_RIGHT -> (transitionProgress - 1f) * 300f
            TransitionType.GLITCH -> if ((transitionProgress * 10).toInt() % 2 == 0) 18f else -18f
            else -> 0f
        }
    }

    // Build real Android ColorMatrix from ColorGradeSettings (Contrast, Saturation, Exposure, Preset)
    val composeColorFilter = remember(colorGrade) {
        val cm = android.graphics.ColorMatrix()
        cm.setSaturation(colorGrade.saturation.coerceIn(0f, 2.5f))

        val c = colorGrade.contrast.coerceIn(0.5f, 2.2f)
        val exp = colorGrade.exposure * 60f
        val translate = (-0.5f * c + 0.5f) * 255f + exp

        // Apply Preset R/G/B tint multipliers
        val (rMult, gMult, bMult) = when (colorGrade.preset) {
            ColorPreset.CINEMATIC_TEAL_ORANGE -> Triple(1.08f, 1.0f, 1.12f)
            ColorPreset.MOODY_DARK -> Triple(0.88f, 0.92f, 1.05f)
            ColorPreset.WARM_FILM -> Triple(1.14f, 1.04f, 0.88f)
            ColorPreset.COOL_BLUE -> Triple(0.88f, 1.02f, 1.18f)
            ColorPreset.VIBRANT -> Triple(1.10f, 1.10f, 1.10f)
            ColorPreset.RETRO_GLITCH -> Triple(1.15f, 0.90f, 1.20f)
            else -> Triple(1.0f, 1.0f, 1.0f)
        }

        val contrastMatrix = android.graphics.ColorMatrix(
            floatArrayOf(
                c * rMult, 0f, 0f, 0f, translate,
                0f, c * gMult, 0f, 0f, translate,
                0f, 0f, c * bMult, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)
        ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(cm.array))
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
        // 1. Base Video or Template Footage with Real-Time Motion & ColorFilter
        if (currentClip != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = dynamicScale
                        scaleY = dynamicScale
                        translationX = dynamicTranslationX
                    }
            ) {
                if (!isRealVideoUri) {
                    AsyncImage(
                        model = currentClip.uri,
                        contentDescription = "Video Frame Preview",
                        colorFilter = composeColorFilter,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = exoPlayer
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            if (view.player != exoPlayer) {
                                view.player = exoPlayer
                            }
                            if (!isPlaying) {
                                val targetSeek = (currentClip.startTrimMs + (offsetInClipMs * currentClip.speed).toLong())
                                    .coerceIn(0L, currentClip.originalDurationMs)
                                if (kotlin.math.abs(exoPlayer.currentPosition - targetSeek) > 250L) {
                                    exoPlayer.seekTo(targetSeek)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VibeSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Video Loaded\nTap '+ Add' or 'Import Video' to load MP4",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 2. Live Color Grading, Vignette, and Transition Flash/Fade Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawColorGradeEffects(colorGrade, size)

            if (activeTransition != null) {
                val fadeAlpha = (1f - transitionProgress).coerceIn(0f, 1f)
                when (activeTransition.type) {
                    TransitionType.FLASH, TransitionType.FADE_WHITE -> {
                        drawRect(color = Color.White.copy(alpha = fadeAlpha * 0.85f))
                    }
                    TransitionType.FADE_BLACK, TransitionType.CROSS_DISSOLVE -> {
                        drawRect(color = Color.Black.copy(alpha = fadeAlpha * 0.85f))
                    }
                    TransitionType.GLITCH -> {
                        drawRect(
                            color = Color(0xFF10B981).copy(alpha = fadeAlpha * 0.35f),
                            blendMode = BlendMode.Screen
                        )
                    }
                    else -> {}
                }
            }
        }

        // 3. Live Motion Graphics Overlay
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

        // 4. Live Subtitles / Captions Overlay
        val activeCaptions = textOverlays.filter {
            currentPositionMs in it.startMs..it.endMs
        }
        for (caption in activeCaptions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                CaptionRenderer(caption = caption, positionMs = currentPositionMs)
            }
        }

        // 5. Center Play / Pause Indicator
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .testTag("play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = EmeraldLight,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // 6. Bottom OSD (On-Screen Display): Timecode, Active Clip, & Color Preset Badge
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Play",
                        tint = EmeraldLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${formatTime(currentPositionMs)} / ${formatTime(totalDurationMs)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (activeTransition != null && activeTransition.type != TransitionType.NONE) {
                    Text(
                        text = "⚡ ${activeTransition.type.name}",
                        color = VioletLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(VioletDark.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = colorGrade.preset.name.replace('_', ' '),
                    color = EmeraldLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawColorGradeEffects(grade: ColorGradeSettings, canvasSize: Size) {
    val tintColor = when (grade.preset) {
        ColorPreset.CINEMATIC_TEAL_ORANGE -> Color(0xFF00B4D8).copy(alpha = 0.14f)
        ColorPreset.MOODY_DARK -> Color(0xFF1E1B4B).copy(alpha = 0.22f)
        ColorPreset.WARM_FILM -> Color(0xFFF59E0B).copy(alpha = 0.14f)
        ColorPreset.COOL_BLUE -> Color(0xFF3B82F6).copy(alpha = 0.16f)
        ColorPreset.VIBRANT -> Color(0xFF10B981).copy(alpha = 0.12f)
        ColorPreset.RETRO_GLITCH -> Color(0xFF8B5CF6).copy(alpha = 0.18f)
        else -> Color.Transparent
    }

    if (tintColor != Color.Transparent) {
        drawRect(color = tintColor, blendMode = BlendMode.Overlay)
    }

    if (grade.vignette > 0f) {
        val radius = canvasSize.minDimension * (1.2f - (grade.vignette * 0.45f))
        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = grade.vignette.coerceIn(0f, 0.88f))),
                center = center,
                radius = radius
            )
        )
    }

    if (grade.exposure > 0.02f) {
        drawRect(color = Color.White.copy(alpha = (grade.exposure * 0.25f).coerceIn(0f, 0.35f)), blendMode = BlendMode.Screen)
    } else if (grade.exposure < -0.02f) {
        drawRect(color = Color.Black.copy(alpha = (-grade.exposure * 0.35f).coerceIn(0f, 0.55f)))
    }
}

@Composable
fun MotionGraphicRenderer(mg: MotionGraphic, pulseAlpha: Float) {
    when (mg.type) {
        MotionGraphicType.LOWER_THIRD -> {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(28.dp)
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
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        MotionGraphicType.HUD_TAG -> {
            Box(
                modifier = Modifier
                    .background(Color(0xFF0B192C).copy(alpha = pulseAlpha * 0.92f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "[ ${mg.primaryText} ]",
                    color = EmeraldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
        MotionGraphicType.KINETIC_TITLE -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = mg.primaryText,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
                if (mg.secondaryText.isNotBlank()) {
                    Text(
                        text = mg.secondaryText,
                        color = EmeraldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        MotionGraphicType.GLOW_PULSE -> {
            Text(
                text = "⚡ ${mg.primaryText}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .background(VioletDark.copy(alpha = pulseAlpha), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
        else -> {
            Text(
                text = mg.primaryText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun CaptionRenderer(caption: TextOverlay, positionMs: Long) {
    val bgColor = try {
        caption.bgColorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Transparent
    } catch (_: Exception) {
        Color.Black.copy(alpha = 0.7f)
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
            val words = caption.text.split(" ")
            val totalCaptionDuration = (caption.endMs - caption.startMs).coerceAtLeast(1L)
            val progress = ((positionMs - caption.startMs).toFloat() / totalCaptionDuration).coerceIn(0f, 1f)
            val activeWordIndex = (progress * words.size).toInt().coerceIn(0, (words.size - 1).coerceAtLeast(0))

            Row(horizontalArrangement = Arrangement.Center) {
                words.forEachIndexed { idx, word ->
                    val isHighlighted = idx <= activeWordIndex
                    Text(
                        text = "$word ",
                        color = if (isHighlighted) EmeraldLight else Color.White.copy(alpha = 0.65f),
                        fontSize = caption.fontSizeSp.sp,
                        fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.SemiBold,
                        textAlign = if (caption.isUrdu) TextAlign.Right else TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = caption.text,
                color = textColor,
                fontSize = caption.fontSizeSp.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = if (caption.isUrdu) TextAlign.Right else TextAlign.Center
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}
