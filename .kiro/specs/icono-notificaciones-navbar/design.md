# Documento de Diseño

## Visión General

Esta funcionalidad implementa un sistema completo de notificaciones para la aplicación Android Aquanqa, que incluye un ícono de notificaciones en la toolbar superior y una pantalla dedicada para mostrar todas las notificaciones del usuario organizadas por fecha.

## Arquitectura

### Componentes Principales

1. **Toolbar Menu Item**: Ícono de notificaciones en la toolbar
2. **NotificationsFragment**: Fragment principal para mostrar la lista de notificaciones
3. **NotificationAdapter**: Adaptador para RecyclerView con ViewHolder pattern
4. **NotificationRepository**: Manejo de datos de notificaciones
5. **NotificationViewModel**: Lógica de presentación y estado
6. **Notification Data Model**: Modelo de datos para notificaciones

### Patrón de Arquitectura

Se seguirá el patrón MVVM (Model-View-ViewModel) consistente con el resto de la aplicación:
- **Model**: NotificationRepository + Data Models
- **View**: NotificationsFragment + Layouts
- **ViewModel**: NotificationViewModel

## Componentes y Interfaces

### 1. Toolbar Integration

**Archivo**: `app/src/main/res/menu/toolbar_menu.xml`
```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <item
        android:id="@+id/action_notifications"
        android:icon="@drawable/ic_notification"
        android:title="Notificaciones"
        app:showAsAction="always" />
</menu>
```

**Integración en MainActivity**: 
- Inflar el menú en `onCreateOptionsMenu()`
- Manejar clicks en `onOptionsItemSelected()`
- Navegar usando NavController

### 2. Navigation Integration

**Actualización en**: `app/src/main/res/navigation/mobile_navigation.xml`
```xml
<fragment
    android:id="@+id/navigation_notifications"
    android:name="com.tecsup.aquanqa.ui.notifications.NotificationsFragment"
    android:label="Notificaciones"
    tools:layout="@layout/fragment_notifications" />
```

### 3. Data Model

**Archivo**: `app/src/main/java/com/tecsup/aquanqa/data/model/Notification.kt`
```kotlin
data class Notification(
    val id: String,
    val title: String,
    val authorName: String,
    val authorImageUrl: String?,
    val timestamp: Long,
    val type: NotificationType,
    val isRead: Boolean = false
)

enum class NotificationType {
    NEW_POST, COMMENT, AD_UPDATE, COMMUNITY_EVENT
}
```

### 4. Repository Layer

**Archivo**: `app/src/main/java/com/tecsup/aquanqa/ui/notifications/NotificationRepository.kt`
- Manejo de datos locales y remotos
- Integración con Firebase Cloud Messaging
- Cache local de notificaciones
- Métodos para marcar como leídas

### 5. ViewModel

**Archivo**: `app/src/main/java/com/tecsup/aquanqa/ui/notifications/NotificationViewModel.kt`
- LiveData para lista de notificaciones agrupadas por fecha
- Manejo de estados (loading, success, error)
- Funciones para marcar notificaciones como leídas
- Formateo de fechas y agrupación

### 6. Fragment y Layout

**Fragment**: `app/src/main/java/com/tecsup/aquanqa/ui/notifications/NotificationsFragment.kt`
- Extiende BaseFragment para consistencia
- RecyclerView con LinearLayoutManager
- Pull-to-refresh functionality
- Empty state handling

**Layout Principal**: `app/src/main/res/layout/fragment_notifications.xml`
- RecyclerView para lista de notificaciones
- SwipeRefreshLayout para actualización
- Empty state view
- Consistent styling con el resto de la app

### 7. RecyclerView Implementation

**Adapter**: `app/src/main/java/com/tecsup/aquanqa/ui/notifications/NotificationAdapter.kt`
- ViewHolder pattern con múltiples tipos de vista
- Date headers y notification items
- Click listeners para interacciones
- Optimización con DiffUtil

**Item Layouts**:
- `item_notification_date_header.xml`: Headers de fecha
- `item_notification.xml`: Items individuales de notificación

## Modelos de Datos

### Notification Model
```kotlin
data class Notification(
    val id: String,
    val title: String,
    val authorName: String,
    val authorImageUrl: String?,
    val timestamp: Long,
    val type: NotificationType,
    val isRead: Boolean = false
)
```

### Grouped Notifications
```kotlin
data class NotificationGroup(
    val dateLabel: String,
    val notifications: List<Notification>
)
```

### Adapter Items
```kotlin
sealed class NotificationItem {
    data class DateHeader(val date: String) : NotificationItem()
    data class NotificationData(val notification: Notification) : NotificationItem()
}
```

## Manejo de Errores

### Estados de Error
1. **Network Error**: Sin conexión a internet
2. **Server Error**: Error del servidor
3. **Empty State**: No hay notificaciones
4. **Loading State**: Cargando datos

### Implementación
- Try-catch blocks en Repository
- Result sealed class para estados
- Error messages user-friendly
- Retry mechanisms

## Estrategia de Testing

### Unit Tests
1. **NotificationViewModel Tests**
   - Agrupación por fechas
   - Formateo de timestamps
   - Manejo de estados

2. **NotificationRepository Tests**
   - Fetch de notificaciones
   - Cache management
   - Error handling

### UI Tests
1. **NotificationsFragment Tests**
   - RecyclerView display
   - Click interactions
   - Empty state display

### Integration Tests
1. **Navigation Tests**
   - Toolbar icon navigation
   - Fragment transitions

## Recursos Reutilizados

### Drawables Existentes
- `ic_notification.xml`: Ícono principal de notificaciones
- `ic_profile.png`: Placeholder para imágenes de autor
- `bg_form_container.xml`: Background para items
- Existing color palette y styles

### Estilos y Colores
- `primary_blue`: Color principal para elementos activos
- `text_primary` y `text_secondary`: Colores de texto
- `background_card`: Background para items
- Existing typography styles

### Layouts Base
- Seguir patrones de `fragment_home.xml`
- Reutilizar componentes de RecyclerView existentes
- Consistent spacing y margins

## Consideraciones de Performance

### RecyclerView Optimization
- ViewHolder pattern
- DiffUtil para actualizaciones eficientes
- Image loading con Glide (ya implementado)
- Lazy loading para listas grandes

### Memory Management
- Weak references en listeners
- Proper lifecycle management
- Cache size limits

### Network Optimization
- Pagination para listas grandes
- Cache de imágenes
- Background sync con WorkManager

## Integración con Firebase

### Cloud Messaging
- Recepción de notificaciones push
- Almacenamiento local de notificaciones
- Sincronización con servidor

### Data Structure
```json
{
  "title": "New post from Olivia",
  "authorName": "Olivia",
  "authorImageUrl": "https://...",
  "timestamp": 1640995200000,
  "type": "NEW_POST"
}
```

## Flujo de Usuario

1. **Acceso**: Usuario toca ícono de notificaciones en toolbar
2. **Navegación**: Sistema navega a NotificationsFragment
3. **Carga**: Fragment carga notificaciones del Repository
4. **Visualización**: Lista se muestra agrupada por fecha
5. **Interacción**: Usuario puede tocar notificaciones individuales
6. **Actualización**: Pull-to-refresh para nuevas notificaciones

## Consideraciones de Accesibilidad

- Content descriptions para iconos
- Proper focus handling
- Text size scaling support
- High contrast support
- Screen reader compatibility