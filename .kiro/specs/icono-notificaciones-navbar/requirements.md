# Documento de Requisitos

## Introducción

Esta funcionalidad agrega un ícono de notificaciones (campana) en la parte superior derecha de la barra de navegación (NavBar) del proyecto Android. Al hacer clic en el ícono, el usuario será redirigido a una pantalla completa de notificaciones que muestra todas las notificaciones recibidas, organizadas por fecha con información detallada de cada una.

## Requisitos

### Requisito 1

**Historia de Usuario:** Como usuario de la aplicación, quiero ver un ícono de notificaciones en la barra superior, para poder acceder rápidamente a mis notificaciones.

#### Criterios de Aceptación

1. CUANDO el usuario abra cualquier pantalla de la aplicación ENTONCES el sistema DEBERÁ mostrar un ícono de campana en la parte superior derecha de la toolbar
2. CUANDO el usuario visualice el ícono ENTONCES el sistema DEBERÁ usar un drawable de campana como ícono de notificaciones
3. CUANDO el ícono esté visible ENTONCES el sistema DEBERÁ posicionarlo en la esquina superior derecha de la toolbar
4. CUANDO existan notificaciones no leídas ENTONCES el sistema DEBERÁ mostrar un indicador visual en el ícono

### Requisito 2

**Historia de Usuario:** Como usuario de la aplicación, quiero hacer clic en el ícono de notificaciones, para poder navegar al fragment de notificaciones.

#### Criterios de Aceptación

1. CUANDO el usuario haga clic en el ícono de notificaciones ENTONCES el sistema DEBERÁ navegar al fragment de notificaciones
2. CUANDO se produzca la navegación ENTONCES el sistema DEBERÁ usar el NavController para realizar la transición
3. CUANDO se navegue al fragment ENTONCES el sistema DEBERÁ mantener la consistencia de la navegación con el resto de la aplicación

### Requisito 3

**Historia de Usuario:** Como usuario de la aplicación, quiero ver una lista completa de mis notificaciones organizadas por fecha, para poder revisar toda la actividad reciente.

#### Criterios de Aceptación

1. CUANDO el usuario acceda al fragment de notificaciones ENTONCES el sistema DEBERÁ mostrar una lista de notificaciones agrupadas por fecha
2. CUANDO se muestren las notificaciones ENTONCES el sistema DEBERÁ organizar las fechas en orden cronológico descendente (más recientes primero)
3. CUANDO se muestre cada grupo de fecha ENTONCES el sistema DEBERÁ incluir encabezados como "Hoy", "Ayer", o la fecha específica
4. CUANDO no existan notificaciones ENTONCES el sistema DEBERÁ mostrar un mensaje indicando que no hay notificaciones

### Requisito 4

**Historia de Usuario:** Como usuario de la aplicación, quiero ver información detallada de cada notificación, para poder entender el contexto y origen de cada una.

#### Criterios de Aceptación

1. CUANDO se muestre cada notificación ENTONCES el sistema DEBERÁ incluir un título breve descriptivo
2. CUANDO se muestre cada notificación ENTONCES el sistema DEBERÁ incluir una imagen de perfil del autor que realizó la publicación
3. CUANDO no exista imagen del autor ENTONCES el sistema DEBERÁ mostrar un ícono genérico de perfil
4. CUANDO se muestre cada notificación ENTONCES el sistema DEBERÁ incluir la fecha y hora de cuándo llegó la notificación
5. CUANDO se muestre cada notificación ENTONCES el sistema DEBERÁ usar un diseño consistente con el resto de la aplicación

### Requisito 5

**Historia de Usuario:** Como usuario de la aplicación, quiero que las notificaciones se muestren de manera visualmente atractiva y organizada, para poder navegar fácilmente por ellas.

#### Criterios de Aceptación

1. CUANDO se muestren las notificaciones ENTONCES el sistema DEBERÁ usar un RecyclerView para optimizar el rendimiento
2. CUANDO se muestre cada item de notificación ENTONCES el sistema DEBERÁ incluir separadores visuales entre elementos
3. CUANDO se muestre la pantalla ENTONCES el sistema DEBERÁ reutilizar recursos existentes de drawable para iconos y estilos
4. CUANDO se muestre la pantalla ENTONCES el sistema DEBERÁ mantener la consistencia visual con el tema de la aplicación