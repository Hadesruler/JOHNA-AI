package com.johna.assistant

import android.Manifest
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import java.net.URL
import org.json.JSONObject
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
 private val main = Handler(Looper.getMainLooper())
 private var recognizer: SpeechRecognizer? = null
 private var tts: TextToSpeech? = null
 private var speaking = false
 private var listening = false
 private var active = false
 private var errors = 0
 private var ttsReady = false
 private var weather by mutableStateOf("Φόρτωση...")
 private var weatherDetails by mutableStateOf("Αθήνα · Open-Meteo")
 private var notifications by mutableStateOf(emptyList<String>())
 private var agentInfo by mutableStateOf("Agents: εντολές κανόνων · χωρίς LLM")
 private var wakeEnabled by mutableStateOf(false)
 private var lastWeatherUpdate = 0L
 private var status by mutableStateOf("Έτοιμη")
 private var heard by mutableStateOf("Πάτησε Μίλα ή ενεργοποίησε τη συνομιλία")
 private var continuous by mutableStateOf(false)
 private var history = mutableStateListOf<Pair<String, String>>()
 private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if (granted) beginListening() else { active = false; status = "Χρειάζεται άδεια μικροφώνου" }
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  tts = TextToSpeech(this, this)
  tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
   override fun onStart(utteranceId: String?) { main.post { speaking = true; status = "Μιλάω" } }
   override fun onDone(utteranceId: String?) { main.post { speaking = false; if (active && continuous) main.postDelayed({ beginListening() }, 450) else status = "Έτοιμη" } }
   override fun onError(utteranceId: String?) { main.post { speaking = false; status = "Σφάλμα φωνής" } }
  })
  NotificationBridge.onUpdate = { items -> main.post { notifications = items } }
  refreshWeather()
  setContent {
   JohnaScreen(status, heard, history.toList(), continuous, weather, weatherDetails, notifications, agentInfo, wakeEnabled,
    onWeather = { refreshWeather() },
    onNotifications = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
    onWake = { wakeEnabled = it; agentInfo = if (it) "Hey Johna: απαιτείται wake-word engine · μη ενεργό" else "Wake word απενεργοποιημένο" },
    onContinuous = { enabled -> continuous = enabled; if (enabled) { active = true; requestListen() } else stopConversation() },
    onListen = { active = true; requestListen() },
    onStop = { stopConversation() },
    onHello = { active = false; speak("Γεια σου! Είμαι η Τζόνα. Είμαι έτοιμη.") })
  }
 }
 override fun onInit(result: Int) {
  if (result == TextToSpeech.SUCCESS) {
   val available = tts?.setLanguage(Locale("el", "GR"))
   ttsReady = available != TextToSpeech.LANG_MISSING_DATA && available != TextToSpeech.LANG_NOT_SUPPORTED
   if (!ttsReady) status = "Δεν βρέθηκε ελληνική φωνή στη συσκευή"
  } else status = "Η υπηρεσία φωνής δεν ξεκίνησε"
 }
 private fun stopConversation() {
  active = false; continuous = false; listening = false
  recognizer?.cancel(); recognizer?.destroy(); recognizer = null
  tts?.stop(); speaking = false; status = "Σταμάτησε"
 }
 private fun requestListen() {
  if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) beginListening()
  else permission.launch(Manifest.permission.RECORD_AUDIO)
 }
 private fun speak(text: String) {
  if (!ttsReady) { status = "Η ελληνική φωνή δεν είναι έτοιμη"; return }
  status = "Μιλάω"; speaking = true
  tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "johna_${System.currentTimeMillis()}")
 }
 private fun beginListening() {
  if (!active || speaking || listening || isFinishing || isDestroyed) return
  if (!SpeechRecognizer.isRecognitionAvailable(this)) { status = "Δεν υπάρχει υπηρεσία αναγνώρισης"; active = false; return }
  recognizer?.destroy(); recognizer = null
  listening = true
  recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
   sr.setRecognitionListener(object : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) { status = "Σε ακούω..." }
    override fun onBeginningOfSpeech() { status = "Ακούω" }
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { status = "Επεξεργάζομαι..." }
    override fun onError(error: Int) {
     listening = false
     errors++
     status = "Σφάλμα αναγνώρισης: $error"
     if (active && continuous && errors <= 3) main.postDelayed({ beginListening() }, 1300L * errors)
     else { active = false; if (errors > 3) status = "Η αναγνώριση σταμάτησε. Πάτησε Μίλα." }
    }
    override fun onResults(results: Bundle?) {
     listening = false; errors = 0
     val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
     if (words.isBlank()) { if (active && continuous) main.postDelayed({ beginListening() }, 600); return }
     heard = words
     val reply = localReply(words)
     history.add(words to reply)
     if (history.size > 25) history.removeAt(0)
     speak(reply)
    }
    override fun onPartialResults(partialResults: Bundle?) {
     val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
     if (!partial.isNullOrBlank()) heard = partial
    }
    override fun onEvent(eventType: Int, params: Bundle?) {}
   })
   val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "el-GR")
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "el-GR")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
   }
   status = "Ενεργοποίηση μικροφώνου..."
   try { sr.startListening(intent) } catch (e: Exception) { listening = false; active = false; status = "Αδυναμία εκκίνησης μικροφώνου" }
  }
 }
 private fun refreshWeather() {
  weather = "Ενημέρωση..."
  Thread {
   try {
    val conn = URL("https://api.open-meteo.com/v1/forecast?latitude=37.9838&longitude=23.7275&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=Europe%2FAthens").openConnection()
    conn.connectTimeout = 6500; conn.readTimeout = 6500
    val data = JSONObject(conn.getInputStream().bufferedReader().use { it.readText() }).getJSONObject("current")
    val t = data.getDouble("temperature_2m").toInt()
    val h = data.getInt("relative_humidity_2m")
    val w = data.getDouble("wind_speed_10m").toInt()
    main.post { weather = "$t°C"; weatherDetails = "Αθήνα · Υγρασία $h% · Άνεμος $w km/h"; lastWeatherUpdate = System.currentTimeMillis() }
   } catch (_: Exception) { main.post { weather = "Χωρίς δεδομένα"; weatherDetails = "Έλεγξε τη σύνδεση · Ανανέωση" } }
  }.start()
 }
 private fun localReply(words: String): String {
  val s = words.lowercase(Locale("el", "GR"))
  return when {
   "καιρ" in s || "θερμοκρασ" in s -> "Στην Αθήνα η θερμοκρασία είναι $weather. $weatherDetails"
   "email" in s || "μέιλ" in s || "ειδοποι" in s -> if (notifications.isEmpty()) "Δεν βλέπω ειδοποιήσεις. Έλεγξε την πρόσβαση ειδοποιήσεων." else "Έχεις ${notifications.size} πρόσφατες ειδοποιήσεις. ${notifications.take(2).joinToString(". ")}"
   "τηλεόρασ" in s || "εκτυπωτ" in s -> "Η σύνδεση με τη συσκευή δεν έχει ρυθμιστεί ακόμη."
   "ώρα" in s -> "Η ώρα είναι ${SimpleDateFormat("HH:mm", Locale("el", "GR")).format(Date())}."
   "ημερομηνία" in s || "τι μέρα" in s -> "Σήμερα είναι ${SimpleDateFormat("EEEE d MMMM", Locale("el", "GR")).format(Date())}."
   "γεια" in s || "καλημέρα" in s || "καλησπέρα" in s -> "Γεια σου! Σε ακούω."
   "ποια είσαι" in s || "πώς σε λένε" in s -> "Είμαι η Τζόνα, η προσωπική σου βοηθός."
   "ευχαριστώ" in s -> "Παρακαλώ!"
   else -> "Άκουσα: $words. Το τοπικό AI δεν έχει εγκατασταθεί ακόμη."
  }
 }
 override fun onDestroy() { NotificationBridge.onUpdate = null; active = false; recognizer?.destroy(); tts?.stop(); tts?.shutdown(); super.onDestroy() }
}

