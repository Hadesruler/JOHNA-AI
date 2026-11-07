package com.johna.assistant

import java.text.Normalizer
import java.util.Locale

/** Pure parser for Greek/English Android media-volume commands; no LLM required. */
object LocalVolumeParser {
 enum class Kind { UP, DOWN, SET, MUTE, UNMUTE }
 data class Command(val kind: Kind, val amount: Int? = null)

 private val numbers = mapOf(
  "μηδεν" to 0, "ενα" to 1, "εναν" to 1, "μια" to 1,
  "δυο" to 2, "τρια" to 3, "τρεις" to 3, "τεσσερα" to 4,
  "πεντε" to 5, "εξι" to 6, "εφτα" to 7, "επτα" to 7,
  "οχτω" to 8, "οκτω" to 8, "εννια" to 9, "δεκα" to 10,
  "zero" to 0, "one" to 1, "two" to 2, "three" to 3,
  "four" to 4, "five" to 5, "six" to 6, "seven" to 7,
  "eight" to 8, "nine" to 9, "ten" to 10
 )

 fun parse(raw: String): Command? {
  val q = Normalizer.normalize(raw.lowercase(Locale("el", "GR")), Normalizer.Form.NFD)
   .replace(Regex("\\p{M}+"), "").trim()
  val tokens = q.split(Regex("[^\\p{L}0-9]+" )).filter { it.isNotEmpty() }
  val has = { word: String -> word in tokens }
  if (has("ξεσιγαση") || has("unmute")) return Command(Kind.UNMUTE)
  if (has("σιγαση") || has("mute") || "χωρις ηχο" in q) return Command(Kind.MUTE)

  val hasVolume = listOf("volume", "ενταση", "φωνη", "ηχο", "φωνα").any { it in tokens }
  if (!hasVolume) return null

  val n = tokens.firstNotNullOfOrNull { token -> token.toIntOrNull()?.takeIf { it in 0..10 } ?: numbers[token] }
  val up = listOf("ανεβασε", "αυξησε", "δυναμωσε", "volume up", "πιο δυνατα").any { it in q }
  val down = listOf("κατεβασε", "χαμηλωσε", "μειωσε", "volume down", "πιο χαμηλα", "πιο σιγα").any { it in q }
  val max = listOf("τερμα", "maximum", "max", "στο φουλ").any { it in q }
  if (max) return Command(Kind.SET, 10)
  if (up) return Command(Kind.UP, n ?: 1)
  if (down) return Command(Kind.DOWN, n ?: 1)
  if (n != null && (has("βαλε") || has("ρυθμισε") || "στο " in q || "σε " in q || hasVolume)) return Command(Kind.SET, n)
  return null
 }

 fun target(current: Int, command: Command): Int = when (command.kind) {
  Kind.UP -> current + (command.amount ?: 1)
  Kind.DOWN -> current - (command.amount ?: 1)
  Kind.SET -> command.amount ?: current
  Kind.MUTE -> 0
  Kind.UNMUTE -> 5
 }.coerceIn(0, 10)
}
