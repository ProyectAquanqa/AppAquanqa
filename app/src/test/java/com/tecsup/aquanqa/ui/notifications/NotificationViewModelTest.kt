package com.tecsup.aquanqa.ui.notifications

import com.tecsup.aquanqa.data.model.Notification
import com.tecsup.aquanqa.data.model.NotificationType
import org.junit.Before
import org.junit.Test

class NotificationViewModelTest {

    private lateinit var repository: NotificationRepository
    private lateinit var viewModel: NotificationViewModel

    @Before
    fun setup() {
        repository = NotificationRepository()
        viewModel = NotificationViewModel(repository)
    }

    @Test
    fun `formatNotificationTimeSpanish should return correct format for minutes`() {
        val now = System.currentTimeMillis()
        val timestamp = now - (30 * 60 * 1000) // 30 minutos atrás
        
        val result = viewModel.formatNotificationTimeSpanish(timestamp)
        
        assert(result == "hace 30 minutos")
    }

    @Test
    fun `formatNotificationTimeSpanish should return correct format for hours`() {
        val now = System.currentTimeMillis()
        val timestamp = now - (2 * 60 * 60 * 1000) // 2 horas atrás
        
        val result = viewModel.formatNotificationTimeSpanish(timestamp)
        
        assert(result == "hace 2 horas")
    }

    @Test
    fun `formatNotificationTimeSpanish should return ayer for yesterday`() {
        val now = System.currentTimeMillis()
        val timestamp = now - (24 * 60 * 60 * 1000) // 1 día atrás
        
        val result = viewModel.formatNotificationTimeSpanish(timestamp)
        
        assert(result == "ayer")
    }
}