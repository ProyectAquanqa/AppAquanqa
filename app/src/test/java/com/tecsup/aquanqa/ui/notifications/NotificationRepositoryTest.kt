package com.tecsup.aquanqa.ui.notifications

import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.model.NotificationType
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.*

class NotificationRepositoryTest {

    private lateinit var repository: NotificationRepository

    @Before
    fun setup() {
        repository = NotificationRepository()
    }

    @Test
    fun `getNotifications should return success with mock data`() = runBlocking {
        // When
        val result = repository.getNotifications()
        
        // Then
        assert(result is Result.Success)
        val notifications = (result as Result.Success).data
        assert(notifications.isNotEmpty())
        assert(notifications.any { it.title == "New post from Olivia" })
    }

    @Test
    fun `groupNotificationsByDate should group notifications correctly`() {
        // Given
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.timeInMillis
        
        val notifications = listOf(
            com.tecsup.aquanqa.data.model.Notification(
                id = "1",
                title = "Today notification",
                authorName = "Author 1",
                authorImageUrl = null,
                timestamp = today,
                type = NotificationType.NEW_POST,
                isRead = false
            ),
            com.tecsup.aquanqa.data.model.Notification(
                id = "2",
                title = "Yesterday notification",
                authorName = "Author 2",
                authorImageUrl = null,
                timestamp = yesterday,
                type = NotificationType.COMMENT,
                isRead = false
            )
        )
        
        // When
        val grouped = repository.groupNotificationsByDate(notifications)
        
        // Then
        assert(grouped.size == 2)
        assert(grouped[0].dateLabel == "Hoy")
        assert(grouped[1].dateLabel == "Ayer")
        assert(grouped[0].notifications.size == 1)
        assert(grouped[1].notifications.size == 1)
    }

    @Test
    fun `formatDateLabel should return correct Spanish format`() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JULY, 20) // 20 de julio de 2024
        val timestamp = calendar.timeInMillis
        
        // When
        val result = repository.formatDateLabel(timestamp)
        
        // Then
        assert(result.contains("20"))
        assert(result.contains("julio"))
    }
}