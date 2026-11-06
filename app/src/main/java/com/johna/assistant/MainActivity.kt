package com.johna.assistant

import android.Manifest
import android.app.Notification
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

 // ─── State ───────────────────────────────────────────────────────────────

 private val main = Handler(Looper.getMainLooper())
 private var recognizer: SpeechRecognizer? = null
 private var tts: TextToSpeech? = null

 // These are mutableStateOf so Compose recomposes when they change
 private var speaking     by mutableStateOf(false)
 private var listening    by mutableStateOf(false)
 private var active       = false
 private var errors       = 0
 private var ttsReady     = false

 private var status          by mutableStateOf("Έτοιμη")
 private var aiResponse      by mutableStateOf("Η Τζόνα σε ακούει. Πάτα Μίλα ή χρησιμοποίησε φωνητική εντολή.")
 private var heard           by mutableStateOf("Πάτησε Μίλα ή ενεργοποίησε τη συνομιλία")
 private var voicePanel      by mutableStateOf("")
 private var weather         by mutableStateOf("Φόρτωση...")
 private var weatherDetails  by mutableStateOf("Αθήνα · Open-Meteo")
 private var continuous      by mutableStateOf(false)
 private var apiKey          by mutableStateOf("")
 private var keyConfigured   by mutableStateOf(false)
 private var googleSignedIn  by mutableStateOf(false)
 private var googleToken     = ""

 private val history       = mutableStateListOf<Pair<String, String>>()
 private val notifications = mutableStateListOf<String>()
 private val foundDevices  = mutableStateListOf<Pair<String, String>>()
 private val gmailMessages = mutableStateListOf<GmailMessage>()
 private val calendarEvents = mutableStateListOf<CalendarEvent>()

 private var deviceScan by mutableStateOf("Πάτησε Αναζήτηση για συσκευές Wi-Fi")
 private val discoveries = mutableListOf<NsdManager.DiscoveryListener>()

 private var requestGeneration = 0
 private var aiPending = false
 private var lastWeatherUpdate = 0L

 private val conversationFile by lazy { java.io.File(filesDir, "johna_conversation.json") }

 // ─── Google Sign-In launchers ────────────────────────────────────────────

 private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  if (result.resultCode == RESULT_OK) {
   GoogleSignIn.getSignedInAccountFromIntent(result.data)
    .addOnSuccessListener { account ->
     googleSignedIn = true
     status = "Google: ${account.email}"
     fetchGoogleData()
    }
    .addOnFailureListener { e -> status = "Sign-in απέτυχε: ${e.message?.take(80)}" }
  }
 }

 private val googleAuthLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
  if (it.resultCode == RESULT_OK) fetchGoogleData()
 }

 // ─── Permission ──────────────────────────────────────────────────────────

 private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if (granted) beginListening() else { active = false; status = "Χρειάζεται άδεια μικροφώνου" }
 }

 // ─── Lifecycle ───────────────────────────────────────────────────────────

 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  apiKey = loadKey()
  keyConfigured = apiKey.isNotBlank()
  loadHistory()

  tts = TextToSpeech(this, this)
  tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
   override fun onStart(id: String?)  { main.post { speaking = true } }
   override fun onDone(id: String?)   { main.post { speaking = false; onTtsDone() } }
   override fun onError(id: String?)  { main.post { speaking = false } }
  })

  NotificationBridge.onUpdate = { items -> main.post { notifications.clear(); notifications.addAll(items) } }
  refreshWeather()

  // Check for existing Google sign-in
  val existingAccount = GoogleSignIn.getLastSignedInAccount(this)
  if (existingAccount != null) { googleSignedIn = true; fetchGoogleData() }

  val sha1 = getAppSha1()

  setContent {
   GlassDashboard(
    status         = status,
    heard          = heard,
    aiResponse     = aiResponse,
    voicePanel     = voicePanel,
    onVoicePanelConsumed = { voicePanel = "" },
    speaking       = speaking,
    listening      = listening,
    continuous     = continuous,
    weather        = weather,
    weatherDetails = weatherDetails,
    notifications  = notifications,
    devices        = foundDevices.toList(),
    deviceScan     = deviceScan,
    keyConfigured  = keyConfigured,
    googleSignedIn = googleSignedIn,
    gmailMessages  = gmailMessages.toList(),
    calendarEvents = calendarEvents.toList(),
    onWeather      = { refreshWeather() },
    onDiscover     = { discoverDevices() },
    onNotifications = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
    onListen       = { active = true; requestListen() },
    onStop         = { stopConversation() },
    onContinuous   = { enabled -> continuous = false; if (enabled) { active = true; requestListen() } else stopConversation() },
    onSaveKey      = { saveKey(it) },
    onClearHistory = { clearHistory() },
    onHello        = { active = false; speak("Γεια σου! Είμαι η Τζόνα. Είμαι έτοιμη.") },
    onTestGroq     = { active = false; askGroq("Απάντησε σύντομα στα ελληνικά: λειτουργείς;") },
    onOpenMail     = { openExternalApp("mail") },
    onOpenChrome   = { openExternalApp("chrome") },
    onOpenCamera   = { openExternalApp("camera") },
    onGoogleSignIn = { signInToGoogle() },
    appSha1        = sha1
   )
  }
 }

 override fun onInit(result: Int) {
  if (result == TextToSpeech.SUCCESS) {
   val r = tts?.setLanguage(Locale("el", "GR"))
   ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
   if (!ttsReady) status = "Δεν βρέθηκε ελληνική φωνή στη συσκευή"
  } else status = "Η υπηρεσία φωνής δεν ξεκίνησε"
 }

 override fun onDestroy() {
  val nsd = getSystemService(NSD_SERVICE) as NsdManager
  discoveries.forEach { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }
  NotificationBridge.onUpdate = null
  active = false; recognizer?.destroy(); tts?.stop(); tts?.shutdown()
  super.onDestroy()
 }

 // ─── Google Sign-In ───────────────────────────────────────────────────────

 private fun signInToGoogle() {
  val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
   .requestEmail()
   .build()
  signInLauncher.launch(GoogleSignIn.getClient(this, gso).signInIntent)
 }

 private fun fetchGoogleData() {
  val account = GoogleSignIn.getLastSignedInAccount(this)?.account ?: return
  status = "Google: Λήψη δεδομένων..."
  Thread {
   try {
    val scopes = "oauth2:${GoogleServices.GMAIL_SCOPE} ${GoogleServices.CALENDAR_SCOPE}"
    val token = GoogleAuthUtil.getToken(this, account, scopes)
    googleToken = token
    val msgs   = GoogleServices.fetchInbox(token)
    val events = GoogleServices.fetchUpcomingEvents(token)
    main.post {
     gmailMessages.clear();  gmailMessages.addAll(msgs)
     calendarEvents.clear(); calendarEvents.addAll(events)
     status = "Gmail: ${msgs.size} μηνύματα · ${events.size} ραντεβού"
    }
   } catch (e: UserRecoverableAuthException) {
    val recoverIntent = e.intent ?: return@Thread
    main.post { googleAuthLauncher.launch(recoverIntent) }
   } catch (e: Exception) {
    main.post { status = "Google: ${e.javaClass.simpleName}" }
   }
  }.start()
 }

 /** Returns the SHA-1 of the app's signing certificate, formatted as colon-separated hex. */
 private fun getAppSha1(): String {
  return try {
   val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
     .signingInfo?.apkContentsSigners
   } else {
    @Suppress("DEPRECATION")
    packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
   }
   val bytes = sigs?.firstOrNull()?.toByteArray() ?: return "N/A"
   MessageDigest.getInstance("SHA-1").digest(bytes).joinToString(":") { "%02X".format(it) }
  } catch (e: Exception) { "Σφάλμα: ${e.message}" }
 }

 // ─── Voice ───────────────────────────────────────────────────────────────

 private fun requestListen() {
  if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) beginListening()
  else micPermission.launch(Manifest.permission.RECORD_AUDIO)
 }

 private fun beginListening() {
  if (!active || speaking || listening || isFinishing || isDestroyed) return
  if (!SpeechRecognizer.isRecognitionAvailable(this)) {
   status = "Δεν υπάρχει υπηρεσία αναγνώρισης"; active = false; return
  }
  recognizer?.destroy(); recognizer = null
  listening = true
  recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
   sr.setRecognitionListener(object : RecognitionListener {
    override fun onReadyForSpeech(p: Bundle?) { status = "Σε ακούω..." }
    override fun onBeginningOfSpeech()         { status = "Ακούω..." }
    override fun onRmsChanged(rms: Float)       {}
    override fun onBufferReceived(buf: ByteArray?) {}
    override fun onEndOfSpeech()               { status = "Επεξεργάζομαι..." }
    override fun onEvent(type: Int, p: Bundle?) {}
    override fun onPartialResults(partial: Bundle?) {}

    override fun onError(error: Int) {
     listening = false; errors++
     status = "Σφάλμα αναγνώρισης ($error)"
     if (active && continuous && errors <= 3) main.postDelayed({ beginListening() }, 1200L * errors)
     else { active = false; if (errors > 3) status = "Η αναγνώριση σταμάτησε. Πάτησε Μίλα." }
    }

    override fun onResults(results: Bundle?) {
     listening = false; errors = 0
     val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
     if (words.isBlank()) { if (active && continuous) main.postDelayed({ beginListening() }, 600); return }
     heard = words
     active = false; continuous = false
     askGroq(words)
    }
   })
   val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "el-GR")
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "el-GR")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
   }
   status = "Ενεργοποίηση μικροφώνου..."
   try { sr.startListening(intent) } catch (e: Exception) { listening = false; active = false; status = "Αδυναμία εκκίνησης" }
  }
 }

 private fun stopConversation() {
  requestGeneration++; aiPending = false
  active = false; continuous = false; listening = false
  recognizer?.cancel(); recognizer?.destroy(); recognizer = null
  tts?.stop(); speaking = false; status = "Σταμάτησε"
 }

 private fun onTtsDone() {
  if (active && continuous && !aiPending) main.postDelayed({ beginListening() }, 450)
  else if (status == "Μιλάω") status = "Έτοιμη"
 }

 private fun speak(text: String) {
  if (!ttsReady) { speaking = false; onTtsDone(); return }
  speaking = true
  status = "Μιλάω"
  tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "johna_${System.currentTimeMillis()}")
 }

 // ─── Intent routing ───────────────────────────────────────────────────────

 private fun norm(s: String) = Normalizer.normalize(s.lowercase(Locale("el", "GR")), Normalizer.Form.NFD)
  .replace(Regex("\\p{M}+"), "")

 private fun handleVoiceCommand(words: String): Boolean {
  val q = norm(words)
  val open  = listOf("ανοιξε", "δειξε", "εμφανισε", "φερε", "ανοιγμα", "θελω να δω")
  val close = listOf("κλεισε", "κρυψε", "πισω", "αρχικη", "επιστροφη", "γυρνα")
  val closeTarget = listOf("παραθυρο", "πανελ", "καιρ", "ρυθμισ", "ειδοποι", "ημερολογ", "συσκευ", "ολα", "οθονη", "πισω", "αρχικη")
  val standaloneClose = q.trim() in listOf("κλεισε", "κλειστο", "πισω", "αρχικη", "αρχικη οθονη", "κλεισε τα ολα", "κλεισε ολα", "γυρνα πισω", "επιστροφη")
  if (standaloneClose || (close.any { it in q } && closeTarget.any { it in q })) {
   voicePanel = "close"; status = "Κλείσιμο παραθύρου"; aiResponse = "Έκλεισα το παράθυρο."; return true
  }

  // Gmail voice commands
  if ("email" in q || "ιμειλ" in q || "αλληλογραφ" in q || "inbox" in q || "μηνυματα" in q) {
   if (open.none { it in q } && "διαβασε" !in q && "πρωτο" !in q) return false
   voicePanel = "mail"
   if (googleSignedIn && gmailMessages.isNotEmpty()) {
    val unread = gmailMessages.count { it.isUnread }
    val answer = "Έχεις ${gmailMessages.size} μηνύματα στο inbox, $unread αδιάβαστα. " +
     gmailMessages.take(2).joinToString(". ") { "Από ${it.from}: ${it.subject}" }
    aiResponse = answer; speak(answer)
   } else if (googleSignedIn) {
    aiResponse = "Φορτώνω τα email..."; fetchGoogleData()
   } else {
    aiResponse = "Πάτα Σύνδεση με Google στο panel για πρόσβαση στο Gmail."
   }
   return true
  }

  // "Read first email"
  if (("διαβασε" in q || "αναγνωσε" in q) && ("πρωτο" in q || "πρωτη" in q || "1" in q)) {
   val msg = gmailMessages.firstOrNull()
   if (msg != null) {
    val answer = "Πρώτο μήνυμα από ${msg.from}. Θέμα: ${msg.subject}. ${msg.snippet.take(200)}"
    aiResponse = answer; speak(answer)
   } else { aiResponse = "Δεν υπάρχουν φορτωμένα email."; speak(aiResponse) }
   return true
  }

  // Calendar voice commands
  if ("ραντεβου" in q || "συμβαντα" in q || (("ημερολογ" in q) && open.any { it in q })) {
   voicePanel = "calendar"
   if (googleSignedIn && calendarEvents.isNotEmpty()) {
    val answer = "Τα επόμενα ραντεβού σου: " +
     calendarEvents.take(3).joinToString(". ") { "${it.startDisplay}: ${it.summary}" }
    aiResponse = answer; speak(answer)
   } else if (googleSignedIn) {
    aiResponse = "Φορτώνω ραντεβού..."; fetchGoogleData()
   } else {
    aiResponse = "Σύνδεσε το Google account σου για πρόσβαση στο Calendar."
   }
   return true
  }

  // Other panels
  if (open.none { it in q }) return false
  if ("chrome" in q || "χρωμ" in q || "browser" in q) { openExternalApp("chrome"); return true }
  if ("καμερ" in q || "φωτογραφικ" in q) { openExternalApp("camera"); return true }
  val panel = when {
   "καιρ" in q || "προγνωσ" in q || "θερμοκρασ" in q -> "weather"
   "ειδοποι" in q                                       -> "notifications"
   "ημερολογ" in q                                      -> "calendar"
   "ρυθμισ" in q                                        -> "settings"
   "συσκευ" in q || "smart home" in q                   -> "devices"
   else -> return false
  }
  voicePanel = panel
  if (panel == "weather") refreshWeather()
  if (panel == "devices") discoverDevices()
  aiResponse = "Άνοιξα το ${when(panel) { "weather"->"panel καιρού"; "notifications"->"panel ειδοποιήσεων"; "calendar"->"ημερολόγιο"; "settings"->"μενού ρυθμίσεων"; else->"panel συσκευών" }}."
  status = "Εντολή συσκευής · εκτελέστηκε"
  return true
 }

 private fun isWeatherQuestion(words: String): Boolean {
  val q = norm(words)
  return listOf("καιρ", "θερμοκρασ", "προγνωσ", "βρεχ", "υγρασι", "ανεμο").any { it in q }
 }

 private fun localDeviceAnswer(words: String): String? {
  val q = words.lowercase(Locale("el", "GR"))
  val now = Date(); val greek = Locale("el", "GR")
  return when {
   "τι ώρα" in q || "ώρα είναι" in q || "πες μου ώρα" in q ->
    "Η ώρα είναι ${SimpleDateFormat("HH:mm", greek).apply { timeZone = TimeZone.getDefault() }.format(now)}."
   "τι ημερομηνία" in q || "τι μέρα" in q || "ποια μέρα" in q ->
    "Σήμερα είναι ${SimpleDateFormat("EEEE d MMMM yyyy", greek).apply { timeZone = TimeZone.getDefault() }.format(now)}."
   "μπαταρια" in q || "battery" in q -> {
    val bm = getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
    val pct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    "Η μπαταρία είναι στο $pct τοις εκατό."
   }
   else -> null
  }
 }

 // ─── Groq AI with streaming ───────────────────────────────────────────────

 private fun answerWeather() {
  voicePanel = "weather"; status = "Καιρός · λήψη..."
  aiResponse = "Ενημερώνω τον καιρό..."
  refreshWeather { result -> aiResponse = result; status = "Καιρός · ενημερώθηκε"; speak(result) }
 }

 private fun askGroq(words: String) {
  if (handleVoiceCommand(words)) return
  if (isWeatherQuestion(words)) { answerWeather(); return }
  val local = localDeviceAnswer(words)
  if (local != null) { aiResponse = local; speak(local); return }
  if (aiPending) { status = "Groq · περιμένω απάντηση"; return }

  val key = apiKey.trim()
  if (key.isBlank() || key.startsWith("{")) {
   aiPending = false
   aiResponse = "Πρόσθεσε το Groq API key στις Ρυθμίσεις."
   status = "Λείπει το API key"; speak(aiResponse); return
  }

  val generation = ++requestGeneration
  aiPending = true
  aiResponse = ""; status = "Groq · αποστολή..."

  val context = history.takeLast(2).toList()
  Thread {
   val messages = JSONArray().put(JSONObject().put("role", "system").put("content",
    "Είσαι η JOHNA, φιλική Ελληνίδα ψηφιακή βοηθός. Απάντα φυσικά, σύντομα, στα ελληνικά. Μην επινοείς ζωντανά δεδομένα."))
   context.forEach { (q, a) ->
    messages.put(JSONObject().put("role", "user").put("content", q))
    messages.put(JSONObject().put("role", "assistant").put("content", a))
   }
   messages.put(JSONObject().put("role", "user").put("content", words))

   val models = listOf("openai/gpt-oss-20b", "qwen/qwen3-32b", "openai/gpt-oss-120b")
   var reply = ""; var success = false; var providerUsed = ""; var diagnostic = ""

   for (model in models) {
    var conn: HttpURLConnection? = null
    try {
     val body = JSONObject()
      .put("model", model)
      .put("messages", messages)
      .put("stream", true)
      .put("max_completion_tokens", 160)
      .put("temperature", 0.2)
     if (model.startsWith("openai/gpt-oss")) body.put("reasoning_effort", "low")
     if (model.startsWith("qwen/"))          body.put("reasoning_effort", "none")

     conn = URL("https://api.groq.com/openai/v1/chat/completions").openConnection() as HttpURLConnection
     conn.requestMethod = "POST"
     conn.connectTimeout = 3000; conn.readTimeout = 20000
     conn.setRequestProperty("Authorization", "Bearer $key")
     conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
     conn.doOutput = true
     conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

     val httpCode = conn.responseCode
     if (httpCode !in 200..299) {
      val err = try { JSONObject((conn.errorStream ?: conn.inputStream).bufferedReader().use { it.readText() }).optJSONObject("error")?.optString("message").orEmpty() } catch (_: Exception) { "" }
      diagnostic = "HTTP $httpCode ($model): ${err.take(180).ifBlank { "Αποτυχία" }}"
      if (httpCode == 401 || httpCode == 403) break
      continue
     }

     // Parse Server-Sent Events
     conn.inputStream.bufferedReader().use { reader ->
      var line = reader.readLine()
      while (line != null) {
       if (line.startsWith("data: ") && !line.contains("[DONE]")) {
        try {
         val delta = JSONObject(line.removePrefix("data: "))
          .optJSONArray("choices")?.optJSONObject(0)
          ?.optJSONObject("delta")?.optString("content") ?: ""
         if (delta.isNotEmpty()) {
          reply += delta
          val snapshot = reply
          main.post { if (generation == requestGeneration) aiResponse = snapshot }
         }
        } catch (_: Exception) { }
       }
       line = reader.readLine()
      }
     }

     if (reply.isNotBlank()) { success = true; providerUsed = model; break }
     else diagnostic = "Κενή SSE απάντηση ($model)"

    } catch (e: java.net.SocketTimeoutException) { diagnostic = "Timeout ($model)" }
    catch (e: java.net.UnknownHostException)      { diagnostic = "DNS: Δεν βρέθηκε Groq"; break }
    catch (e: javax.net.ssl.SSLException)          { diagnostic = "SSL αποτυχία"; break }
    catch (e: Exception)                           { diagnostic = "Σφάλμα ($model): ${e.javaClass.simpleName}" }
    finally { conn?.disconnect() }
   }

   if (!success) reply = "Groq: ${diagnostic.ifBlank { "Άγνωστο σφάλμα" }}"

   main.post {
    if (generation != requestGeneration || isDestroyed) return@post
    aiPending = false; aiResponse = reply
    status = if (success) "Johna · $providerUsed" else "Groq · αποτυχία"
    if (success) {
     history.add(words to reply)
     while (history.size > 20) history.removeAt(0)
     persistHistory()
    }
    speak(reply)
   }
  }.start()
 }

 // ─── Weather ─────────────────────────────────────────────────────────────

 private fun refreshWeather(onAnswer: ((String) -> Unit)? = null) {
  if (onAnswer == null && System.currentTimeMillis() - lastWeatherUpdate < 300_000) return
  weather = "Ενημέρωση..."
  Thread {
   try {
    val url = "https://api.open-meteo.com/v1/forecast?latitude=37.9838&longitude=23.7275&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=Europe%2FAthens"
    val conn = URL(url).openConnection(); conn.connectTimeout = 5000; conn.readTimeout = 6000
    val data = JSONObject(conn.getInputStream().bufferedReader().use { it.readText() }).getJSONObject("current")
    val t = data.getDouble("temperature_2m").toInt()
    val h = data.getInt("relative_humidity_2m")
    val w = data.getDouble("wind_speed_10m").toInt()
    val condition = when (data.getInt("weather_code")) {
     0        -> "αίθριος"; 1, 2 -> "λίγες νεφώσεις"; 3 -> "συννεφιά"
     45, 48   -> "ομίχλη"; in 51..57 -> "ψιχάλες"; in 61..82 -> "βροχή"
     in 71..86 -> "χιόνι"; in 95..99 -> "καταιγίδα"; else -> "μεταβλητός"
    }
    val answer = "Αυτή τη στιγμή στην Αθήνα έχει $t βαθμούς, $condition, υγρασία $h% και άνεμο $w km/h."
    main.post {
     weather = "$t°C"
     weatherDetails = "Αθήνα · $condition · Υγρασία $h% · Άνεμος $w km/h"
     lastWeatherUpdate = System.currentTimeMillis()
     onAnswer?.invoke(answer)
    }
   } catch (_: Exception) {
    main.post {
     weather = "Χωρίς δεδομένα"; weatherDetails = "Έλεγξε τη σύνδεση"
     onAnswer?.invoke("Δεν μπόρεσα να ανακτήσω ζωντανά δεδομένα καιρού.")
    }
   }
  }.start()
 }

 // ─── NSD device discovery ─────────────────────────────────────────────────

 private fun discoverDevices() {
  val nsd = getSystemService(NSD_SERVICE) as NsdManager
  discoveries.forEach { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }
  discoveries.clear(); foundDevices.clear(); deviceScan = "Αναζήτηση στο τοπικό δίκτυο..."
  val types = listOf("_googlecast._tcp.", "_http._tcp.", "_hap._tcp.", "_ipp._tcp.", "_airplay._tcp.")
  types.forEach { type ->
   val listener = object : NsdManager.DiscoveryListener {
    override fun onDiscoveryStarted(t: String) {}
    override fun onDiscoveryStopped(t: String) {}
    override fun onStartDiscoveryFailed(t: String, c: Int) { main.post { deviceScan = "Μερικές υπηρεσίες δεν είναι διαθέσιμες" } }
    override fun onStopDiscoveryFailed(t: String, c: Int) {}
    override fun onServiceLost(info: NsdServiceInfo) { main.post { foundDevices.removeAll { it.first == info.serviceName && it.second == type } } }
    override fun onServiceFound(info: NsdServiceInfo) {
     main.post {
      val e = info.serviceName to type
      if (!foundDevices.contains(e)) foundDevices.add(e)
      deviceScan = "Βρέθηκαν ${foundDevices.size} υπηρεσίες"
     }
    }
   }
   try { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener); discoveries.add(listener) } catch (_: Exception) {}
  }
  main.postDelayed({
   discoveries.toList().forEach { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }; discoveries.clear()
   if (foundDevices.isEmpty()) deviceScan = "Δεν εντοπίστηκαν συσκευές mDNS"
  }, 12000)
 }

 // ─── External app launch ──────────────────────────────────────────────────

 private fun openExternalApp(kind: String) {
  try {
   val intent = when (kind) {
    "mail"   -> packageManager.getLaunchIntentForPackage("com.google.android.gm")
                 ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com/mail/u/0/#inbox"))
    "chrome" -> packageManager.getLaunchIntentForPackage("com.android.chrome")
                 ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
    "camera" -> Intent("android.media.action.STILL_IMAGE_CAMERA")
    else -> return
   }
   startActivity(intent); status = "Άνοιγμα: $kind"
  } catch (e: Exception) { aiResponse = "Δεν βρέθηκε εφαρμογή: $kind"; status = "Αδυναμία ανοίγματος" }
 }

 // ─── Encrypted key storage ────────────────────────────────────────────────

 private fun cipherKey(): javax.crypto.SecretKey {
  val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  val existing = store.getKey("johna_gemini_key", null) as? javax.crypto.SecretKey
  if (existing != null) return existing
  val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
  gen.init(KeyGenParameterSpec.Builder("johna_gemini_key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
  return gen.generateKey()
 }

 private fun saveKey(value: String) {
  try {
   val cipher = Cipher.getInstance("AES/GCM/NoPadding")
   cipher.init(Cipher.ENCRYPT_MODE, cipherKey())
   val payload = Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
   getSharedPreferences("johna_secure", MODE_PRIVATE).edit().putString("key", payload).apply()
   apiKey = value; keyConfigured = true; status = "API key αποθηκεύτηκε"
  } catch (_: Exception) { status = "Αποτυχία αποθήκευσης key" }
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

 // ─── Conversation history ─────────────────────────────────────────────────

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
   history.takeLast(20).forEach { (q, r) -> arr.put(JSONObject().put("user", q).put("assistant", r)) }
   conversationFile.writeText(arr.toString())
  } catch (_: Exception) {}
 }

 private fun clearHistory() { history.clear(); conversationFile.delete(); status = "Η μνήμη διαγράφηκε" }
}

// ─── Notification bridge ─────────────────────────────────────────────────────

object NotificationBridge {
 @Volatile var onUpdate: ((List<String>) -> Unit)? = null
}

class JohnaNotificationListener : NotificationListenerService() {
 private val recent = LinkedHashMap<String, String>()
 override fun onListenerConnected() { super.onListenerConnected(); activeNotifications?.forEach { add(it) }; publish() }
 override fun onNotificationPosted(sbn: StatusBarNotification?)  { if (sbn != null) { add(sbn); publish() } }
 override fun onNotificationRemoved(sbn: StatusBarNotification?) { if (sbn != null) { recent.remove(sbn.key); publish() } }
 private fun add(sbn: StatusBarNotification) {
  if (sbn.packageName == packageName) return
  val extras = sbn.notification.extras
  val title   = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
  val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
  if (title.isBlank() && message.isBlank()) return
  val label = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString() } catch (_: Exception) { sbn.packageName }
  recent[sbn.key] = "$label · $title: $message".take(180)
  while (recent.size > 30) recent.remove(recent.keys.first())
 }
 private fun publish() { NotificationBridge.onUpdate?.invoke(recent.values.toList().asReversed()) }
}
