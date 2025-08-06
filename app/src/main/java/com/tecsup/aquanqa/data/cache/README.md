# Cache Strategy Inteligente - Documentación

## 📋 Resumen

Este sistema de cache inteligente reemplaza el cache básico anterior con una solución robusta que incluye:

- **Duraciones específicas** por tipo de dato
- **Cache por usuario** para datos personalizados
- **Invalidación inteligente** con fallbacks automáticos
- **Metadata completa** para debugging y analytics
- **Thread-safety** para operaciones concurrentes

## 🏗️ Arquitectura

### Componentes Principales

1. **CacheStrategy**: Define tipos de cache y duraciones
2. **CacheEntry**: Wrapper con metadata para cada entrada
3. **IntelligentCache**: Motor de cache thread-safe
4. **CacheManager**: API de alto nivel para operaciones específicas

### Flujo de Datos

```
Repository → CacheManager → IntelligentCache → CacheEntry
     ↓              ↓              ↓              ↓
   API Call    Domain Logic   Storage Logic   Metadata
```

## ⏱️ Duraciones de Cache

| Tipo de Dato | Duración | Razón |
|--------------|----------|-------|
| Categorías | 30 minutos | Cambian muy poco |
| Eventos | 5 minutos | Contenido dinámico |
| Perfil Usuario | 15 minutos | Datos personales estables |
| Notificaciones | 2 minutos | Información tiempo real |

## 🚀 Uso Básico

### En el Repository

```kotlin
// Guardar en cache
cacheManager.cacheEvents(events, userId)

// Obtener del cache
when (val result = cacheManager.getCachedEvents(userId)) {
    is CacheResult.Hit -> return Result.Success(result.data)
    is CacheResult.Expired -> // Usar como fallback
    is CacheResult.Miss -> // Llamar API
}
```

### En el ViewModel

```kotlin
// Refresh con cache inteligente
fun refreshData(forceRefresh: Boolean = true) {
    repository.refreshAllData()
    loadCategories(forceRefresh)
}
```

## 🔧 Funcionalidades Avanzadas

### Cache por Usuario

```kotlin
// Cada usuario tiene su propio cache
cacheManager.cacheEvents(events, "user123")
cacheManager.getCachedEvents("user123")
```

### Invalidación Selectiva

```kotlin
// Invalidar solo eventos de una categoría
repository.invalidateCache(categoryName = "deportes")

// Invalidar todo el cache de un usuario
cacheManager.invalidateUserCache("user123")
```

### Estadísticas y Debugging

```kotlin
val stats = cacheManager.getDetailedStats()
println("Total entries: ${stats.totalEntries}")
println("Expired entries: ${stats.expiredEntries}")
```

### Observar Cambios

```kotlin
cacheManager.observeCacheUpdates().collect { event ->
    when (event) {
        is CacheUpdateEvent.DataUpdated -> // Cache actualizado
        is CacheUpdateEvent.DataInvalidated -> // Cache invalidado
    }
}
```

## 🛡️ Manejo de Errores

### Fallback Automático

El sistema usa cache expirado como fallback cuando:
- La API falla
- No hay conexión a internet
- Ocurre un error de red

```kotlin
// Si la API falla, usa cache expirado automáticamente
when (val cacheResult = cacheManager.getCachedEvents(userId)) {
    is CacheResult.Expired -> {
        Log.w(TAG, "Using expired cache as fallback")
        return Result.Success(cacheResult.data)
    }
}
```

## 📊 Beneficios vs Implementación Anterior

### Antes (Cache Básico)
- ❌ Duraciones fijas para todos los datos
- ❌ Sin diferenciación por usuario
- ❌ Sin fallbacks automáticos
- ❌ Sin metadata para debugging
- ❌ Variables globales no thread-safe

### Ahora (Cache Inteligente)
- ✅ Duraciones específicas por tipo
- ✅ Cache aislado por usuario
- ✅ Fallbacks automáticos con datos expirados
- ✅ Metadata completa y debugging
- ✅ Thread-safe con ConcurrentHashMap
- ✅ Observabilidad con StateFlow
- ✅ Invalidación granular

## 🔍 Debugging

### Logs Automáticos

El sistema genera logs detallados:

```
D/IntelligentCache: Cache hit: EVENTS::user123::events - CacheEntry(age=45s, source=API, version=1)
D/CacheManager: Events cached: 25 items for user: user123
W/HomeRepository: API failed, using expired events cache as fallback
```

### Estadísticas en Tiempo Real

```kotlin
// En desarrollo, puedes monitorear el cache
val stats = viewModel.getCacheStats()
Log.d("Cache", "Memory usage: ${stats.memoryUsageEstimate} bytes")
```

## 🎯 Mejores Prácticas

1. **Usa forceRefresh solo cuando sea necesario** (pull-to-refresh)
2. **Aprovecha los fallbacks automáticos** para mejor UX offline
3. **Monitorea las estadísticas** en desarrollo
4. **Invalida cache específico** en lugar de limpiar todo
5. **Confía en las duraciones automáticas** - están optimizadas

## 🔄 Migración desde Cache Anterior

El nuevo sistema es **drop-in compatible**. Solo necesitas:

1. Actualizar imports en Repository
2. Usar los nuevos métodos con parámetros opcionales
3. Aprovechar las nuevas funcionalidades gradualmente

No se requieren cambios en UI o ViewModels existentes.