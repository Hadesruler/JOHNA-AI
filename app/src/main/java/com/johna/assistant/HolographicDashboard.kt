package com.johna.assistant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

private val ice = Color(0xFF64DFFF)
private val lilac = Color(0xFFBB87FF)
private val milk = Color(0xFFF0F7FF)
private val dim = Color(0xFF99B7D8)

/** Atmospheric vector backdrop, animated but cheap enough for the Redmi Pad 2. */
@Composable
fun JohnaAuroraBackdrop(modifier: Modifier = Modifier) {
 val inf = rememberInfiniteTransition(label = "nebula")
 val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(48000, easing = LinearEasing)), label="auroraDrift")
 Canvas(modifier) {
  val w = size.width; val h = size.height
  drawRect(Brush.verticalGradient(listOf(Color(0xFF031126), Color(0xFF071A40), Color(0xFF0D1641), Color(0xFF021126))))
  fun aurora(x: Float, y: Float, rad: Float, color: Color, alpha: Float) {
   val c=Offset(w*x, h*y)
   drawCircle(Brush.radialGradient(listOf(color.copy(alpha=alpha), Color.Transparent),c,w*rad),w*rad,c)
  }
  aurora(.54f + .035f*sin(t*6.283f),.54f,.53f,Color(0xFF1B65CF),.38f)
  aurora(.28f,.85f,.4f,Color(0xFF7138C6),.30f)
  aurora(.79f,.76f,.38f,Color(0xFF155B9D),.32f)
  aurora(.53f,.96f,.44f,Color(0xFF1FC5FF),.20f)
  for(i in 0 until 65) {
   val x = ((i * .61803399f + .19f)%1f)*w
   val y = ((i * .381966f + .07f)%1f)*h*.92f
   val a=.17f+((i%5)*.065f)
   drawCircle(Color(0xFFB5DFFF).copy(alpha=a),if(i%8==0)1.8f else .8f,Offset(x,y))
  }
  // Low-contrast perspective waves and a distant horizon.
  for(i in 0 until 6) {
   val y = h*(.75f + i*.044f)
   drawLine(Color(0xFF56C4FF).copy(alpha=.05f + i*.01f),Offset(0f,y),Offset(w,y),1.3f)
  }
  fun ridge(baseY: Float, amplitude: Float, color: Color, seed: Int) {
   val path=Path().apply { moveTo(0f,h); lineTo(0f,baseY)
    for(i in 0..50) {
     val x=w*(i/50f)
     val f=sin(i*1.3f+seed)*.42f+sin(i*.57f+seed*3f)*.32f+cos(i*.19f+seed)*.26f
     lineTo(x,baseY - amplitude*(f+.45f))
    }; lineTo(w,h); close() }
   drawPath(path,color)
  }
  ridge(h*.91f,h*.055f,Color(0xAA0A1740),3)
  ridge(h*.965f,h*.037f,Color(0xD8091732),7)
  drawLine(Color(0x6641CCFF),Offset(w*.17f,h*.972f),Offset(w*.84f,h*.972f),1.5f)
 }
}

@Composable
private fun NeonCard(
 title: String,
 icon: ImageVector,
 modifier: Modifier = Modifier,
 onClick: () -> Unit,
 suffix: String = "",
 content: @Composable ColumnScope.() -> Unit
) {
 val shape=RoundedCornerShape(23.dp)
 Column(
  modifier
   .shadow(12.dp,shape,ambientColor=Color(0x66015AFF),spotColor=Color(0x55114FE0))
   .border(1.dp,Brush.linearGradient(listOf(ice.copy(alpha=.86f),lilac.copy(alpha=.5f),ice.copy(alpha=.38f))),shape)
   .background(Brush.verticalGradient(listOf(Color(0xD7102B53),Color(0xD3091A37))),shape)
   .clickable(onClick=onClick)
   .padding(horizontal=14.dp,vertical=12.dp)
 ) {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   Icon(icon,null,tint=ice,modifier=Modifier.size(19.dp))
   Spacer(Modifier.width(8.dp))
   Text(title,color=milk,fontSize=14.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f),maxLines=1)
   if(suffix.isNotBlank()) Text(suffix,color=dim,fontSize=9.sp)
   Icon(Icons.Default.ChevronRight,null,tint=ice.copy(alpha=.65f),modifier=Modifier.size(17.dp))
  }
  Spacer(Modifier.height(8.dp))
  content()
 }
}