@Composable
fun JohnaScreen(status: String, heard: String, history: List<Pair<String,String>>, continuous: Boolean,
 weather: String, weatherDetails: String, notifications: List<String>, agentInfo: String, wakeEnabled: Boolean,
 onWeather: () -> Unit, onNotifications: () -> Unit, onWake: (Boolean) -> Unit,
 onContinuous: (Boolean) -> Unit, onListen: () -> Unit, onStop: () -> Unit, onHello: () -> Unit) {
 val bg = Color(0xFF080717)
 val panel = Color(0xFF15122B)
 val purple = Color(0xFFB13CFF)
 val cyan = Color(0xFF18D8FF)
 val muted = Color(0xFFADA5CC)
 val narrow = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 650
 val clock = remember { SimpleDateFormat("HH:mm", Locale("el", "GR")).format(Date()) }
 val date = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale("el", "GR")).format(Date()) }
 val pulse by rememberInfiniteTransition(label="neon").animateFloat(.78f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label="pulse")
 Column(Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(12.dp)) {
  Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
   Column { Text(clock, color=Color.White, fontSize=25.sp, fontWeight=FontWeight.Bold); Text(date, color=cyan, fontSize=11.sp) }
   Column(horizontalAlignment=Alignment.CenterHorizontally) {
    Text("JOHNA AI", color=Color(0xFFF2D8FF), fontSize=if(narrow) 24.sp else 33.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp)
    Text("HOME COMMAND CENTER · v0.4", color=muted, fontSize=9.sp)
   }
   Text("◉  EL", color=cyan, fontSize=13.sp)
  }
  Spacer(Modifier.height(12.dp))
  if (narrow) {
   NeonOrb(status, heard, pulse, purple, cyan, onListen)
   Spacer(Modifier.height(10.dp))
   WeatherPanel(weather, weatherDetails, panel, cyan, onWeather)
   Spacer(Modifier.height(8.dp))
   AgentPanel(panel, purple, cyan)
   Spacer(Modifier.height(8.dp))
   NotificationsPanel(notifications, panel, cyan, onNotifications)
  } else {
   Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    Column(Modifier.weight(1f)) {
     WeatherPanel(weather, weatherDetails, panel, cyan, onWeather)
     Spacer(Modifier.height(10.dp))
     AgentPanel(panel, purple, cyan)
    }
    Column(Modifier.weight(1.35f), horizontalAlignment=Alignment.CenterHorizontally) {
     NeonOrb(status, heard, pulse, purple, cyan, onListen)
     Spacer(Modifier.height(10.dp))
     DevicePanel(panel, cyan)
    }
    Column(Modifier.weight(1f)) {
     NotificationsPanel(notifications, panel, cyan, onNotifications)
     Spacer(Modifier.height(10.dp))
     CalendarPanel(panel, purple)
    }
   }
  }
  if (narrow) { Spacer(Modifier.height(8.dp)); DevicePanel(panel, cyan); Spacer(Modifier.height(8.dp)); CalendarPanel(panel, purple) }
  Spacer(Modifier.height(12.dp))
  NeonCard(panel, purple) {
   Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) { Text("Συνεχής συνομιλία", color=Color.White, fontSize=14.sp); Text("Ενεργή μόνο με ανοιχτή εφαρμογή", color=muted, fontSize=10.sp) }
    Switch(checked=continuous, onCheckedChange=onContinuous)
   }
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    Button(onClick=onListen, modifier=Modifier.weight(1f), colors=ButtonDefaults.buttonColors(containerColor=purple)) { Text("◉ Μίλα στη Johna") }
    OutlinedButton(onClick=onStop) { Text("Στοπ", color=Color.White) }
   }
   Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) { Text("Hey Johna", color=Color.White); Text("Δεν έχει εγκατασταθεί wake-word engine", color=muted, fontSize=10.sp) }
    Switch(checked=wakeEnabled, onCheckedChange=onWake)
   }
   TextButton(onClick=onHello) { Text("Δοκιμή γυναικείας φωνής", color=cyan) }
  }
  Spacer(Modifier.height(12.dp))
  NeonCard(panel, cyan) {
   Text("ΙΣΤΟΡΙΚΟ ΣΥΝΟΜΙΛΙΑΣ", color=cyan, fontSize=12.sp, fontWeight=FontWeight.Bold)
   if(history.isEmpty()) Text("Δεν υπάρχουν ακόμη συνομιλίες", color=muted, fontSize=11.sp)
   history.takeLast(5).asReversed().forEach { (q,r) ->
    Text("Εσύ: $q", color=Color.White, fontSize=12.sp)
    Text("Johna: $r", color=Color(0xFFE2BBFF), fontSize=11.sp)
    HorizontalDivider(color=Color(0xFF302747))
   }
  }
  Spacer(Modifier.height(8.dp))
  Text("AI: κανόνες · Τοπικό LLM, wake word και συνδέσεις συσκευών εκκρεμούν", color=muted, fontSize=10.sp)
 }
}

