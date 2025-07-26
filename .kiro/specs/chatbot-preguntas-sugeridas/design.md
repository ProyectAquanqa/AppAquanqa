# Design Document

## Overview

El diseño mejora el sistema de preguntas sugeridas del chatbot existente en la aplicación móvil Aquanqa para garantizar que SIEMPRE se muestren preguntas después de cada respuesta. La arquitectura actual ya tiene una base sólida con MVVM, pero necesita mejoras en la lógica de respaldo y gestión de preguntas frecuentes.

### Current Architecture Analysis

La aplicación ya implementa:
- **MVVM Architecture**: `ChatbotViewModel`, `ChatbotRepository`, `ChatbotFragment`
- **Unified Chat Adapter**: Maneja mensajes y sugerencias en un solo RecyclerView
- **API Integration**: Conexión con backend Django que ya retorna `recommendedQuestions`
- **Sealed Classes**: `ChatItem` para diferentes tipos de elementos en el chat

### Problem Statement

Actualmente, si el backend no retorna preguntas recomendadas, el usuario se queda sin opciones para continuar la conversación. Necesitamos implementar un sistema de respaldo con preguntas frecuentes.

## Architecture

### Enhanced Components

```
┌─────────────────────────────────────────────────────────────┐
│                    ChatbotFragment                          │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                ChatAdapter                              ││
│  │  - MessageItem                                          ││
│  │  - SuggestionHeader                                     ││
│  │  - SuggestionItem                                       ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 ChatbotViewModel                            │
│  - Enhanced suggestion logic                                │
│  - Fallback to frequent questions                           │
│  - Caching mechanism                                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                ChatbotRepository                            │
│  - API calls for responses                                  │
│  - Frequent questions management                            │
│  - Local caching                                            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              FrequentQuestionsManager                       │
│  - Load frequent questions                                  │
│  - Cache management                                         │
│  - Statistics tracking                                      │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. FrequentQuestionsManager

**Purpose**: Gestiona las preguntas más frecuentes como respaldo cuando no hay preguntas específicas.

```kotlin
interface FrequentQuestionsManager {
    suspend fun getFrequentQuestions(): List<RecommendedQuestion>
    suspend fun updateFrequentQuestions(questions: List<RecommendedQuestion>)
    suspend fun trackQuestionUsage(questionId: Int)
    fun getCachedFrequentQuestions(): List<RecommendedQuestion>
}
```

**Key Features**:
- Carga preguntas frecuentes desde API o almacenamiento local
- Mantiene caché en memoria para acceso rápido
- Rastrea estadísticas de uso de preguntas

### 2. Enhanced ChatbotRepository

**New Methods**:
```kotlin
suspend fun getFrequentQuestions(): Result<List<RecommendedQuestion>>
suspend fun trackQuestionClick(questionId: Int): Result<Unit>
```

**Enhanced Logic**:
- Integra `FrequentQuestionsManager`
- Proporciona preguntas de respaldo automáticamente
- Maneja errores de red con datos en caché

### 3. Enhanced ChatbotViewModel

**New Logic**:
```kotlin
private fun processResponse(response: ChatbotResponse) {
    // Add bot response
    addBotMessage(response.answer)
    
    // Always show suggestions
    val suggestions = if (response.recommendedQuestions.isNotEmpty()) {
        response.recommendedQuestions
    } else {
        frequentQuestionsManager.getCachedFrequentQuestions().take(4)
    }
    
    if (suggestions.isNotEmpty()) {
        addSuggestions(suggestions)
    }
}
```

### 4. API Enhancements

**New Endpoints** (Backend):
```
GET /api/chatbot/frequent-questions/
POST /api/chatbot/track-question-usage/
```

**Enhanced Response Handling**:
- Siempre procesar sugerencias después de cada respuesta
- Implementar lógica de respaldo en el cliente

## Data Models

### Enhanced Models

**FrequentQuestionCache**:
```kotlin
data class FrequentQuestionCache(
    val questions: List<RecommendedQuestion>,
    val lastUpdated: Long,
    val expirationTime: Long = 24 * 60 * 60 * 1000L // 24 hours
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > lastUpdated + expirationTime
}
```

**QuestionUsageTracker**:
```kotlin
data class QuestionUsageEvent(
    val questionId: Int,
    val timestamp: Long,
    val context: String? = null
)
```

### Storage Strategy

**SharedPreferences** para caché local:
```kotlin
class FrequentQuestionsCache {
    companion object {
        private const val PREF_NAME = "frequent_questions_cache"
        private const val KEY_QUESTIONS = "cached_questions"
        private const val KEY_LAST_UPDATED = "last_updated"
    }
}
```

## Error Handling

### Fallback Strategy

1. **Primary**: Preguntas específicas del backend
2. **Secondary**: Preguntas frecuentes del backend
3. **Tertiary**: Preguntas frecuentes en caché local
4. **Final**: Preguntas hardcodeadas por defecto

### Error Scenarios

**Network Failure**:
```kotlin
private suspend fun handleNetworkError(): List<RecommendedQuestion> {
    return frequentQuestionsManager.getCachedFrequentQuestions().ifEmpty {
        getDefaultQuestions()
    }
}

