package com.example.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioTrack
import com.example.model.AudioType
import com.example.ui.theme.*
import java.util.UUID

@Composable
fun AudioDesignSheet(
    audioTracks: List<AudioTrack>,
    currentPlayheadMs: Long,
    onPlaySfx: (String) -> Unit,
    onAddTrack: (AudioTrack) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sfxLibrary = listOf(
        "WHOOSH" to "Cinematic Whoosh Transition",
        "IMPACT" to "Dramatic Low Impact Hit",
        "RISER" to "Tension Audio Pitch Riser",
        "BASS_DROP" to "Heavy 808 Trap Sub Drop",
        "GLITCH" to "Digital Artifact Glitch Burst",
        "CHIME" to "Success Bell Chime"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f)
            .background(VibeSurface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("audio_design_sheet")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = EmeraldLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SOUND DESIGN & AUDIO MIXER", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Divider(color = VibeBorder, modifier = Modifier.padding(vertical = 8.dp))

        Text("INTELLIGENT SFX ACCENTS", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        for ((type, desc) in sfxLibrary) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(VibeSurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(type, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(desc, color = TextSecondary, fontSize = 10.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Test listen button
                    IconButton(
                        onClick = { onPlaySfx(type) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Listen", tint = EmeraldLight, modifier = Modifier.size(18.dp))
                    }

                    // Add to timeline button
                    Button(
                        onClick = {
                            onPlaySfx(type)
                            val track = AudioTrack(
                                id = UUID.randomUUID().toString(),
                                uri = null,
                                name = "$type SFX",
                                type = AudioType.SOUND_EFFECT,
                                startOffsetMs = currentPlayheadMs,
                                durationMs = 800L
                            )
                            onAddTrack(track)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Add", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
