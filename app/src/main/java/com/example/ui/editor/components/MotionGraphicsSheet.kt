package com.example.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.model.MotionGraphic
import com.example.model.MotionGraphicType
import com.example.ui.theme.*
import java.util.UUID

@Composable
fun MotionGraphicsSheet(
    activeGraphics: List<MotionGraphic>,
    currentPlayheadMs: Long,
    onAddGraphic: (MotionGraphic) -> Unit,
    onRemoveGraphic: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var titleInput by remember { mutableStateOf("VIBE CUT HIGHLIGHT") }
    var subtitleInput by remember { mutableStateOf("AI MOTION GRAPHICS") }
    var selectedType by remember { mutableStateOf(MotionGraphicType.LOWER_THIRD) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.70f)
            .background(VibeSurface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("motion_graphics_sheet")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MOTION GRAPHICS ENGINE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Divider(color = VibeBorder, modifier = Modifier.padding(vertical = 8.dp))

        Text("SELECT GRAPHIC STYLE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                MotionGraphicType.LOWER_THIRD to "Lower 3rd",
                MotionGraphicType.HUD_TAG to "HUD Tag",
                MotionGraphicType.KINETIC_TITLE to "Kinetic",
                MotionGraphicType.GLOW_PULSE to "Neon Glow"
            ).forEach { (type, label) ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) VioletDark else VibeSurfaceElevated,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) VioletLight else VibeBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedType = type }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text("Primary Text", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VioletLight,
                unfocusedBorderColor = VibeBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = subtitleInput,
            onValueChange = { subtitleInput = it },
            label = { Text("Secondary Text / Subtitle", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VioletLight,
                unfocusedBorderColor = VibeBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val graphic = MotionGraphic(
                    id = UUID.randomUUID().toString(),
                    type = selectedType,
                    primaryText = titleInput,
                    secondaryText = subtitleInput,
                    startMs = currentPlayheadMs,
                    endMs = currentPlayheadMs + 3500L
                )
                onAddGraphic(graphic)
            },
            colors = ButtonDefaults.buttonColors(containerColor = VioletSecondary),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add to Timeline at Current Time", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("ACTIVE MOTION GRAPHICS IN TIMELINE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        for (item in activeGraphics) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VibeSurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(item.primaryText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${item.type.name} • ${item.startMs / 1000f}s - ${item.endMs / 1000f}s",
                        color = VioletLight,
                        fontSize = 10.sp
                    )
                }
                IconButton(onClick = { onRemoveGraphic(item.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
