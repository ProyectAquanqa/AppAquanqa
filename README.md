# Aquanqa - App Móvil

## Configuración de la API

La aplicación móvil se conecta a una API Django para la autenticación y otras funcionalidades. Para configurar la conexión:

1. Asegúrate de que el servidor Django esté en ejecución en tu red local:
   ```
   cd Aquanqa_noticias_web
   python manage.py runserver 192.168.18.13:8000
   ```

2. La IP configurada actualmente es `192.168.18.13`. Si necesitas cambiarla, modifica el archivo:
   `app/src/main/java/com/tecsup/aquanqa/data/api/ApiConfig.kt`

## Funcionalidades Implementadas

### Login
- Autenticación con DNI y contraseña
- Almacenamiento seguro de tokens JWT
- Validación de formularios
- Manejo de errores

## Arquitectura

La aplicación sigue el patrón de arquitectura MVVM (Model-View-ViewModel):

- **Model**: Clases de datos y repositorios que manejan la lógica de negocio
- **View**: Actividades y fragmentos que muestran la interfaz de usuario
- **ViewModel**: Clases que conectan los modelos con las vistas y manejan la lógica de presentación

## Tecnologías Utilizadas

- **Retrofit**: Para las llamadas a la API REST
- **OkHttp**: Para interceptores y logging de red
- **DataStore**: Para almacenamiento seguro de tokens
- **Corrutinas**: Para operaciones asíncronas
- **LiveData**: Para observar cambios en los datos
- **ViewModel**: Para separar la lógica de la UI

## Configuración para Producción

Cuando la API esté desplegada en un servidor de producción, deberás actualizar la URL base en:
`app/src/main/java/com/tecsup/aquanqa/data/api/ApiConfig.kt`

```kotlin
const val BASE_URL = "https://tu-dominio-de-produccion.com/"
```

Recuerda también actualizar la configuración de seguridad para usar HTTPS en producción. 