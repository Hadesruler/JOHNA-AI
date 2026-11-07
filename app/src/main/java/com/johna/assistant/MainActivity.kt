package com.johna.assistant

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
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
 private var soundPool: SoundPool? = null
 private val soundIds = mutableMapOf<String, Int>()
 private var soundEnabled by mutableStateOf(true)
 private var volumeLevel by mutableIntStateOf(5)
 private var micEnergy by mutableFloatStateOf(0f)

 // These are mutableStateOf so Compose recomposes when they change
 private var speaking     by mutableStateOf(false)
 private var listening    by mutableStateOf(false)
 private var active       = false
 private var wakeEnabled by mutableStateOf(false)
 private var awaitingWakeCommand = false
 private var wakeRetries = 0
 private var foreground = true
 private var errors       = 0
 private var ttsReady     = false

 private var status          by mutableStateOf("Έτοιμη")
 private var aiResponse      by mutableStateOf("Η Τζόνα σε ακούει. Πάτα Μίλα ή χρησιμοποίησε φωνητική εντολή.")
 private var heard           by mutableStateOf("Πάτησε Μίλα ή ενεργοποίησε τη συνομιλία")
 private var voicePanel      by mutableStateOf("")
 private var weather         by mutableStateOf("Φόρτωση...")
 private var weatherDetails  by mutableStateOf("Αθήνα · Open-Meteo")
 private var weatherForecast by mutableStateOf<WeatherForecast?>(null)
 private var weatherLoading by mutableStateOf(false)
 private var weatherError by mutableStateOf("")
 private var continuous      by mutableStateOf(false)
 private var apiKey          by mutableStateOf("")
 private var keyConfigured   by mutableStateOf(false)
 private var googleSignedIn  by mutableStateOf(false)

 private val history       = mutableStateListOf<Pair<String, String>>()
 private val notifications = mutableStateListOf<String>()
 private val foundDevices  = mutableStateListOf<Pair<String, String>>()
 private val launchableApps = mutableStateListOf<AppShortcut>()
 private val gmailMessages = mutableStateListOf<GmailMessage>()
 private val calendarEvents = mutableStateListOf<CalendarEvent>()

 private var deviceScan by mutableStateOf("Πάτησε Αναζήτηση για συσκευές Wi-Fi")
 private val discoveries = mutableListOf<NsdManager.DiscoveryListener>()

 private var requestGeneration = 0
 private var aiPending = false
 private var lastWeatherUpdate = 0L

 private val conversationFile by lazy { java.io.File(filesDir, "johna_conversation.json") }

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
  soundEnabled = getSharedPreferences("johna_glass", MODE_PRIVATE).getBoolean("soundEffects", true)
  initSounds()
  volumeLevel = readCurrentMediaVolumeLevel()

  tts = TextToSpeech(this, this)
  tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
   override fun onStart(id: String?)  { main.post { speaking = true } }
   override fun onDone(id: String?)   { main.post { speaking = false; onTtsDone() } }
   override fun onError(id: String?)  { main.post { speaking = false } }
  })
  refreshWeather()
  refreshApps()

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
    micEnergy      = micEnergy,
    volumeLevel    = volumeLevel,
    soundEffectsEnabled = soundEnabled,
    onSoundEffectsChanged = { enabled ->
     soundEnabled = enabled
     getSharedPreferences("johna_glass", MODE_PRIVATE).edit().putBoolean("soundEffects", enabled).apply()
    },
    onPanelTransition = { opened -> playSound(if (opened) "panel_open" else "panel_close") },
    continuous     = wakeEnabled,
    weather        = weather,
    weatherDetails = weatherDetails,
    weatherForecast = weatherForecast,
    weatherLoading = weatherLoading,
    weatherError = weatherError,
    notifications  = notifications,
    devices        = foundDevices.toList(),
    deviceScan     = deviceScan,
    installedApps = launchableApps.toList(),
    keyConfigured  = keyConfigured,
    googleSignedIn = googleSignedIn,
    gmailMessages  = gmailMessages.toList(),
    calendarEvents = calendarEvents.toList(),
    onWeather      = { refreshWeather() },
    onDiscover     = { discoverDevices() },
    onLaunchApp = { launchInstalledApp(it) },
    onNotifications = { status = "Ειδοποιήσεις: η πρόσβαση είναι απενεργοποιημένη" },
    onListen       = { active = true; requestListen() },
    onStop         = { stopConversation() },
    onContinuous   = { enabled -> configureWakeListening(enabled) },
    onSaveKey      = { saveKey(it) },
    onClearHistory = { clearHistory() },
    onHello        = { active = false; speak("Γεια σου! Είμαι η Τζόνα. Είμαι έτοιμη.") },
    onTestGroq     = { active = false; askGroq("Απάντησε σύντομα στα ελληνικά: λειτουργείς;") },
    onOpenMail     = { openExternalApp("mail") },
    onOpenChrome   = { openExternalApp("chrome") },
    onOpenCamera   = { openExternalApp("camera") },
    onOpenSmartHome = { openExternalApp("smarthome") },
    onGoogleSignIn = { openExternalApp("mail") },
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

 override fun onStart() {
  super.onStart()
  foreground = true
  volumeLevel = readCurrentMediaVolumeLevel()
 }
 override fun onStop() {
  foreground = false
  if (wakeEnabled) { wakeEnabled = false; active = false; listening = false; recognizer?.cancel(); status = "Wake mode σταμάτησε εκτός εφαρμογής" }
  super.onStop()
 }

 override fun onDestroy() {
  val nsd = getSystemService(NSD_SERVICE) as NsdManager
  discoveries.forEach { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }
  active = false; recognizer?.destroy(); tts?.stop(); tts?.shutdown()
  soundPool?.release(); soundPool = null
  super.onDestroy()
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

 // ─── Audio controls and synthesized UI sounds ─────────────────────────────

 private fun initSounds() {
  try {
   val attr = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
   val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attr).build()
   soundPool = pool
   soundIds["panel_open"] = pool.load(this, R.raw.johna_panel_open, 1)
   soundIds["panel_close"] = pool.load(this, R.raw.johna_panel_close, 1)
   soundIds["volume_up"] = pool.load(this, R.raw.johna_volume_up, 1)
   soundIds["volume_down"] = pool.load(this, R.raw.johna_volume_down, 1)
  } catch (_: Exception) { soundPool = null }
 }

 private fun playSound(kind: String) {
  if (!soundEnabled) return
  val id = soundIds[kind] ?: return
  val level = if (speaking) .12f else .28f
  try { soundPool?.play(id, level, level, 1, 0, 1f) } catch (_: Exception) {}
 }

 private fun readCurrentMediaVolumeLevel(): Int {
  return try {
   val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
   val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
   kotlin.math.round(am.getStreamVolume(AudioManager.STREAM_MUSIC) * 10f / max).toInt().coerceIn(0, 10)
  } catch (_: Exception) { 5 }
 }

 /** 0..10 is a friendly UI scale mapped to the actual Android media stream. */
 private fun executeVolumeCommand(q: String): Boolean {
  val command = LocalVolumeParser.parse(q) ?: return false
  val previous = readCurrentMediaVolumeLevel()
  val target = LocalVolumeParser.target(previous, command)
  return try {
   val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
   val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
   val mapped = kotlin.math.round(target * max / 10f).toInt().coerceIn(0, max)
   am.setStreamVolume(AudioManager.STREAM_MUSIC, mapped, 0)
   volumeLevel = readCurrentMediaVolumeLevel()
   playSound(if (target >= previous) "volume_up" else "volume_down")
   status = "Ένταση ${volumeLevel}/10"
   aiResponse = "Ένταση ήχου: ${volumeLevel} από 10"
   true
  } catch (e: SecurityException) {
   status = "Η ρύθμιση έντασης δεν επιτρέπεται από το Android"
   aiResponse = status
   true
  } catch (_: Exception) {
   status = "Αδυναμία αλλαγής έντασης"
   aiResponse = status
   true
  }
 }


 // ─── Voice ───────────────────────────────────────────────────────────────

 /** Experimental foreground-only wake phrase recognition using Android's STT service.
  * This is NOT a low-power, always-on local hotword engine. */
 private fun configureWakeListening(enabled: Boolean) {
  wakeEnabled = enabled
  continuous = false
  wakeRetries = 0
  awaitingWakeCommand = false
  if (enabled) {
   active = true
   status = "Johna · wake mode (μόνο με ανοιχτή εφαρμογή)"
   requestListen()
  } else {
   active = false; listening = false
   recognizer?.cancel(); recognizer?.destroy(); recognizer = null
   status = "Wake mode απενεργοποιήθηκε"
  }
 }

 private fun removeWakePrefix(input: String): String? {
  val cleaned = norm(input).trim().replace(Regex("[,.!?]+"), " ").trim()
  val prefixes = listOf("johna", "τζονα", "τζωνα", "τζοννα", "τζωνa", "τσονα")
  for (w in prefixes) {
   if (cleaned == w) return ""
   if (cleaned.startsWith("$w ")) return cleaned.removePrefix(w).trim()
  }
  return null
 }

 private fun requestListen() {
  if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) beginListening()
  else micPermission.launch(Manifest.permission.RECORD_AUDIO)
 }

 private fun beginListening() {
  if (!active || speaking || listening || aiPending || !foreground || isFinishing || isDestroyed) return
  if (!SpeechRecognizer.isRecognitionAvailable(this)) {
   status = "Δεν υπάρχει υπηρεσία αναγνώρισης"; active = false; return
  }
  recognizer?.destroy(); recognizer = null
  listening = true
  micEnergy = 0f
  recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
   sr.setRecognitionListener(object : RecognitionListener {
    override fun onReadyForSpeech(p: Bundle?) { status = "Σε ακούω..." }
    override fun onBeginningOfSpeech()         { status = "Ακούω..." }
    override fun onRmsChanged(rms: Float)       { micEnergy = ((rms + 2f) / 12f).coerceIn(0f, 1f) }
    override fun onBufferReceived(buf: ByteArray?) {}
    override fun onEndOfSpeech()               { micEnergy = 0f; status = "Επεξεργάζομαι..." }
    override fun onEvent(type: Int, p: Bundle?) {}
    override fun onPartialResults(partial: Bundle?) {}

    override fun onError(error: Int) {
     listening = false
     micEnergy = 0f
     // Only expected silence errors may retry. Abort on permissions/network/busy errors.
     if (wakeEnabled && foreground && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) && wakeRetries < 5) {
      wakeRetries++
      status = "Johna · αναμονή λέξης ενεργοποίησης"
      main.postDelayed({ active = true; beginListening() }, 1100L)
     } else {
      active = false; wakeEnabled = false; awaitingWakeCommand = false
      status = "Ακρόαση σταμάτησε ($error) · πάτησε Wake για επανεκκίνηση"
     }
    }

    override fun onResults(results: Bundle?) {
     listening = false
     micEnergy = 0f
     val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
     if (wakeEnabled) {
      val stripped = removeWakePrefix(words)
      when {
       awaitingWakeCommand && words.isNotBlank() -> {
        awaitingWakeCommand = false; wakeRetries = 0
        active = false; askGroq(words)
       }
       stripped != null -> {
        wakeRetries = 0
        if (stripped.isBlank()) {
         awaitingWakeCommand = true
         status = "Johna · πες την εντολή"
         main.postDelayed({ active = true; beginListening() }, 350L)
        } else {
         active = false; askGroq(stripped)
        }
       }
       else -> {
        if (++wakeRetries > 5) {
         wakeEnabled = false; active = false
         status = "Wake mode σε παύση · πάτησε Wake"
        } else main.postDelayed({ active = true; beginListening() }, 1100L)
       }
      }
     } else {
      if (words.isNotBlank()) { heard = words; active = false; askGroq(words) }
      else active = false
     }
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
  active = false; continuous = false; listening = false; wakeEnabled = false; awaitingWakeCommand = false
  recognizer?.cancel(); recognizer?.destroy(); recognizer = null
  micEnergy = 0f
  tts?.stop(); speaking = false; status = "Σταμάτησε"
 }

 private fun onTtsDone() {
  if (wakeEnabled && foreground && !aiPending) main.postDelayed({ active = true; beginListening() }, 900)
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
  if (executeVolumeCommand(q)) return true
  val open  = listOf("ανοιξε", "δειξε", "εμφανισε", "φερε", "ανοιγμα", "θελω να δω")
  val close = listOf("κλεισε", "κρυψε", "πισω", "αρχικη", "επιστροφη", "γυρνα")
  val closeTarget = listOf("παραθυρο", "πανελ", "καμερ", "καιρ", "ρυθμισ", "ειδοποι", "ημερολογ", "συσκευ", "ολα", "οθονη", "πισω", "αρχικη")
  val standaloneClose = q.trim() in listOf("κλεισε", "κλειστο", "πισω", "αρχικη", "αρχικη οθονη", "κλεισε τα ολα", "κλεισε ολα", "γυρνα πισω", "επιστροφη")
  if (close.any { it in q } && "καμερ" in q) {
   aiResponse = "Για να κλείσεις την εξωτερική εφαρμογή Κάμερας χρησιμοποίησε το Πίσω του Android."
   status = "Κάμερα · εξωτερική εφαρμογή"
   return true
  }
  if (standaloneClose || (close.any { it in q } && closeTarget.any { it in q })) {
   voicePanel = "close"; status = "Κλείσιμο παραθύρου"; aiResponse = "Έκλεισα το παράθυρο."; return true
  }

  if (listOf("google home", "γκουγκλ χομ", "home assistant", "σπιτι μου", "εξυπνο σπιτι").any { it in q }
      && (open.any { it in q } || "συνδεσ" in q)) {
   voicePanel = "devices"
   openExternalApp("smarthome")
   return true
  }
  // Do not send uncontrolled power/lock commands based on STT guesses.
  if (listOf("αναψε", "σβησε", "ξεκλειδωσε", "κλειδωσε", "ανοιξε την πορτα").any { it in q }
      && listOf("φως", "λαμπ", "πορτα", "σπιτι", "ρολο").any { it in q }) {
   voicePanel = "devices"
   aiResponse = "Για ασφαλή έλεγχο συσκευών χρησιμοποίησε το Google Home ή Home Assistant. Δεν έχει ρυθμιστεί εξουσιοδοτημένος controller."
   speak(aiResponse)
   return true
  }

  // Gmail voice commands
  if ("email" in q || "e-mail" in q || "mail" in q || "μειλ" in q || "ιμειλ" in q || "αλληλογραφ" in q || "inbox" in q || "μηνυματα" in q) {
   if (open.none { it in q } && "διαβασε" !in q && "πρωτο" !in q) return false
   voicePanel = "mail"
   if (googleSignedIn && gmailMessages.isNotEmpty()) {
    val unread = gmailMessages.count { it.isUnread }
    val answer = "Έχεις ${gmailMessages.size} μηνύματα στο inbox, $unread αδιάβαστα. " +
     gmailMessages.take(2).joinToString(". ") { "Από ${it.from}: ${it.subject}" }
    aiResponse = answer; speak(answer)
   } else if (googleSignedIn) {
    aiResponse = "Το Gmail είναι διαθέσιμο από την επίσημη εφαρμογή."
   } else {
    aiResponse = "Για ασφαλή πρόσβαση στα μηνύματα ανοίγω την επίσημη εφαρμογή Gmail."; openExternalApp("mail")
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
    aiResponse = "Το Ημερολόγιο είναι διαθέσιμο από την επίσημη εφαρμογή."
   } else {
    aiResponse = "Για τα ραντεβού σου ανοίγω την επίσημη εφαρμογή Ημερολογίου."; openExternalApp("calendar")
   }
   return true
  }

  // Other panels
  if (open.none { it in q }) return false
  if ("εφαρμογ" in q || "ολες τις εφαρμογες" in q) { voicePanel = "apps"; return true }
  // Explicit command and a visible installed launcher app: no arbitrary package or shell execution.
  val app = launchableApps.firstOrNull { entry ->
   val name=norm(entry.title)
   name.length >= 4 && (q.endsWith(name) || q.contains(" $name"))
  }
  if(app != null) { launchInstalledApp(app.packageName); return true }
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
  if (handleVoiceCommand(words)) {
   // Non-speaking local commands also return to foreground wake listening.
   if (wakeEnabled && foreground && !speaking && !aiPending) {
    main.postDelayed({ if (wakeEnabled && !listening && !speaking) { active = true; beginListening() } }, 750)
   }
   return
  }
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

   val models = listOf("openai/gpt-oss-20b", "qwen/qwen3-32b")
   var reply = ""; var success = false; var providerUsed = ""; var diagnostic = ""

   for (model in models) {
    var conn: HttpURLConnection? = null
    try {
     val body = JSONObject()
      .put("model", model)
      .put("messages", messages)
      .put("stream", true)
      .put("max_completion_tokens", 240)
      .put("temperature", 0.2)
     if (model.startsWith("openai/gpt-oss")) body.put("reasoning_effort", "low")
     if (model.startsWith("qwen/"))          body.put("reasoning_effort", "none")

     conn = URL("https://api.groq.com/openai/v1/chat/completions").openConnection() as HttpURLConnection
     conn.requestMethod = "POST"
     conn.connectTimeout = 2500; conn.readTimeout = 7000
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

    } catch (e: java.net.SocketTimeoutException) {
     if (reply.isNotBlank()) { success = true; providerUsed = model; break }
     diagnostic = "Timeout ($model)"
    }
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
  if (onAnswer != null && System.currentTimeMillis() - lastWeatherUpdate < 300_000 && weatherForecast != null) {
   if (onAnswer != null) onAnswer("Στην Αθήνα έχει ${weatherForecast!!.currentTemperature} βαθμούς με υγρασία ${weatherForecast!!.relativeHumidity}%.")
   return
  }
  if (weatherLoading) return
  weatherLoading = true; weatherError = ""
  Thread {
   try {
    val url = "https://api.open-meteo.com/v1/forecast?latitude=37.9838&longitude=23.7275" +
      "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,wind_gusts_10m,surface_pressure,uv_index" +
      "&hourly=temperature_2m,precipitation_probability,weather_code" +
      "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code,sunrise,sunset" +
      "&forecast_days=7&timezone=Europe%2FAthens"
    val conn = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout=5000; readTimeout=8000 }
    try {
     if (conn.responseCode !in 200..299) throw IllegalStateException("Open-Meteo HTTP ${conn.responseCode}")
     val root=JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
     val current=root.getJSONObject("current")
     val hourly=root.getJSONObject("hourly")
     val daily=root.getJSONObject("daily")
     val hourTime=hourly.getJSONArray("time")
     val hourTemperature=hourly.getJSONArray("temperature_2m")
     val hourRain=hourly.getJSONArray("precipitation_probability")
     val hourCodes=hourly.getJSONArray("weather_code")
     val nowHour=current.optString("time").take(13)
     val first=(0 until hourTime.length()).firstOrNull { hourTime.optString(it).startsWith(nowHour) } ?: 0
     val hours=(first until minOf(first+24,hourTime.length())).map { i ->
      ForecastHour(hourTime.getString(i),hourTemperature.optDouble(i).toInt(),hourRain.optInt(i),hourCodes.optInt(i))
     }
     val dayTime=daily.getJSONArray("time")
     val highs=daily.getJSONArray("temperature_2m_max")
     val lows=daily.getJSONArray("temperature_2m_min")
     val rain=daily.getJSONArray("precipitation_probability_max")
     val codes=daily.getJSONArray("weather_code")
     val sunrise=daily.getJSONArray("sunrise")
     val sunset=daily.getJSONArray("sunset")
     val days=(0 until dayTime.length()).map { i ->
      ForecastDay(dayTime.getString(i),lows.optDouble(i).toInt(),highs.optDouble(i).toInt(),rain.optInt(i),codes.optInt(i),sunrise.optString(i),sunset.optString(i))
     }
     val result=WeatherForecast(
      updated=current.optString("time"),currentTemperature=current.optDouble("temperature_2m").toInt(),
      feelsLike=current.optDouble("apparent_temperature").toInt(),relativeHumidity=current.optInt("relative_humidity_2m"),
      windKmh=current.optDouble("wind_speed_10m").toInt(),windGustKmh=current.optDouble("wind_gusts_10m").toInt(),
      pressureHpa=current.optDouble("surface_pressure").toInt(),uvIndex=current.optDouble("uv_index"),
      currentCode=current.optInt("weather_code"),hours=hours,days=days)
     val speech="Στην Αθήνα έχει ${result.currentTemperature} βαθμούς, αισθητή θερμοκρασία ${result.feelsLike}, υγρασία ${result.relativeHumidity}% και άνεμο ${result.windKmh} χιλιόμετρα την ώρα."
     main.post {
      weatherForecast=result; weatherLoading=false; weather="${result.currentTemperature}°C"
      weatherDetails="Αθήνα · Υγρασία ${result.relativeHumidity}% · Άνεμος ${result.windKmh} km/h"
      lastWeatherUpdate=System.currentTimeMillis(); onAnswer?.invoke(speech)
     }
    } finally { conn.disconnect() }
   } catch (e: Exception) {
    main.post {
     weatherLoading=false; weatherError="Αποτυχία πρόγνωσης: ${e.javaClass.simpleName}"
     if (weatherForecast==null) { weather="Χωρίς δεδομένα"; weatherDetails="Έλεγξε τη σύνδεση" }
     onAnswer?.invoke("Δεν μπόρεσα να ανακτήσω ενημερωμένο καιρό.")
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

 // ─── Android app catalog / safe launching ───────────────────────────────
 private fun refreshApps() {
  try {
   val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
   val discovered = packageManager.queryIntentActivities(intent, 0).mapNotNull { item ->
    val pkg=item.activityInfo?.packageName ?: return@mapNotNull null
    if (pkg == packageName) return@mapNotNull null
    AppShortcut(item.loadLabel(packageManager).toString(),pkg)
   }.distinctBy { it.packageName }.sortedBy { it.title.lowercase(Locale.getDefault()) }
   launchableApps.clear(); launchableApps.addAll(discovered)
  } catch (e: Exception) { status = "Δεν ήταν δυνατή η ανάκτηση εφαρμογών" }
 }

 private fun launchInstalledApp(pkg: String) {
  if (launchableApps.none { it.packageName == pkg }) return
  try {
   val intent=packageManager.getLaunchIntentForPackage(pkg)
   if (intent == null) { status = "Δεν βρέθηκε εκκινήσιμη δραστηριότητα"; return }
   startActivity(intent)
   status = "Άνοιγμα εφαρμογής"
  } catch (e: Exception) { status = "Αδυναμία ανοίγματος εφαρμογής" }
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
    "smarthome" -> packageManager.getLaunchIntentForPackage("com.google.android.apps.chromecast.app")
               ?: packageManager.getLaunchIntentForPackage("io.homeassistant.companion.android")
               ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://home.google.com"))
     "calendar" -> Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis()))
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

