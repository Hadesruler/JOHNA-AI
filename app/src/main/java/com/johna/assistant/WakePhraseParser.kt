package com.johna.assistant

import java.text.Normalizer
import java.util.Locale

/** Pure foreground wake phrase parsing, independent from the Android recognizer. */
object WakePhraseParser {
 fun strip(input: String): String? {
  val cleaned = Normalizer.normalize(input.lowercase(Locale("el", "GR")), Normalizer.Form.NFD)
   .replace(Regex("\\p{M}+"), "")
   .replace(Regex("[,.!?]+"), " ")
   .replace(Regex("\\s+"), " ").trim()
  val names = listOf("johna", "τζονα", "τζωνα", "τζοννα", "τσονα", "τζοαννα", "τζονια")
  val greetings = listOf("", "hey ", "hei ", "χευ ", "χει ", "γεια ", "γεια σου ")
  for (name in names) for (greet in greetings) {
   val prefix = greet + name
   if (cleaned == prefix) return ""
   if (cleaned.startsWith("$prefix ")) return cleaned.removePrefix(prefix).trim()
  }
  return null
 }
}
