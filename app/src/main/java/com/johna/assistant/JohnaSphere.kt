package com.johna.assistant

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * Genuine 3D sphere using orthographic projection of lat/lon surface grid.
 * - Rotates slowly around Y-axis while speaking.
 * - Pulses a teal glow ring while listening.
 * - Soft specular highlight and rim light always visible.
 */
@Composable
fun JohnaSphere(
 speaking: Boolean,
 listening: Boolean = false,
 modifier: Modifier = Modifier
) {
 var turn by remember { mutableFloatStateOf(0f) }
 var glowPhase by remember { mutableFloatStateOf(0f) }

 LaunchedEffect(speaking) {
  while (speaking) { delay(32); turn = (turn + 0.27f) % 360f }
 }
 LaunchedEffect(listening) {
  var t = 0f
  while (listening) { delay(20); t += 0.07f; glowPhase = sin(t).toFloat() }
  glowPhase = 0f
 }

 Canvas(modifier) {
  val radius = min(size.width, size.height) * .405f
  val c = Offset(size.width / 2f, size.height / 2f)

  // ── Listening outer pulse ring ──────────────────────────────────────────
  if (listening) {
   val ringR = radius * (1.4f + .22f * glowPhase)
   drawCircle(
    Brush.radialGradient(
     listOf(Color(0x5500EFCC), Color(0x1500C8AA), Color.Transparent),
     center = c, radius = ringR * 1.15f
    ),
    radius = ringR, center = c
   )
   drawCircle(
    color = Color(0xAA00FFCC),
    radius = ringR, center = c,
    style = Stroke(width = 3.5f + 2f * glowPhase)
   )
   // second inner ring for depth
   drawCircle(
    color = Color(0x6600CCAA),
    radius = radius * (1.12f + .1f * glowPhase), center = c,
    style = Stroke(width = 2f)
   )
  }

  // ── Main sphere body — radial gradient simulating directional light ──────
  drawCircle(
   Brush.radialGradient(
    listOf(Color(0xFF90F9FF), Color(0xFF3B66B9), Color(0xFF10234E), Color(0xFF040A22)),
    center = Offset(c.x - radius * .48f, c.y - radius * .45f),
    radius = radius * 2.05f
   ),
   radius = radius, center = c
  )

  // ── Surface lat/lon grid (orthographic projection) ───────────────────────
  val edge = Path().apply {
   addOval(
    androidx.compose.ui.geometry.Rect(
     c.x - radius, c.y - radius, c.x + radius, c.y + radius
    )
   )
  }
  clipPath(edge) {
   val a = turn.toDouble() * PI / 180.0
   val lineColor = Color(0xFFB6FAFF).copy(alpha = .57f)

   fun project(latitude: Double, longitude: Double): Pair<Offset, Boolean> {
    val lat = latitude * PI / 180.0
    val lon = longitude * PI / 180.0 + a
    val x = cos(lat) * sin(lon)
    val y = sin(lat)
    val z = cos(lat) * cos(lon)
    return Offset(c.x + (radius * x).toFloat(), c.y - (radius * y).toFloat()) to (z >= 0)
   }

   fun curve(points: List<Pair<Offset, Boolean>>) {
    var prev: Offset? = null
    for ((pt, visible) in points) {
     if (visible && prev != null) drawLine(lineColor, prev, pt, 1.7f)
     prev = if (visible) pt else null
    }
   }

   for (longitude in 0 until 360 step 20) {
    curve((-90..90 step 3).map { project(it.toDouble(), longitude.toDouble()) })
   }
   for (latitude in -75..75 step 15) {
    curve((0..360 step 3).map { project(latitude.toDouble(), it.toDouble()) })
   }
  }

  // ── Rim light (always on) ────────────────────────────────────────────────
  drawCircle(
   color = Color(0xFFA1DFFF).copy(alpha = .7f),
   radius = radius, center = c,
   style = Stroke(width = 2.5f)
  )

  // ── Outer soft ambient glow ──────────────────────────────────────────────
  drawCircle(
   Brush.radialGradient(
    listOf(Color(0x44A3F2FF), Color.Transparent),
    center = c, radius = radius * 1.25f
   ),
   radius = radius * 1.15f, center = c
  )

  // ── Speaking: inner warm fill ────────────────────────────────────────────
  if (speaking) {
   drawCircle(
    Brush.radialGradient(
     listOf(Color(0x556060FF), Color(0x22A0DFFF), Color.Transparent),
     center = c, radius = radius * .8f
    ),
    radius = radius * .75f, center = c
   )
  }

  // ── Specular highlight (top-left bright spot) ────────────────────────────
  drawCircle(
   Brush.radialGradient(
    listOf(Color(0xCCFFFFFF), Color(0x44C8FFFF), Color.Transparent),
    center = Offset(c.x - radius * .32f, c.y - radius * .34f),
    radius = radius * .38f
   ),
   radius = radius * .34f,
   center = Offset(c.x - radius * .32f, c.y - radius * .34f)
  )
 }
}
