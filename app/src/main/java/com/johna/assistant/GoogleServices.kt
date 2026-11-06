package com.johna.assistant

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

// ─── Data models ────────────────────────────────────────────────────────────

data class GmailMessage(
 val id: String,
 val from: String,
 val subject: String,
 val snippet: String,
 val dateDisplay: String,
 val isUnread: Boolean
)

data class CalendarEvent(
 val id: String,
 val summary: String,
 val startDisplay: String,
 val location: String = ""
)

// ─── REST helpers (run on background thread only) ───────────────────────────

object GoogleServices {
 const val GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
 const val CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.readonly"

 /** Fetch up to [maxResults] inbox message summaries using the Gmail REST API. */
 fun fetchInbox(token: String, maxResults: Int = 15): List<GmailMessage> {
  return try {
   val listJson = getJson(
    token,
    "https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=$maxResults&labelIds=INBOX"
   )
   val arr = listJson.optJSONArray("messages") ?: return emptyList()
   (0 until minOf(arr.length(), maxResults)).mapNotNull { i ->
    runCatching { fetchMessage(token, arr.getJSONObject(i).getString("id")) }.getOrNull()
   }
  } catch (_: Exception) { emptyList() }
 }

 private fun fetchMessage(token: String, id: String): GmailMessage? {
  val json = getJson(
   token,
   "https://gmail.googleapis.com/gmail/v1/users/me/messages/$id?format=metadata" +
    "&metadataHeaders=From&metadataHeaders=Subject&metadataHeaders=Date"
  )
  val headers = json.getJSONObject("payload").getJSONArray("headers")
  var from = ""; var subject = "(χωρίς θέμα)"; var rawDate = ""
  for (i in 0 until headers.length()) {
   val h = headers.getJSONObject(i)
   when (h.getString("name")) {
    "From"    -> from    = h.getString("value").substringBefore("<").trim().trim('"')
    "Subject" -> subject = h.getString("value")
    "Date"    -> rawDate = h.getString("value")
   }
  }
  val labels = json.optJSONArray("labelIds")
  val isUnread = (0 until (labels?.length() ?: 0)).any { labels!!.getString(it) == "UNREAD" }
  val dateDisplay = formatMailDate(rawDate)
  return GmailMessage(id, from.ifBlank { "Άγνωστος" }, subject, json.optString("snippet"), dateDisplay, isUnread)
 }

 /** Fetch upcoming calendar events starting from now. */
 fun fetchUpcomingEvents(token: String, maxResults: Int = 10): List<CalendarEvent> {
  return try {
   val now = URLEncoder.encode(
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
     .apply { timeZone = TimeZone.getTimeZone("UTC") }
     .format(Date()),
    "UTF-8"
   )
   val json = getJson(
    token,
    "https://www.googleapis.com/calendar/v3/calendars/primary/events" +
     "?maxResults=$maxResults&orderBy=startTime&singleEvents=true&timeMin=$now"
   )
   val items = json.optJSONArray("items") ?: return emptyList()
   (0 until items.length()).map { i ->
    val item = items.getJSONObject(i)
    val start = item.optJSONObject("start")?.run {
     val dt = optString("dateTime"); val d = optString("date")
     formatEventStart(dt.ifBlank { d })
    } ?: "?"
    CalendarEvent(
     id = item.getString("id"),
     summary = item.optString("summary", "(χωρίς τίτλο)"),
     startDisplay = start,
     location = item.optString("location")
    )
   }
  } catch (_: Exception) { emptyList() }
 }

 // ── Helpers ─────────────────────────────────────────────────────────────────

 private fun getJson(token: String, url: String): JSONObject {
  val conn = URL(url).openConnection() as HttpURLConnection
  conn.connectTimeout = 6000; conn.readTimeout = 10000
  conn.setRequestProperty("Authorization", "Bearer $token")
  conn.setRequestProperty("Accept", "application/json")
  val code = conn.responseCode
  val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
   .bufferedReader().use { it.readText() }
  if (code !in 200..299) throw RuntimeException("HTTP $code: ${body.take(300)}")
  return JSONObject(body)
 }

 private fun formatMailDate(raw: String): String {
  if (raw.isBlank()) return ""
  return try {
   // RFC 2822 date: "Sat, 7 Nov 2026 10:30:00 +0200"
   val sdf = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US)
   val parsed = sdf.parse(raw) ?: return raw
   val diff = System.currentTimeMillis() - parsed.time
   val days = (diff / 86_400_000).toInt()
   when {
    days == 0 -> SimpleDateFormat("HH:mm", Locale("el", "GR")).format(parsed)
    days == 1 -> "Χθες"
    days < 7  -> SimpleDateFormat("EEEE", Locale("el", "GR")).format(parsed)
    else      -> SimpleDateFormat("d MMM", Locale("el", "GR")).format(parsed)
   }
  } catch (_: Exception) { raw.take(16) }
 }

 private fun formatEventStart(raw: String): String {
  if (raw.isBlank()) return "?"
  return try {
   if (raw.length == 10) {
    // date-only "yyyy-MM-dd"
    val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw) ?: return raw
    SimpleDateFormat("EEE, d MMM", Locale("el", "GR")).format(d)
   } else {
    // datetime with offset
    val d = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(raw) ?: return raw
    SimpleDateFormat("EEE d MMM, HH:mm", Locale("el", "GR")).format(d)
   }
  } catch (_: Exception) { raw.take(16) }
 }
}
