package com.johna.assistant

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

/**
 * JOHNA Energy Core: luminous volumetric-looking center and separate hollow
 * perspective rings. No latitude/longitude or globe mesh.
 * Animation is UI-only; wake detection and audio processing live elsewhere.
 */
@Composable
fun JohnaSphere(
 speaking: Boolean,
 listening: Boolean = false,
 modifier: Modifier = Modifier,
 voiceEnergy: Float = 0f
) {
 val motion = rememberInfiniteTransition(label = "johnaEnergy")
 val orbit by motion.animateFloat(
  initialValue = 0f, targetValue = 360f,
  animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing)),
  label = "orbit"
 )
 val breathe by motion.animateFloat(
  initialValue = 0f, targetValue = 1f,
  animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
  label = "coreBreath"
 )
 val shimmer by motion.animateFloat(
  initialValue = 0f, targetValue = 1f,
  animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
  label = "speakingShimmer"
 )
 Canvas(modifier) {
  val c = Offset(size.width * .5f, size.height * .5f)
  val radius = size.minDimension * .295f
  val energy = when {
   speaking -> .52f + shimmer * .38f
   listening -> (.20f + voiceEnergy.coerceIn(0f, 1f) * .8f)
   else -> .16f + breathe * .11f
  }
  val mainBlue = Color(0xFF61E6FF)
  val lilac = Color(0xFFB39CFF)
  val phase = orbit * PI.toFloat() / 180f

  // Ambient field remains transparent outside the orb; dashboard stays readable.
  drawCircle(Brush.radialGradient(
   listOf(Color(0x66269EFF).copy(alpha = .12f + energy * .19f), Color(0x224759BF), Color.Transparent),
   center = c, radius = radius * 2.2f
  ), radius = radius * 2.2f, center = c)

  fun ringPoint(t: Float, major: Float, squish: Float, rotation: Float): Offset {
   val x = major * cos(t)
   val y = major * squish * sin(t)
   val cr = cos(rotation)
   val sr = sin(rotation)
   return Offset(c.x + x * cr - y * sr, c.y + x * sr + y * cr)
  }

  fun ring(major: Float, squish: Float, rotation: Float, front: Boolean, ringColor: Color) {
   val path = Path()
   var drawing = false
   for (i in 0..180) {
    val a = (i.toFloat() / 180f) * (2f * PI.toFloat())
    val isFront = sin(a) >= 0f
    if (isFront == front) {
     val p = ringPoint(a, major, squish, rotation)
     if (!drawing) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
     drawing = true
    } else drawing = false
   }
   drawPath(path, ringColor.copy(alpha = if (front) .17f else .08f), style = Stroke(width = radius * .085f))
   drawPath(path, ringColor.copy(alpha = if (front) .90f else .31f), style = Stroke(width = if (front) 2.7f else 1.3f))
  }

  // Back halves go behind the inner energy core, not across its surface.
  val tilt = .18f * sin(phase * .27f)
  ring(radius * 1.39f, .41f, -.38f + tilt, false, mainBlue)
  ring(radius * 1.15f, .66f, .91f - tilt, false, lilac)

  // Several soft transparent layers make a luminous ball, rather than a grid globe.
  drawCircle(
   Brush.radialGradient(
    listOf(
     Color(0xFFE5FDFF), Color(0xFFAAEFFF), Color(0xFF287DD6),
     Color(0xFF122A75), Color(0xFF080F37), Color(0xFF051029)
    ),
    center = Offset(c.x - radius * .23f, c.y - radius * .29f),
    radius = radius * 1.6f
   ), radius = radius, center = c
  )
  drawCircle(
   Brush.radialGradient(
    listOf(Color(0xCC73F8FF).copy(alpha = .32f + .38f * energy), Color(0x885AB8FF), Color.Transparent),
    center = c, radius = radius * .8f
   ), radius = radius * .76f, center = c
  )
  drawCircle(Color(0xBB82F4FF), radius = radius, center = c,
   style = Stroke(width = 1.7f + energy * 2f))

  // Two separate foreground orbital paths create a hollow 3D frame.
  ring(radius * 1.39f, .41f, -.38f + tilt, true, mainBlue)
  ring(radius * 1.15f, .66f, .91f - tilt, true, lilac)

  // Bright nodes slowly travel along the outer orbit, emphasizing rotation.
  for (i in 0..2) {
   val a = phase + i * 2.094f
   val pt = ringPoint(a, radius * 1.39f, .41f, -.38f + tilt)
   drawCircle(Color(0x442FFAFF), radius * .085f, center = pt)
   drawCircle(Color(0xFFE9FEFF), radius * .019f, center = pt)
  }

  // Speech / microphone energy pulses around the core.
  if (speaking || listening) {
   for (i in 0..2) {
    val wave = (breathe + i * .33f) % 1f
    drawCircle(mainBlue.copy(alpha = (1f - wave) * (.12f + energy * .22f)),
     radius = radius * (1.1f + wave * .51f), center = c,
     style = Stroke(width = 1.5f + (1f - wave) * 1.7f))
   }
  }
  drawCircle(
   Brush.radialGradient(listOf(Color.White.copy(alpha = .48f), Color(0x5582ECFF), Color.Transparent),
    center = Offset(c.x - radius * .31f, c.y - radius * .34f), radius = radius * .43f),
   radius = radius * .43f,
   center = Offset(c.x - radius * .31f, c.y - radius * .34f)
  )
 }
}
