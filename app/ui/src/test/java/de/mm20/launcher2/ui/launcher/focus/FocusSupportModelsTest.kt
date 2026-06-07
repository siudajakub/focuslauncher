package de.mm20.launcher2.ui.launcher.focus

import de.mm20.launcher2.preferences.FocusHabit
import de.mm20.launcher2.preferences.FocusResumeContext
import de.mm20.launcher2.preferences.ScheduleDockMapping
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.UserHandle
import de.mm20.launcher2.search.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId

class FocusSupportModelsTest {

    @Test
    fun `normalizeScheduleEventName trims lowercases and collapses spaces`() {
        assertEquals(
            "deep work",
            normalizeScheduleEventName("  Deep   Work  "),
        )
    }

    @Test
    fun `resolveDailyScheduleSnapshot returns current and next blocks`() {
        val now = dateTime(2026, 4, 2, 10, 30)
        val snapshot = resolveDailyScheduleSnapshot(
            events = listOf(
                scheduleEvent("Morning", dateTime(2026, 4, 2, 8, 0), dateTime(2026, 4, 2, 10, 0)),
                scheduleEvent("Writing", dateTime(2026, 4, 2, 10, 0), dateTime(2026, 4, 2, 12, 0)),
                scheduleEvent("Admin", dateTime(2026, 4, 2, 13, 0), dateTime(2026, 4, 2, 14, 0)),
            ),
            nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )

        assertEquals("Writing", snapshot.currentBlock?.label)
        assertEquals("Admin", snapshot.nextBlock?.label)
        assertNull(snapshot.upcomingBlock)
        assertEquals(90, snapshot.minutesUntilCurrentBlockEnds)
    }

    @Test
    fun `resolveDailyScheduleSnapshot returns upcoming block when nothing active`() {
        val now = dateTime(2026, 4, 2, 7, 30)
        val snapshot = resolveDailyScheduleSnapshot(
            events = listOf(
                scheduleEvent("Writing", dateTime(2026, 4, 2, 10, 0), dateTime(2026, 4, 2, 12, 0)),
                scheduleEvent("Admin", dateTime(2026, 4, 2, 13, 0), dateTime(2026, 4, 2, 14, 0)),
            ),
            nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )

        assertNull(snapshot.currentBlock)
        assertEquals("Writing", snapshot.upcomingBlock?.label)
        assertEquals(150, snapshot.minutesUntilUpcomingBlockStarts)
    }

    @Test
    fun `resolveHabitStatuses marks overdue and incomplete habits`() {
        val today = LocalDate.of(2026, Month.APRIL, 2)
        val now = LocalDateTime.of(2026, Month.APRIL, 2, 11, 0)
        val statuses = resolveHabitStatuses(
            habits = listOf(
                FocusHabit(id = "1", title = "Walk", deadlineMinutes = 9 * 60, completedDates = emptySet()),
                FocusHabit(id = "2", title = "Meditate", deadlineMinutes = 12 * 60, completedDates = emptySet()),
                FocusHabit(id = "3", title = "Journal", deadlineMinutes = 8 * 60, completedDates = setOf(today.toString())),
            ),
            today = today,
            now = now,
        )

        assertTrue(statuses.first { it.habit.id == "1" }.overdue)
        assertFalse(statuses.first { it.habit.id == "2" }.overdue)
        assertTrue(statuses.first { it.habit.id == "3" }.completed)
    }

    @Test
    fun `resolveHabitGate returns first overdue habit title`() {
        val today = LocalDate.of(2026, Month.APRIL, 2)
        val now = LocalDateTime.of(2026, Month.APRIL, 2, 11, 0)
        val gate = resolveHabitGate(
            habits = listOf(
                FocusHabit(id = "1", title = "Walk", deadlineMinutes = 9 * 60, completedDates = emptySet()),
                FocusHabit(id = "2", title = "Meditate", deadlineMinutes = 8 * 60, completedDates = emptySet()),
            ),
            today = today,
            now = now,
        )

        assertTrue(gate.blocked)
        assertEquals("Meditate", gate.primaryOverdueHabitTitle)
        assertEquals(2, gate.overdueCount)
    }

    @Test
    fun `resolveActiveDockApps matches mapping by normalized event name`() {
        val apps = listOf(
            app("pkg.writer", "Writer"),
            app("pkg.mail", "Mail"),
        )

        val resolved = resolveActiveDockApps(
            currentBlock = DailyScheduleBlock("  Deep   Work ", 0L, 1L),
            mappings = listOf(
                ScheduleDockMapping(
                    eventName = "deep work",
                    appKeys = listOf(apps[1].key, apps[0].key),
                )
            ),
            apps = apps,
        )

        assertEquals(listOf("pkg.mail/.Main", "pkg.writer/.Main"), resolved.map { it.key })
    }