private fun getDefaultQuestions(): List<RecommendedQuestion> {
    return listOf(
        RecommendedQuestion(-1, "¿Cómo puedo contactar con soporte?"),
        RecommendedQuestion(-2, "¿Cuáles son los horarios de atención?"),
        RecommendedQuestion(-3, "¿Dónde puedo encontrar más información?"),
        RecommendedQuestion(-4, "¿Cómo puedo reportar un problema?")
    )
}
```

**API Response Issues**:
- Timeout: Usar caché local
- Empty response: Usar preguntas frecuentes
- Malformed data: Log error y usar respaldo

## Testing Strategy

### Unit Tests

**FrequentQuestionsManagerTest**:
```kotlin
@Test
fun `should return cached questions when network fails`()

@Test
fun `should update cache when new questions received`()

@Test
fun `should track question usage correctly`()

@Test
fun `should handle cache expiration properly`()
```

**ChatbotViewModelTest**:
```kotlin
@Test
fun `should always show suggestions after bot response`()

@Test
fun `should use frequent questions when no specific suggestions`()

@Test
fun `should handle suggestion click correctly`()

@Test
fun `should maintain suggestion state during configuration changes`()
```

### Integration Tests

**ChatbotRepositoryTest**:
```kotlin
@Test
fun `should integrate frequent questions manager correctly`()

@Test
fun `should handle API failures gracefully`()

@Test
fun `should track question usage via API`()
```

### UI Tests

**ChatbotFragmentTest**:
```kotlin
@Test
fun `should display suggestions after every bot response`()

@Test
fun `should handle suggestion clicks correctly`()

@Test
fun `should show fallback questions when API fails`()

@Test
fun `should maintain scroll position when suggestions added`()
```

## Performance Considerations

### Caching Strategy

**Memory Cache**:
- Mantener últimas 4 preguntas frecuentes en memoria
- Invalidar caché cada 24 horas
- Tamaño máximo: 50 preguntas

**Disk Cache**:
- SharedPreferences para persistencia
- Compresión JSON para optimizar espacio
- Limpieza automática de datos antiguos

### Network Optimization

**Request Batching**:
- Agrupar tracking de uso de preguntas
- Enviar estadísticas en lotes cada 5 minutos

**Response Caching**:
- Caché de preguntas frecuentes por 24 horas
- Caché de respuestas específicas por 1 hora

### UI Performance

**RecyclerView Optimization**:
- Usar DiffUtil para actualizaciones eficientes
- ViewHolder recycling para sugerencias
- Lazy loading para listas grandes

## Security Considerations

### Data Protection

**Local Storage**:
- No almacenar información sensible en caché
- Encriptar datos de usuario si es necesario
- Limpiar caché al cerrar sesión

**API Security**:
- Validar tokens JWT en requests de tracking
- Rate limiting para prevenir spam
- Sanitizar input de preguntas

### Privacy

**Usage Tracking**:
- Anonimizar datos de uso
- Respetar configuraciones de privacidad
- Permitir opt-out de tracking

## Monitoring and Analytics

### Metrics to Track

**User Engagement**:
- Tasa de clic en preguntas sugeridas
- Tiempo promedio entre respuesta y siguiente pregunta
- Preguntas más populares por categoría

**System Performance**:
- Tiempo de respuesta de API
- Tasa de éxito de caché
- Errores de red y recuperación

**Quality Metrics**:
- Efectividad de preguntas sugeridas
- Satisfacción del usuario con sugerencias
- Tasa de abandono después de sugerencias