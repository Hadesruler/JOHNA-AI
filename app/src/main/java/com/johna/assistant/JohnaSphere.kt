package com.johna.assistant

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import kotlinx.coroutines.delay
import kotlin.math.*

/** Perspective-like orthographic projection of latitude/longitude curves on a shaded sphere.
 * Surface grid physically rotates around the Y axis; only the front hemisphere is drawn.
 */
@Composable
fun JohnaSphere(speaking:Boolean, modifier:Modifier=Modifier) {
 var turn by remember { mutableFloatStateOf(0f) }
 LaunchedEffect(speaking) {
  while(speaking) { delay(32); turn=(turn+0.27f)%360f }
 }
 Canvas(modifier) {
  val radius = min(size.width,size.height)*.405f
  val c = Offset(size.width/2f,size.height/2f)
  drawCircle(Brush.radialGradient(
   listOf(Color(0xFF90F9FF),Color(0xFF3B66B9),Color(0xFF10234E),Color(0xFF040A22)),
   center=Offset(c.x-radius*.48f,c.y-radius*.45f),radius=radius*2.05f),radius,c)
  val edge=Path().apply { addOval(androidx.compose.ui.geometry.Rect(c.x-radius,c.y-radius,c.x+radius,c.y+radius)) }
  clipPath(edge) {
   val a=turn.toDouble()*PI/180.0
   val lineColor=Color(0xFFB6FAFF).copy(alpha=.57f)
   fun project(latitude:Double,longitude:Double): Pair<Offset,Boolean> {
    val lat=latitude*PI/180.0
    val lon=longitude*PI/180.0+a
    val x=cos(lat)*sin(lon)
    val y=sin(lat)
    val z=cos(lat)*cos(lon)
    return Offset(c.x+(radius*x).toFloat(),c.y-(radius*y).toFloat()) to (z>=0)
   }
   fun curve(points:List<Pair<Offset,Boolean>>) {
    var prev:Offset?=null
    for((pt,visible) in points) {
     if(visible && prev!=null) drawLine(lineColor,prev,pt,1.7f)
     prev=if(visible) pt else null
    }
   }
   for (longitude in 0 until 360 step 20) {
    curve((-90..90 step 3).map { project(it.toDouble(),longitude.toDouble()) })
   }
   for(latitude in -75..75 step 15) {
    curve((0..360 step 3).map { project(latitude.toDouble(),it.toDouble()) })
   }
  }
  drawCircle(color=Color(0xFFA1DFFF).copy(alpha=.7f),radius=radius,center=c,style=Stroke(width=2.5f))
  drawCircle(brush=Brush.radialGradient(listOf(Color(0x44A3F2FF),Color.Transparent),center=c,radius=radius*1.25f),radius=radius*1.15f,center=c)
 }
}
