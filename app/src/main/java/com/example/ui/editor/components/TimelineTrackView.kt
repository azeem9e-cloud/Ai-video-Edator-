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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pixels per second at 1.0 zoom: 80.dp
    val pxPerSec = (80 * zoomFactor).coerceIn(40f, 240f)
    val msPerDp = 1000f / pxPerSec
    val totalWidthDp = ((totalDurationMs / 1000f) * pxPerSec).coerceAtLeast(360f).dp

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VibeSurface)
            .border(1.dp, VibeBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(top = 8.dp)
            .testTag("timeline_container")
    ) {
        // Timeline Header: Quick Editing Actions (Split, Trim, Delete, Zoom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onSplit,
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSurfaceElevated),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("split_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Split",
                        tint = EmeraldLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Split", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                if (selectedClipId != null) {
                    Button(
                        onClick = onDeleteClip,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF451A1A)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("delete_clip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Zoom In / Out Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onZoomChange((zoomFactor - 0.25f).coerceAtLeast(0.5f)) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "${(zoomFactor * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = { onZoomChange((zoomFactor + 0.25f).coerceAtMost(3.0f)) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Divider(color = VibeBorder, thickness = 1.dp)

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
                // 1. Time Ruler
                TimeRuler(
                    totalDurationMs = totalDurationMs,
                    pxPerSec = pxPerSec,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )

                // 2. Video Track (Clips)
                Text(
                    text = "VIDEO TRACK",
                    color = TextTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(VibeBackground, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (c in clips) {
                        val clipWidthDp = ((c.durationMs / 1000f) * pxPerSec).coerceAtLeast(40f).dp
                        val isSelected = c.id == selectedClipId

                        Box(
                            modifier = Modifier
                                .width(clipWidthDp)
                                .fillMaxHeight()
                                .background(
                                    color = if (isSelected) VideoTrackColor else VideoTrackColor.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) EmeraldLight else VideoTrackBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectClip(c.id) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    text = c.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${c.durationMs / 1000f}s • ${c.speed}x",
                                    color = EmeraldLight,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // 3. Audio Track (BGM & SFX)
                Text(
                    text = "AUDIO & SOUND DESIGN",
                    color = TextTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(VibeBackground, RoundedCornerShape(6.dp))
                        .padding(2.dp)
                ) {
                    for (audio in audioTracks) {
                        val offsetDp = ((audio.startOffsetMs / 1000f) * pxPerSec).dp
                        val widthDp = ((audio.durationMs / 1000f) * pxPerSec).coerceAtLeast(30f).dp

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
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(VibeBackground, RoundedCornerShape(6.dp))
                        .padding(2.dp)
                ) {
                    for (txt in textOverlays) {
                        val offsetDp = ((txt.startMs / 1000f) * pxPerSec).dp
                        val widthDp = (((txt.endMs - txt.startMs) / 1000f) * pxPerSec).coerceAtLeast(24f).dp

                        Box(
                            modifier = Modifier
                                .offset(x = offsetDp)
                                .width(widthDp)
                                .fillMaxHeight()
                                .background(TextTrackColor, RoundedCornerShape(4.dp))
                                .border(1.dp, TextTrackBorder, RoundedCornerShape(4.dp))
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

            // Draggable Playhead Scrubber Line
            val playheadOffsetDp = 16.dp + ((currentPositionMs / 1000f) * pxPerSec).dp
            Box(
                modifier = Modifier
                    .offset(x = playheadOffsetDp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(EmeraldLight)
            ) {
                // Playhead needle head
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-4).dp)
                        .size(14.dp)
                        .background(EmeraldLight, CircleShape)
                )
            }

            // Scrubber drag detector covering full area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalDurationMs, pxPerSec) {
                        detectTapGestures { offset ->
                            val clickedSec = (offset.x - 16.dp.toPx()) / (pxPerSec * density)
                            val targetMs = (clickedSec * 1000).toLong().coerceIn(0L, totalDurationMs)
                            onSeek(targetMs)
                        }
                    }
                    .pointerInput(totalDurationMs, pxPerSec) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val draggedSec = (change.position.x - 16.dp.toPx()) / (pxPerSec * density)
                            val targetMs = (draggedSec * 1000).toLong().coerceIn(0L, totalDurationMs)
                            onSeek(targetMs)
                        }
                    }
            )
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
            // Major second tick
            drawLine(
                color = Color(0xFF64748B),
                start = Offset(x, 0f),
                end = Offset(x, size.height * 0.6f),
                strokeWidth = 2f
            )

            // Minor half-second tick
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
