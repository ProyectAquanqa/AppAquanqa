# Aquanqa - App Móvil

## Descripción General

Aquanqa es una aplicación móvil educativa e informativa desarrollada para Tecsup, que proporciona a los usuarios acceso a diversas funcionalidades como notificaciones, chat bot, perfil de usuario, anuncios y sistema de gestión de almuerzos.

## Funcionalidades Implementadas

### Sistema de Autenticación
- Autenticación con DNI y contraseña
- Almacenamiento seguro de tokens JWT
- Validación de formularios
- Manejo de errores de autenticación
- Recuperación automática de sesión

### Notificaciones Push
- Integración con Firebase Cloud Messaging
- Gestión de canales de notificación
- Visualización de eventos y anuncios importantes
- Solicitud de permisos de notificación

### Chat Bot
- Sistema de chat automatizado
- Preguntas recomendadas
- Historial de conversación
- Optimización de memoria para conversaciones largas

### Gestión de Perfil
- Visualización de información de usuario
- Edición de datos personales
- Carga y recorte de imágenes de perfil
- Validación de campos

### Sistema de Anuncios
- Visualización de anuncios institucionales
- Soporte para imágenes y contenido multimedia
- Interfaz expandible para descripciones largas

### Gestión de Almuerzos
- Visualización del menú diario
- Sistema de pedidos
- Caché inteligente para funcionamiento offline

### Navegación
- Menú lateral (Navigation Drawer)
- Barra de navegación inferior
- Navegación entre fragmentos con Navigation Component

## Arquitectura

La aplicación sigue el patrón de arquitectura MVVM (Model-View-ViewModel):

- **Model**: Clases de datos y repositorios que manejan la lógica de negocio
- **View**: Actividades y fragmentos que muestran la interfaz de usuario
- **ViewModel**: Clases que conectan los modelos con las vistas y manejan la lógica de presentación
- **Repository Pattern**: Para abstracción de fuentes de datos

## Tecnologías Utilizadas

### Principales
- **Kotlin**: Lenguaje principal de desarrollo
- **AndroidX**: Librerías modernas de Android
- **Material Design**: Componentes de UI de Google

### Networking y Datos
- **Retrofit + Gson**: Para las llamadas a la API REST y serialización JSON
- **OkHttp**: Para interceptores y logging de red
- **DataStore**: Para almacenamiento seguro de tokens y preferencias

### UI y Experiencia de Usuario
- **View Binding & Data Binding**: Para binding de vistas
- **Glide**: Carga y cache de imágenes
- **CircleImageView**: Para imágenes de perfil circulares
- **UCrop**: Recorte de imágenes
- **PhotoView**: Zoom de imágenes

### Programación Asíncrona
- **Coroutines**: Para operaciones asíncronas
- **LiveData**: Para observar cambios en los datos
- **ViewModel**: Para separar la lógica de la UI

### Servicios en la Nube
- **Firebase**: Cloud Messaging y Analytics

## Configuración de la API

La aplicación se conecta a una API Django para la autenticación y otras funcionalidades:

1. Para desarrollo local, asegúrate de que el servidor Django esté en ejecución:
   ```bash
   cd Aquanqa_noticias_web
   python manage.py runserver 192.168.18.13:8000
   ```

2. La IP configurada actualmente es `192.168.18.13`. Si necesitas cambiarla, modifica el archivo:
   `app/src/main/java/com/tecsup/aquanqa/data/api/ApiConfig.kt`

## Configuración para Producción

Cuando la API esté desplegada en un servidor de producción, deberás actualizar la URL base en:
`app/src/main/java/com/tecsup/aquanqa/data/api/ApiConfig.kt`

