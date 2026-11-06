package com.example.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CaptionAnimation
import com.example.model.TextOverlay
import com.example.ui.theme.*
import java.util.UUID

@Composable
fun SubtitlesSheet(
    activeCaptions: List<TextOverlay>,
    currentPlayheadMs: Long,
    onAddCaption: (TextOverlay) -> Unit,
    onRemoveCaption: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var captionText by remember { mutableStateOf("Watch this incredible moment unfold!") }
    var selectedAnim by remember { mutableStateOf(CaptionAnimation.KARAOKE) }
    var isUrdu by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf("#34D399") } // Emerald default

    val colorOptions = listOf(
        "#FFFFFF" to "White",
        "#34D399" to "Emerald",
        "#FBBF24" to "Gold",
        "#A78BFA" to "Violet",
        "#38BDF8" to "Cyan",
        "#F43F5E" to "Rose"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.70f)
            .background(VibeSurface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("subtitles_sheet")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SUBTITLES & CAPTIONS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Divider(color = VibeBorder, modifier = Modifier.padding(vertical = 8.dp))

        // Language toggle (Urdu / English)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LANGUAGE SUPPORT", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isUrdu,
                    onClick = {
                        isUrdu = false
                        captionText = "Watch this incredible moment unfold!"
                    },
                    label = { Text("English", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = isUrdu,
                    onClick = {
                        isUrdu = true
                        captionText = "یہ خوبصورت لمحہ ضرور دیکھیں!"
                    },
                    label = { Text("اردو (Urdu)", fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = captionText,
            onValueChange = { captionText = it },
            label = { Text(if (isUrdu) "متن درج کریں" else "Caption Text", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldLight,
                unfocusedBorderColor = VibeBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Animation Style selector
        Text("ANIMATION / KARAOKE STYLE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CaptionAnimation.values().forEach { anim ->
                FilterChip(
                    selected = selectedAnim == anim,
                    onClick = { selectedAnim = anim },
                    label = { Text(anim.name, fontSize = 11.sp) }
                )
            }
        }

        // Color selector
        Text("HIGHLIGHT COLOR", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            colorOptions.forEach { (hex, name) ->
                val isSelected = selectedColor == hex
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(android.graphics.Color.parseColor(hex)), RoundedCornerShape(8.dp))
                        .border(
                            2.dp,
                            if (isSelected) Color.White else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedColor = hex }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val overlay = TextOverlay(
                    id = UUID.randomUUID().toString(),
                    text = captionText,
                    startMs = currentPlayheadMs,
                    endMs = currentPlayheadMs + 3000L,
                    textColorHex = selectedColor,
                    animationStyle = selectedAnim,
                    isUrdu = isUrdu
                )
                onAddCaption(overlay)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Insert Caption at Current Time", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("ACTIVE CAPTIONS IN TIMELINE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        for (item in activeCaptions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VibeSurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${item.animationStyle.name} • ${item.startMs / 1000f}s - ${item.endMs / 1000f}s",
                        color = EmeraldLight,
                        fontSize = 10.sp
                    )
                }
                IconButton(onClick = { onRemoveCaption(item.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
