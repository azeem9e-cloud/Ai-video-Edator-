package com.example.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColorGradeSettings
import com.example.model.ColorPreset
import com.example.ui.theme.*

@Composable
fun ColorGradingSheet(
    colorGrade: ColorGradeSettings,
    onUpdateGrade: (ColorGradeSettings) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.65f)
            .background(VibeSurface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("color_grading_sheet")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ColorLens, contentDescription = null, tint = EmeraldLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text("COLOR GRADING & LUT PRESETS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        Divider(color = VibeBorder, modifier = Modifier.padding(vertical = 8.dp))

        // Preset Chips
        Text("CINEMATIC LOOKS", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorPreset.values().forEach { preset ->
                val isSelected = colorGrade.preset == preset
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val updated = when (preset) {
                            ColorPreset.CINEMATIC_TEAL_ORANGE -> colorGrade.copy(preset = preset, contrast = 1.25f, saturation = 1.3f, temperature = 0.2f, vignette = 0.35f)
                            ColorPreset.MOODY_DARK -> colorGrade.copy(preset = preset, exposure = -0.15f, contrast = 1.35f, saturation = 0.9f, vignette = 0.5f)
                            ColorPreset.WARM_FILM -> colorGrade.copy(preset = preset, temperature = 0.35f, contrast = 1.15f, saturation = 1.1f, vignette = 0.2f)
                            ColorPreset.COOL_BLUE -> colorGrade.copy(preset = preset, temperature = -0.3f, contrast = 1.2f, saturation = 1.1f)
                            ColorPreset.VIBRANT -> colorGrade.copy(preset = preset, contrast = 1.2f, saturation = 1.45f)
                            ColorPreset.RETRO_GLITCH -> colorGrade.copy(preset = preset, contrast = 1.4f, saturation = 1.4f, vignette = 0.45f)
                            else -> ColorGradeSettings(preset = preset)
                        }
                        onUpdateGrade(updated)
                    },
                    label = { Text(preset.name.replace('_', ' '), fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = VibeSurfaceElevated,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sliders
        GradeSlider(
            label = "Exposure",
            value = colorGrade.exposure,
            range = -0.5f..0.5f,
            onValueChange = { onUpdateGrade(colorGrade.copy(exposure = it)) }
        )

        GradeSlider(
            label = "Contrast",
            value = colorGrade.contrast,
            range = 0.5f..2.0f,
            onValueChange = { onUpdateGrade(colorGrade.copy(contrast = it)) }
        )

        GradeSlider(
            label = "Saturation",
            value = colorGrade.saturation,
            range = 0.0f..2.0f,
            onValueChange = { onUpdateGrade(colorGrade.copy(saturation = it)) }
        )

        GradeSlider(
            label = "Vignette (Cinematic Falloff)",
            value = colorGrade.vignette,
            range = 0.0f..0.8f,
            onValueChange = { onUpdateGrade(colorGrade.copy(vignette = it)) }
        )
    }
}

@Composable
fun GradeSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(String.format("%.2f", value), color = EmeraldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = EmeraldLight,
                activeTrackColor = EmeraldPrimary,
                inactiveTrackColor = VibeBorder
            )
        )
    }
}
