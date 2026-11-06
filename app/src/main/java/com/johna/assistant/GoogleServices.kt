package com.johna.assistant

/** Display models retained so the UI can be upgraded to OAuth later without layout changes.
 * This safer build deliberately does not fetch private Google data. */
data class GmailMessage(val id: String, val from: String, val subject: String, val snippet: String, val dateDisplay: String, val isUnread: Boolean)
data class CalendarEvent(val id: String, val summary: String, val startDisplay: String, val location: String = "")
