package com.johna.assistant

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val white    = Color(0xFFF2F6FF)
private val aqua     = Color(0xFF75E6FF)
private val lavender = Color(0xFFC6A2FF)
private val teal     = Color(0xFF00EFCC)

// ─── Glass card wrapper ──────────────────────────────────────────────────────

@Composable
private fun GlassBox(
 title: String,
 modifier: Modifier = Modifier,
 onExpand: () -> Unit = {},
 content: @Composable ColumnScope.() -> Unit
) {
 Column(
  modifier
   .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black, spotColor = Color.Black)
   .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .22f), lavender.copy(alpha = .14f), aqua.copy(alpha = .22f))), RoundedCornerShape(24.dp))
   .background(Brush.verticalGradient(listOf(Color(0x77223454), Color(0x9B101D39))), RoundedCornerShape(24.dp))
   .clickable { onExpand() }
   .padding(15.dp)
 ) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
   Text(title, color = white, fontSize = 15.sp, fontWeight = FontWeight.Medium)
   Text("↗", color = aqua.copy(alpha = .75f), fontSize = 16.sp)
  }
  Spacer(Modifier.height(10.dp))
  content()
 }
}

// ─── Main dashboard ──────────────────────────────────────────────────────────

@Composable
fun GlassDashboard(
 status: String,
 heard: String,
 aiResponse: String,
 voicePanel: String,
 onVoicePanelConsumed: () -> Unit,
 speaking: Boolean,
 listening: Boolean,
 micEnergy: Float,
 volumeLevel: Int,
 soundEffectsEnabled: Boolean,
 onSoundEffectsChanged: (Boolean) -> Unit,
 onPanelTransition: (Boolean) -> Unit,
 continuous: Boolean,
 weather: String,
 weatherDetails: String,
 weatherForecast: WeatherForecast?,
 weatherLoading: Boolean,
 weatherError: String,
 notifications: List<String>,
 devices: List<Pair<String, String>>,
 deviceScan: String,
 installedApps: List<AppShortcut>,
 keyConfigured: Boolean,
 googleSignedIn: Boolean,
 gmailMessages: List<GmailMessage>,
 calendarEvents: List<CalendarEvent>,
 onWeather: () -> Unit,
 onDiscover: () -> Unit,
 onLaunchApp: (String) -> Unit,
 onNotifications: () -> Unit,
 onListen: () -> Unit,
 onStop: () -> Unit,
 onContinuous: (Boolean) -> Unit,
 onSaveKey: (String) -> Unit,
 onClearHistory: () -> Unit,
 onHello: () -> Unit,
 onTestGroq: () -> Unit,
 onOpenMail: () -> Unit,
 onOpenChrome: () -> Unit,
 onOpenCamera: () -> Unit,
 onOpenSmartHome: () -> Unit,
 onGoogleSignIn: () -> Unit,
 appSha1: String
) {
 var focused  by remember { mutableStateOf("") }
 var seconds  by remember { mutableIntStateOf(10) }
 var theme    by remember { mutableIntStateOf(0) }
 var ambientEnabled by remember { mutableStateOf(true) }
 var settings by remember { mutableStateOf(false) }
 var lastPanelInteraction by remember { mutableIntStateOf(0) }
 var selectedDock by remember { mutableIntStateOf(-1) }
 LaunchedEffect(selectedDock) { if (selectedDock >= 0) { delay(260); selectedDock = -1 } }
 var priorPanelVisible by remember { mutableStateOf(false) }
 var panelTransitionInitialized by remember { mutableStateOf(false) }
 LaunchedEffect(focused, settings) {
  val visibleNow = focused.isNotEmpty() || settings
  if (panelTransitionInitialized && visibleNow != priorPanelVisible) onPanelTransition(visibleNow)
  priorPanelVisible = visibleNow
  panelTransitionInitialized = true
 }

 LaunchedEffect(voicePanel) {
  if (voicePanel.isNotBlank()) {
   when (voicePanel) {
    "close"    -> { focused = ""; settings = false }
    "settings" -> { focused = ""; settings = true }
    else       -> { settings = false; focused = voicePanel }
   }
   onVoicePanelConsumed()
  }
 }

 var keyDraft by remember { mutableStateOf("") }
 var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
 LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }

 // Auto-close panel after [seconds] of inactivity (0 = disabled)
 LaunchedEffect(focused, seconds, lastPanelInteraction) {
  if (focused.isNotEmpty() && seconds > 0) { delay(seconds * 1000L); focused = "" }
 }

 val accents = listOf(aqua, lavender, Color(0xFFFFCB87), Color(0xFFB7E5FF))
 val accent  = accents[theme]

 val context = androidx.compose.ui.platform.LocalContext.current
 val prefs   = remember { context.getSharedPreferences("johna_glass", 0) }
 LaunchedEffect(Unit) {
  theme = prefs.getInt("theme", 0).coerceIn(0, 3)
  ambientEnabled = prefs.getBoolean("ambientEnabled", true)
 }

 val time = remember(now) { SimpleDateFormat("HH:mm", Locale("el", "GR")).format(Date(now)) }
 val date = remember(now) { SimpleDateFormat("EEE, d MMM yyyy", Locale("el", "GR")).format(Date(now)) }

 val infinite = rememberInfiniteTransition(label = "orb")
 val pulse    by infinite.animateFloat(.97f, 1.025f, infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "pulse")

 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF08101F),Color(0xFF0B1630),Color(0xFF040912))))) {
  if (ambientEnabled) {
   AmbientLivingBackdrop(accent = accent, speaking = speaking, listening = listening, modifier = Modifier.fillMaxSize())
  }
  Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x140A1C33), Color(0x04060D25), Color(0x66040B1E)))))

  Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp)) {

   // ── Header ──────────────────────────────────────────────────────────────
   Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
    Column { Text(time, color = white, fontSize = 30.sp, fontWeight = FontWeight.Light); Text(date, color = white, fontSize = 11.sp) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
     Text("J O H N A", color = white, fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = 3.sp)
     Text("ETHEREAL ASSISTANT · v1.0.15", color = accent.copy(alpha = .8f), fontSize = 10.sp)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
     Text("◈", color = accent, modifier = Modifier.clickable { theme = (theme + 1) % 4; prefs.edit().putInt("theme", theme).apply() }.padding(9.dp), fontSize=20.sp)
     Text("⚙", color = white, modifier = Modifier.clickable { settings = true }.padding(9.dp), fontSize=19.sp)
    }
   }

   Spacer(Modifier.height(12.dp))

   // ── Three-column layout ──────────────────────────────────────────────────
   Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

    // Left column
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
     GlassBox("☁  Καιρός · Αθήνα", Modifier.weight(1f), onExpand = { focused = "weather" }) {
      val today = weatherForecast?.days?.firstOrNull()
      val isNight = today != null && (weatherForecast!!.updated < today.sunrise || weatherForecast!!.updated >= today.sunset)
      Text(if (weather.contains("°")) "${if (isNight) "☾" else "☀"}  $weather" else "☁  $weather", color = white, fontSize = 33.sp, fontWeight = FontWeight.Light)
      Text(weatherDetails, color = white.copy(alpha = .85f), fontSize = 11.sp, maxLines = 2)
      Spacer(Modifier.weight(1f)); Text("☀       ☁       🌧       ⛈", color = accent, fontSize = 20.sp)
      Text("Πρόγνωση καιρού", color = white.copy(alpha = .75f), fontSize = 11.sp)
     }
     GlassBox("▦  Ημερολόγιο", Modifier.weight(.9f), onExpand = { focused = "calendar" }) {
      Text(date, color = white, fontSize = 14.sp); Spacer(Modifier.height(10.dp))
      if (googleSignedIn && calendarEvents.isNotEmpty()) {
       Text(calendarEvents.first().let { "${it.startDisplay}: ${it.summary}" }, color = white.copy(alpha = .85f), fontSize = 11.sp, maxLines = 2)
      } else {
       Text(if (googleSignedIn) "Δεν υπάρχουν προσεχή ραντεβού" else "Άνοιξε το Ημερολόγιο για ραντεβού", color = white.copy(alpha = .85f), fontSize = 11.sp)
      }
     }
    }

    // Centre column — orb
    Column(Modifier.weight(1.36f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
     JohnaSphere(
      speaking = speaking,
      listening = listening,
      voiceEnergy = micEnergy,
      modifier = Modifier
       .fillMaxWidth(.90f)
       .aspectRatio(1f)
       .graphicsLayer { scaleX = pulse; scaleY = pulse }
       .clickable { onListen() }
     )
     Text(status, color = white, fontSize = 16.sp, fontWeight = FontWeight.Medium)
     AnimatedContent(targetState = volumeLevel, label = "mediaLevel") { level ->
      Row(verticalAlignment = Alignment.CenterVertically) {
       Text("MEDIA  ", color = accent, fontSize = 10.sp)
       repeat(10) { n ->
        Box(Modifier.padding(horizontal = 1.dp).width(7.dp).height(5.dp)
         .background(if (n < level) aqua else Color(0x447B8BA3), RoundedCornerShape(2.dp)))
       }
       Text("  $level/10", color = white.copy(alpha = .8f), fontSize = 10.sp)
      }
     }
     Text(
      aiResponse,
      color = aqua, fontSize = 12.sp, textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().heightIn(max = 105.dp).verticalScroll(rememberScrollState())
     )
     Spacer(Modifier.height(12.dp))
     Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Button(onClick = onListen, colors = ButtonDefaults.buttonColors(containerColor = Color(0xBB7555C6))) {
       Text(if (listening) "🎙  Ακούω..." else "🎙  Μίλα στη Johna")
      }
      OutlinedButton(onClick = { onContinuous(!continuous) }) {
       Text(if (continuous) "Wake ON" else "Wake: Johna", color = if (continuous) aqua else white)
      }
      OutlinedButton(onClick = onStop) { Text("Στοπ", color = white) }
     }
     Text("Hey Johna · foreground wake (πειραματικό)", color = white.copy(alpha = .58f), fontSize = 10.sp)
    }

    // Right column
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
     GlassBox("♧  Ειδοποιήσεις (${notifications.size})", Modifier.weight(1f), onExpand = { focused = "notifications" }) {
      if (notifications.isEmpty()) Text("Δεν υπάρχουν διαθέσιμες ειδοποιήσεις ή δεν έχει δοθεί άδεια", color = white.copy(alpha = .8f), fontSize = 12.sp)
      else notifications.take(3).forEach { Text("• $it", color = white, fontSize = 12.sp, maxLines = 2); Spacer(Modifier.height(7.dp)) }
     }
     GlassBox("⌂  Smart Home", Modifier.weight(.9f), onExpand = { focused = "devices" }) {
      Text("${devices.size} υπηρεσίες δικτύου", color = white, fontSize = 14.sp)
      Text(deviceScan, color = white.copy(alpha = .8f), fontSize = 11.sp, maxLines = 2)
      Text("Άνοιγμα Google Home / Home Assistant", color = accent, fontSize = 11.sp, modifier = Modifier.clickable { onOpenSmartHome() })
      Spacer(Modifier.height(7.dp)); Text("◉  Αναζήτηση συσκευών", color = accent, fontSize = 12.sp, modifier = Modifier.clickable { onDiscover() })
     }
    }
   }

   Spacer(Modifier.height(14.dp))

   // ── Floating minimal dock: replaces the oversized rectangular tiles ──
   Row(
    Modifier.align(Alignment.CenterHorizontally)
     .fillMaxWidth(.83f)
     .height(62.dp)
     .shadow(18.dp, RoundedCornerShape(38.dp))
     .border(1.dp, accent.copy(alpha=.24f), RoundedCornerShape(38.dp))
     .background(Color(0xD2111D34), RoundedCornerShape(38.dp))
     .padding(horizontal=12.dp, vertical=5.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.CenterVertically
   ) {
    listOf(
     "◎" to "Μίλα", "☁" to "Καιρός", "▦" to "Ημερολόγιο",
     "⌂" to "Σπίτι", "◫" to "Apps", "✉" to "Mail"
    ).forEachIndexed { index, item ->
     val activeScale by animateFloatAsState(if (selectedDock == index) 1.08f else 1f,label="dockFloat$index")
     Column(
      Modifier.weight(1f).fillMaxHeight()
       .graphicsLayer {scaleX=activeScale;scaleY=activeScale}
       .background(if(selectedDock==index)accent.copy(alpha=.15f)else Color.Transparent,RoundedCornerShape(28.dp))
       .clickable {
        selectedDock=index
        when(index) {
         0 -> onListen()
         1 -> focused="weather"
         2 -> focused="calendar"
         3 -> focused="devices"
         4 -> focused="apps"
         5 -> focused="mail"
        }
       },
      horizontalAlignment=Alignment.CenterHorizontally,
      verticalArrangement=Arrangement.Center
     ) {
      Text(item.first,color=if(index==0)accent else white.copy(alpha=.85f),fontSize=19.sp)
      Text(item.second,color=white.copy(alpha=.75f),fontSize=9.sp)
     }
    }
   }
  }

  // ── Dim overlay ──────────────────────────────────────────────────────────
  AnimatedVisibility(visible = focused.isNotEmpty(), enter = fadeIn(tween(180)), exit = fadeOut(tween(230))) {
   Box(Modifier.fillMaxSize().background(Color(0xB8000719)).clickable { focused = "" })
  }

  // ── Sliding glass panel ──────────────────────────────────────────────────
  AnimatedContent(
   targetState = focused,
   transitionSpec = {
    val fromLeft = targetState in listOf("weather", "calendar", "mail", "apps")
    val toLeft   = initialState in listOf("weather", "calendar", "mail", "apps")
    (slideInHorizontally(initialOffsetX = { w -> if (fromLeft) -w else w }, animationSpec = tween(390, easing = FastOutSlowInEasing))
     + fadeIn(tween(210))) togetherWith
     (slideOutHorizontally(targetOffsetX = { w -> if (toLeft) -w else w }, animationSpec = tween(310, easing = FastOutSlowInEasing))
      + fadeOut(tween(190)))
   },
   label = "panelWhoosh"
  ) { panel ->
   if (panel.isNotEmpty()) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
     GlassBox(
      when (panel) {
       "weather"       -> "☁  Καιρός · Αθήνα"
       "calendar"      -> "▦  Ημερολόγιο Google"
       "mail"          -> "✉  Gmail · johnyexoperience@gmail.com"
       "notifications" -> "♧  Ειδοποιήσεις"
       "apps"          -> "▦  Εγκατεστημένες εφαρμογές"
       else            -> "⌂  Έξυπνες συσκευές"
      },
      Modifier.fillMaxWidth(if (panel == "weather") .94f else .76f)
       .fillMaxHeight(if (panel == "weather") .95f else .88f),
      onExpand = { focused = "" }
     ) {
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
       when (panel) {
        "weather" -> {
         WeatherDetailPanel(weatherForecast,weatherLoading,weatherError,onRefresh=onWeather,onActivity={lastPanelInteraction++})
        }

        "calendar" -> {
         Text(date, color = white, fontSize = 23.sp)
         Spacer(Modifier.height(12.dp))
         if (!googleSignedIn) {
          Text("Σύνδεση απαιτείται για εμφάνιση ραντεβού Google Calendar.", color = white)
          Spacer(Modifier.height(12.dp))
          Button(onClick = onGoogleSignIn, colors = ButtonDefaults.buttonColors(containerColor = Color(0xBB4285F4))) {
           Text("Άνοιγμα Gmail")
          }
         } else if (calendarEvents.isEmpty()) {
          Text("Δεν βρέθηκαν προσεχή ραντεβού.", color = white)
         } else {
          calendarEvents.forEach { event ->
           Column(
            Modifier.fillMaxWidth().padding(vertical = 4.dp)
             .background(Color(0x33FFFFFF), RoundedCornerShape(10.dp)).padding(12.dp)
           ) {
            Text(event.summary, color = white, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("🕐 ${event.startDisplay}", color = lavender, fontSize = 12.sp)
            if (event.location.isNotBlank()) Text("📍 ${event.location}", color = white.copy(alpha = .7f), fontSize = 11.sp)
           }
           Spacer(Modifier.height(6.dp))
          }
         }
        }

        "mail" -> {
         Text("Εισερχόμενα Gmail", color = aqua, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
         Text("johnyexoperience@gmail.com", color = white.copy(alpha = .75f), fontSize = 12.sp)
         Spacer(Modifier.height(12.dp))
         if (!googleSignedIn) {
          Text("Σύνδεση απαιτείται για πρόσβαση στο inbox.", color = white)
          Spacer(Modifier.height(12.dp))
          Button(onClick = onGoogleSignIn, colors = ButtonDefaults.buttonColors(containerColor = Color(0xBB4285F4))) {
           Text("Άνοιγμα Gmail")
          }
          Spacer(Modifier.height(8.dp))
          TextButton(onClick = onOpenMail) { Text("Άνοιγμα Gmail app", color = aqua) }
         } else if (gmailMessages.isEmpty()) {
          Text("Φόρτωση μηνυμάτων...", color = white)
         } else {
          gmailMessages.take(12).forEach { msg ->
           Column(
            Modifier.fillMaxWidth().padding(vertical = 3.dp)
             .background(if (msg.isUnread) Color(0x33007AFF) else Color(0x22FFFFFF), RoundedCornerShape(10.dp))
             .padding(10.dp)
           ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
             Text(msg.from, color = if (msg.isUnread) teal else white, fontWeight = if (msg.isUnread) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
             Text(msg.dateDisplay, color = white.copy(alpha = .6f), fontSize = 10.sp)
             if (msg.isUnread) Text("  ●", color = teal, fontSize = 10.sp)
            }
            Text(msg.subject, color = white, fontSize = 12.sp, maxLines = 1)
            if (msg.snippet.isNotBlank()) Text(msg.snippet, color = white.copy(alpha = .65f), fontSize = 10.sp, maxLines = 2)
           }
           Spacer(Modifier.height(4.dp))
          }
          Spacer(Modifier.height(8.dp))
          TextButton(onClick = onOpenMail) { Text("Άνοιγμα Gmail app →", color = aqua) }
         }
        }

        "apps" -> {
         var appFilter by remember { mutableStateOf("") }
         Text("ΕΦΑΡΜΟΓΕΣ ANDROID",color=aqua,fontSize=12.sp)
         Text("Ανοίγουν μέσα από τον ασφαλή Android launcher. Κάθε εφαρμογή διατηρεί το δικό της περιβάλλον και δικαιώματα.",color=white.copy(alpha=.75f),fontSize=11.sp)
         OutlinedTextField(value=appFilter,onValueChange={appFilter=it;lastPanelInteraction++},label={Text("Αναζήτηση εφαρμογής")},singleLine=true,modifier=Modifier.fillMaxWidth())
         installedApps.filter { it.title.contains(appFilter,ignoreCase=true) }.take(45).forEach { app ->
          TextButton(onClick={lastPanelInteraction++;onLaunchApp(app.packageName)}) {
           Text("◉  ${app.title}",color=white,fontSize=14.sp)
          }
         }
        }

        "notifications" -> {
         if (notifications.isEmpty()) Text("Δεν υπάρχουν διαθέσιμες ειδοποιήσεις.", color = white)
         else notifications.forEach { Text("• $it", color = white); Spacer(Modifier.height(4.dp)) }
         TextButton(onClick = onNotifications) { Text("Πρόσβαση ειδοποιήσεων ανενεργή", color = aqua) }
        }

        else -> { // devices
         Text("ΣΥΝΔΕΔΕΜΕΝΟ ΣΠΙΤΙ · DISCOVERY",color=aqua,fontSize=12.sp,letterSpacing=1.sp)
         Text(deviceScan, color = white)
         Text("Εντοπισμός ≠ εξουσιοδοτημένος έλεγχος. Για έλεγχο χρειάζεται σύνδεση του αντίστοιχου controller.",color=white.copy(alpha=.72f),fontSize=11.sp)
         Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
          Button(onClick={lastPanelInteraction++;onDiscover()}) { Text("↻ Σάρωση δικτύου") }
          OutlinedButton(onClick={lastPanelInteraction++;onOpenSmartHome()}) { Text("Άνοιγμα Controller") }
         }
         devices.forEach { device ->
          Column(Modifier.fillMaxWidth().padding(vertical=5.dp).background(Color(0x2237A6FF),RoundedCornerShape(12.dp)).padding(12.dp)) {
           Text("◉ ${device.first}",color=white,fontSize=14.sp)
           Text(device.second,color=aqua,fontSize=10.sp)
           Text("Ανακαλύφθηκε στο LAN · απαιτείται συμβατό API για χειρισμό",color=white.copy(alpha=.7f),fontSize=10.sp)
          }
         }
         if(devices.isEmpty()) Text("Δεν έχουν εντοπιστεί τοπικές συσκευές ακόμα.",color=white.copy(alpha=.8f))
        }
       }
      }

      // Panel footer
      Spacer(Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
       Text("Αυτόματο κλείσιμο", color = white, fontSize = 11.sp)
       Spacer(Modifier.width(8.dp))
       listOf(0, 5, 10, 20).forEach { n ->
        TextButton(onClick = { seconds = n }) {
         Text(if (n == 0) "Όχι" else "${n}s", color = if (seconds == n) aqua else white, fontSize = 11.sp)
        }
       }
      }
      TextButton(onClick = { focused = "" }) { Text("✕ Κλείσιμο", color = white) }
     }
    }
   }
  }

  // ── Settings dialog ──────────────────────────────────────────────────────
  if (settings) AlertDialog(
   containerColor = Color(0xFF17233D),
   titleContentColor = white,
   textContentColor = white,
   onDismissRequest = { settings = false },
   title = { Text("Ρυθμίσεις JOHNA AI") },
   text = {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
     Row(verticalAlignment = Alignment.CenterVertically) {
      Text("Ζωντανό φόντο", modifier = Modifier.weight(1f), color = white)
      Switch(checked = ambientEnabled, onCheckedChange = {
       ambientEnabled = it
       prefs.edit().putBoolean("ambientEnabled", it).apply()
      })
     }
     Row(verticalAlignment = Alignment.CenterVertically) {
      Text("Ηχητικά εφέ JOHNA", modifier = Modifier.weight(1f), color = white)
      Switch(checked = soundEffectsEnabled, onCheckedChange = onSoundEffectsChanged)
     }
     Text(if (keyConfigured) "✅ Groq API key αποθηκευμένο" else "⚠ Δεν υπάρχει Groq API key")
     OutlinedTextField(keyDraft, { keyDraft = it }, label = { Text("Νέο Groq API key") }, singleLine = true)
     TextButton(onClick = { if (keyDraft.isNotBlank()) onSaveKey(keyDraft.trim()); keyDraft = "" }) { Text("Αποθήκευση κρυπτογραφημένα") }
     HorizontalDivider()
     Text(if (googleSignedIn) "✅ Google: Συνδεδεμένος" else "Google σύνδεση μέσα στη JOHNA: ανενεργή")
     Button(onClick = onGoogleSignIn, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))) { Text("Άνοιγμα επίσημης εφαρμογής Gmail") }
     HorizontalDivider()
     Text("SHA-1 υπογραφής (για Google Cloud Console):", fontSize = 11.sp)
     Text(appSha1, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
     HorizontalDivider()
     TextButton(onClick = onClearHistory) { Text("Διαγραφή μνήμης συνομιλιών") }
     TextButton(onClick = onHello) { Text("Δοκιμή φωνής") }
     TextButton(onClick = { settings = false; onTestGroq() }) { Text("Δοκιμή σύνδεσης Groq") }
    }
   },
   confirmButton = { TextButton(onClick = { settings = false }) { Text("Κλείσιμο") } }
  )
 }
}
