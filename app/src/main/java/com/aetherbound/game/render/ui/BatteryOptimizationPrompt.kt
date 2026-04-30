package com.aetherbound.game.render.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aetherbound.game.core.data.ThotConnectionKeeper
import com.aetherbound.game.render.theme.AetherColors

/**
 * One-time prompt before the player's first multiplayer match: asks them
 * to add Aetherbound + Thot to "Don't optimise battery".
 *
 * Aggressive OEM battery savers (Xiaomi MIUI, Huawei EMUI, OPPO ColorOS,
 * Samsung Adaptive Battery) override Android's ForegroundService
 * priority guarantees, killing background apps regardless. Adding both
 * apps to the unrestricted list gives multiplayer matches the resilience
 * the standard Android API alone can't provide.
 */
@Composable
fun BatteryOptimizationPrompt(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(14.dp))
                .background(AetherColors.Obsidian)
                .border(2.dp, AetherColors.GoldBright, RoundedCornerShape(14.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "BATTERY OPTIMISATION",
                color = AetherColors.GoldBright,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Multiplayer matches use Thot's encrypted connection. Aggressive battery " +
                    "savers (especially on Xiaomi, Huawei, OPPO, Samsung) can interrupt the " +
                    "connection mid-match.",
                color = AetherColors.ParchmentText, fontSize = 13.sp,
            )
            Text(
                "Add Aetherbound and Thot to your phone's \"Don't optimise battery\" list " +
                    "for stable matches. You only need to do this once.",
                color = AetherColors.MutedText, fontSize = 12.sp,
            )

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AetherColors.GoldGradient)
                        .clickable {
                            openBatteryOptimizationSettings(ctx, ThotConnectionKeeper.THOT_PACKAGE)
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "OPEN SETTINGS",
                        color = AetherColors.Obsidian,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AetherColors.Slate)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        "SKIP",
                        color = AetherColors.MutedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * Returns true when battery-optimisation is currently disabled for both
 * Aetherbound and Thot — i.e. the prompt no longer needs to fire.
 */
fun isBatteryOptimisationOk(ctx: Context): Boolean {
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    val ourSelf = pm.isIgnoringBatteryOptimizations(ctx.packageName)
    val thotIgnored = pm.isIgnoringBatteryOptimizations(ThotConnectionKeeper.THOT_PACKAGE)
    return ourSelf && thotIgnored
}

@Suppress("DEPRECATION")
private fun openBatteryOptimizationSettings(ctx: Context, targetPackage: String) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$targetPackage")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    } else {
        // Older Androids — just open generic battery settings.
        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    runCatching { ctx.startActivity(intent) }
}
