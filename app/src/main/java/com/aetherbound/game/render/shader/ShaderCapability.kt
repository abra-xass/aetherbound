package com.aetherbound.game.render.shader

import android.os.Build

/**
 * AGSL via android.graphics.RuntimeShader requires API 33 (Android 13).
 * Below that we fall back to compose-Canvas-only effects: simple bloom via
 * additive layering, ripple via vector path scale, no real distortion.
 */
object ShaderCapability {
    val supportsAgsl: Boolean = Build.VERSION.SDK_INT >= 33
}
