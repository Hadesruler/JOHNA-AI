package com.johna.assistant

/** Immutable UI models; values are downloaded from Open-Meteo, never inferred by the LLM. */
data class ForecastHour(val isoTime: String, val temperature: Int, val precipitationProbability: Int, val weatherCode: Int)
data class ForecastDay(val date: String, val minTemperature: Int, val maxTemperature: Int, val precipitationProbability: Int, val weatherCode: Int, val sunrise: String, val sunset: String)
data class WeatherForecast(
 val city: String = "Αθήνα", val updated: String = "", val currentTemperature: Int = 0,
 val feelsLike: Int = 0, val relativeHumidity: Int = 0, val windKmh: Int = 0,
 val windGustKmh: Int = 0, val pressureHpa: Int = 0, val uvIndex: Double = 0.0,
 val currentCode: Int = 0, val hours: List<ForecastHour> = emptyList(),
 val days: List<ForecastDay> = emptyList()
)
