package com.johna.assistant

import android.Manifest
import android.content.Intent
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
  setContent {
   JohnaScreen(status, heard, history.toList(), continuous,
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
 private fun localReply(words: String): String {
  val s = words.lowercase(Locale("el", "GR"))
  return when {
   "ώρα" in s -> "Η ώρα είναι ${SimpleDateFormat("HH:mm", Locale("el", "GR")).format(Date())}."
   "ημερομηνία" in s || "τι μέρα" in s -> "Σήμερα είναι ${SimpleDateFormat("EEEE d MMMM", Locale("el", "GR")).format(Date())}."
   "γεια" in s || "καλημέρα" in s || "καλησπέρα" in s -> "Γεια σου! Σε ακούω."
   "ποια είσαι" in s || "πώς σε λένε" in s -> "Είμαι η Τζόνα, η προσωπική σου βοηθός."
   "ευχαριστώ" in s -> "Παρακαλώ!"
   else -> "Άκουσα: $words. Το τοπικό AI δεν έχει εγκατασταθεί ακόμη."
  }
 }
 override fun onDestroy() { active = false; recognizer?.destroy(); tts?.stop(); tts?.shutdown(); super.onDestroy() }
}

@Composable
fun JohnaScreen(status: String, heard: String, history: List<Pair<String,String>>, continuous: Boolean,
 onContinuous: (Boolean) -> Unit, onListen: () -> Unit, onStop: () -> Unit, onHello: () -> Unit) {
 val purple = Color(0xFF8D3AF4); val magenta = Color(0xFFE843C4)
 val pulse by rememberInfiniteTransition(label = "orb").animateFloat(0.94f, 1.07f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
 Column(Modifier.fillMaxSize().background(Color(0xFF0D0B17)).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
  Text("JOHNA", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Light, letterSpacing = 6.sp)
  Text("AI HOME CENTER · v0.2", color = Color(0xFFAB9CBF), fontSize = 11.sp)
  Spacer(Modifier.height(16.dp))
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   StatusTile("ΦΩΝΗ", "Google · EL", Modifier.weight(1f))
   StatusTile("AI", "Τοπικό · εκκρεμεί", Modifier.weight(1f))
   StatusTile("ΛΕΙΤΟΥΡΓΙΑ", if (continuous) "Συνομιλία" else "Χειροκίνητη", Modifier.weight(1f))
  }
  Spacer(Modifier.height(12.dp))
  Box(Modifier.size(194.dp), contentAlignment = Alignment.Center) {
   Canvas(Modifier.fillMaxSize()) {
    val r = size.minDimension / 2
    drawCircle(brush = Brush.radialGradient(listOf(magenta.copy(alpha = .2f), purple.copy(alpha = .08f), Color.Transparent)), radius = r * pulse)
    drawCircle(color = magenta.copy(alpha = .38f), radius = r * .68f * pulse, style = Stroke(2.dp.toPx()))
    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFA5EE), magenta, purple, Color(0xFF301050))), radius = r * .54f * pulse)
   }
  }
  Text(status, color = Color(0xFFF1C7FF), fontSize = 17.sp)
  Spacer(Modifier.height(7.dp))
  Text(heard, color = Color(0xFFB4A5C7), fontSize = 13.sp, textAlign = TextAlign.Center)
  Spacer(Modifier.height(14.dp))
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
   Column(Modifier.weight(1f)) {
    Text("Συνεχής συνομιλία", color = Color.White, fontSize = 15.sp)
    Text("Όσο η εφαρμογή είναι ανοιχτή", color = Color(0xFFAB9CBF), fontSize = 11.sp)
   }
   Switch(checked = continuous, onCheckedChange = onContinuous)
  }
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   Button(onClick = onListen, colors = ButtonDefaults.buttonColors(containerColor = purple), modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Μίλα στη Johna") }
   OutlinedButton(onClick = onStop, modifier = Modifier.weight(.55f), shape = RoundedCornerShape(14.dp)) { Text("Στοπ") }
  }
  OutlinedButton(onClick = onHello, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Δοκιμή γυναικείας φωνής", color = Color(0xFFEACBFF)) }
  Spacer(Modifier.height(16.dp))
  Text("DASHBOARD", color = Color(0xFFAB9CBF), fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.align(Alignment.Start))
  Spacer(Modifier.height(8.dp))
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   StatusTile("LG TV", "Σύνδεση εκκρεμεί", Modifier.weight(1f))
   StatusTile("BAMBU A1", "Σύνδεση εκκρεμεί", Modifier.weight(1f))
  }
  Spacer(Modifier.height(8.dp))
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   StatusTile("SMART HOME", "Σύνδεση εκκρεμεί", Modifier.weight(1f))
   StatusTile("WEB", "Σύνδεση εκκρεμεί", Modifier.weight(1f))
  }
  Spacer(Modifier.height(16.dp))
  Text("ΙΣΤΟΡΙΚΟ ΣΥΝΟΜΙΛΙΑΣ", color = Color(0xFFAB9CBF), fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.Start))
  Spacer(Modifier.height(8.dp))
  if (history.isEmpty()) Text("Δεν υπάρχουν ακόμη συνομιλίες", color = Color(0xFF897C9B), fontSize = 12.sp)
  history.takeLast(6).asReversed().forEach { (question, answer) ->
   Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).background(Color(0xFF20182F), RoundedCornerShape(12.dp)).padding(12.dp)) {
    Text("Εσύ: $question", color = Color.White, fontSize = 13.sp)
    Spacer(Modifier.height(4.dp))
    Text("Johna: $answer", color = Color(0xFFD8B6FF), fontSize = 12.sp)
   }
  }
  Spacer(Modifier.height(10.dp))
  Text("Τοπικό AI και Hey Johna: δεν έχουν ενεργοποιηθεί ακόμη", color = Color(0xFF897C9B), fontSize = 11.sp, textAlign = TextAlign.Center)
 }
}

@Composable
private fun StatusTile(title: String, value: String, modifier: Modifier = Modifier) {
 Column(modifier.background(Color(0xFF20182F), RoundedCornerShape(12.dp)).padding(10.dp)) {
  Text(title, color = Color(0xFFE5C6FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
  Spacer(Modifier.height(5.dp))
  Text(value, color = Color(0xFFB8A7CA), fontSize = 10.sp)
 }
}
