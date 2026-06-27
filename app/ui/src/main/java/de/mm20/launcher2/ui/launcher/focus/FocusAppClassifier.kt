package de.mm20.launcher2.ui.launcher.focus

import de.mm20.launcher2.preferences.ui.SearchUiSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FocusAppClassifier : KoinComponent {
    private val searchUiSettings: SearchUiSettings by inject()

    fun classify(key: String): Flow<FocusAppType> {
        return combine(
            searchUiSettings.focusEssentialAppKeys,
            searchUiSettings.focusDistractingAppKeys,
        ) { essentials, distracting ->
            when {
                key in essentials -> FocusAppType.Essential
                key in distracting -> FocusAppType.Distracting
                else -> FocusAppType.Normal
            }
        }
    }

    fun classify(keys: List<String>): Flow<Map<String, FocusAppType>> {
        return combine(
            searchUiSettings.focusEssentialAppKeys,
            searchUiSettings.focusDistractingAppKeys,
        ) { essentials, distracting ->
            keys.associateWith { key ->
                when {
                    key in essentials -> FocusAppType.Essential
                    key in distracting -> FocusAppType.Distracting
                    else -> FocusAppType.Normal
                }
            }
        }
    }

    suspend fun classifyNow(key: String): FocusAppType {
        val essentials = searchUiSettings.focusEssentialAppKeys.first()
        val distracting = searchUiSettings.focusDistractingAppKeys.first()
        return classifyWith(key, essentials, distracting)
    }

    /**
     * Pure, in-memory classification against already-loaded key sets. Use this to classify many
     * keys without reading DataStore once per key.
     */
    fun classifyWith(
        key: String,
        essentialKeys: Collection<String>,
        distractingKeys: Collection<String>,
    ): FocusAppType {
        return when {
            key in essentialKeys -> FocusAppType.Essential
            key in distractingKeys -> FocusAppType.Distracting
            else -> FocusAppType.Normal
        }
    }

    fun essentialKeys(): Flow<Set<String>> = searchUiSettings.focusEssentialAppKeys.map { it.toSet() }
    fun distractingKeys(): Flow<Set<String>> = searchUiSettings.focusDistractingAppKeys.map { it.toSet() }
}
