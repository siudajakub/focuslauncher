package de.mm20.launcher2.ui.settings.focussystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.AppOpsManager
import android.content.Intent
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import de.mm20.launcher2.calendar.CalendarRepository
import de.mm20.launcher2.calendar.providers.CalendarList
import de.mm20.launcher2.preferences.FocusAdaptiveFrictionMode
import de.mm20.launcher2.preferences.ui.SearchUiSettings
import de.mm20.launcher2.ui.launcher.focus.TimeBlindnessService
import de.mm20.launcher2.ui.launcher.focus.shouldShowFocusQuickStart
import de.mm20.launcher2.ui.settings.SettingsActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FocusSystemSettingsScreenVM : ViewModel(), KoinComponent {
    private val context: Context by inject()
    private val searchUiSettings: SearchUiSettings by inject()
    private val calendarRepository: CalendarRepository by inject()
    private val permissionsManager: de.mm20.launcher2.permissions.PermissionsManager by inject()

    val focusModeEnabled = searchUiSettings.focusModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val hideDistractingApps = searchUiSettings.focusHideDistractingApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val focusEnableDnd = searchUiSettings.focusEnableDnd
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val fadeDistractingApps = searchUiSettings.focusFadeDistractingApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val noIconsMode = searchUiSettings.focusNoIconsMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val commuteModeEnabled = searchUiSettings.focusCommuteModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val atAGlanceEnabled = searchUiSettings.focusAtAGlanceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val reviewSuggestionsEnabled = searchUiSettings.focusReviewSuggestionsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val environmentContextEnabled = searchUiSettings.focusEnvironmentContextEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val environmentChargingContextEnabled = searchUiSettings.focusEnvironmentChargingContextEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val focusOneSecEnabled = searchUiSettings.focusOneSecEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val focusMicroDelaysEnabled = searchUiSettings.focusMicroDelaysEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val focusDistractingDailyLaunchLimit = searchUiSettings.focusDistractingDailyLaunchLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    val focusTimeBlindnessRemindersEnabled = searchUiSettings.focusTimeBlindnessRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val focusTimeBlindnessIntervalMinutes = searchUiSettings.focusTimeBlindnessIntervalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 15)

    // Time blindness reminders rely on Usage Access to know which app is in the
    // foreground. Without it the service can never detect a distracting app, so the
    // UI surfaces this state and lets the user grant it. Refreshed on screen resume
    // because the grant happens in system settings, outside this process.
    val usageAccessGranted = MutableStateFlow(hasUsageAccess())

    fun refreshUsageAccess() {
        usageAccessGranted.value = hasUsageAccess()
    }

    @Suppress("DEPRECATION")
    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService<AppOpsManager>() ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    val focusTodoistApiToken = searchUiSettings.focusTodoistApiToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    val environmentExplainabilityEnabled = searchUiSettings.focusEnvironmentExplainabilityEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val dailyScheduleEnabled = searchUiSettings.focusDailyScheduleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val applyToPersonalProfile = searchUiSettings.focusApplyToPersonalProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val applyToWorkProfile = searchUiSettings.focusApplyToWorkProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val applyToPrivateProfile = searchUiSettings.focusApplyToPrivateProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    val hasManageProfilesPermission = permissionsManager.hasPermission(de.mm20.launcher2.permissions.PermissionGroup.ManageProfiles)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun requestManageProfilesPermission(context: androidx.appcompat.app.AppCompatActivity) {
        permissionsManager.requestPermission(context, de.mm20.launcher2.permissions.PermissionGroup.ManageProfiles)
    }

    val hasNotificationPolicyPermission = permissionsManager.hasPermission(de.mm20.launcher2.permissions.PermissionGroup.NotificationPolicy)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun requestNotificationPolicyPermission(context: androidx.appcompat.app.AppCompatActivity) {
        permissionsManager.requestPermission(context, de.mm20.launcher2.permissions.PermissionGroup.NotificationPolicy)
    }

    val adaptiveFrictionMode = searchUiSettings.focusAdaptiveFrictionMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), FocusAdaptiveFrictionMode.Auto)

    val calendars = calendarRepository.getCalendars()
        .map { calendars ->
            calendars.sortedWith(
                compareBy<CalendarList>({ it.name.lowercase() }, { it.owner ?: "" }, { it.providerId })
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val selectedDailyScheduleCalendar = combine(
        calendars,
        searchUiSettings.focusDailyScheduleCalendarId,
    ) { calendars, selectedId ->
        calendars.firstOrNull { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val shouldPromoteQuickStart = combine(
        searchUiSettings.focusModeEnabled,
        searchUiSettings.focusEssentialAppKeys,
        searchUiSettings.focusDistractingAppKeys,
        searchUiSettings.focusDailyScheduleEnabled,
        searchUiSettings.focusHabitsEnabled,
    ) { focusModeEnabled, essentialKeys, distractingKeys, dailyScheduleEnabled, habitsEnabled ->
        shouldShowFocusQuickStart(
            focusModeEnabled = focusModeEnabled,
            essentialCount = essentialKeys.size,
            distractingCount = distractingKeys.size,
            dailyScheduleEnabled = dailyScheduleEnabled,
            habitsEnabled = habitsEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    fun setFocusModeEnabled(enabled: Boolean) {
        searchUiSettings.setFocusModeEnabled(enabled)
    }

    fun setHideDistractingApps(enabled: Boolean) {
        searchUiSettings.setFocusHideDistractingApps(enabled)
    }

    fun setFocusEnableDnd(enabled: Boolean) {
        searchUiSettings.setFocusEnableDnd(enabled)
    }

    fun setFadeDistractingApps(enabled: Boolean) {
        searchUiSettings.setFocusFadeDistractingApps(enabled)
    }

    fun setNoIconsMode(enabled: Boolean) {
        searchUiSettings.setFocusNoIconsMode(enabled)
    }

    fun setCommuteModeEnabled(enabled: Boolean) {
        searchUiSettings.setFocusCommuteModeEnabled(enabled)
    }

    fun setAtAGlanceEnabled(enabled: Boolean) {
        searchUiSettings.setFocusAtAGlanceEnabled(enabled)
    }

    fun setReviewSuggestionsEnabled(enabled: Boolean) {
        searchUiSettings.setFocusReviewSuggestionsEnabled(enabled)
    }

    fun setApplyToPersonalProfile(enabled: Boolean) {
        searchUiSettings.setFocusApplyToPersonalProfile(enabled)
    }

    fun setApplyToWorkProfile(enabled: Boolean) {
        searchUiSettings.setFocusApplyToWorkProfile(enabled)
    }

    fun setApplyToPrivateProfile(enabled: Boolean) {
        searchUiSettings.setFocusApplyToPrivateProfile(enabled)
    }

    fun setAdaptiveFrictionMode(mode: FocusAdaptiveFrictionMode) {
        searchUiSettings.setFocusAdaptiveFrictionMode(mode)
    }

    fun setEnvironmentContextEnabled(enabled: Boolean) {
        searchUiSettings.setFocusEnvironmentContextEnabled(enabled)
    }

    fun setEnvironmentChargingContextEnabled(enabled: Boolean) {
        searchUiSettings.setFocusEnvironmentChargingContextEnabled(enabled)
    }

    fun setEnvironmentExplainabilityEnabled(enabled: Boolean) {
        searchUiSettings.setFocusEnvironmentExplainabilityEnabled(enabled)
    }

    fun setFocusOneSecEnabled(enabled: Boolean) {
        searchUiSettings.setFocusOneSecEnabled(enabled)
    }

    fun setFocusMicroDelaysEnabled(enabled: Boolean) {
        searchUiSettings.setFocusMicroDelaysEnabled(enabled)
    }

    fun setFocusDistractingDailyLaunchLimit(limit: Int) {
        searchUiSettings.setFocusDistractingDailyLaunchLimit(limit)
    }

    fun setFocusTimeBlindnessRemindersEnabled(enabled: Boolean) {
        searchUiSettings.setFocusTimeBlindnessRemindersEnabled(enabled)
        // Start/stop the foreground poller immediately. Previously the service was
        // only ever started by the boot receiver, so enabling the toggle did nothing
        // until the next reboot.
        val intent = Intent(context, TimeBlindnessService::class.java)
        if (enabled) {
            intent.action = TimeBlindnessService.ACTION_START
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }

    fun setFocusTimeBlindnessIntervalMinutes(minutes: Int) {
        searchUiSettings.setFocusTimeBlindnessIntervalMinutes(minutes)
    }

    fun setFocusTodoistApiToken(token: String) {
        searchUiSettings.setFocusTodoistApiToken(token)
    }

    fun applyBalancedPreset() {
        searchUiSettings.setFocusOneSecEnabled(false)
        searchUiSettings.setFocusModeEnabled(true)
        searchUiSettings.setFocusHideDistractingApps(true)
        searchUiSettings.setFocusDefaultDelaySeconds(4)
        searchUiSettings.setFocusDefaultSessionMinutes(15)
        searchUiSettings.setFocusEnableDnd(true)
        searchUiSettings.setFocusStartRitualEnabled(false)
        searchUiSettings.setFocusMicroStepPromptEnabled(true)
        searchUiSettings.setFocusRecoveryEnabled(true)
        searchUiSettings.setFocusRecoveryResumeTimeoutMinutes(15)
        searchUiSettings.setFocusTransitionWarningsEnabled(true)
        searchUiSettings.setFocusTransitionWarningLeadMinutes(10)
        searchUiSettings.setFocusBlockPrepPromptsEnabled(true)
        searchUiSettings.setFocusPrepLeadTimeMinutes(10)
        searchUiSettings.setFocusBlockAwareSessionSizingEnabled(true)
        searchUiSettings.setFocusEscalatingFrictionEnabled(true)
        searchUiSettings.setFocusEscalationWindowMinutes(15)
        searchUiSettings.setFocusEscalationExtraDelaySeconds(5)
        searchUiSettings.setFocusDistractingSessionCapMinutes(15)
        searchUiSettings.setFocusAtAGlanceEnabled(true)
        searchUiSettings.setFocusCommuteModeEnabled(false)
        searchUiSettings.setFocusEnvironmentContextEnabled(true)
        searchUiSettings.setFocusEnvironmentExplainabilityEnabled(true)
        searchUiSettings.setFocusReviewSuggestionsEnabled(true)
    }

    fun applyHardFocusPreset() {
        searchUiSettings.setFocusOneSecEnabled(true)
        searchUiSettings.setFocusModeEnabled(true)
        searchUiSettings.setFocusHideDistractingApps(true)
        searchUiSettings.setFocusDefaultDelaySeconds(10)
        searchUiSettings.setFocusDefaultSessionMinutes(10)
        searchUiSettings.setFocusEnableDnd(true)
        searchUiSettings.setFocusStartRitualEnabled(true)
        searchUiSettings.setFocusMicroStepPromptEnabled(true)
        searchUiSettings.setFocusRecoveryEnabled(true)
        searchUiSettings.setFocusRecoveryResumeTimeoutMinutes(10)
        searchUiSettings.setFocusTransitionWarningsEnabled(true)
        searchUiSettings.setFocusTransitionWarningLeadMinutes(10)
        searchUiSettings.setFocusBlockPrepPromptsEnabled(true)
        searchUiSettings.setFocusPrepLeadTimeMinutes(15)
        searchUiSettings.setFocusBlockAwareSessionSizingEnabled(true)
        searchUiSettings.setFocusEscalatingFrictionEnabled(true)
        searchUiSettings.setFocusEscalationWindowMinutes(20)
        searchUiSettings.setFocusEscalationExtraDelaySeconds(10)
        searchUiSettings.setFocusDistractingSessionCapMinutes(10)
        searchUiSettings.setFocusAtAGlanceEnabled(true)
        searchUiSettings.setFocusCommuteModeEnabled(false)
        searchUiSettings.setFocusEnvironmentContextEnabled(true)
        searchUiSettings.setFocusEnvironmentExplainabilityEnabled(true)
        searchUiSettings.setFocusReviewSuggestionsEnabled(true)
    }

    fun applyMinimalPreset() {
        searchUiSettings.setFocusOneSecEnabled(false)
        searchUiSettings.setFocusModeEnabled(true)
        searchUiSettings.setFocusHideDistractingApps(true)
        searchUiSettings.setFocusDefaultDelaySeconds(2)
        searchUiSettings.setFocusDefaultSessionMinutes(20)
        searchUiSettings.setFocusEnableDnd(false)
        searchUiSettings.setFocusStartRitualEnabled(false)
        searchUiSettings.setFocusMicroStepPromptEnabled(false)
        searchUiSettings.setFocusRecoveryEnabled(true)
        searchUiSettings.setFocusRecoveryResumeTimeoutMinutes(15)
        searchUiSettings.setFocusTransitionWarningsEnabled(false)
        searchUiSettings.setFocusBlockPrepPromptsEnabled(false)
        searchUiSettings.setFocusBlockAwareSessionSizingEnabled(false)
        searchUiSettings.setFocusEscalatingFrictionEnabled(false)
        searchUiSettings.setFocusDistractingSessionCapMinutes(20)
        searchUiSettings.setFocusAtAGlanceEnabled(true)
        searchUiSettings.setFocusCommuteModeEnabled(false)
        searchUiSettings.setFocusEnvironmentContextEnabled(false)
        searchUiSettings.setFocusEnvironmentExplainabilityEnabled(false)
        searchUiSettings.setFocusReviewSuggestionsEnabled(true)
    }
}
