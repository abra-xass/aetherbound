package com.aetherbound.game.render.battle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aetherbound.game.render.theme.AetherColors

@Composable
fun HpBar(
    name: String,
    level: Int,
    currentVigor: Int,
    maxVigor: Int,
    modifier: Modifier = Modifier,
) {
    val frac = (currentVigor.toFloat() / maxVigor).coerceIn(0f, 1f)
    val animFrac by animateFloatAsState(targetValue = frac, animationSpec = tween(420), label = "hp")
    val color = when {
        animFrac > 0.5f -> Color(0xFF59B66B)
        animFrac > 0.2f -> Color(0xFFE8B460)
        else -> AetherColors.WarningRed
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = AetherColors.GoldBright,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(
                text = "Lv $level",
                style = MaterialTheme.typography.bodyMedium,
                color = AetherColors.MutedText,
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(14.dp),
        ) {
            // groove
            drawRoundRect(
                color = AetherColors.ObsidianDeep,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
            )
            // gold inner border
            drawRoundRect(
                color = AetherColors.GoldDeep,
                size = Size(size.width - 2f, size.height - 2f),
                topLeft = Offset(1f, 1f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(1f),
            )
            // fill
            val fillW = (size.width - 4f) * animFrac
            if (fillW > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(color, color.copy(alpha = 0.7f))
                    ),
                    topLeft = Offset(2f, 2f),
                    size = Size(fillW, size.height - 4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius((size.height - 4f) / 2),
                )
                // gloss
                drawRoundRect(
                    color = Color(0x33FFFFFF),
                    topLeft = Offset(2f, 2f),
                    size = Size(fillW, (size.height - 4f) / 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius((size.height - 4f) / 4),
                )
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
        Text(
            text = "$currentVigor / $maxVigor",
            style = MaterialTheme.typography.bodyMedium,
            color = AetherColors.ParchmentText,
        )
    }
}
