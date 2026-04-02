package de.mm20.launcher2.data.customattrs

import de.mm20.launcher2.database.entities.CustomAttributeEntity
import de.mm20.launcher2.ktx.jsonObjectOf
import kotlinx.serialization.Serializable
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDateTime

@Serializable
enum class FocusClassification {
    Essential,
    Normal,
    Distracting,
    BlockedNow,
}

@Serializable
enum class FocusSchedulePreset {
    None,
    Evenings,
    Weekends,
    EveningsAndWeekends,
}

@Serializable
data class FocusBlockRule(
    val daysMask: Int = EVERY_DAY_MASK,
    val startMinutes: Int = EVENING_START_MINUTES,
    val endMinutes: Int = MORNING_END_MINUTES,
    val enabled: Boolean = false,
) {
    fun isActive(now: LocalDateTime): Boolean {
        if (!enabled) return false
        val dayBit = 1 shl (now.dayOfWeek.value - 1)
        if ((daysMask and dayBit) == 0) return false
        val currentMinutes = now.hour * 60 + now.minute
        return if (startMinutes == endMinutes) {
            false
        } else if (startMinutes < endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    companion object {
        const val EVERY_DAY_MASK = 0b1111111
        const val EVERY_WEEKDAY_MASK = 0b0011111
        const val WEEKEND_MASK = 0b1100000
        const val EVENING_START_MINUTES = 18 * 60
        const val MORNING_END_MINUTES = 8 * 60
    }
}

data class FocusProfile(
    val classification: FocusClassification = FocusClassification.Normal,
    val launchDelaySeconds: Int = 10,
    val sessionDurationMinutes: Int = 15,
    val schedulePreset: FocusSchedulePreset = FocusSchedulePreset.None,
    val temporaryUnlockUntilMillis: Long = 0L,
    val dailyBudgetMinutes: Int = 0,
    val hardBlockRules: List<FocusBlockRule> = emptyList(),
    val recurrenceFrictionEnabled: Boolean = false,
    val recurrenceWindowMinutes: Int = 15,
    val recurrenceStepDelaySeconds: Int = 3,
    val recurrenceMaxDelaySeconds: Int = 30,
    val lockDuringFocusSession: Boolean = true,
    val allowEmergencyBypass: Boolean = true,
) : CustomAttribute {
    override fun toDatabaseEntity(key: String): CustomAttributeEntity {
        return CustomAttributeEntity(
            key = key,
            type = CustomAttributeType.Focus.value,
            value = jsonObjectOf(
                "classification" to classification.name,
                "launch_delay_seconds" to launchDelaySeconds,
                "session_duration_minutes" to sessionDurationMinutes,
                "schedule_preset" to schedulePreset.name,
                "temporary_unlock_until_millis" to temporaryUnlockUntilMillis,
                "daily_budget_minutes" to dailyBudgetMinutes,
                "hard_block_rules" to JSONArray().apply {
                    hardBlockRules.forEach { rule ->
                        put(
                            jsonObjectOf(
                                "days_mask" to rule.daysMask,
                                "start_minutes" to rule.startMinutes,
                                "end_minutes" to rule.endMinutes,
                                "enabled" to rule.enabled,
                            )
                        )
                    }
                },
                "recurrence_friction_enabled" to recurrenceFrictionEnabled,
                "recurrence_window_minutes" to recurrenceWindowMinutes,
                "recurrence_step_delay_seconds" to recurrenceStepDelaySeconds,
                "recurrence_max_delay_seconds" to recurrenceMaxDelaySeconds,
                "lock_during_focus_session" to lockDuringFocusSession,
                "allow_emergency_bypass" to allowEmergencyBypass,
            ).toString(),
        )
    }

    fun withTemporaryUnlockUntil(untilMillis: Long): FocusProfile {
        return copy(temporaryUnlockUntilMillis = untilMillis)
    }

    fun clearTemporaryUnlock(): FocusProfile {
        return copy(temporaryUnlockUntilMillis = 0L)
    }

    fun hasTemporaryUnlock(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return temporaryUnlockUntilMillis > nowMillis
    }

    fun isScheduledBlockActive(now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (hardBlockRules.any { it.isActive(now) }) return true
        val minutes = now.hour * 60 + now.minute
        val isWeekend = now.dayOfWeek == DayOfWeek.SATURDAY || now.dayOfWeek == DayOfWeek.SUNDAY
        val isEveningWindow = minutes >= EVENING_START_MINUTES || minutes < MORNING_END_MINUTES
        return when (schedulePreset) {
            FocusSchedulePreset.None -> false
            FocusSchedulePreset.Evenings -> isEveningWindow
            FocusSchedulePreset.Weekends -> isWeekend
            FocusSchedulePreset.EveningsAndWeekends -> isWeekend || isEveningWindow
        }
    }

    fun requiresGate(now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (hasTemporaryUnlock()) return false
        return classification == FocusClassification.Distracting ||
            classification == FocusClassification.BlockedNow ||
            isScheduledBlockActive(now)
    }

    fun shouldHideFromBrowse(focusSessionActive: Boolean = false): Boolean {
        if (hasTemporaryUnlock()) return false
        return classification == FocusClassification.Distracting ||
            classification == FocusClassification.BlockedNow ||
            (focusSessionActive && lockDuringFocusSession && classification != FocusClassification.Essential)
    }

    companion object {
        private const val EVENING_START_MINUTES = 18 * 60
        private const val MORNING_END_MINUTES = 8 * 60

        fun fromDatabaseEntity(entity: CustomAttributeEntity): FocusProfile {
            val payload = JSONObject(entity.value)
            val rules = payload.optJSONArray("hard_block_rules")?.let { array ->
                buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        add(
                            FocusBlockRule(
                                daysMask = item.optInt("days_mask", FocusBlockRule.EVERY_DAY_MASK),
                                startMinutes = item.optInt("start_minutes", EVENING_START_MINUTES).coerceIn(0, 23 * 60 + 59),
                                endMinutes = item.optInt("end_minutes", MORNING_END_MINUTES).coerceIn(0, 23 * 60 + 59),
                                enabled = item.optBoolean("enabled", true),
                            )
                        )
                    }
                }
            } ?: emptyList()
            val launchDelaySeconds = payload.optInt("launch_delay_seconds", 10).coerceIn(0, 15)
            return FocusProfile(
                classification = payload.optString("classification")
                    .takeIf { it.isNotBlank() }
                    ?.let { value -> FocusClassification.entries.firstOrNull { it.name == value } }
                    ?: FocusClassification.Normal,
                launchDelaySeconds = launchDelaySeconds,
                sessionDurationMinutes = payload.optInt("session_duration_minutes", 15).coerceIn(1, 120),
                schedulePreset = payload.optString("schedule_preset")
                    .takeIf { it.isNotBlank() }
                    ?.let { value -> FocusSchedulePreset.entries.firstOrNull { it.name == value } }
                    ?: FocusSchedulePreset.None,
                temporaryUnlockUntilMillis = payload.optLong("temporary_unlock_until_millis", 0L),
                dailyBudgetMinutes = payload.optInt("daily_budget_minutes", 0).coerceAtLeast(0),
                hardBlockRules = rules,
                recurrenceFrictionEnabled = payload.optBoolean("recurrence_friction_enabled", false),
                recurrenceWindowMinutes = payload.optInt("recurrence_window_minutes", 15).coerceIn(1, 240),
                recurrenceStepDelaySeconds = payload.optInt("recurrence_step_delay_seconds", 3).coerceIn(1, 30),
                recurrenceMaxDelaySeconds = payload.optInt("recurrence_max_delay_seconds", 30).coerceIn(launchDelaySeconds, 120),
                lockDuringFocusSession = payload.optBoolean("lock_during_focus_session", true),
                allowEmergencyBypass = payload.optBoolean("allow_emergency_bypass", true),
            )
        }
    }
}
