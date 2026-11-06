package com.johna.assistant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val white = Color(0xFFF2F6FF)
private val aqua = Color(0xFF75E6FF)
private val lavender = Color(0xFFC6A2FF)

@Composable
private fun GlassBox(title: String, modifier: Modifier = Modifier, onExpand: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
 Column(modifier.shadow(18.dp, RoundedCornerShape(24.dp), ambientColor=Color.Black, spotColor=Color.Black)
  .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha=.65f),lavender.copy(alpha=.35f),aqua.copy(alpha=.55f))), RoundedCornerShape(24.dp))
  .background(Color(0x77425483),RoundedCornerShape(24.dp)).clickable { onExpand() }.padding(15.dp)) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
   Text(title, color=white, fontSize=17.sp, fontWeight=FontWeight.SemiBold)
   Text("⤢", color=aqua, fontSize=18.sp)
  }
  Spacer(Modifier.height(10.dp))
  content()
 }
}

@Composable
fun GlassDashboard(status:String, heard:String, aiResponse:String, voicePanel:String, onVoicePanelConsumed:()->Unit, continuous:Boolean, weather:String, weatherDetails:String,
 notifications:List<String>, devices:List<Pair<String,String>>, deviceScan:String, keyConfigured:Boolean,
 onWeather:()->Unit,onDiscover:()->Unit,onNotifications:()->Unit,onListen:()->Unit,onStop:()->Unit,
 onContinuous:(Boolean)->Unit,onSaveKey:(String)->Unit,onClearHistory:()->Unit,onHello:()->Unit,onTestGroq:()->Unit) {
 var focused by remember { mutableStateOf("") }
 var seconds by remember { mutableIntStateOf(0) }
 var theme by remember { mutableIntStateOf(0) }
 var settings by remember { mutableStateOf(false) }
 LaunchedEffect(voicePanel) {
  if (voicePanel.isNotBlank()) {
   if (voicePanel == "settings") settings = true
   else focused = if (voicePanel == "close") "" else voicePanel
   onVoicePanelConsumed()
  }
 }
 var keyDraft by remember { mutableStateOf("") }
 var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
 LaunchedEffect(Unit) { while(true) { now=System.currentTimeMillis(); delay(1000) } }
 LaunchedEffect(focused,seconds) { if(focused.isNotEmpty() && seconds>0) { delay(seconds*1000L);focused="";seconds=0 } }
 val accents=listOf(aqua,lavender,Color(0xFFFFCB87),Color(0xFFB7E5FF))
 val accent=accents[theme]
 val time=remember(now){SimpleDateFormat("HH:mm",Locale("el","GR")).format(Date(now))}
 val date=remember(now){SimpleDateFormat("EEE, d MMM yyyy",Locale("el","GR")).format(Date(now))}
 val infinite=rememberInfiniteTransition(label="orb")
 val pulse by infinite.animateFloat(.94f,1.045f,infiniteRepeatable(tween(2100),RepeatMode.Reverse),label="pulse")
 val context=androidx.compose.ui.platform.LocalContext.current
 val prefs=remember {context.getSharedPreferences("johna_glass",0)}
 LaunchedEffect(Unit) { theme=prefs.getInt("theme",0).coerceIn(0,3) }
 Box(Modifier.fillMaxSize().background(Color(0xFF10172C))) {
  Image(painterResource(R.drawable.johna_landscape_bg),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x5512213A),Color(0x33060D25),Color(0x99040B1E)))))
  Column(Modifier.fillMaxSize().padding(horizontal=20.dp,vertical=10.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
    Column {Text(time,color=white,fontSize=30.sp,fontWeight=FontWeight.Light);Text(date,color=white,fontSize=11.sp)}
    Column(horizontalAlignment=Alignment.CenterHorizontally) {Text("✧  J O H N A   A I  ✧",color=white,fontSize=25.sp,fontWeight=FontWeight.Light);Text("GLASS COMMAND CENTER",color=accent,fontSize=9.sp)}
    Text("◈  THEMES",color=accent,modifier=Modifier.clickable { theme=(theme+1)%4;prefs.edit().putInt("theme",theme).apply() }.padding(10.dp))
   }
   Spacer(Modifier.height(12.dp))
   Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     GlassBox("☁  Καιρός · Αθήνα",Modifier.weight(1f),onExpand={focused="weather"}) {
      Text(if(weather.contains("°")) "☀  $weather" else "☁  $weather",color=white,fontSize=33.sp,fontWeight=FontWeight.Light)
      Text(weatherDetails,color=white.copy(alpha=.85f),fontSize=11.sp,maxLines=2)
      Spacer(Modifier.weight(1f));Text("☀       ☁       🌧       ⛈",color=accent,fontSize=20.sp)
      Text("Πρόγνωση καιρού",color=white.copy(alpha=.75f),fontSize=11.sp)
     }
     GlassBox("▦  Ημερολόγιο",Modifier.weight(.9f),onExpand={focused="calendar"}) {
      Text(date,color=white,fontSize=14.sp);Spacer(Modifier.height(10.dp))
      Text("Σύνδεσε Google Calendar για ραντεβού",color=white.copy(alpha=.85f),fontSize=12.sp)
     }
    }
    Column(Modifier.weight(1.36f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
     Image(painterResource(R.drawable.johna_glass_orb),"JOHNA AI",Modifier.fillMaxWidth(.78f).aspectRatio(1f).graphicsLayer {scaleX=pulse;scaleY=pulse}.clickable {onListen()},contentScale=ContentScale.Fit)
     Text(status,color=white,fontSize=16.sp,fontWeight=FontWeight.Medium)
     Text(heard,color=white.copy(alpha=.8f),fontSize=11.sp,textAlign=TextAlign.Center,maxLines=2)
     Text(aiResponse,color=aqua,fontSize=12.sp,textAlign=TextAlign.Center,maxLines=5,modifier=Modifier.fillMaxWidth().heightIn(max=105.dp).verticalScroll(rememberScrollState()))
     Spacer(Modifier.height(12.dp))
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
      Button(onClick=onListen,colors=ButtonDefaults.buttonColors(containerColor=Color(0xBB7555C6))) {Text("🎙  Μίλα στη Johna")}
      OutlinedButton(onClick=onStop){Text("Στοπ",color=white)}
     }
     Row(verticalAlignment=Alignment.CenterVertically) {Text("Συνεχής συνομιλία",color=white,fontSize=11.sp);Switch(checked=continuous,onCheckedChange=onContinuous,modifier=Modifier.height(35.dp))}
    }
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     GlassBox("♧  Ειδοποιήσεις (${notifications.size})",Modifier.weight(1f),onExpand={focused="notifications"}) {
      if(notifications.isEmpty()) Text("Δεν υπάρχουν διαθέσιμες ειδοποιήσεις ή δεν έχει δοθεί άδεια",color=white.copy(alpha=.8f),fontSize=12.sp)
      else notifications.take(3).forEach {Text("• $it",color=white,fontSize=12.sp,maxLines=2);Spacer(Modifier.height(7.dp))}
     }
     GlassBox("⌂  Smart Home",Modifier.weight(.9f),onExpand={focused="devices"}) {
      Text("${devices.size} υπηρεσίες δικτύου",color=white,fontSize=14.sp)
      Text(deviceScan,color=white.copy(alpha=.8f),fontSize=11.sp,maxLines=2)
      Spacer(Modifier.height(7.dp));Text("◉  Αναζήτηση συσκευών",color=accent,fontSize=12.sp,modifier=Modifier.clickable {onDiscover()})
     }
    }
   }
   Spacer(Modifier.height(14.dp))
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    listOf("◉" to "Συνομιλία","☀" to "Καιρός","▦" to "Ημερολόγιο","♧" to "Ειδοποιήσεις","⌂" to "Συσκευές","⚙" to "Ρυθμίσεις").forEachIndexed { index,item ->
     Column(Modifier.weight(1f).height(82.dp).shadow(12.dp,RoundedCornerShape(20.dp))
      .border(1.dp,accent.copy(alpha=.7f),RoundedCornerShape(20.dp)).background(Color(0x88546696),RoundedCornerShape(20.dp))
      .clickable { when(index){0->onListen();1->focused="weather";2->focused="calendar";3->focused="notifications";4->focused="devices";else->settings=true} }
      .padding(7.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
       Text(item.first,color=white,fontSize=26.sp);Text(item.second,color=white,fontSize=11.sp)
      }
    }
   }
  }
  AnimatedVisibility(focused.isNotEmpty(),enter=fadeIn(tween(250))+scaleIn(initialScale=.7f)+slideInVertically {it/6},exit=fadeOut(tween(250))+scaleOut(targetScale=.85f)) {
   Box(Modifier.fillMaxSize().background(Color(0xB8000719)).clickable {focused=""},contentAlignment=Alignment.Center) {
    GlassBox(when(focused){"weather"->"☁  Καιρός · Αθήνα";"calendar"->"▦  Ημερολόγιο";"notifications"->"♧  Ειδοποιήσεις";else->"⌂  Έξυπνες συσκευές"},Modifier.fillMaxWidth(.76f).fillMaxHeight(.8f),onExpand={focused=""}) {
     when(focused) {
      "weather"->{Text(weather,color=white,fontSize=54.sp);Text(weatherDetails,color=white);Spacer(Modifier.height(12.dp));Text("☀    ☁    🌧    ⛈",fontSize=36.sp);TextButton(onClick=onWeather){Text("Ανανέωση",color=aqua)}}
      "calendar"->{Text(date,color=white,fontSize=23.sp);Text("Τα συμβάντα Google Calendar απαιτούν εξουσιοδότηση OAuth.",color=white)}
      "notifications"->{if(notifications.isEmpty()) Text("Δεν υπάρχουν διαθέσιμες ειδοποιήσεις.",color=white) else notifications.forEach {Text("• $it",color=white)};TextButton(onClick=onNotifications){Text("Διαχείριση πρόσβασης",color=aqua)}}
      else->{Text(deviceScan,color=white);devices.forEach {Text("• ${it.first} · ${it.second}",color=white,fontSize=12.sp)};TextButton(onClick=onDiscover){Text("Αναζήτηση Wi-Fi",color=aqua)}}
     }
     Spacer(Modifier.weight(1f));Row(verticalAlignment=Alignment.CenterVertically) {
      Text("Αυτόματο κλείσιμο",color=white,fontSize=12.sp);Spacer(Modifier.width(10.dp));listOf(0,5,10,20).forEach {n->TextButton(onClick={seconds=n}){Text(if(n==0) "Όχι" else "${n}s",color=if(seconds==n) aqua else white)}}
     }
     TextButton(onClick={focused=""}){Text("✕ Κλείσιμο",color=white)}
    }
   }
  }
  if(settings) AlertDialog(onDismissRequest={settings=false},title={Text("Ρυθμίσεις JOHNA")},text={Column {
   Text(if(keyConfigured) "Groq API key αποθηκευμένο" else "Δεν υπάρχει Groq API key")
   OutlinedTextField(keyDraft,{keyDraft=it},label={Text("Νέο API key")},singleLine=true)
   TextButton(onClick={if(keyDraft.isNotBlank())onSaveKey(keyDraft.trim());keyDraft=""}){Text("Αποθήκευση κρυπτογραφημένα")}
   TextButton(onClick=onClearHistory){Text("Διαγραφή μνήμης συνομιλιών")}
   TextButton(onClick=onHello){Text("Δοκιμή φωνής")}
   TextButton(onClick={settings=false;onTestGroq()}){Text("Δοκιμή σύνδεσης Groq (χωρίς μικρόφωνο)")}
  }},confirmButton={TextButton(onClick={settings=false}){Text("Κλείσιμο")}})
 }
}
