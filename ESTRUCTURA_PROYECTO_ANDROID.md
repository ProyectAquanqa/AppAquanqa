# Estructura del Proyecto Android - Aquanqa

## Descripción General
Este es un proyecto Android desarrollado en Kotlin para la aplicación **Aquanqa**, que parece ser una aplicación educativa o informativa relacionada con Tecsup. La aplicación incluye funcionalidades como notificaciones push, chat bot, perfil de usuario, anuncios y sistema de almuerzo.

---

## Estructura de Directorios y Archivos

###  Directorio Raíz

#### Archivos de Configuración Principal
- **`build.gradle.kts`** - Archivo de configuración principal de Gradle para todo el proyecto. Define plugins globales y configuraciones compartidas.
- **`settings.gradle.kts`** - Configuración de módulos del proyecto y repositorios de dependencias.
- **`gradle.properties`** - Propiedades globales de Gradle (configuración de memoria, AndroidX, etc.).
- **`gradlew`** / **`gradlew.bat`** - Scripts wrapper de Gradle para Linux/Mac y Windows respectivamente.
- **`local.properties`** - Archivo local con rutas del SDK de Android (no se versiona).
- **`README.md`** - Documentación del proyecto.
- **`.gitignore`** - Archivos y carpetas ignorados por Git.

#### Carpetas del Sistema
- **`.git/`** - Repositorio Git con historial de versiones.
- **`.gradle/`** - Cache y archivos temporales de Gradle.
- **`.idea/`** - Configuración del IDE Android Studio.
- **`.kotlin/`** - Cache del compilador de Kotlin.
- **`.kiro/`** - Configuración de la herramienta Kiro (asistente de desarrollo).

#### Configuración de Gradle
- **`gradle/`**
  - **`libs.versions.toml`** - Catálogo de versiones centralizado para dependencias.
  - **`wrapper/`** - Archivos del wrapper de Gradle.

---

### 📱 Módulo de la Aplicación (`app/`)

#### Archivos de Configuración del Módulo
- **`build.gradle.kts`** - Configuración específica del módulo app, dependencias, versiones, y configuración de compilación.
- **`google-services.json`** - Configuración de Firebase para servicios de Google.
- **`proguard-rules.pro`** - Reglas de ofuscación para builds de release.
- **`.gitignore`** - Archivos ignorados específicos del módulo app.

#### Código Fuente (`app/src/`)

##### 📂 `main/` - Código Principal de la Aplicación

###### Configuración de la App
- **`AndroidManifest.xml`** - Manifiesto de la aplicación con permisos, actividades, servicios y configuración.
- **`ic_launcher-playstore.png`** - Icono de la aplicación para Google Play Store.

###### Código Java/Kotlin (`main/java/com/tecsup/aquanqa/`)

**Archivos Principales:**
- **`AquanqaApplication.kt`** - Clase Application personalizada, punto de entrada de la app.
- **`MainActivity.kt`** - Actividad principal que contiene la navegación y estructura base.

** `data/` - Capa de Datos (Arquitectura MVVM)**
- **`api/`** - Interfaces y configuración de API REST.
- **`auth/`** - Manejo de autenticación y tokens.
- **`common/`** - Clases comunes de datos.
- **`manager/`** - Gestores de datos y lógica de negocio.
- **`model/`** - Modelos de datos (DTOs, entidades).
- **`network/`** - Configuración de red y interceptores.
- **`preferences/`** - Manejo de preferencias locales (SharedPreferences/DataStore).
- **`repository/`** - Repositorios que abstraen las fuentes de datos.

** `services/` - Servicios de Android**
- **`MyFirebaseMessagingService.kt`** - Servicio para manejar notificaciones push de Firebase.

** `ui/` - Capa de Presentación (UI)**
- **`adapters/`** - Adaptadores para RecyclerView y otros componentes.
- **`anuncios/`** - Fragmentos y ViewModels para la sección de anuncios.
- **`base/`** - Clases base para Fragments y Activities.
- **`chatbot/`** - Interfaz y lógica del chat bot.
- **`home/`** - Pantalla principal/inicio de la aplicación.
- **`login/`** - Pantallas de autenticación y login.
- **`lunch/`** - Sistema de gestión de almuerzos.
- **`notifications/`** - Gestión y visualización de notificaciones.
- **`profile/`** - Perfil de usuario y configuraciones.

