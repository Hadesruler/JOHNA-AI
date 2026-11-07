package com.johna.assistant

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

/**
 * JOHNA Nebula Orb: softly deforming, translucent energy cloud.
 * No textured sphere, outline, rigid orbit, wireframe, or rotating bitmap.
 * The animation uses deterministic Canvas paths and gradients; it works offline.
 * Voice energy gently expands the cloud; speaking/listening alter its brightness.
 */
@Composable
fun JohnaSphere(
    speaking: Boolean,
    listening: Boolean = false,
    modifier: Modifier = Modifier,
    voiceEnergy: Float = 0f
) {
    val transition = rememberInfiniteTransition(label = "johnaNebula")
    val slow by transition.animateFloat(
        0f, 2f * PI.toFloat(),
        infiniteRepeatable(tween(27000, easing = LinearEasing)), label = "nebulaFlow"
    )
    val counter by transition.animateFloat(
        0f, 2f * PI.toFloat(),
        infiniteRepeatable(tween(39000, easing = LinearEasing)), label = "counterFlow"
    )
    val breathing by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nebulaBreathing"
    )
    val speechLight by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(920, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nebulaSpeech"
    )

    Canvas(modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val center = Offset(size.width * .50f, size.height * .49f)
        val radius = size.minDimension * .32f
        val energy = voiceEnergy.coerceIn(0f, 1f)
        val liveliness = when {
            listening -> .32f + energy * .54f
            speaking -> .38f + speechLight * .38f
            else -> .12f + breathing * .13f
        }
        val cy = Color(0xFF56F4FF)
        val azure = Color(0xFF398CFF)
        val lavender = Color(0xFFC785FF)
        val violet = Color(0xFF875CFF)
        val pale = Color(0xFFD7FCFF)

        // A diffused surrounding atmosphere with no visible circular edge.
        drawCircle(
            Brush.radialGradient(
                listOf(azure.copy(alpha = .12f + liveliness * .06f),
                    violet.copy(alpha = .09f), Color.Transparent),
                center = center, radius = radius * 2.10f
            ), radius = radius * 2.10f, center = center
        )

        // Overlapping mist volumes: local centres drift on different periods.
        for (i in 0 until 13) {
            val a = slow * (if (i % 2 == 0) 1f else -.73f) + i * 2.39996f
            val b = counter * .53f + i * .93f
            val fx = sin(a * .69f + b * .37f) * radius * (.20f + (i % 3) * .11f)
            val fy = cos(b * .87f - a * .25f) * radius * (.23f + (i % 4) * .07f)
            val pos = Offset(center.x + fx, center.y + fy)
            val rr = radius * (.59f + (i % 4) * .11f + breathing * .032f + energy * .055f)
            val pigment = when (i % 4) {
                0 -> cy
                1 -> lavender
                2 -> azure
                else -> violet
            }
            drawCircle(
                Brush.radialGradient(
                    listOf(pigment.copy(alpha = .15f + liveliness * .09f),
                        pigment.copy(alpha = .075f), pigment.copy(alpha = .018f),
                        Color.Transparent),
                    center = pos, radius = rr
                ), radius = rr, center = pos
            )
        }

        // Faint internal plasma ribbons, softly tapered at the ends.
        // Thick translucent strokes are the glow, thin ones are only diffuse wisps.
        for (i in 0 until 15) {
            val seed = i * .821f
            val orientation = if (i % 2 == 0) 1f else -1f
            val start = -1f
            val steps = 58
            val path = Path()
            for (s in 0..steps) {
                val t = start + 2f * s / steps
                val yWave = sin(t * (2.1f + i % 4 * .32f) + slow * orientation + seed) * .22f
                val yCurl = cos(t * 3.3f - counter * .64f + seed * 1.4f) * .12f
                val layer = sin(seed * 1.8f + counter * .20f) * .39f
                // The ends of the ribbons curl and dissolve, not straight-cut.
                val fade = max(0f, 1f - abs(t).pow(2.2f))
                val x = center.x + radius * (t * .94f + .08f * sin(seed + slow * .47f) * fade)
                val y = center.y + radius * (layer + yWave + yCurl) * fade
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val pigment = when (i % 3) {
                0 -> cy
                1 -> lavender
                else -> azure
            }
            drawPath(path, pigment.copy(alpha = .038f + liveliness * .027f),
                style = Stroke(width = radius * .14f))
            drawPath(path, pigment.copy(alpha = .085f + liveliness * .064f),
                style = Stroke(width = radius * .044f))
            drawPath(path, pigment.copy(alpha = .13f + liveliness * .07f),
                style = Stroke(width = 1.3f))
        }

        // Several radiant, drifting pockets keep the center alive, not metallic.
        for (i in 0..3) {
            val a = slow * .73f + i * 1.83f
            val pos = Offset(
                center.x + sin(a) * radius * .29f,
                center.y + cos(a * .81f + i) * radius * .27f
            )
            val glow = radius * (.37f + i * .055f + liveliness * .08f)
            val pigment = if (i % 2 == 0) pale else lavender
            drawCircle(
                Brush.radialGradient(
                    listOf(pigment.copy(alpha = .20f + liveliness * .10f),
                        pigment.copy(alpha = .07f), Color.Transparent),
                    center = pos, radius = glow
                ), radius = glow, center = pos
            )
        }

        // A few low-intensity floating sparks, irregularly distributed.
        for (i in 0 until 33) {
            val a = i * 2.399963f + slow * (if (i % 2 == 0) .21f else -.13f)
            val d = radius * (.21f + ((i * 7) % 19) / 19f * 1.23f)
            val p = Offset(center.x + cos(a) * d, center.y + sin(a) * d * .88f)
            val opacity = (.08f + .14f * abs(sin(counter + i * .7f)))
            drawCircle((if (i % 4 == 0) lavender else cy).copy(alpha = opacity),
                radius = if (i % 9 == 0) 1.7f else .85f, center = p)
        }
    }
}
