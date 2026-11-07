package com.johna.assistant

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.text.SimpleDateFormat
import java.util.Locale

private val cyan = Color(0xFF75E6FF)
private val light = Color(0xFFF3F7FF)

private fun weatherIcon(code: Int): String = when(code) {
 0 -> "☀"; 1,2 -> "🌤"; 3 -> "☁"; 45,48 -> "🌫"; in 51..67 -> "🌧"
 in 71..77, in 85..86 -> "❄"; in 80..82 -> "🌦"; in 95..99 -> "⛈"; else -> "☁"
}

@Composable private fun StatPill(label: String, value: String) {
 Column(Modifier.padding(4.dp).background(Color(0x253FE9FE),RoundedCornerShape(14.dp)).border(1.dp,Color(0x4475E6FF),RoundedCornerShape(14.dp)).padding(horizontal=14.dp, vertical=9.dp)) {
  Text(label,color=light.copy(alpha=.76f),fontSize=10.sp)
  Text(value,color=light,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
 }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeatherDetailPanel(data: WeatherForecast?, loading: Boolean, error: String, onRefresh: () -> Unit, onActivity: () -> Unit) {
 var mode by remember { mutableStateOf(0) }
 var selected by remember { mutableIntStateOf(0) }
 Column(Modifier.fillMaxWidth()) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
   Text("ΠΡΟΓΝΩΣΗ · ΑΘΗΝΑ", color=cyan, fontSize=12.sp, letterSpacing=2.sp)
   TextButton(onClick={onActivity();onRefresh()}) { Text(if(loading) "Ενημέρωση..." else "↻ Ανανέωση", color=cyan) }
  }
  if(data==null) {
   Text(if(error.isNotBlank()) error else "Λήψη πραγματικών δεδομένων Open-Meteo...", color=light)
   return@Column
  }
  AnimatedWeatherSky(data)
  Spacer(Modifier.height(8.dp))
  Row(verticalAlignment=Alignment.CenterVertically) {
   Text(weatherIcon(data.currentCode),fontSize=55.sp)
   Spacer(Modifier.width(14.dp))
   Column {
    Text("${data.currentTemperature}°",fontSize=57.sp,color=light,fontWeight=FontWeight.Light)
    Text("Αίσθηση ${data.feelsLike}°  ·  ${data.updated.take(16).replace('T',' ')}",fontSize=11.sp,color=light.copy(alpha=.8f))
   }
  }
  FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
   StatPill("Υγρασία","${data.relativeHumidity}%")
   StatPill("Άνεμος","${data.windKmh} km/h")
   StatPill("Ριπές","${data.windGustKmh} km/h")
   StatPill("Πίεση","${data.pressureHpa} hPa")
   StatPill("UV","${data.uvIndex}")
  }
  Spacer(Modifier.height(12.dp))
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   listOf("ΑΝΑ ΩΡΑ", "7 ΗΜΕΡΕΣ").forEachIndexed { idx, title ->
    FilterChip(selected=mode==idx,onClick={mode=idx;onActivity()},label={Text(title)})
   }
  }
  if (mode==0) {
   val hours=data.hours.take(24)
   if(hours.isEmpty()) Text("Δεν υπάρχουν ωριαία δεδομένα",color=light)
   else {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     hours.forEach { h ->
      val barHeight=(18+(h.temperature+15).coerceIn(0,55)*1.55).dp
      Column(Modifier.width(52.dp).clickable { onActivity() },horizontalAlignment=Alignment.CenterHorizontally) {
       Text(h.isoTime.takeLast(5),color=light.copy(alpha=.8f),fontSize=11.sp)
       Text(weatherIcon(h.weatherCode),fontSize=19.sp)
       Text("${h.temperature}°",color=light,fontSize=13.sp)
       Box(Modifier.height(104.dp).width(17.dp),contentAlignment=Alignment.BottomCenter) {
        Box(Modifier.width(16.dp).height(barHeight.coerceAtMost(96.dp)).background(Brush.verticalGradient(listOf(Color(0xFFC6A2FF),cyan)),RoundedCornerShape(8.dp)))
       }
       Text("${h.precipitationProbability}%",color=cyan,fontSize=10.sp)
      }
     }
    }
    Text("Θερμοκρασία και πιθανότητα βροχής ανά ώρα",color=light.copy(alpha=.6f),fontSize=10.sp)
   }
  } else {
   val days=data.days
   if(days.isEmpty()) Text("Δεν υπάρχει εβδομαδιαία πρόγνωση",color=light)
   else {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     days.forEachIndexed { i,d ->
      val active = selected==i
      Column(Modifier.width(94.dp).border(1.dp,if(active) cyan else Color(0x4075E6FF),RoundedCornerShape(14.dp)).background(if(active) Color(0x4450A5FF) else Color(0x222B507A),RoundedCornerShape(14.dp)).clickable { selected=i;onActivity() }.padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
       Text(d.date.takeLast(5),fontSize=12.sp,color=light)
       Text(weatherIcon(d.weatherCode),fontSize=25.sp)
       Text("${d.maxTemperature}° / ${d.minTemperature}°",fontSize=12.sp,color=light)
       Text("☂ ${d.precipitationProbability}%",fontSize=11.sp,color=cyan)
      }
     }
    }
    val d=days[selected.coerceIn(0,days.lastIndex)]
    Spacer(Modifier.height(8.dp))
    Text("${d.date} · Ανατολή ${d.sunrise.takeLast(5)} · Δύση ${d.sunset.takeLast(5)}",color=light,fontSize=12.sp)
   }
  }
  if (error.isNotBlank()) Text(error,color=Color(0xFFFFC698),fontSize=11.sp)
  Text("Πηγή: Open-Meteo · Οι προβλέψεις ανανεώνονται online",color=light.copy(alpha=.58f),fontSize=10.sp)
 }
}
