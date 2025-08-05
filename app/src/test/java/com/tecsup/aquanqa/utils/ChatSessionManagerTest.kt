package com.tecsup.aquanqa.utils

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.tecsup.aquanqa.data.model.chatbot.Message
import com.tecsup.aquanqa.data.model.chatbot.ChatItem
import com.tecsup.aquanqa.data.model.chatbot.Sender
import com.tecsup.aquanqa.data.repository.ChatbotRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.junit.Assert.*

/**
 * Test unitario para verificar la optimización de memoria del ChatSessionManager.
 */
class ChatSessionManagerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockRepository: ChatbotRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        // Limpiar el estado del singleton antes de cada test
        ChatSessionManager.clearSession()
    }

    @Test
    fun `memory optimization should trigger when limit is exceeded`() {
        // Given: Inicializar sesión
        ChatSessionManager.initializeSession(mockRepository)
        
        // When: Añadir muchos mensajes (más del límite)
        repeat(60) { index ->
            ChatSessionManager.addUserMessage("Mensaje de prueba $index")
            ChatSessionManager.addBotMessage("Respuesta de prueba $index")
        }
        
        // Then: Verificar que la optimización se activó
        val stats = ChatSessionManager.getMemoryStats()
        assertTrue("La lista debería haberse optimizado", stats.totalItems <= 50)
        assertTrue("Debería mantener algunos mensajes", stats.totalItems > 0)
    }

    @Test
    fun `welcome message should be preserved during memory optimization`() {
        // Given: Inicializar sesión
        ChatSessionManager.initializeSession(mockRepository)
        
        // When: Añadir muchos mensajes
        repeat(60) { index ->
            ChatSessionManager.addUserMessage("Mensaje $index")
        }
        
        // Then: El mensaje de bienvenida debería seguir presente
        val currentMessages = ChatSessionManager.getCurrentMessages()
        val welcomeMessage = currentMessages.find { item ->
            item is ChatItem.MessageItem && 
            item.message.text.contains("¡Hola! Soy AquaBot")
        }
        
        assertNotNull("El mensaje de bienvenida debería preservarse", welcomeMessage)
    }

    @Test
    fun `memory stats should provide accurate information`() {
        // Given: Inicializar sesión
        ChatSessionManager.initializeSession(mockRepository)
        
        // When: Añadir algunos mensajes
        ChatSessionManager.addUserMessage("Test message 1")
        ChatSessionManager.addBotMessage("Bot response 1")
        
        // Then: Las estadísticas deberían ser precisas
        val stats = ChatSessionManager.getMemoryStats()
        assertTrue("Debería tener mensajes", stats.messageItems > 0)
        assertTrue("Total debería ser mayor que 0", stats.totalItems > 0)
        assertEquals("Total debería ser suma de partes", 
            stats.totalItems, 
            stats.messageItems + stats.suggestionItems + stats.headerItems)
    }
}