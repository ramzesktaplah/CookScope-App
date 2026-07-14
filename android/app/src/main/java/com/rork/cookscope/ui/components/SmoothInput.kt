package com.rork.cookscope.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

private val CaretWidth = 2.dp
private val CaretHeight = 18.dp

/**
 * A smooth, spring-animated custom-caret input.
 *
 * Hides the native cursor, measures caret position via [TextLayoutResult.getCursorRect],
 * and renders a gold gradient caret that springs to the caret X position.
 *
 * Text state is held internally to prevent parent recompositions on every keystroke.
 * The parent only receives a callback when the user submits (send action or IME send).
 *
 * @param onSend Called with the trimmed text when the user submits.
 * @param placeholder Hint shown when the field is empty and not focused.
 * @param isLoading Whether to show a spinner in the send button.
 * @param singleLine If true, the field is constrained to one line.
 * @param maxLines Maximum lines when not single-line.
 * @param imeAction The IME action to display on the keyboard.
 */
@Composable
fun SmoothInput(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Type a message...",
    isLoading: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else 4,
    imeAction: ImeAction = ImeAction.Send,
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var isFocused by remember { mutableStateOf(false) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val caretX = remember { Animatable(0f) }
    val caretAlpha = remember { Animatable(0f) }
    val density = LocalDensity.current

    val text = textFieldValue.text
    val selection = textFieldValue.selection
    val isCollapsed = selection.start == selection.end
    val caretIndex = selection.start.coerceIn(0, text.length)

    val targetX = remember(layoutResult, caretIndex) {
        layoutResult?.getCursorRect(caretIndex)?.left ?: 0f
    }

    val isVisible = isFocused && isCollapsed && !isLoading && text.isNotEmpty()

    LaunchedEffect(isVisible, targetX) {
        launch {
            caretAlpha.animateTo(
                targetValue = if (isVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 120),
            )
        }
        launch {
            caretX.animateTo(
                targetValue = targetX,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    fun submit() {
        val trimmed = text.trim()
        if (trimmed.isNotBlank() && !isLoading) {
            onSend(trimmed)
            textFieldValue = TextFieldValue("")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    )
                )
            )
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                // Placeholder
                if (text.isEmpty() && !isFocused) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }

                // Custom spring caret overlay
                if (caretAlpha.value > 0.01f) {
                    val caretOffsetDp = with(density) { caretX.value.toDp() }
                    Box(
                        modifier = Modifier
                            .zIndex(1f)
                            .width(CaretWidth)
                            .height(CaretHeight)
                            .offset(x = caretOffsetDp.coerceAtLeast(0.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                    )
                                )
                            ),
                    )
                }

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { state ->
                            isFocused = state.isFocused
                        },
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp,
                    ),
                    cursorBrush = SolidColor(Color.Unspecified), // hide native cursor
                    keyboardOptions = KeyboardOptions(imeAction = imeAction),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    singleLine = singleLine,
                    maxLines = maxLines,
                    onTextLayout = { result ->
                        layoutResult = result
                    },
                )
            }

            // Send button
            IconButton(
                onClick = { submit() },
                enabled = text.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (text.isNotBlank() && !isLoading) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                )
                            )
                        }
                    ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
