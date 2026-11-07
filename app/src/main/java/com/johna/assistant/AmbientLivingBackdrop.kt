package com.johna.assistant

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Low-overhead ambient scenery inside the JOHNA dashboard (not an Android system wallpaper).
 * Draws a small, bounded set of drifting lights and slowly breathing holographic rings.
 * All geometry is vector-based; no network, sensors or extra permissions.
 */
@Composable
fun AmbientLivingBackdrop(
 accent: Color,
 speaking: Boolean,
 listening: Boolean,
 modifier: Modifier = Modifier
) {
 val transition = rememberInfiniteTransition(label = "ambient-wallpaper")
 val drift by transition.animateFloat(
  initialValue = 0f, targetValue = 1f,
  animationSpec = infiniteRepeatable(tween(36000, easing = LinearEasing)),
  label = "slow-drift"
 )
 val breath by transition.animateFloat(
  initialValue = 0f, targetValue = 1f,
  animationSpec = infiniteRepeatable(tween(6500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
  label = "ambient-breath"
 )
 Canvas(modifier) {
  val w = size.width
  val h = size.height
  if (w <= 0f || h <= 0f) return@Canvas
  val halo = Offset(w * .51f, h * .48f)
  val haloRadius = size.minDimension * (.29f + .035f * breath)
  drawCircle(
   brush = Brush.radialGradient(
    listOf(accent.copy(alpha = if (speaking) .15f else .065f),
     Color(0xFF493C89).copy(alpha = .035f), Color.Transparent),
    center = halo, radius = haloRadius * 1.75f
   ),
   radius = haloRadius * 1.75f,
   center = halo
  )
  for (ring in 0..2) {
   val rr = haloRadius * (1.06f + ring * .24f)
   drawCircle(
    color = accent.copy(alpha = .045f + breath * .018f),
    radius = rr, center = halo,
    style = Stroke(width = 1f + ring * .25f)
   )
  }
  // Slow, repeatable deterministic motion; avoids per-frame allocations of image assets.
  for (i in 0 until 25) {
   val u = ((i * 0.6180339887 + .17) % 1.0).toFloat()
   val v = ((i * 0.3819660113 + .31) % 1.0).toFloat()
   val theta = (drift * 2f * PI + i * .73).toDouble()
   val x = (u * w + sin(theta).toFloat() * w * .019f)
   val y = (v * h + cos(theta * .74).toFloat() * h * .024f)
   val radius = (if (i % 5 == 0) 2.1f else 1.0f) * (1f + breath * .26f)
   drawCircle(accent.copy(alpha = if (listening) .31f else .16f),
    radius = radius, center = Offset(x, y))
  }
  // Thin motion lines give the backdrop movement while keeping foreground text readable.
  for (i in 0 until 7) {
   val y = h * (.15f + i * .105f)
   val shift = sin((drift * 2f * PI + i).toDouble()).toFloat() * w * .025f
   drawLine(accent.copy(alpha = .038f), Offset(w * .08f + shift, y),
    Offset(w * .91f + shift, y - h * .045f), strokeWidth = 1f)
  }
 }
}