@Composable
private fun GlassAction(label: String, icon: ImageVector, onClick: () -> Unit, strong: Boolean=false) {
 val shape=RoundedCornerShape(18.dp)
 Row(
  Modifier.border(1.dp,if(strong)lilac else ice.copy(alpha=.49f),shape)
   .background(if(strong) Brush.horizontalGradient(listOf(Color(0xBB6236C3),Color(0xC7355CAB)))
   else Brush.horizontalGradient(listOf(Color(0xB20A315A),Color(0xA3102453))),shape)
   .clickable(onClick=onClick).padding(horizontal=11.dp,vertical=8.dp),
  verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)
 ) {
  Icon(icon,null,tint=if(strong) milk else ice,modifier=Modifier.size(16.dp))
  Text(label,color=milk,fontSize=11.sp,maxLines=1)
 }
}

@Composable
private fun VoiceWaveform(speaking:Boolean,listening:Boolean,energy:Float,modifier:Modifier=Modifier) {
 val inf=rememberInfiniteTransition(label="voiceWave")
 val phase by inf.animateFloat(0f,6.28318f,infiniteRepeatable(tween(1800,easing=LinearEasing)),label="wavePhase")
 Canvas(modifier) {
  val w=size.width;val h=size.height
  val power=if(listening) (.2f+energy*.8f) else if(speaking) .75f else .2f
  val path=Path()
  for(i in 0..120) {
   val u=i/120f;val env=sin(u*PI.toFloat()).pow(2)
   val y=h*.5f + sin(u*29f-phase*1.4f)*env*h*.25f*power + sin(u*52f+phase)*env*h*.12f*power
   if(i==0)path.moveTo(u*w,y) else path.lineTo(u*w,y)
  }
  drawPath(path,ice.copy(alpha=.12f),style=Stroke(width=12f))
  drawPath(path,lilac.copy(alpha=.72f),style=Stroke(width=3f))
  drawPath(path,Color(0xFF86F8FF),style=Stroke(width=1.5f))
  for(i in 0..6) { val x=w*i/6f;drawCircle(ice.copy(alpha=.5f),1.7f,Offset(x,h*.5f)) }
 }
}

