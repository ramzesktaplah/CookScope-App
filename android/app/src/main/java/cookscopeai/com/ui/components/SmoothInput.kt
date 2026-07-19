package cookscopeai.com.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cookscopeai.com.ui.theme.GoldPrimary

/**
 * A smooth gold-accented input bar with a stable native cursor.
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

    val text = textFieldValue.text

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
                    cursorBrush = SolidColor(GoldPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = imeAction),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    singleLine = singleLine,
                    maxLines = maxLines,
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