    @Test
    fun `prep prompt becomes active only inside lead window`() {
        val prep = resolvePrepCard(
            currentBlock = scheduleEvent("Writing", dateTime(2026, 4, 2, 10, 0), dateTime(2026, 4, 2, 11, 0)),
            nextBlock = scheduleEvent("Admin", dateTime(2026, 4, 2, 11, 0), dateTime(2026, 4, 2, 12, 0)),
            minutesUntilCurrentBlockEnds = 4,
            leadMinutes = 5,
        )

        assertTrue(prep.show)
        assertEquals("Admin", prep.nextBlockLabel)
    }

    @Test
    fun `prep prompt stays hidden outside the lead window`() {
        val prep = resolvePrepCard(
            currentBlock = scheduleEvent("Writing", dateTime(2026, 4, 2, 10, 0), dateTime(2026, 4, 2, 11, 0)),
            nextBlock = scheduleEvent("Admin", dateTime(2026, 4, 2, 11, 0), dateTime(2026, 4, 2, 12, 0)),
            minutesUntilCurrentBlockEnds = 12,
            leadMinutes = 5,
        )

        assertFalse(prep.show)
        assertEquals("Admin", prep.nextBlockLabel)
        assertNull(prep.minutesUntilNextBlock)
    }

    @Test
    fun `stale recovery disappears when block changes`() {
        val state = resolveScheduleAwareResumeCard(
            lastContext = FocusResumeContext(
                taskLabel = "Gmail",
                scheduleBlockLabel = "Writing",
                microStep = "Reply",
                interruptedAtMillis = 100_000L,
            ),
            currentBlockLabel = "Admin",
            nowMillis = 120_000L,
            expiryMillis = 60_000L,
            followsCurrentBlock = true,
        )

        assertFalse(state.show)
    }

    @Test
    fun `recovery stays visible when matching block is resumed and expires after timeout`() {
        val matching = resolveScheduleAwareResumeCard(
            lastContext = FocusResumeContext(
                taskLabel = "Gmail",
                scheduleBlockLabel = "Writing",
                microStep = "Reply",
                interruptedAtMillis = 100_000L,
            ),
            currentBlockLabel = " writing ",
            nowMillis = 120_000L,
            expiryMillis = 60_000L,
            followsCurrentBlock = true,
        )

        assertTrue(matching.show)
        assertEquals(" writing ", matching.taskLabel)
        assertEquals("Reply", matching.microStep)
        assertEquals("Writing", matching.relatedBlockLabel)

        val expired = resolveScheduleAwareResumeCard(
            lastContext = FocusResumeContext(
                taskLabel = "Gmail",
                scheduleBlockLabel = "Writing",
                microStep = "Reply",
                interruptedAtMillis = 100_000L,
            ),
            currentBlockLabel = "Writing",
            nowMillis = 200_001L,
            expiryMillis = 100_000L,
            followsCurrentBlock = true,
        )

        assertFalse(expired.show)
    }

    @Test
    fun `current block guidance prioritizes recovery over prep and now`() {
        val guidance = resolveFocusGuidance(
            currentBlock = scheduleEvent("Writing", dateTime(2026, 4, 2, 10, 0), dateTime(2026, 4, 2, 11, 0)),
            prepState = PrepCardState(show = true, nextBlockLabel = "Admin"),
            resumeState = ResumeCardState(show = true, taskLabel = "Writing", microStep = "Open doc"),
        )

        assertEquals(FocusGuidanceType.Recover, guidance.type)
        assertEquals("Writing", guidance.taskLabel)
    }

    @Test
    fun `prep guidance outranks ready and now`() {
        val guidance = resolveFocusGuidance(
            currentBlock = scheduleEvent("Writing", dateTime(2026, 4, 2, 10, 0), dateTime(2026, 4, 2, 11, 0)),
            prepState = PrepCardState(show = true, nextBlockLabel = "Admin", minutesUntilNextBlock = 4),
            resumeState = ResumeCardState(show = false),
            blockPlan = FocusBlockPlan(
                date = "2026-04-02",
                normalizedBlockLabel = "writing",
                blockLabel = "Writing",
                tinyStep = "Open the draft",
            ),
            blockReadiness = FocusBlockReadiness.Ready,
        )

        assertEquals(FocusGuidanceType.Prep, guidance.type)
        assertEquals("Admin", guidance.nextBlockLabel)
    }