/** Live data only: no fabricated events, notifications or connected devices. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HolographicHome(
 status: String,aiResponse:String,ambientEnabled:Boolean,themeAccent:Color,
 speaking:Boolean,listening:Boolean,micEnergy:Float,volumeLevel:Int,continuous:Boolean,
 weather:String,weatherDetails:String,forecast:WeatherForecast?,
 notifications:List<String>,calendarEvents:List<CalendarEvent>,googleSignedIn:Boolean,
 devices:List<Pair<String,String>>,deviceScan:String,
 installedApps:List<AppShortcut>,
 onExpand:(String)->Unit,onSettings:()->Unit,onThemes:()->Unit,
 onListen:()->Unit,onWake:()->Unit,onStop:()->Unit,onWeather:()->Unit,
 onDiscover:()->Unit,onHomeController:()->Unit,onMail:()->Unit,onChrome:()->Unit,
 onQuickCommand:(String)->Unit,onLaunchApp:(String)->Unit
) {
 var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
 LaunchedEffect(Unit) { while(true){delay(15000L);clock=System.currentTimeMillis()} }
 val localTime=remember(clock){SimpleDateFormat("HH:mm",Locale("el","GR")).format(Date(clock))}
 val date=remember(clock){SimpleDateFormat("EEE, d MMM yyyy",Locale("el","GR")).format(Date(clock))}
 Box(Modifier.fillMaxSize()) {
  if(ambientEnabled) JohnaAuroraBackdrop(Modifier.fillMaxSize())
  else Box(Modifier.fillMaxSize().background(Color(0xFF07142D)))
  Column(Modifier.fillMaxSize().padding(horizontal=19.dp,vertical=12.dp)) {
   Row(Modifier.fillMaxWidth().height(53.dp),verticalAlignment=Alignment.CenterVertically) {
    Text(localTime,color=milk,fontSize=21.sp,fontWeight=FontWeight.Light)
    Spacer(Modifier.width(10.dp))
    Text(date,color=dim,fontSize=11.sp)
    Spacer(Modifier.width(14.dp))
    Icon(Icons.Default.Cloud,null,tint=ice,modifier=Modifier.size(15.dp))
    Spacer(Modifier.width(4.dp))
    Text(if(forecast!=null) "${forecast.currentTemperature}°C · ${forecast.city}" else "Καιρός",color=dim,fontSize=11.sp)
    Spacer(Modifier.weight(1f))
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
     Text("J O H N A   A I",color=milk,fontSize=24.sp,letterSpacing=4.sp,fontWeight=FontWeight.Light)
     Text("Ο ΠΡΟΣΩΠΙΚΟΣ ΣΟΥ AI ΒΟΗΘΟΣ",color=themeAccent.copy(alpha=.85f),fontSize=8.sp,letterSpacing=2.sp)
    }
    Spacer(Modifier.weight(1f))
    IconButton(onClick=onThemes) {Icon(Icons.Default.AutoAwesome,"Αλλαγή θέματος",tint=lilac)}
    IconButton(onClick=onSettings) {Icon(Icons.Default.Settings,"Ρυθμίσεις",tint=milk)}
   }
   Spacer(Modifier.height(9.dp))
   Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(15.dp)) {
    Column(Modifier.weight(1.10f),verticalArrangement=Arrangement.spacedBy(11.dp)) {
     NeonCard("Καιρός · ${forecast?.city ?: "Αθήνα"}",Icons.Default.WbSunny,Modifier.weight(1.17f),onClick={onExpand("weather")},suffix="LIVE") {
      Row(verticalAlignment=Alignment.CenterVertically) {
       Icon(Icons.Default.WbSunny,null,tint=Color(0xFFFFD25C),modifier=Modifier.size(36.dp))
       Spacer(Modifier.width(9.dp))
       Text(if(forecast==null) weather else "${forecast.currentTemperature}°C",color=milk,fontSize=39.sp,fontWeight=FontWeight.Light,maxLines=1)
      }
      Text(weatherDetails,color=dim,fontSize=10.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
      if(forecast!=null){
       Spacer(Modifier.height(5.dp))
       AnimatedWeatherSky(forecast,Modifier.height(73.dp))
       Spacer(Modifier.height(4.dp))
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        forecast.hours.take(5).forEach { hour ->
         Column(horizontalAlignment=Alignment.CenterHorizontally) {
          Text(hour.isoTime.takeLast(5),color=dim,fontSize=9.sp)
          Icon(if(hour.weatherCode in 51..99)Icons.Default.Cloud else Icons.Default.WbSunny,null,tint=ice,modifier=Modifier.size(15.dp))
          Text("${hour.temperature}°",color=milk,fontSize=11.sp)
         }
        }
       }
      } else {
       Spacer(Modifier.weight(1f));Text("Άνοιξε για ωριαία και επταήμερη πρόγνωση",color=ice,fontSize=10.sp)
      }
     }
     NeonCard("Ημερολόγιο",Icons.Default.CalendarMonth,Modifier.weight(.90f),onClick={onExpand("calendar")},suffix=date.take(12)) {
      if(!googleSignedIn) Text("Ο λογαριασμός Calendar δεν έχει συνδεθεί στη JOHNA",color=dim,fontSize=11.sp)
      else if(calendarEvents.isEmpty()) Text("Δεν υπάρχουν διαθέσιμα ραντεβού",color=dim,fontSize=11.sp)
      else calendarEvents.take(3).forEachIndexed { index,e ->
       Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.width(3.dp).height(27.dp).background(if(index%2==0)ice else lilac,RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Column {Text(e.startDisplay,color=ice,fontSize=10.sp);Text(e.summary,color=milk,fontSize=11.sp,maxLines=1)}
       }
      }
     }
     NeonCard("Εφαρμογές & συντομεύσεις",Icons.Default.Apps,Modifier.weight(.79f),onClick={onExpand("apps")},suffix="ANDROID") {
      Text("Οι εφαρμογές ανοίγουν στο δικό τους περιβάλλον",color=dim,fontSize=10.sp,maxLines=1)
      Spacer(Modifier.height(5.dp))
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
       GlassAction("Gmail",Icons.Default.Email,onClick=onMail)
       GlassAction("Chrome",Icons.Default.Public,onClick=onChrome)
      }
      Text("${installedApps.size} εφαρμογές διαθέσιμες στον launcher",color=ice.copy(alpha=.73f),fontSize=10.sp,modifier=Modifier.padding(top=7.dp))
     }
    }
    Column(Modifier.weight(1.35f).fillMaxHeight(),horizontalAlignment=Alignment.CenterHorizontally) {
     Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center) {
      JohnaSphere(speaking,listening,Modifier.fillMaxWidth(.98f).aspectRatio(1f).clickable(onClick=onListen),micEnergy)
     }
     Text("Είμαι η Johna",color=milk,fontSize=22.sp,fontWeight=FontWeight.Light)
     Text(status,color=ice,fontSize=11.sp,maxLines=2,textAlign=TextAlign.Center)
     VoiceWaveform(speaking,listening,micEnergy,Modifier.fillMaxWidth(.83f).height(36.dp))
     Text(aiResponse,color=dim,fontSize=11.sp,textAlign=TextAlign.Center,maxLines=3,overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth().heightIn(min=26.dp,max=55.dp))
     Spacer(Modifier.height(9.dp))
     Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){
      GlassAction(if(listening)"Ακούω..." else "Μίλα στη Johna",Icons.Default.Mic,onListen,true)
      GlassAction(if(continuous)"Wake ON" else "Wake",Icons.Default.GraphicEq,onWake)
      GlassAction("Στοπ",Icons.Default.Stop,onStop)
     }
     Spacer(Modifier.height(10.dp))
     FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalArrangement=Arrangement.spacedBy(6.dp)) {
      GlassAction("Άνοιξε Gmail",Icons.Default.Email,onMail)
      Spacer(Modifier.width(5.dp))
      GlassAction("Άνοιξε καιρό",Icons.Default.Cloud,onClick={onExpand("weather")})
      Spacer(Modifier.width(5.dp))
      GlassAction("Πήγαινε αρχική",Icons.Default.Home,onClick={onExpand("close")})
      Spacer(Modifier.width(5.dp))
      GlassAction("Volume +2",Icons.Default.VolumeUp,onClick={onQuickCommand("volume up 2")})
      Spacer(Modifier.width(5.dp))
      GlassAction("Volume −2",Icons.Default.VolumeDown,onClick={onQuickCommand("volume down 2")})
     }
     Spacer(Modifier.height(10.dp))
     Text("MEDIA  $volumeLevel/10     ·     Έλεγχος με φωνή και άγγιγμα",color=ice.copy(alpha=.72f),fontSize=10.sp)
     Spacer(Modifier.height(13.dp))
    }
    Column(Modifier.weight(1.1f),verticalArrangement=Arrangement.spacedBy(11.dp)) {
     NeonCard("Ειδοποιήσεις (${notifications.size})",Icons.Default.Notifications,Modifier.weight(.92f),onClick={onExpand("notifications")},suffix="ΟΛΕΣ") {
      if(notifications.isEmpty()) Text("Δεν υπάρχουν διαθέσιμες ειδοποιήσεις ή δεν έχει δοθεί πρόσβαση",color=dim,fontSize=11.sp)
      else notifications.take(3).forEach { notification ->
       Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
        Icon(Icons.Default.NotificationsActive,null,tint=ice,modifier=Modifier.size(15.dp)); Spacer(Modifier.width(7.dp))
        Text(notification,color=milk,fontSize=11.sp,maxLines=2)
       }
      }
     }
     NeonCard("Smart Home & Συσκευές",Icons.Default.Home,Modifier.weight(1.05f),onClick={onExpand("devices")},suffix="LAN") {
      Row(verticalAlignment=Alignment.CenterVertically){
       Icon(Icons.Default.Search,null,tint=ice,modifier=Modifier.size(31.dp))
       Spacer(Modifier.width(9.dp))
       Column {Text("${devices.size} υπηρεσίες εντοπίστηκαν",color=milk,fontSize=12.sp)
        Text(deviceScan,color=dim,fontSize=10.sp,maxLines=2) }
      }
      Spacer(Modifier.height(12.dp))
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
       Box(Modifier.weight(1f)) { ConnectionTile(Icons.Default.Lightbulb,"Φώτα","Χρειάζεται σύνδεση",false) }
       Box(Modifier.weight(1f)) { ConnectionTile(Icons.Default.AcUnit,"Κλιματισμός","Χρειάζεται σύνδεση",false) }
      }
      Spacer(Modifier.height(7.dp))
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
       Box(Modifier.weight(1f)) { ConnectionTile(Icons.Default.CameraAlt,"Κάμερες","Μέσω controller",false) }
       Box(Modifier.weight(1f)) { ConnectionTile(Icons.Default.Wifi,"Network","Σάρωση LAN",devices.isNotEmpty()) }
      }
      Spacer(Modifier.height(9.dp))
      Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
       GlassAction("Αναζήτηση",Icons.Default.Search,onDiscover)
       GlassAction("Controller",Icons.Default.OpenInNew,onHomeController)
      }
     }
    }
   }
  }
 }
}

@Composable
private fun ConnectionTile(icon:ImageVector,name:String,detail:String,online:Boolean){
 val shape=RoundedCornerShape(13.dp)
 Column(Modifier.fillMaxWidth().border(1.dp,ice.copy(alpha=.22f),shape)
  .background(Color(0x8A18355C),shape).padding(9.dp)) {
  Icon(icon,null,tint=if(online)ice else lilac,modifier=Modifier.size(21.dp))
  Spacer(Modifier.height(4.dp))
  Text(name,color=milk,fontSize=11.sp,maxLines=1)
  Text(detail,color=if(online)Color(0xFF75F5C1) else dim,fontSize=9.sp,maxLines=1)
 }
}