@Composable
private fun NeonCard(bg: Color, accent: Color, content: @Composable ColumnScope.() -> Unit) {
 Column(Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha=.65f), RoundedCornerShape(16.dp))
  .background(bg, RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement=Arrangement.spacedBy(7.dp), content=content)
}

@Composable
private fun NeonOrb(status: String, heard: String, pulse: Float, purple: Color, cyan: Color, onListen: () -> Unit) {
 Column(Modifier.fillMaxWidth(), horizontalAlignment=Alignment.CenterHorizontally) {
  Box(Modifier.size(242.dp).clickable(onClick=onListen), contentAlignment=Alignment.Center) {
   Canvas(Modifier.fillMaxSize()) {
    val r=size.minDimension/2f
    drawCircle(brush=Brush.radialGradient(listOf(purple.copy(alpha=.17f), Color.Transparent)), radius=r)
    drawCircle(purple.copy(alpha=.25f), radius=r*.94f, style=Stroke(2.dp.toPx()))
    drawCircle(cyan.copy(alpha=.45f), radius=r*.84f*pulse, style=Stroke(3.dp.toPx()))
    drawCircle(purple.copy(alpha=.85f), radius=r*.71f*pulse, style=Stroke(7.dp.toPx()))
    drawCircle(brush=Brush.radialGradient(listOf(Color(0xFF3B124D),Color(0xFF0A0920))),radius=r*.64f)
    val middle=center.y
    for (i in 0 until 27) {
     val x=center.x+(i-13)*5.5.dp.toPx()
     val h=(7+(i*17%24))*pulse*1f
     drawLine(if(i%3==0) cyan else purple, androidx.compose.ui.geometry.Offset(x,middle-h), androidx.compose.ui.geometry.Offset(x,middle+h), strokeWidth=2.3.dp.toPx())
    }
   }
  }
  Text(status, color=Color(0xFFF0D1FF), fontSize=20.sp, fontWeight=FontWeight.SemiBold)
  Text(heard.take(100), color=Color(0xFFBDB4D3), fontSize=12.sp, textAlign=TextAlign.Center)
 }
}

