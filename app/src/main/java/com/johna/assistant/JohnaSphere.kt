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
 * JOHNA's ethereal voice presence. An animated, translucent energy form made
 * of light and moving wisps, deliberately NOT a globe or a wireframe mesh.
 * Its shimmer indicates TTS playback, not measured TTS volume.
 */
@Composable
fun JohnaSphere(
 speaking: Boolean,
 listening: Boolean = false,
 modifier: Modifier = Modifier,
 voiceEnergy: Float = 0f
) {
 val motion = rememberInfiniteTransition(label = "etherealPresence")
 val phase by motion.animateFloat(
  0f, 2f * PI.toFloat(),
  infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "fluidOrbit")
 val breath by motion.animateFloat(
  0f, 1f,
  infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathing")
 val voicePulse by motion.animateFloat(
  0f, 1f,
  infiniteRepeatable(tween(680, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "speakingPulse")
 Canvas(modifier) {
  val c = Offset(size.width / 2f, size.height / 2f)
  val r = size.minDimension * .30f
  val amplitude = when {
   listening -> (.26f + voiceEnergy.coerceIn(0f,1f) * .72f)
   speaking -> (.39f + voicePulse * .38f)
   else -> (.14f + breath * .12f)
  }
  val cyan = Color(0xFF83F5FF)
  val periwinkle = Color(0xFF8397FF)
  val violet = Color(0xFFD4AEFF)
  fun at(rad: Float, a: Float) = Offset(c.x + cos(a)*rad, c.y + sin(a)*rad)

  // Outer vapor/aurora: several soft gradients, no spherical hard edge.
  drawCircle(brush = Brush.radialGradient(
   colors = listOf(cyan.copy(alpha=.20f + amplitude*.14f),
    periwinkle.copy(alpha=.11f), violet.copy(alpha=.07f), Color.Transparent),
   center = c, radius = r*2.1f), radius = r*2.1f, center=c)
  drawCircle(brush = Brush.radialGradient(
   colors=listOf(Color(0xFFE9FFFF).copy(alpha=.65f),
    cyan.copy(alpha=.37f+amplitude*.22f),
    periwinkle.copy(alpha=.17f), violet.copy(alpha=.10f),Color.Transparent),
   center=Offset(c.x-r*.16f,c.y-r*.19f), radius=r*1.43f),
   radius=r*1.36f, center=c)
  drawCircle(brush = Brush.radialGradient(
   colors=listOf(Color.White.copy(alpha=.89f),cyan.copy(alpha=.51f),Color.Transparent),
   center=Offset(c.x-r*.05f,c.y-r*.08f), radius=r*.63f),
   radius=r*.64f,center=c)

  // Wisps travel around an organic energy silhouette (not latitude/longitude).
  for(i in 0 until 24) {
   val a = i*(2f*PI.toFloat()/24f) + phase*.19f
   val bend = .15f*sin(phase*1.4f+i*.83f)
   val path=Path().apply {
    val start=at(r*(.62f+.08f*sin(a*3f+phase)),a)
    val cp1=at(r*(1.25f + .14f*sin(phase+i)),a+.22f+bend)
    val cp2=at(r*(1.37f + .10f*cos(phase*.7f+i)),a+.83f+bend)
    val end=at(r*(.78f+.08f*cos(phase+i)),a+1.39f)
    moveTo(start.x,start.y)
    cubicTo(cp1.x,cp1.y,cp2.x,cp2.y,end.x,end.y)
   }
   val tint=if(i%3==0) violet else if(i%3==1) cyan else periwinkle
   // Aura layer and bright drifting filament.
   drawPath(path,tint.copy(alpha=.065f+amplitude*.065f),style=Stroke(width=r*.064f))
   drawPath(path,tint.copy(alpha=.31f+amplitude*.25f),style=Stroke(width=1.1f+(i%3)*.40f))
  }

  // A soft hollow arc, deliberately irregular and extremely faint.
  val arc = Path().apply {
   val s=at(r*1.45f,-.8f+phase*.025f)
   moveTo(s.x,s.y)
   cubicTo(c.x+r*1.52f,c.y-r*.16f,c.x+r*.37f,c.y+r*1.38f,c.x-r*.84f,c.y+r*.99f)
  }
  drawPath(arc,cyan.copy(alpha=.09f),style=Stroke(width=r*.085f))
  drawPath(arc,cyan.copy(alpha=.35f),style=Stroke(width=1.5f))

  // Slow luminous motes. Positions are deterministic to keep the effect steady.
  for(i in 0 until 39) {
   val a=i*2.399963f+phase*(if(i%2==0).36f else -.19f)
   val distance=r*(.45f+(i%11)/11f*1.24f+.07f*sin(phase+i))
   val p=at(distance,a)
   val brightness=(.20f+.36f*(.5f+.5f*sin(phase*2.1f+i))).coerceIn(0f,1f)
   drawCircle(if(i%3==0)violet.copy(alpha=brightness) else cyan.copy(alpha=brightness),
    radius=if(i%7==0)2.6f else 1.2f,center=p)
  }

  // Listening/speaking energy is a translucent breath, never a rigid circular grid.
  if(speaking || listening) {
   drawCircle(brush=Brush.radialGradient(
    listOf(cyan.copy(alpha=.14f+amplitude*.15f),violet.copy(alpha=.09f),Color.Transparent),
    center=c,radius=r*(1.4f+breath*.31f)),
    radius=r*(1.4f+breath*.31f),center=c)
  }
 }
}
