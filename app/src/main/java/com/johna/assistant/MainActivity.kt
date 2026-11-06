package com.johna.assistant

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.Manifest
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import java.net.URL
import java.net.HttpURLConnection
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore
import org.json.JSONArray
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import androidx.compose.animation.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
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
import kotlinx.coroutines.delay
import java.util.TimeZone

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
 private var apiKey by mutableStateOf("")
 private var keyConfigured by mutableStateOf(false)
 private var requestGeneration = 0
 private var deviceScan by mutableStateOf("Πάτησε Αναζήτηση για συσκευές Wi-Fi")
 private val foundDevices = mutableStateListOf<Pair<String,String>>()
 private val discoveries = mutableListOf<NsdManager.DiscoveryListener>()
 private fun discoverDevices() {
  val nsd = getSystemService(NSD_SERVICE) as NsdManager
  discoveries.forEach { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }
  discoveries.clear(); foundDevices.clear(); deviceScan = "Αναζήτηση στο τοπικό δίκτυο..."
  listOf("_googlecast._tcp.", "_http._tcp.", "_hap._tcp.", "_matter._tcp.", "_ipp._tcp.", "_airplay._tcp.").forEach { type ->
   val listener = object : NsdManager.DiscoveryListener {
    override fun onDiscoveryStarted(t: String) {}
    override fun onDiscoveryStopped(t: String) {}
    override fun onStartDiscoveryFailed(t: String, code: Int) { main.post { deviceScan = "Μερικές υπηρεσίες δεν είναι διαθέσιμες ($code)" } }
    override fun onStopDiscoveryFailed(t: String, code: Int) {}
    override fun onServiceLost(info: NsdServiceInfo) { main.post { foundDevices.removeAll { it.first == info.serviceName && it.second == type } } }
    override fun onServiceFound(info: NsdServiceInfo) { main.post {
     val entry = info.serviceName to type
     if (!foundDevices.contains(entry)) foundDevices.add(entry)
     deviceScan = "Βρέθηκαν ${foundDevices.size} υπηρεσίες · πάτησε για λεπτομέρειες"
    } }
   }
   try { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener); discoveries.add(listener) }
   catch (_: Exception) { }
  }
  main.postDelayed({ discoveries.toList().forEach { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }; discoveries.clear()
   if (foundDevices.isEmpty()) deviceScan = "Δεν εντοπίστηκαν συμβατές υπηρεσίες mDNS"
  }, 12000)
 }

 private val conversationFile by lazy { java.io.File(filesDir, "johna_conversation.json") }
 private fun cipherKey(): javax.crypto.SecretKey {
  val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  val existing = store.getKey("johna_gemini_key", null) as? javax.crypto.SecretKey
  if (existing != null) return existing
  val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
  generator.init(KeyGenParameterSpec.Builder("johna_gemini_key",
   KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
   .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
  return generator.generateKey()
 }
 private fun saveKey(value: String) {
  try {
   val cipher = Cipher.getInstance("AES/GCM/NoPadding")
   cipher.init(Cipher.ENCRYPT_MODE, cipherKey())
   val payload = Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
   getSharedPreferences("johna_secure", MODE_PRIVATE).edit().putString("key", payload).apply()
   apiKey = value; keyConfigured = true; status = "JOHNA Cloud AI"
  } catch (_: Exception) { status = "Αποτυχία αποθήκευσης κλειδιού" }
 }
 private fun loadKey(): String {
  return try {
   val payload = getSharedPreferences("johna_secure", MODE_PRIVATE).getString("key", null) ?: return ""
   val bytes = Base64.decode(payload, Base64.NO_WRAP)
   val cipher = Cipher.getInstance("AES/GCM/NoPadding")
   cipher.init(Cipher.DECRYPT_MODE, cipherKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
   String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
  } catch (_: Exception) { "" }
 }
 private fun loadHistory() {
  try {
   val arr = JSONArray(conversationFile.readText())
   for (i in 0 until arr.length()) {
    val item = arr.getJSONObject(i)
    history.add(item.getString("user") to item.getString("assistant"))
   }
  } catch (_: Exception) {}
 }
 private fun persistHistory() {
  try {
   val arr = JSONArray()
   history.takeLast(20).forEach { (q,r) -> arr.put(JSONObject().put("user",q).put("assistant",r)) }
   conversationFile.writeText(arr.toString())
  } catch (_: Exception) {}
 }
 private fun clearHistory() { history.clear(); conversationFile.delete(); status = "Η μνήμη διαγράφηκε" }
 private fun localDeviceAnswer(words: String): String? {
  val q = words.lowercase(Locale("el", "GR"))
  val now = Date()
  val greek = Locale("el", "GR")
  return when {
   "τι ώρα" in q || "την ώρα" in q || "ώρα είναι" in q || "πες μου ώρα" in q -> {
    val fmt = SimpleDateFormat("HH:mm", greek)
    fmt.timeZone = TimeZone.getDefault()
    "Η ώρα είναι ${fmt.format(now)}."
   }
   "τι ημερομηνία" in q || "τι μέρα" in q || "ποια μέρα" in q || "σημερινή ημερομηνία" in q -> {
    val fmt = SimpleDateFormat("EEEE d MMMM yyyy", greek)
    fmt.timeZone = TimeZone.getDefault()
    "Σήμερα είναι ${fmt.format(now)}."
   }
   else -> null
  }
 }
 private fun askGemini(words: String) {
  val local = localDeviceAnswer(words)
  if (local != null) { speak(local); return }
  val generation = ++requestGeneration
  val context = history.takeLast(6).toList()
  val key = apiKey.trim()
  if (key.isBlank() || key.startsWith("{")) { speak("Πρόσθεσε το Groq API key στις Ρυθμίσεις AI."); return }
  status = "JOHNA Cloud AI · σύνδεση..."
  Thread {
   var reply = ""
   var success = false
   var providerUsed = ""
   var diagnostic = ""
   val messages = JSONArray().put(JSONObject().put("role", "system").put("content",
    "Είσαι η JOHNA, φιλική Ελληνίδα ψηφιακή βοηθός. Απάντα φυσικά, σύντομα, στα ελληνικά. Μην επινοείς ζωντανά δεδομένα ή πρόσβαση σε συσκευές."))
   context.forEach { (q,a) ->
    messages.put(JSONObject().put("role","user").put("content",q))
    messages.put(JSONObject().put("role","assistant").put("content",a))
   }
   messages.put(JSONObject().put("role","user").put("content",words))
   var conn: HttpURLConnection? = null
   try {
    val body = JSONObject().put("model", "llama-3.3-70b-versatile").put("messages", messages)
     .put("stream", false).put("max_tokens", 300).put("temperature", 0.5)
    conn = URL("https://api.groq.com/openai/v1/chat/completions").openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = 8000
    conn.readTimeout = 25000
    conn.setRequestProperty("Authorization", "Bearer $key")
    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
    conn.doOutput = true
    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
    val httpCode = conn.responseCode
    if (httpCode in 200..299) {
     val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
     reply = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty().trim()
     if (reply.isNotBlank()) { success = true; providerUsed = "Groq" }
     else diagnostic = "HTTP $httpCode: Κενή απάντηση μοντέλου"
    } else {
     diagnostic = when (httpCode) {
      400 -> "HTTP 400: Μη αποδεκτό αίτημα ή μοντέλο"
      401 -> "HTTP 401: Μη έγκυρο API key"
      403 -> "HTTP 403: Δεν επιτρέπεται η πρόσβαση"
      404 -> "HTTP 404: Δεν βρέθηκε το μοντέλο ή η υπηρεσία"
      408 -> "HTTP 408: Έληξε ο χρόνος αναμονής"
      413 -> "HTTP 413: Πολύ μεγάλο αίτημα"
      429 -> "HTTP 429: Όριο χρήσης Groq"
      in 500..599 -> "HTTP $httpCode: Προσωρινό πρόβλημα Groq"
      else -> "HTTP $httpCode: Αποτυχία αιτήματος"
     }
    }
   } catch (e: java.net.SocketTimeoutException) {
    diagnostic = "Timeout: Η σύνδεση άργησε υπερβολικά"
   } catch (e: java.net.UnknownHostException) {
    diagnostic = "DNS: Δεν βρέθηκε ο διακομιστής Groq"
   } catch (e: javax.net.ssl.SSLException) {
    diagnostic = "SSL: Αποτυχία ασφαλούς σύνδεσης"
   } catch (e: Exception) {
    diagnostic = "Σφάλμα σύνδεσης: ${e.javaClass.simpleName}"
   } finally { conn?.disconnect() }
   if (!success) reply = "Groq: ${diagnostic.ifBlank { "Άγνωστο σφάλμα" }}"
   main.post {
    if (generation != requestGeneration || isDestroyed) return@post
    if (success) {
     status = "JOHNA AI · $providerUsed"
     history.add(words to reply)
     while (history.size > 20) history.removeAt(0)
     persistHistory()
    }
    speak(reply)
   }
  }.start()
 }
 private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if (granted) beginListening() else { active = false; status = "Χρειάζεται άδεια μικροφώνου" }
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  apiKey = loadKey()
  keyConfigured = apiKey.isNotBlank()
  loadHistory()
  tts = TextToSpeech(this, this)
  tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
   override fun onStart(utteranceId: String?) { main.post { speaking = true; status = "Μιλάω" } }
   override fun onDone(utteranceId: String?) { main.post { speaking = false; if (active && continuous) main.postDelayed({ beginListening() }, 450) else status = "Έτοιμη" } }
   override fun onError(utteranceId: String?) { main.post { speaking = false; status = "Σφάλμα φωνής" } }
  })
  NotificationBridge.onUpdate = { items -> main.post { notifications = items } }
  refreshWeather()
  setContent {
   GlassDashboard(status, heard, continuous, weather, weatherDetails, notifications, foundDevices.toList(), deviceScan, keyConfigured,
    onWeather = { refreshWeather() },
    onDiscover = { discoverDevices() },
    onSaveKey = { saveKey(it) },
    onClearHistory = { clearHistory() },
    onNotifications = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
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
  requestGeneration++; active = false; continuous = false; listening = false
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
     askGemini(words)
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
 override fun onDestroy() { val nsd = getSystemService(NSD_SERVICE) as NsdManager; discoveries.forEach { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }; NotificationBridge.onUpdate = null; active = false; recognizer?.destroy(); tts?.stop(); tts?.shutdown(); super.onDestroy() }
}

@Composable
fun JohnaScreen(status: String, heard: String, continuous: Boolean,
 weather: String, weatherDetails: String, notifications: List<String>, agentInfo: String, wakeEnabled: Boolean, keyConfigured: Boolean, devices: List<Pair<String,String>>, deviceScan: String,
 onWeather: () -> Unit, onDiscover: () -> Unit, onSaveKey: (String) -> Unit, onClearHistory: () -> Unit, onNotifications: () -> Unit, onWake: (Boolean) -> Unit,
 onContinuous: (Boolean) -> Unit, onListen: () -> Unit, onStop: () -> Unit, onHello: () -> Unit) {
 var showAiSettings by remember { mutableStateOf(false) }
 val context = androidx.compose.ui.platform.LocalContext.current
 val prefs = remember { context.getSharedPreferences("johna_theme", android.content.Context.MODE_PRIVATE) }
 var themeIndex by remember { mutableIntStateOf(prefs.getInt("theme",0)) }
 var showThemes by remember { mutableStateOf(false) }
 val themes = listOf("Cyber Purple", "Midnight Blue", "Arctic White", "Amber Tech")
 var groqDraft by remember { mutableStateOf("") }
 val bg = listOf(Color(0xFF080717),Color(0xFF061323),Color(0xFFE6EDF4),Color(0xFF110E09))[themeIndex]
 val panel = listOf(Color(0xFF15122B),Color(0xFF10253A),Color(0xFF263A53),Color(0xFF241C13))[themeIndex]
 val purple = listOf(Color(0xFFB13CFF),Color(0xFF4A8BFF),Color(0xFF4C71D5),Color(0xFFFFA32B))[themeIndex]
 val cyan = listOf(Color(0xFF18D8FF),Color(0xFF37D6F5),Color(0xFF2C91B8),Color(0xFFFFCB65))[themeIndex]
 val muted = Color(0xFFADA5CC)
 val narrow = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 650
 var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
 LaunchedEffect(Unit) {
  while (true) { currentTime = System.currentTimeMillis(); delay(1000L) }
 }
 val clock = remember(currentTime) { SimpleDateFormat("HH:mm", Locale("el", "GR")).format(Date(currentTime)) }
 val date = remember(currentTime) { SimpleDateFormat("EEE, d MMM yyyy", Locale("el", "GR")).format(Date(currentTime)) }
 val pulse by rememberInfiniteTransition(label="neon").animateFloat(.78f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label="pulse")
 Column(Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(12.dp)) {
  Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
   Column { Text(clock, color=Color.White, fontSize=25.sp, fontWeight=FontWeight.Bold); Text(date, color=cyan, fontSize=11.sp) }
   Column(horizontalAlignment=Alignment.CenterHorizontally) {
    Text("JOHNA AI", color=Color(0xFFF2D8FF), fontSize=if(narrow) 24.sp else 33.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp)
    Text("HOME COMMAND CENTER · v0.7.0", color=muted, fontSize=9.sp)
   }
   TextButton(onClick={showThemes=true}) { Text("◈ THEMES", color=cyan, fontSize=12.sp) }
  }
  Spacer(Modifier.height(12.dp))
  AnimatedVisibility(visible=true, enter=fadeIn(tween(600))+slideInVertically(initialOffsetY={it/5})) {
   Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    MiniTile("AI", "●", purple, Modifier.weight(1f))
    MiniTile("Wi-Fi", "◉", cyan, Modifier.weight(1f))
    MiniTile("HOME", "⌂", purple, Modifier.weight(1f))
    MiniTile("VOICE", "♫", cyan, Modifier.weight(1f))
   }
  }
  Spacer(Modifier.height(10.dp))
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
     DevicePanel(panel, cyan, devices, deviceScan, onDiscover)
    }
    Column(Modifier.weight(1f)) {
     NotificationsPanel(notifications, panel, cyan, onNotifications)
     Spacer(Modifier.height(10.dp))
     CalendarPanel(panel, purple)
    }
   }
  }
  if (narrow) { Spacer(Modifier.height(8.dp)); DevicePanel(panel, cyan, devices, deviceScan, onDiscover); Spacer(Modifier.height(8.dp)); CalendarPanel(panel, purple) }
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
   Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.SpaceBetween) {
    Text(if (keyConfigured) "● CLOUD AI · ΡΥΘΜΙΣΜΕΝΟ" else "○ CLOUD AI · ΡΥΘΜΙΣΗ", color=cyan, fontSize=12.sp, fontWeight=FontWeight.Bold)
    TextButton(onClick={ showAiSettings = !showAiSettings }) { Text("Ρυθμίσεις AI", color=cyan) }
   }
   if (showAiSettings) {
    Text("Το API key αποθηκεύεται κρυπτογραφημένο στη συσκευή.", color=muted, fontSize=11.sp)
    Text("Μόνο Groq Cloud AI · δωρεάν χρήση με όρια", color=muted, fontSize=11.sp)
    OutlinedTextField(value=groqDraft, onValueChange={groqDraft=it}, label={Text("Groq API key")}, visualTransformation=PasswordVisualTransformation(), singleLine=true, modifier=Modifier.fillMaxWidth())
    Button(onClick={ onSaveKey(groqDraft.trim()); groqDraft=""; showAiSettings=false }, enabled=groqDraft.isNotBlank()) { Text("Αποθήκευση Groq key") }
    OutlinedButton(onClick=onClearHistory) { Text("Διαγραφή αποθηκευμένων συνομιλιών") }
   }
  }
  Spacer(Modifier.height(8.dp))
  Text("Groq Cloud AI · Ελληνική φωνή · Smart Home", color=muted, fontSize=10.sp)
 }
 if(showThemes) AlertDialog(onDismissRequest={showThemes=false}, title={Text("JOHNA THEMES")},
  text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
   themes.forEachIndexed { index, title ->
    Row(Modifier.fillMaxWidth().clickable { themeIndex=index; prefs.edit().putInt("theme",index).apply(); showThemes=false }.padding(10.dp),
      verticalAlignment=Alignment.CenterVertically) {
     Box(Modifier.size(22.dp).background(listOf(Color(0xFFB13CFF),Color(0xFF4A8BFF),Color(0xFFE6EDF4),Color(0xFFFFA32B))[index], RoundedCornerShape(6.dp)))
     Spacer(Modifier.width(12.dp)); Text(title)
     if(index==themeIndex) Text("  ✓")
    }
   }
  }}, confirmButton={TextButton(onClick={showThemes=false}){Text("Κλείσιμο")}})
}