@Composable
private fun WeatherPanel(weather: String, details: String, panel: Color, accent: Color, refresh: () -> Unit) {
 NeonCard(panel,accent) {
  Text("☁  ΚΑΙΡΟΣ · ΑΘΗΝΑ", color=accent, fontSize=13.sp, fontWeight=FontWeight.Bold)
  Text(weather, color=Color.White, fontSize=32.sp, fontWeight=FontWeight.Bold)
  Text(details, color=Color(0xFFBDB4D3), fontSize=11.sp)
  TextButton(onClick=refresh) { Text("↻ Ανανέωση", color=accent) }
 }
}

@Composable
private fun AgentPanel(panel: Color, purple: Color, cyan: Color) {
 NeonCard(panel,purple) {
  Text("AI AGENTS", color=purple, fontWeight=FontWeight.Bold)
  listOf("⌂  Home Agent" to "Σύνδεση εκκρεμεί", "✉  Mail Agent" to "Ειδοποιήσεις Android", "▣  Work Agent" to "Προγραμματισμένο", "✦  Dental Agent" to "Προγραμματισμένο", "◎  Web Agent" to "Καιρός ενεργός").forEach { (title,desc) ->
   Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
    Column { Text(title,color=Color.White,fontSize=12.sp); Text(desc,color=Color(0xFFADA5CC),fontSize=10.sp) }
    Text(if(desc.contains("ενεργός")) "●" else "○", color=if(desc.contains("ενεργός")) cyan else Color.Gray)
   }
   HorizontalDivider(color=Color(0xFF302747))
  }
 }
}