    @Test
    fun `guidance becomes ready when block plan has a tiny step and no stronger guidance exists`() {
        val guidance = resolveFocusGuidance(
            currentBlock = scheduleEvent("Writing", dateTime(2026, 4, 2, 10, 0), dateTime(2026, 4, 2, 11, 0)),
            prepState = PrepCardState(show = false),
            resumeState = ResumeCardState(show = false),
            blockPlan = FocusBlockPlan(
                date = "2026-04-02",
                normalizedBlockLabel = "writing",
                blockLabel = "Writing",
                tinyStep = "Open the draft",
            ),
            blockReadiness = FocusBlockReadiness.Ready,
        )

        assertEquals(FocusGuidanceType.Ready, guidance.type)
        assertEquals("Open the draft", guidance.suggestedMicroStep)
    }

    @Test
    fun `done block guidance never resolves to ready`() {
        val guidance = resolveFocusGuidance(
            currentBlock = scheduleEvent("Writing", dateTime(2026, 4, 2, 10, 0), dateTime(2026, 4, 2, 11, 0)),
            prepState = PrepCardState(show = false),
            resumeState = ResumeCardState(show = false),
            blockPlan = FocusBlockPlan(
                date = "2026-04-02",
                normalizedBlockLabel = "writing",
                blockLabel = "Writing",
                doneForBlock = true,
            ),
            blockReadiness = FocusBlockReadiness.DoneForBlock,
        )

        assertEquals(FocusGuidanceType.Now, guidance.type)
        assertTrue(guidance.completedForBlock)
    }

    @Test
    fun `now guidance appears when no stronger guidance exists`() {
        val guidance = resolveFocusGuidance(
            currentBlock = scheduleEvent("Writing", dateTime(2026, 4, 2, 10, 0), dateTime(2026, 4, 2, 11, 0)),
            prepState = PrepCardState(show = false),
            resumeState = ResumeCardState(show = false),
        )

        assertEquals(FocusGuidanceType.Now, guidance.type)
        assertEquals("Writing", guidance.blockLabel)
    }

    @Test
    fun `empty state resolves to none`() {
        val guidance = resolveFocusGuidance(
            currentBlock = null,
            prepState = PrepCardState(show = false),
            resumeState = ResumeCardState(show = false),
        )

        assertEquals(FocusGuidanceType.None, guidance.type)
    }

    @Test
    fun `block aware session length is capped by remaining block time and configured max`() {
        val minutes = resolveBlockAwareSessionMinutes(
            defaultMinutes = 30,
            capMinutes = 15,
            minutesUntilCurrentBlockEnds = 12,
            enabled = true,
        )

        assertEquals(12, minutes)
    }

    @Test
    fun `block aware session length respects configured max when block remains longer`() {
        val minutes = resolveBlockAwareSessionMinutes(
            defaultMinutes = 30,
            capMinutes = 15,
            minutesUntilCurrentBlockEnds = 40,
            enabled = true,
        )

        assertEquals(15, minutes)
    }

    @Test
    fun `block aware session length falls back to capped default when disabled`() {
        val minutes = resolveBlockAwareSessionMinutes(
            defaultMinutes = 30,
            capMinutes = 15,
            minutesUntilCurrentBlockEnds = 2,
            enabled = false,
        )

        assertEquals(15, minutes)
    }

    private fun dateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): LocalDateTime {
        return LocalDateTime.of(year, month, day, hour, minute)
    }

    private fun scheduleEvent(
        label: String,
        start: LocalDateTime,
        end: LocalDateTime,
    ): DailyScheduleBlock {
        return DailyScheduleBlock(
            label = label,
            startTimeMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            endTimeMillis = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
    }

    private fun app(packageName: String, label: String): Application {
        return FakeApplication(packageName, label)
    }

    private class FakeApplication(
        packageName: String,
        override val label: String,
    ) : Application {
        override val key: String = "$packageName/.Main"
        override val domain: String = "app"
        override val componentName: ComponentName = ComponentName(packageName, ".Main")
        override val isSuspended: Boolean = false
        override val user: UserHandle
            get() = throw UnsupportedOperationException()
        override val versionName: String? = null
        override val canUninstall: Boolean = false
        override val canShareApk: Boolean = false
        override val labelOverride: String? = null
        override fun overrideLabel(label: String) = this
        override fun launch(context: Context, options: Bundle?) = false
        override fun uninstall(context: Context) = Unit
        override fun openAppDetails(context: Context) = Unit
        override fun getSerializer() = throw UnsupportedOperationException()
    }
}
