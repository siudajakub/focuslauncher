package de.mm20.launcher2.ui.launcher.focus.todoist

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.io.IOException
import java.time.LocalDate

@Serializable
data class TodoistTask(
    val id: String,
    val content: String,
    val description: String = "",
    val priority: Int = 1,
    val due: DueDate? = null,
    val url: String = "",
    val duration: Duration? = null,
)

@Serializable
data class DueDate(
    val date: String,
    val string: String = "",
    val lang: String = "en",
    @SerialName("is_recurring") val isRecurring: Boolean = false,
    val timezone: String? = null,
)

@Serializable
data class Duration(
    val amount: Int,
    val unit: String,
)

@Serializable
private data class TodoistTaskPage(
    val results: List<TodoistTask> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

sealed interface TodoistTasksResult {
    data class Success(val tasks: List<TodoistTask>) : TodoistTasksResult
    data object InvalidToken : TodoistTasksResult
    data object ConnectionError : TodoistTasksResult
    data object ServiceError : TodoistTasksResult
}

class TodoistClient(
    private val token: String,
    private val client: HttpClient = createHttpClient(),
) : Closeable {

    suspend fun getActiveTasks(): TodoistTasksResult {
        val tasks = mutableListOf<TodoistTask>()
        var cursor: String? = null

        return try {
            do {
                val response = client.get(TASKS_URL) {
                    header("Authorization", "Bearer $token")
                    cursor?.let { parameter("cursor", it) }
                }

                when (response.status) {
                    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                        return TodoistTasksResult.InvalidToken
                    }
                    HttpStatusCode.OK -> Unit
                    else -> return TodoistTasksResult.ServiceError
                }

                val page = response.body<TodoistTaskPage>()
                tasks += page.results
                cursor = page.nextCursor
            } while (cursor != null)

            TodoistTasksResult.Success(tasks)
        } catch (_: IOException) {
            TodoistTasksResult.ConnectionError
        } catch (_: Exception) {
            TodoistTasksResult.ServiceError
        }
    }

    override fun close() {
        client.close()
    }

    companion object {
        private const val TASKS_URL = "https://api.todoist.com/api/v1/tasks"

        private fun createHttpClient() = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }
    }
}

fun tasksForDate(tasks: List<TodoistTask>, date: LocalDate): List<TodoistTask> {
    val isoDate = date.toString()
    return tasks
        .filter { it.due?.date?.take(10) == isoDate }
        .sortedWith(compareByDescending<TodoistTask> { it.priority }.thenBy { it.due?.date })
}
