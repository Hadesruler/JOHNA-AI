package com.johna.assistant

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** An animated day/night + current-conditions canvas derived from Open-Meteo data. */
@Composable
fun AnimatedWeatherSky(data: WeatherForecast, modifier: Modifier = Modifier) {
 val firstDay = data.days.firstOrNull()
 val sunUp = firstDay?.sunrise.orEmpty()
 val sunDown = firstDay?.sunset.orEmpty()
 val now = data.updated
 val night = sunUp.isNotEmpty() && sunDown.isNotEmpty() && (now < sunUp || now >= sunDown)
 val rain = data.currentCode in 51..67 || data.currentCode in 80..82 || data.currentCode in 95..99
 val snow = data.currentCode in 71..77 || data.currentCode in 85..86
 val clouds = data.currentCode in 2..3 || data.currentCode in 45..48 || rain || snow
 val transition = rememberInfiniteTransition(label="weatherSky")
 val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(24000, easing=LinearEasing)), label="cloudDrift")
 val fall by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing=LinearEasing)), label="rainFall")
 val glow by transition.animateFloat(.83f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse),label="sunGlow")
 Canvas(modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(20.dp))) {
  val w=size.width; val h=size.height
  val sky = if (night) listOf(Color(0xFF030C25),Color(0xFF17295A),Color(0xFF263C6A))
            else listOf(Color(0xFF215FBB),Color(0xFF50ACDD),Color(0xFFB6DEF7))
  drawRect(Brush.verticalGradient(sky))
  val lightCenter=Offset(w*.76f,h*.32f)
  if(night) {
   for(i in 0 until 38) {
    val x=((i* .6180339f + .13f)%1f)*w
    val y=((i* .382f + .11f)%1f)*h*.69f
    val alpha=.22f+((i%5)*.12f)*glow
    drawCircle(Color.White.copy(alpha=alpha.coerceIn(0f,.88f)), radius=if(i%6==0)2.1f else 1.05f, center=Offset(x,y))
   }
   drawCircle(Color(0x77B2DAFF), radius=h*.145f,center=lightCenter)
   drawCircle(Color(0xFFE4F3FF),radius=h*.089f,center=lightCenter)
   drawCircle(Color(0xFF7485AC),radius=h*.018f,center=lightCenter+Offset(8f,5f))
  } else {
   drawCircle(Brush.radialGradient(listOf(Color(0x66FFF4B5),Color.Transparent),center=lightCenter,radius=h*.37f),radius=h*.37f,center=lightCenter)
   drawCircle(Color(0xFFFFE9AF),radius=h*.105f*glow,center=lightCenter)
  }
  if(clouds) {
   for(i in 0..3) {
    val offset=(((i*.32f+drift*.18f)%1.35f)-.2f)*w
    val cy=h*(.22f+(i%3)*.20f)
    val base=Color.White.copy(alpha=if(night).19f else .55f)
    drawOval(base, topLeft=Offset(offset,cy),size=androidx.compose.ui.geometry.Size(w*.23f,h*.13f))
    drawCircle(base,radius=h*.085f,center=Offset(offset+w*.08f,cy))
    drawCircle(base,radius=h*.065f,center=Offset(offset+w*.16f,cy+h*.02f))
   }
  }
  if(rain || snow) {
   for(i in 0 until 40) {
    val x=((i*.618034f+.15f)%1f)*w
    val y=(((i*.279f+fall*(if(snow) .55f else 1.4f))%1f))*h
    if(snow) drawCircle(Color(0xCCFFFFFF),radius=2.3f,center=Offset(x,y))
    else drawLine(Color(0x8856DBFF),start=Offset(x,y),end=Offset(x-4f,y+14f),strokeWidth=1.6f)
   }
  }
  // Subtle horizon and instrument-style highlights.
  drawLine(Color.White.copy(alpha=.16f),Offset(0f,h*.87f),Offset(w,h*.87f),1f)
  drawCircle(Color(0x4469E5FF), radius=h*.82f,center=Offset(w*.51f,h*1.62f),style=Stroke(width=1.5f))
 }
}
