package com.example.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransitionType
import com.example.ui.theme.*

@Composable
fun TransitionsSheet(
    selectedClipId: String?,
    onApplyTransition: (TransitionType) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transitions = listOf(
        TransitionType.CROSS_DISSOLVE to "Dissolve",
        TransitionType.ZOOM_IN to "Zoom In",
        TransitionType.GLITCH to "Glitch",
        TransitionType.FLASH to "White Flash",
        TransitionType.SLIDE_LEFT to "Slide Left",
        TransitionType.SLIDE_RIGHT to "Slide Right",
        TransitionType.FADE_BLACK to "Fade Black",
        TransitionType.NONE to "None"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.55f)
            .background(VibeSurface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(16.dp)
            .testTag("transitions_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shuffle, contentDescription = null, tint = EmeraldLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SCENE TRANSITIONS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Divider(color = VibeBorder, modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = if (selectedClipId != null) "Select transition for clip cut:" else "Choose default transition for all cuts:",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(transitions) { (type, label) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(VibeSurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, VibeBorder, RoundedCornerShape(8.dp))
                        .clickable { onApplyTransition(type) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
