package com.johna.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
 private var recognizer: SpeechRecognizer? = null
 private var tts: TextToSpeech? = null
 private var status by mutableStateOf("Έτοιμη για συνομιλία")
 private var heard by mutableStateOf("Πάτησε το μικρόφωνο και μίλησέ μου")
 private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if (granted) startListening() else status = "Χρειάζεται άδεια μικροφώνου"
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  tts = TextToSpeech(this, this)
  setContent { JohnaScreen(status, heard, onListen = { if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening() else permission.launch(Manifest.permission.RECORD_AUDIO) }, onHello = { speak("Γεια σου! Είμαι η Τζόνα. Η πρώτη μου έκδοση είναι έτοιμη για δοκιμές.") }) }
 }
 override fun onInit(result: Int) {
  if (result == TextToSpeech.SUCCESS) {
   val available = tts?.setLanguage(Locale("el", "GR"))
   if (available == TextToSpeech.LANG_MISSING_DATA || available == TextToSpeech.LANG_NOT_SUPPORTED) status = "Δεν βρέθηκε ελληνική φωνή TTS στη συσκευή"
  }
 }
 private fun speak(text: String) { status = "Μιλάω"; tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "johna_reply") }
 private fun startListening() {
  if (!SpeechRecognizer.isRecognitionAvailable(this)) { status = "Δεν υπάρχει διαθέσιμη υπηρεσία αναγνώρισης ομιλίας"; return }
  recognizer?.destroy()
  recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
   sr.setRecognitionListener(object : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) { status = "Σε ακούω..." }
    override fun onBeginningOfSpeech() { status = "Ακούω" }
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { status = "Επεξεργάζομαι..." }
    override fun onError(error: Int) { status = "Σφάλμα αναγνώρισης: $error — δοκίμασε ξανά" }
    override fun onResults(results: Bundle?) {
     val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
     heard = words
     val reply = when {
      words.contains("ώρα", true) -> "Η ώρα είναι ${java.text.SimpleDateFormat("HH:mm", Locale("el", "GR")).format(java.util.Date())}."
      words.contains("γεια", true) -> "Γεια σου! Σε ακούω."
      words.contains("ποια είσαι", true) -> "Είμαι η Τζόνα, η προσωπική σου βοηθός."
      else -> "Άκουσα: $words. Αυτή είναι η πρώτη δοκιμαστική έκδοσή μου. Οι εντολές συσκευών και το AI θα προστεθούν αργότερα."
     }
     speak(reply)
    }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
   })
   val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "el-GR")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
   }
   status = "Ενεργοποίηση μικροφώνου..."
   sr.startListening(intent)
  }
 }
 override fun onDestroy() { recognizer?.destroy(); tts?.stop(); tts?.shutdown(); super.onDestroy() }
}

@Composable
fun JohnaScreen(status: String, heard: String, onListen: () -> Unit, onHello: () -> Unit) {
 val transition = rememberInfiniteTransition(label = "orb")
 val pulse by transition.animateFloat(initialValue = 0.93f, targetValue = 1.08f, animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
 val purple = Color(0xFF8D3AF4); val magenta = Color(0xFFE843C4)
 Column(Modifier.fillMaxSize().background(Color(0xFF0D0B17)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
  Text("JOHNA", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light, letterSpacing = 7.sp)
  Text("AI HOME CENTER · v0.1", color = Color(0xFFAB9CBF), fontSize = 11.sp)
  Spacer(Modifier.weight(1f))
  Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
   Canvas(Modifier.fillMaxSize()) {
    val r = size.minDimension / 2
    drawCircle(brush = Brush.radialGradient(listOf(magenta.copy(alpha = .20f), purple.copy(alpha = .10f), Color.Transparent)), radius = r * pulse)
    drawCircle(color = magenta.copy(alpha = .35f), radius = r * .63f * pulse, style = Stroke(width = 2.dp.toPx()))
    drawCircle(brush = Brush.radialGradient(listOf(Color(0xFFFFA5EE), magenta, purple, Color(0xFF301050))), radius = r * .52f * pulse)
    drawCircle(color = Color.White.copy(alpha = .13f), radius = r * .40f * pulse, style = Stroke(width = 1.dp.toPx()))
   }
  }
  Spacer(Modifier.height(12.dp))
  Text(status, color = Color(0xFFF1C7FF), fontSize = 17.sp)
  Spacer(Modifier.height(14.dp))
  Text(heard, color = Color(0xFFB4A5C7), fontSize = 14.sp, textAlign = TextAlign.Center)
  Spacer(Modifier.weight(1f))
  Button(onClick = onListen, colors = ButtonDefaults.buttonColors(containerColor = purple), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Μίλα στη Johna", fontSize = 17.sp) }
  Spacer(Modifier.height(10.dp))
  OutlinedButton(onClick = onHello, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) { Text("Δοκιμή γυναικείας φωνής", color = Color(0xFFEACBFF)) }
  Spacer(Modifier.height(12.dp))
  Text("Πρωτότυπο: χωρίς συνεχή ακρόαση ή σύνδεση AI", color = Color(0xFF897C9B), fontSize = 11.sp)
 }
}
