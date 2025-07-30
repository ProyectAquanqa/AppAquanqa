# Plan de Implementación

- [ ] 1. Crear modelo de datos para notificaciones
  - Implementar data class Notification con propiedades básicas (id, title, authorName, authorImageUrl, timestamp, type, isRead)
  - Crear enum NotificationType para diferentes tipos de notificaciones
  - Implementar data class NotificationGroup para agrupación por fechas
  - Crear sealed class NotificationItem para items del adapter
  - _Requisitos: 4.1, 4.2, 4.4, 4.5_

- [ ] 2. Implementar NotificationRepository para manejo de datos
  - Crear clase NotificationRepository siguiendo el patrón existente
  - Implementar método getNotifications() que retorne Result<List<Notification>>
  - Agregar función para generar datos mock de notificaciones para testing
  - Implementar agrupación de notificaciones por fecha
  - _Requisitos: 3.1, 3.2, 3.3, 5.1_

- [ ] 3. Crear NotificationViewModel con lógica de presentación
  - Implementar NotificationViewModel extendiendo ViewModel
  - Agregar LiveData para lista de notificaciones agrupadas
  - Implementar función loadNotifications() con manejo de estados
  - Crear función para formatear fechas en español (Hoy, Ayer, fecha específica)
  - Agregar NotificationViewModelFactory siguiendo el patrón existente
  - _Requisitos: 3.1, 3.2, 3.3, 3.4_

- [ ] 4. Crear layouts para la pantalla de notificaciones
  - Implementar fragment_notifications.xml con RecyclerView y empty state
  - Crear item_notification_date_header.xml para headers de fecha
  - Diseñar item_notification.xml con imagen de autor, título y timestamp
  - Reutilizar colores y estilos existentes de la aplicación
  - _Requisitos: 3.1, 4.1, 4.2, 4.4, 5.2, 5.3, 5.4_

- [ ] 5. Implementar NotificationAdapter para RecyclerView
  - Crear NotificationAdapter con ViewHolder pattern
  - Implementar múltiples tipos de vista (header y notification item)
  - Agregar DiffUtil.ItemCallback para optimización
  - Implementar click listeners para items de notificación
  - _Requisitos: 5.1, 5.2, 5.3_

- [ ] 6. Crear NotificationsFragment siguiendo BaseFragment
  - Implementar NotificationsFragment extendiendo BaseFragment
  - Configurar RecyclerView con LinearLayoutManager
  - Implementar setupObservers() para observar ViewModel
  - Agregar manejo de estados (loading, success, error, empty)
  - _Requisitos: 3.1, 3.4, 5.4_

- [ ] 7. Agregar ícono de notificaciones a la toolbar
  - Crear toolbar_menu.xml con item de notificaciones
  - Modificar MainActivity para inflar el menú en onCreateOptionsMenu()
  - Implementar onOptionsItemSelected() para manejar click del ícono
  - Usar NavController para navegar al fragment de notificaciones
  - _Requisitos: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3_

- [ ] 8. Actualizar navegación para incluir NotificationsFragment
  - Agregar fragment de notificaciones en mobile_navigation.xml
  - Configurar navigation_notifications con id y layout correcto
  - Actualizar strings.xml con título "Notificaciones"
  - Verificar que la navegación funcione correctamente
  - _Requisitos: 2.1, 2.2, 2.3, 3.4_

- [ ] 9. Integrar carga de imágenes con Glide
  - Implementar carga de imágenes de autor usando Glide existente
  - Agregar placeholder con ic_profile.png para imágenes faltantes
  - Configurar transformación circular para imágenes de perfil
  - Manejar errores de carga de imágenes
  - _Requisitos: 4.2, 4.3_

- [ ] 10. Implementar funcionalidad de datos mock
  - Crear función generateMockNotifications() en Repository
  - Generar notificaciones de ejemplo con diferentes tipos y fechas
  - Incluir notificaciones para "Hoy", "Ayer" y fechas anteriores
  - Usar nombres y tipos variados para simular datos reales
  - _Requisitos: 3.1, 3.2, 4.1, 4.4, 4.5_

- [ ] 11. Configurar manejo de visibilidad del FAB
  - Actualizar MainActivity para ocultar FAB en fragment de notificaciones
  - Modificar el listener de navegación existente
  - Agregar navigation_notifications a la condición shouldHideFab
  - Verificar que el comportamiento sea consistente
  - _Requisitos: 2.3_

- [ ] 12. Agregar tests unitarios básicos
  - Crear NotificationViewModelTest para testing de agrupación por fechas
  - Implementar test para formateo de timestamps
  - Agregar test para NotificationRepository con datos mock
  - Verificar que los tests pasen correctamente
  - _Requisitos: 3.1, 3.2, 3.3_