@Composable
private fun MiniTile(label:String, glyph:String, accent:Color, modifier:Modifier=Modifier) {
 val t=rememberInfiniteTransition(label="tile-$label")
 val alpha by t.animateFloat(.55f,1f,infiniteRepeatable(tween(1600),RepeatMode.Reverse),label="glow")
 Column(modifier.border(1.dp,accent.copy(alpha=alpha),RoundedCornerShape(12.dp)).background(Color(0xFF141A2B),RoundedCornerShape(12.dp)).padding(10.dp), horizontalAlignment=Alignment.CenterHorizontally) {
  val iconRes = when(label) { "AI"->R.drawable.johna_ai; "Wi-Fi"->R.drawable.johna_wifi; "HOME"->R.drawable.johna_home; else->R.drawable.johna_voice }
  Image(painterResource(iconRes),contentDescription=label,modifier=Modifier.size(32.dp).graphicsLayer { rotationZ=(1f-alpha)*12f })
  Text(label,color=Color.White,fontSize=9.sp)
 }
}

@Composable
private fun NeonCard(bg: Color, accent: Color, content: @Composable ColumnScope.() -> Unit) {
 val transition=rememberInfiniteTransition(label="cardGlow")
 val glow by transition.animateFloat(.3f,.75f,infiniteRepeatable(tween(2300),RepeatMode.Reverse),label="glow")
 Column(Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha=glow), RoundedCornerShape(16.dp))
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
private fun DevicePanel(panel: Color, accent: Color, devices: List<Pair<String,String>>, scanStatus: String, onDiscover: () -> Unit) {
 var selected by remember { mutableStateOf<Pair<String,String>?>(null) }
 NeonCard(panel,accent) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
   Text("⌂  ΕΞΥΠΝΕΣ ΣΥΣΚΕΥΕΣ", color=accent, fontWeight=FontWeight.Bold, fontSize=12.sp)
   TextButton(onClick=onDiscover) { Text("↻ Αναζήτηση", color=accent) }
  }
  Text(scanStatus, color=Color(0xFFADA5CC), fontSize=10.sp)
  devices.take(18).forEach { (name, type) ->
   val symbol = when { "cast" in type -> "▣"; "matter" in type || "hap" in type -> "⌂"; "ipp" in type -> "▤"; else -> "◈" }
   Row(Modifier.fillMaxWidth().padding(vertical=3.dp).background(Color(0xFF21193D),RoundedCornerShape(10.dp)).clickable { selected=name to type }.padding(10.dp), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    Text(symbol, color=accent, fontSize=22.sp)
    Column { Text(name,color=Color.White,fontSize=12.sp); Text(type,color=Color(0xFFADA5CC),fontSize=10.sp) }
   }
  }
 }
 if (selected != null) AlertDialog(onDismissRequest={ selected=null },
  title={ Text(selected!!.first) },
  text={ Text("Εντοπίστηκε υπηρεσία ${selected!!.second} στο Wi-Fi. Η σύνδεση και ο έλεγχος απαιτούν υποστήριξη του πρωτοκόλλου και πιθανώς pairing. Δεν έχει πραγματοποιηθεί σύνδεση.") },
  confirmButton={ TextButton(onClick={ selected=null }) { Text("Κλείσιμο") } })
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
