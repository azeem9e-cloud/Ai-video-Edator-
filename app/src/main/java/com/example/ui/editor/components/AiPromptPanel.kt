package com.example.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.EditingPlan
import com.example.ui.theme.*

@Composable
fun AiPromptPanel(
    chatMessages: List<ChatMessage>,
    isLoading: Boolean,
    errorMessage: String?,
    latestPlan: EditingPlan?,
    hasCustomApiKey: Boolean = false,
    activeProviderLabel: String = "Built-in Gemini",
    onOpenApiKeyDialog: () -> Unit = {},
    onSendPrompt: (prompt: String, referenceUrl: String?) -> Unit,
    onPreviewPlan: (EditingPlan) -> Unit,
    onApplyPlan: (EditingPlan) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    var referenceUrlInput by remember { mutableStateOf("") }
    var showLinkClonerBox by remember { mutableStateOf(true) }
    val clipboardManager = LocalClipboardManager.current

    val quickPrompts = listOf(
        "Clone Viral Shorts Style (Fast Cuts + Karaoke)",
        "Make this video cinematic and professional",
        "Add Urdu Animated Subtitles & Glow",
        "Add Smooth Zoom Transitions & Whoosh SFX",
        "Make the colors look dark & premium",
        "Highlight important moments with HUD tags"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.86f)
            .background(VibeSurface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(16.dp)
            .testTag("ai_prompt_panel")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Editor",
                    tint = EmeraldLight,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "VIBE CUT AI & LINK CLONER",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Engine: $activeProviderLabel",
                        color = if (hasCustomApiKey) EmeraldLight else VioletLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Button to open Custom Free API Key Popup directly from AI Panel
                OutlinedButton(
                    onClick = onOpenApiKeyDialog,
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(EmeraldLight)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp).testTag("ai_panel_api_key_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Set API Key",
                        tint = EmeraldLight,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasCustomApiKey) "API Key ✓" else "Set API Key",
                        color = EmeraldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = { showLinkClonerBox = !showLinkClonerBox },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Toggle Link Box",
                        tint = VioletLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (showLinkClonerBox) "Hide Link" else "Link",
                        color = VioletLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }
        }

        HorizontalDivider(color = VibeBorder, modifier = Modifier.padding(vertical = 8.dp))

        // Reference Video Link Style Cloner Card
        if (showLinkClonerBox) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141D30)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(VioletSecondary)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("reference_link_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentPasteSearch,
                                contentDescription = null,
                                tint = VioletLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CLONE EDITING STYLE FROM VIDEO LINK",
                                color = VioletLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        TextButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    referenceUrlInput = clipText
                                } else {
                                    referenceUrlInput = "https://youtube.com/shorts/viral-neon-edit"
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste URL", color = EmeraldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = referenceUrlInput,
                            onValueChange = { referenceUrlInput = it },
                            placeholder = {
                                Text(
                                    "Paste YouTube Shorts, TikTok, or Instagram Reel link...",
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reference_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VioletLight,
                                unfocusedBorderColor = VibeBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val urlToUse = referenceUrlInput.ifBlank { "https://youtube.com/shorts/viral-reference" }
                                val customNote = promptInput.ifBlank {
                                    "Copy the exact eye-catching editing style, transitions, color grading, captions, and sound effects from this video link onto my video."
                                }
                                promptInput = ""
                                onSendPrompt(customNote, urlToUse)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VioletSecondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("clone_style_button")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clone Style", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Suggestion Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (qp in quickPrompts) {
                SuggestionChip(
                    onClick = {
                        onSendPrompt(qp, referenceUrlInput.takeIf { it.isNotBlank() })
                    },
                    label = { Text(qp, fontSize = 11.sp, color = TextPrimary) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = VibeSurfaceElevated
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = VibeBorder
                    )
                )
            }
        }

        // Chat Message History
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages) { msg ->
                val isUser = msg.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .background(
                                color = if (isUser) VioletDark.copy(alpha = 0.5f) else VibeSurfaceElevated,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isUser) VioletSecondary else VibeBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isUser) "You" else "Vibe Cut AI ($activeProviderLabel)",
                                color = if (isUser) VioletLight else EmeraldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.text,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )

                            // Interactive Plan Card with Live Preview, Apply, and Refinement Options
                            if (msg.proposedPlan != null) {
                                val plan = msg.proposedPlan
                                Spacer(modifier = Modifier.height(10.dp))
                                PlanSummaryCard(
                                    plan = plan,
                                    onPreview = { onPreviewPlan(plan) },
                                    onApply = { onApplyPlan(plan) },
                                    onSelectOption = { optionText ->
                                        onSendPrompt(optionText, plan.referenceUrl)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = EmeraldLight,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Analyzing via $activeProviderLabel & preparing preview...",
                            color = EmeraldLight,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Prompt Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Bataiye video kaisi edit karni hai...", fontSize = 12.sp, color = TextTertiary) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldLight,
                    unfocusedBorderColor = VibeBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 2
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (promptInput.isNotBlank() || referenceUrlInput.isNotBlank()) {
                        val text = promptInput.ifBlank { "Apply the style from the reference link" }
                        val url = referenceUrlInput.takeIf { it.isNotBlank() }
                        promptInput = ""
                        onSendPrompt(text, url)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(EmeraldPrimary, RoundedCornerShape(12.dp))
                    .testTag("ai_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Prompt",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
fun PlanSummaryCard(
    plan: EditingPlan,
    onPreview: () -> Unit,
    onApply: () -> Unit,
    onSelectOption: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldDark)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!plan.referenceUrl.isNullOrBlank()) "🔗 CLONED LINK EDITING PLAN" else "⚡ PROPOSED AI EDITING PLAN",
                    color = EmeraldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Badge(containerColor = VioletDark) {
                    Text("Ask & Preview Ready", color = VioletLight, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• Color LUT: ${plan.colorGrade.preset.name.replace('_', ' ')}\n• Motion Graphics: ${plan.motionGraphics.firstOrNull()?.type?.name ?: "Dynamic"} (${plan.motionGraphics.firstOrNull()?.primaryText ?: ""})\n• Transition: ${plan.transitions.firstOrNull()?.type?.name ?: "Smooth Zoom"} (${plan.transitions.size} clips)\n• Sound FX: ${plan.soundEffects.firstOrNull()?.name ?: "Cinematic Whoosh"}",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            // Interactive Question Options ("Poochne wala system")
            if (plan.suggestedOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kya aap isme kuch aur add karwana chahte hain? (Tap to customize):",
                    color = VioletLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    plan.suggestedOptions.forEach { opt ->
                        AssistChip(
                            onClick = { onSelectOption(opt) },
                            label = { Text("+ $opt", fontSize = 10.sp, color = Color.White) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = VibeSurfaceElevated)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Live Preview vs Direct Apply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(EmeraldLight)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_plan_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Live Preview", color = EmeraldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apply_plan_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm & Apply", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