@Composable
private fun NotificationsPanel(items: List<String>, panel: Color, accent: Color, open: () -> Unit) {
 NeonCard(panel,accent) {
  Text("✉  ΕΙΔΟΠΟΙΗΣΕΙΣ (${items.size})", color=accent, fontWeight=FontWeight.Bold, fontSize=13.sp)
  if(items.isEmpty()) Text("Δεν υπάρχουν ειδοποιήσεις ή δεν έχει δοθεί άδεια", color=Color(0xFFADA5CC), fontSize=11.sp)
  items.take(4).forEach { Text(it, color=Color.White, fontSize=11.sp, maxLines=2); HorizontalDivider(color=Color(0xFF302747)) }
  TextButton(onClick=open) { Text("Διαχείριση πρόσβασης", color=accent) }
 }
}

@Composable
private fun DevicePanel(panel: Color, accent: Color) {
 NeonCard(panel,accent) {
  Text("ΣΥΣΚΕΥΕΣ", color=accent, fontWeight=FontWeight.Bold, fontSize=12.sp)
  Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
   listOf("▣" to "LG TV", "▤" to "Bambu A1", "⌂" to "Smart Home", "◎" to "Web").forEach { (icon,title) ->
    Column(Modifier.weight(1f).background(Color(0xFF21193D),RoundedCornerShape(10.dp)).padding(6.dp), horizontalAlignment=Alignment.CenterHorizontally) {
     Text(icon,color=accent,fontSize=23.sp)
     Text(title,color=Color.White,fontSize=10.sp, textAlign=TextAlign.Center)
     Text("Εκκρεμεί",color=Color(0xFFADA5CC),fontSize=8.sp)
    }
   }
  }
 }
}

@Composable
private fun CalendarPanel(panel: Color, accent: Color) {
 val cal=remember { java.util.Calendar.getInstance() }
 val month=remember { SimpleDateFormat("LLLL yyyy", Locale("el","GR")).format(cal.time) }
 val today=cal.get(java.util.Calendar.DAY_OF_MONTH)
 val maxDay=cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
 NeonCard(panel,accent) {
  Text("▦  $month",color=accent,fontWeight=FontWeight.Bold)
  val days=(1..maxDay).toList().chunked(7)
  days.forEach { week ->
   Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly) {
    week.forEach { d -> Text("$d", color=if(d==today) accent else Color.White, fontWeight=if(d==today) FontWeight.Bold else FontWeight.Normal, fontSize=11.sp, modifier=Modifier.weight(1f), textAlign=TextAlign.Center) }
    repeat(7-week.size) { Spacer(Modifier.weight(1f)) }
   }
  }
 }
}

/** Reads only Android notifications after the user explicitly enables Notification Access. */
object NotificationBridge {
 @Volatile var onUpdate: ((List<String>) -> Unit)? = null
}

class JohnaNotificationListener : NotificationListenerService() {
 private val recent = LinkedHashMap<String, String>()
 override fun onListenerConnected() { super.onListenerConnected(); activeNotifications?.forEach { add(it) }; publish() }
 override fun onNotificationPosted(sbn: StatusBarNotification?) { if (sbn != null) { add(sbn); publish() } }
 override fun onNotificationRemoved(sbn: StatusBarNotification?) { if (sbn != null) { recent.remove(sbn.key); publish() } }
 private fun add(sbn: StatusBarNotification) {
  if (sbn.packageName == packageName) return
  val extras = sbn.notification.extras
  val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
  val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
  if (title.isBlank() && message.isBlank()) return
  val label = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString() } catch (_: Exception) { sbn.packageName }
  recent[sbn.key] = "$label · $title: $message".take(180)
  while (recent.size > 30) recent.remove(recent.keys.first())
 }
 private fun publish() { NotificationBridge.onUpdate?.invoke(recent.values.toList().asReversed()) }
}
