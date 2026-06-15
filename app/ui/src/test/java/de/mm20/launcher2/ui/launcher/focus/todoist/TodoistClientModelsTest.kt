package de.mm20.launcher2.ui.launcher.focus.todoist

import org.junit.Test
import org.junit.Assert.assertEquals
import java.time.LocalDate

class TodoistClientModelsTest {

    @Test
    fun `tasksForDate keeps only tasks due on the plan date`() {
        val planDate = LocalDate.of(2026, 6, 16)
        val tasks = listOf(
            task(id = "tomorrow", dueDate = "2026-06-16"),
            task(id = "timed", dueDate = "2026-06-16T14:00:00"),
            task(id = "today", dueDate = "2026-06-15"),
            task(id = "later", dueDate = "2026-06-17"),
            TodoistTask(id = "unscheduled", content = "No due date"),
        )

        assertEquals(listOf("tomorrow", "timed"), tasksForDate(tasks, planDate).map { it.id })
    }

    @Test
    fun `tasksForDate puts higher priority tasks first`() {
        val planDate = LocalDate.of(2026, 6, 16)
        val tasks = listOf(
            task(id = "normal", dueDate = "2026-06-16", priority = 1),
            task(id = "urgent", dueDate = "2026-06-16", priority = 4),
        )

        assertEquals(listOf("urgent", "normal"), tasksForDate(tasks, planDate).map { it.id })
    }

    private fun task(id: String, dueDate: String, priority: Int = 1) = TodoistTask(
        id = id,
        content = id,
        priority = priority,
        due = DueDate(date = dueDate),
    )
}
