package de.mm20.launcher2.weather.breezy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.mm20.launcher2.crashreporter.CrashReporter
import de.mm20.launcher2.preferences.weather.WeatherSettings
import de.mm20.launcher2.serialization.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.zip.GZIPInputStream
import de.mm20.launcher2.ktx.readTextLimited

class BreezyWeatherReceiver : BroadcastReceiver(), KoinComponent {

    private val settings: WeatherSettings by inject()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        scope.launch {
            val provider = settings.providerId.first()
            if (provider != BreezyWeatherProvider.Id) {
                return@launch
            }
            val weatherData = if (intent.hasExtra("WeatherGz")) {
                val gz = intent.getByteArrayExtra("WeatherGz") ?: return@launch


                val json = try {
                    val inputStream = GZIPInputStream(gz.inputStream())
                    inputStream.use { it.readTextLimited(2 * 1024 * 1024) }
                } catch (e: Exception) {
                    CrashReporter.logException(e)
                    return@launch
                }

                try {
                    Json.Lenient.decodeFromString<List<BreezyWeatherData>>(json).firstOrNull()
                } catch (e: SerializationException) {
                    CrashReporter.logException(e)
                    return@launch
                }
            } else if (intent.hasExtra("WeatherJson")) {
                val json = intent.getStringExtra("WeatherJson") ?: return@launch
                try {
                    Json.Lenient.decodeFromString<BreezyWeatherData>(json)
                } catch (e: SerializationException) {
                    CrashReporter.logException(e)
                    return@launch
                }
            } else {
                null
            }

            if (weatherData == null) {
                Log.e("BreezyWeatherReceiver", "Broadcast was received but it did not contain weather data")
                return@launch
            }

            BreezyWeatherProvider(context).pushWeatherData(weatherData)

        }
    }
}