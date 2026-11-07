package com.johna.assistant

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.*

/** Gentle atmospheric light behind the dashboard; not an Android system wallpaper. */
@Composable
fun AmbientLivingBackdrop(accent: Color, speaking: Boolean, listening: Boolean, modifier: Modifier=Modifier) {
 val anim=rememberInfiniteTransition(label="livingAmbient")
 val drift by anim.animateFloat(0f,1f,infiniteRepeatable(tween(42000,easing=LinearEasing)),label="ambientDrift")
 val breath by anim.animateFloat(0f,1f,infiniteRepeatable(tween(6800,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="ambientBreath")
 Canvas(modifier) {
  val w=size.width; val h=size.height
  if(w<=0f||h<=0f)return@Canvas
  val halo=Offset(w*(.50f+.018f*sin(drift*2f*PI.toFloat())),h*.48f)
  drawCircle(brush=Brush.radialGradient(
   listOf(accent.copy(alpha=if(speaking).105f else .064f),Color(0xFF6D4BDA).copy(alpha=.045f),Color.Transparent),
   center=halo,radius=size.minDimension*.67f),
   radius=size.minDimension*.67f,center=halo)
  // Detached atmospheric nebulae on each side.
  val left=Offset(w*.06f,h*.78f)
  val right=Offset(w*.95f,h*.16f)
  drawCircle(Brush.radialGradient(listOf(Color(0xFF467CC3).copy(alpha=.078f),Color.Transparent),center=left,radius=h*.67f),radius=h*.67f,center=left)
  drawCircle(Brush.radialGradient(listOf(Color(0xFF7960C4).copy(alpha=.08f),Color.Transparent),center=right,radius=h*.72f),radius=h*.72f,center=right)
  for(i in 0 until 29) {
   val x=((i*.6180339f+.19f)%1f)*w+sin(drift*2f*PI.toFloat()+i).toFloat()*w*.012f
   val y=((i*.381966f+.33f)%1f)*h+cos(drift*2f*PI.toFloat()+i*.7f).toFloat()*h*.019f
   drawCircle(accent.copy(alpha=(.11f+.07f*breath)*(if(listening)1.6f else 1f)),radius=if(i%7==0)2.2f else .9f,center=Offset(x,y))
  }
 }
}
