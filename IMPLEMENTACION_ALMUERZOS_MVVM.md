# Implementación MVVM para Almuerzos - Comedor Tecsup

## Resumen

Se ha implementado completamente la funcionalidad de almuerzos siguiendo el patrón MVVM con código limpio, claro y conciso. La implementación incluye integración con la API de Django, filtrado automático de días feriados, y funcionalidad de click para abrir links de pedidos en el navegador.

## Arquitectura MVVM Implementada

### 1. **Modelo de Datos** (`Almuerzo.kt`)

```kotlin
data class Almuerzo(
    val id: Int,
    val fecha: String,
    val entrada: String,
    val platoFondo: String,
    val refresco: String,
    val esFeriado: Boolean,
    val link: String?,
    val nombreDia: String,
    val createdAt: String,
    val updatedAt: String
)
```

**Características:**
- Mapea directamente con la API de Django usando `@SerializedName`
- Incluye todos los campos necesarios para mostrar el menú
- Documentación completa de cada campo

### 2. **Repository** (`LunchRepository.kt`)

**Responsabilidades:**
- Comunicación con la API REST
- Filtrado automático de días feriados (`es_feriado=false`)
- Manejo de autenticación con tokens JWT
- Gestión de errores de red y API

**Métodos principales:**
- `getAlmuerzos()`: Obtiene lista filtrada de almuerzos
- `getAlmuerzoById(id)`: Obtiene almuerzo específico
- `hasAvailableLunches()`: Verifica disponibilidad

### 3. **ViewModel** (`LunchViewModel.kt`)

**Estados manejados:**
- **Loading**: Mientras se cargan los datos
- **Success**: Datos cargados exitosamente
- **Error**: Error al cargar con mensaje descriptivo
- **Empty**: No hay almuerzos disponibles

**LiveData expuestas:**
```kotlin
val almuerzos: LiveData<List<Almuerzo>>
val isLoading: LiveData<Boolean>
val error: LiveData<String?>
val isEmpty: LiveData<Boolean>
```

**Métodos públicos:**
- `loadAlmuerzos()`: Carga inicial
- `refreshAlmuerzos()`: Pull-to-refresh
- `clearError()`: Limpiar mensajes de error
- `getAlmuerzoById(id)`: Búsqueda por ID

### 4. **Adapter** (`LunchAdapter.kt`)

**Funcionalidades implementadas:**
- **Click en cards**: Abre link de pedidos en navegador automáticamente
- **DiffUtil**: Optimización de rendimiento para cambios en la lista
- **Validación de URLs**: Verifica que hay apps disponibles para abrir links
- **Manejo de errores**: Toast informativos para links no disponibles
- **Callback opcional**: Para acciones adicionales en clicks

**Método clave:**
```kotlin
private fun openLinkInBrowser(link: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(link)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
```

### 5. **Fragment** (`LunchFragment.kt`)

**Características principales:**
- **Inyección de dependencias manual** siguiendo patrón de la app
- **Manejo completo de estados** con UI responsiva
- **Pull-to-refresh** con SwipeRefreshLayout
- **Snackbar con retry** para errores de red
- **Estados de carga y lista vacía** claramente definidos

**Observadores implementados:**
```kotlin
// Lista de almuerzos
lunchViewModel.almuerzos.observe(viewLifecycleOwner) { almuerzos ->
    lunchAdapter.submitList(almuerzos)
}

// Estado de carga
lunchViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
    binding.swipeRefreshLayout?.isRefreshing = isLoading
}

// Errores con retry
lunchViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
    errorMessage?.let { showError(it) }
}

// Estado vacío
lunchViewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
    if (isEmpty) showEmptyState()
}
```

## Integración con API

### Endpoint agregado al ApiService

```kotlin
@GET("api/almuerzos/")
suspend fun getAlmuerzos(
    @Header("Authorization") token: String,
    @Query("es_feriado") esFeriado: Boolean = false,
    @Query("ordering") ordering: String = "fecha"
): Response<List<Almuerzo>>
```

**Filtros aplicados automáticamente:**
- `es_feriado=false`: Excluye días feriados
- `ordering=fecha`: Ordena por fecha ascendente

## Funcionalidades Clave

### ✅ **Solo Días No Feriados**
La API automáticamente filtra `es_feriado=false`, mostrando únicamente días laborables.

### ✅ **Click para Abrir Links**
Al hacer click en cualquier card, se abre automáticamente el link de pedidos en el navegador web del dispositivo.

### ✅ **Patrón MVVM Completo**
- **Model**: `Almuerzo` con mapeo directo de API
- **View**: `LunchFragment` con manejo de estados
- **ViewModel**: `LunchViewModel` con LiveData y lógica de negocio
- **Repository**: `LunchRepository` con acceso a datos

### ✅ **Código Limpio y Documentado**
- Comentarios KDoc en todas las clases y métodos principales
- Separación clara de responsabilidades
- Sin duplicación de código
- Nombres descriptivos y consistentes

### ✅ **Manejo de Estados**
- **Loading**: Indicadores de progreso
- **Success**: Lista funcional con datos reales
- **Error**: Snackbar con opción de retry
- **Empty**: Mensaje informativo cuando no hay datos

### ✅ **Pull-to-Refresh**
SwipeRefreshLayout configurado para actualizar datos deslizando hacia abajo.

## Flujo de Datos

```
API Django (almuerzos/) 
    ↓
LunchRepository (filtro es_feriado=false)
    ↓  
LunchViewModel (manejo de estados)
    ↓
LunchFragment (observadores LiveData)
    ↓
LunchAdapter (click → abrir navegador)
    ↓
Usuario ve menús y puede hacer pedidos
```

## Factory Pattern

Se implementó `LunchViewModelFactory` siguiendo el patrón establecido en la app para inyección de dependencias:

```kotlin
class LunchViewModelFactory(
    private val repository: LunchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LunchViewModel(repository) as T
    }
}
```

## Testing y Debugging

Se incluyen logs para debugging que se pueden remover en producción:
- Cantidad de almuerzos cargados
- Clicks en elementos
- Estados de carga y errores

## Resultado Final

La implementación cumple con todos los requisitos:

1. **✅ Patrón MVVM**: Implementado correctamente
2. **✅ Código limpio**: Sin duplicación, bien estructurado
3. **✅ Solo días no feriados**: Filtro automático en API
4. **✅ Click abre navegador**: Funcionalidad implementada en adapter
5. **✅ Integración con API**: Endpoint configurado y funcional
6. **✅ Documentación clara**: Comentarios descriptivos en todo el código
7. **✅ Manejo de errores**: Estados y retry implementados
8. **✅ UI responsiva**: Loading, empty states, pull-to-refresh

La funcionalidad está lista para ser usada por los estudiantes de Tecsup para consultar los menús diarios del comedor y realizar sus pedidos de forma sencilla y eficiente.