** `utils/` - Utilidades y Helpers**
- **`ChatSessionManager.kt`** - Gestión de sesiones de chat.
- **`DateUtils.kt`** - Utilidades para manejo de fechas.
- **`ImageDisplayHelper.kt`** - Helper para mostrar imágenes.
- **`ImagePickerManager.kt`** - Gestión de selección de imágenes.
- **`InfiniteScrollListener.kt`** - Listener para scroll infinito.
- **`NotificationPermissionHelper.kt`** - Helper para permisos de notificaciones.
- **`ProfileFieldHelper.kt`** - Utilidades para campos de perfil.

##### 📂 `res/` - Recursos de la Aplicación

**Recursos Visuales:**
- **`color/`** - Definiciones de colores en XML.
- **`drawable/`** - Imágenes vectoriales, formas y drawables.
- **`font/`** - Fuentes personalizadas.
- **`mipmap-*/`** - Iconos de la aplicación en diferentes densidades (hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi).

**Layouts y Navegación:**
- **`layout/`** - Archivos XML de diseño de pantallas y componentes.
- **`menu/`** - Definiciones de menús.
- **`navigation/`** - Gráficos de navegación para Navigation Component.

**Configuración y Valores:**
- **`values/`** - Strings, colores, dimensiones, estilos (configuración base).
- **`values-land/`** - Recursos específicos para orientación horizontal.
- **`values-w600dp/`** - Recursos para pantallas de al menos 600dp de ancho.
- **`values-w1240dp/`** - Recursos para pantallas de al menos 1240dp de ancho.
- **`xml/`** - Archivos XML de configuración (backup rules, file paths, etc.).

##### 📂 `test/` - Pruebas Unitarias
- **`java/com/tecsup/aquanqa/`** - Tests unitarios para la lógica de negocio.

##### 📂 `androidTest/` - Pruebas de Instrumentación
- **`java/com/tecsup/aquanqa/`** - Tests de UI y integración que requieren dispositivo Android.

---

## Tecnologías y Librerías Utilizadas

### Arquitectura
- **MVVM (Model-View-ViewModel)** - Patrón arquitectónico principal
- **Repository Pattern** - Para abstracción de datos
- **Navigation Component** - Para navegación entre pantallas

### Principales Dependencias
- **Kotlin** - Lenguaje principal de desarrollo
- **AndroidX** - Librerías modernas de Android
- **Material Design** - Componentes de UI de Google
- **View Binding & Data Binding** - Para binding de vistas
- **Lifecycle Components** - ViewModel y LiveData
- **Retrofit + Gson** - Cliente HTTP y serialización JSON
- **OkHttp** - Cliente HTTP con interceptores
- **Glide** - Carga y cache de imágenes
- **Firebase** - Cloud Messaging y Analytics
- **DataStore** - Almacenamiento de preferencias moderno
- **Coroutines** - Programación asíncrona
- **UCrop** - Recorte de imágenes
- **CircleImageView** - Imágenes circulares
- **PhotoView** - Zoom de imágenes

### Testing
- **JUnit** - Framework de testing
- **Mockito** - Mocking para tests
- **Espresso** - Tests de UI
- **Architecture Testing** - Tests para componentes de arquitectura

---

## Funcionalidades Principales

1. **Sistema de Autenticación** - Login y gestión de usuarios
2. **Notificaciones Push** - Mediante Firebase Cloud Messaging
3. **Chat Bot** - Sistema de chat automatizado
4. **Gestión de Perfil** - Edición de perfil con imágenes
5. **Sistema de Anuncios** - Visualización de anuncios
6. **Gestión de Almuerzos** - Sistema para gestionar comidas
7. **Navegación por Drawer** - Menú lateral de navegación

---

## Configuración del Proyecto

- **Namespace:** `com.tecsup.aquanqa`
- **Min SDK:** 29 (Android 10)
- **Target SDK:** 35 (Android 14)
- **Compile SDK:** 35
- **Java Version:** 17
- **Kotlin JVM Target:** 17

---

## Permisos de la Aplicación

- **INTERNET** - Conexión a internet
- **ACCESS_NETWORK_STATE** - Estado de la red
- **READ_EXTERNAL_STORAGE** - Lectura de almacenamiento (SDK ≤ 32)
- **READ_MEDIA_IMAGES** - Lectura de imágenes (SDK > 32)
- **CAMERA** - Acceso a la cámara
- **POST_NOTIFICATIONS** - Envío de notificaciones
- **WAKE_LOCK** - Mantener dispositivo despierto
- **RECEIVE** - Recibir mensajes de Google Cloud Messaging

---

*Este documento proporciona una visión completa de la estructura del proyecto Android Aquanqa, facilitando la comprensión y el mantenimiento del código.*