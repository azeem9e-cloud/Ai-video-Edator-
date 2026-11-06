package com.example.ui.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun TimelineTrackView(
    clips: List<VideoClip>,
    audioTracks: List<AudioTrack>,
    textOverlays: List<TextOverlay>,
    motionGraphics: List<MotionGraphic>,
    transitions: List<TransitionEffect>,
    currentPositionMs: Long,
    totalDurationMs: Long,
    selectedClipId: String?,
    zoomFactor: Float,
    onSeek: (Long) -> Unit,
    onSelectClip: (String) -> Unit,
    onSplit: () -> Unit,
    onDeleteClip: () -> Unit,
    onUpdateClipSpeed: (String, Float) -> Unit,
    onTrimClip: (String, Long, Long) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val pxPerSec = (80 * zoomFactor).coerceIn(40f, 240f)
    val totalWidthDp = ((totalDurationMs / 1000f) * pxPerSec).coerceAtLeast(360f).dp
    val scrollState = rememberScrollState()
    val selectedClip = clips.find { it.id == selectedClipId } ?: clips.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VibeSurface)
            .border(1.dp, VibeBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(top = 6.dp)
            .testTag("timeline_container")
    ) {
        // Timeline Header: Quick Editing Actions (Split, Speed, Trim, Delete, Zoom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSplit,
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSurfaceElevated),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("split_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Split",
                        tint = EmeraldLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Split", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                if (selectedClip != null) {
                    // Speed cycle button (0.5x -> 1.0x -> 1.5x -> 2.0x)
                    val nextSpeed = when {
                        selectedClip.speed < 0.9f -> 1.0f
                        selectedClip.speed < 1.3f -> 1.5f
                        selectedClip.speed < 1.8f -> 2.0f
                        else -> 0.5f
                    }
                    Button(
                        onClick = { onUpdateClipSpeed(selectedClip.id, nextSpeed) },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletDark.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("speed_clip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = VioletLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${selectedClip.speed}x", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Trim -0.5s button
                    Button(
                        onClick = {
                            val newEnd = (selectedClip.endTrimMs - 500L).coerceAtLeast(selectedClip.startTrimMs + 1000L)
                            onTrimClip(selectedClip.id, selectedClip.startTrimMs, newEnd)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VibeSurfaceElevated),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("trim_shorten_button")
                    ) {
                        Text("Trim -0.5s", color = EmeraldLight, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Trim +0.5s button (restore up to originalDurationMs)
                    Button(
                        onClick = {
                            val newEnd = (selectedClip.endTrimMs + 500L).coerceAtMost(selectedClip.originalDurationMs.coerceAtLeast(selectedClip.endTrimMs + 500L))
                            onTrimClip(selectedClip.id, selectedClip.startTrimMs, newEnd)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VibeSurfaceElevated),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("trim_extend_button")
                    ) {
                        Text("+0.5s", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (clips.size > 1) {
                        Button(
                            onClick = onDeleteClip,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF451A1A)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("delete_clip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Zoom In / Out Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { onZoomChange((zoomFactor - 0.25f).coerceAtLeast(0.5f)) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "${(zoomFactor * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = { onZoomChange((zoomFactor + 0.25f).coerceAtMost(3.0f)) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = VibeBorder, thickness = 1.dp)

        // Tracks Scrollable Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .width(totalWidthDp + 160.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Interactive Time Ruler (Tap or drag here to scrub playhead without blocking clip clicks!)
                TimeRuler(
                    totalDurationMs = totalDurationMs,
                    pxPerSec = pxPerSec,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .pointerInput(totalDurationMs, pxPerSec) {
                            detectTapGestures { offset ->
                                val clickedSec = offset.x / (pxPerSec * density)
                                val targetMs = (clickedSec * 1000).toLong().coerceIn(0L, totalDurationMs)
                                onSeek(targetMs)
                            }
                        }
                        .pointerInput(totalDurationMs, pxPerSec) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val draggedSec = change.position.x / (pxPerSec * density)
                                val targetMs = (draggedSec * 1000).toLong().coerceIn(0L, totalDurationMs)
                                onSeek(targetMs)
                            }
                        }
                )

                // 2. Video Track (Clips - Directly clickable & selectable!)
                Text(
                    text = "VIDEO TRACK (TAP CLIP TO SELECT & EDIT)",
                    color = TextTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(VibeBackground, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    var runningStartMs = 0L
                    for (c in clips) {
                        val clipStartMs = runningStartMs
                        val clipWidthDp = ((c.durationMs / 1000f) * pxPerSec).coerceAtLeast(48f).dp
                        val isSelected = c.id == selectedClipId || (selectedClipId == null && c == clips.firstOrNull())
                        val clipTransition = transitions.find { it.clipId == c.id }

                        Box(
                            modifier = Modifier
                                .width(clipWidthDp)
                                .fillMaxHeight()
                                .background(
                                    color = if (isSelected) VideoTrackColor else VideoTrackColor.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) EmeraldLight else VideoTrackBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    onSelectClip(c.id)
                                    onSeek(clipStartMs + 100L)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = c.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (clipTransition != null && clipTransition.type != TransitionType.NONE) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "⚡${clipTransition.type.name.take(4)}",
                                            color = VioletLight,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "${String.format("%.1f", c.durationMs / 1000f)}s • ${c.speed}x",
                                    color = EmeraldLight,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        runningStartMs += c.durationMs
                    }
                }

                // 3. Audio Track (BGM & SFX)
                Text(
                    text = "AUDIO & SOUND DESIGN",
                    color = TextTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(VibeBackground, RoundedCornerShape(6.dp))
                        .padding(2.dp)
                ) {
                    for (audio in audioTracks) {
                        val offsetDp = ((audio.startOffsetMs / 1000f) * pxPerSec).dp
                        val widthDp = ((audio.durationMs / 1000f) * pxPerSec).coerceAtLeast(36f).dp

                        Box(
                            modifier = Modifier
                                .offset(x = offsetDp)
                                .width(widthDp)
                                .fillMaxHeight()
                                .background(
                                    if (audio.type == AudioType.SOUND_EFFECT) AmberAccent.copy(alpha = 0.35f) else AudioTrackColor,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    if (audio.type == AudioType.SOUND_EFFECT) AmberAccent else AudioTrackBorder,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { onSeek(audio.startOffsetMs) }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "♫ ${audio.name}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 4. Captions & Motion Graphics Track
                Text(
                    text = "TITLES & CAPTIONS",
                    color = TextTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(VibeBackground, RoundedCornerShape(6.dp))
                        .padding(2.dp)
                ) {
                    for (txt in textOverlays) {
                        val offsetDp = ((txt.startMs / 1000f) * pxPerSec).dp
                        val widthDp = (((txt.endMs - txt.startMs) / 1000f) * pxPerSec).coerceAtLeast(28f).dp

                        Box(
                            modifier = Modifier
                                .offset(x = offsetDp)
                                .width(widthDp)
                                .fillMaxHeight()
                                .background(TextTrackColor, RoundedCornerShape(4.dp))
                                .border(1.dp, TextTrackBorder, RoundedCornerShape(4.dp))
                                .clickable { onSeek(txt.startMs) }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "💬 ${txt.text}",
                                color = VioletLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Playhead Indicator Line
            val playheadOffsetDp = 16.dp + ((currentPositionMs / 1000f) * pxPerSec).dp
            Box(
                modifier = Modifier
                    .offset(x = playheadOffsetDp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(EmeraldLight)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-2).dp)
                        .size(12.dp)
                        .background(EmeraldLight, CircleShape)
                )
            }
        }
    }
}

@Composable
fun TimeRuler(
    totalDurationMs: Long,
    pxPerSec: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val totalSec = (totalDurationMs / 1000f).toInt() + 1
        for (sec in 0..totalSec) {
            val x = sec * pxPerSec * density
            drawLine(
                color = Color(0xFF64748B),
                start = Offset(x, 0f),
                end = Offset(x, size.height * 0.65f),
                strokeWidth = 2f
            )

            val halfX = x + (pxPerSec * density / 2f)
            drawLine(
                color = Color(0xFF334155),
                start = Offset(halfX, 0f),
                end = Offset(halfX, size.height * 0.35f),
                strokeWidth = 1f
            )
        }
    }
}
