package com.akito.million_egg.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoResizedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    minFontSize: TextUnit = 8.sp,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val targetFontSize = if (fontSize != TextUnit.Unspecified) {
        fontSize
    } else if (style.fontSize != TextUnit.Unspecified) {
        style.fontSize
    } else {
        14.sp
    }

    val targetColor = if (color != Color.Unspecified) {
        color
    } else if (style.color != Color.Unspecified) {
        style.color
    } else {
        LocalTextStyle.current.color
    }

    var resizedFontSize by remember(text, targetFontSize) {
        mutableStateOf(targetFontSize)
    }
    var shouldDraw by remember(text, targetFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        color = targetColor,
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        },
        fontSize = resizedFontSize,
        fontWeight = fontWeight ?: style.fontWeight,
        textAlign = textAlign ?: style.textAlign,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = maxLines > 1,
        style = style,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && resizedFontSize > minFontSize) {
                val nextSize = (resizedFontSize.value - 1f).coerceAtLeast(minFontSize.value)
                if (nextSize < resizedFontSize.value) {
                    resizedFontSize = nextSize.sp
                } else {
                    shouldDraw = true
                }
            } else {
                shouldDraw = true
            }
        }
    )
}
