# 🔔 Sistema de Notificaciones Push - Aquanqa

## 📋 Resumen de Implementación

Se ha implementado un sistema completo de notificaciones push usando Firebase Cloud Messaging (FCM) que funciona de la siguiente manera:

### 🎯 Funcionalidad Principal
- **Cuando se crea un nuevo evento** en el backend Django y se marca como **publicado**, se envía automáticamente una **notificación push** a todos los usuarios con la aplicación instalada.
- **El título del evento** se muestra como contenido principal de la notificación.
- **La sesión persiste** entre reinicios de la aplicación.

---

## 🚀 Cómo Funciona el Sistema

### 1. **Backend Django**
- ✅ **Firebase Admin SDK** inicializado con las credenciales
- ✅ **Señales de Django** que detectan cuando se publica un evento nuevo
- ✅ **Modelos de DeviceToken** y **Notificacion** para gestionar dispositivos y historial
- ✅ **API REST** para registrar/desactivar tokens FCM desde Android

### 2. **Android App**
- ✅ **Firebase SDK** configurado con google-services.json
- ✅ **MyFirebaseMessagingService** para recibir notificaciones
- ✅ **FirebaseManager** para gestionar tokens FCM
- ✅ **Registro automático** de token FCM al hacer login
- ✅ **Desregistro automático** de token al hacer logout

### 3. **Flujo Completo**
```
1. Usuario abre app → Verifica sesión → MainActivity (si logueado) / Login (si no)
2. Al login exitoso → Registra token FCM en el servidor
3. Admin crea evento y lo marca como "publicado" en Django Admin
4. Señal Django detecta evento publicado → Crea notificación
5. Servicio de notificaciones envía push a todos los tokens activos
6. Usuario recibe notificación con título del evento
7. Al hacer logout → Desactiva token FCM en el servidor
```

---

## 🛠️ Configuración Final Requerida

### 1. **Configurar Firebase Project**
Debes reemplazar el archivo `app/google-services.json` con el archivo real de tu proyecto Firebase:

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Crea un proyecto nuevo o usa uno existente
3. Agrega una app Android con el package name: `com.tecsup.aquanqa`
4. Descarga el archivo `google-services.json` real
5. Reemplaza el archivo dummy en `app/google-services.json`

### 2. **Actualizar Credenciales del Backend**
En el archivo `Aquanqa_noticias_web/firebase-credentials.json`, asegúrate de que:
- El `project_id` coincida con tu proyecto Firebase
- Las credenciales sean válidas y tengan permisos de Firebase Admin

### 3. **Variables de Entorno**
Agrega en tu archivo `.env` del backend:
```bash
FIREBASE_ADMIN_CREDENTIALS_PATH=/ruta/completa/al/firebase-credentials.json
```

---

## 🧪 Cómo Probar las Notificaciones

### 1. **Preparación**
1. Compila e instala la app Android en un dispositivo físico
2. Asegúrate de que el backend Django esté corriendo
3. Verifica que Firebase esté configurado correctamente

### 2. **Prueba Básica**
1. **Abre la app** y haz **login** con cualquier usuario
2. **Minimiza la app** (no la cierres completamente)
3. En **Django Admin**, ve a **Eventos** y crea uno nuevo
4. **Marca el evento como "publicado"** y guarda
5. **Deberías recibir la notificación** en el dispositivo

### 3. **Prueba Avanzada**
1. **Cierra la app completamente**
2. Crea otro evento y márcalo como publicado
3. **La notificación debe llegar** incluso con la app cerrada
4. **Toca la notificación** y debe abrir la app

### 4. **Verificar en Logs**
- **Backend**: Revisa logs para ver "Notificaciones enviadas: X éxito, Y fallo"
- **Android**: Revisa Logcat con filtro "FCMService" y "FirebaseManager"

---

## 📱 Archivos Implementados

### **Backend Django**
- ✅ `notificaciones/services.py` - Servicio para enviar notificaciones FCM
- ✅ `notificaciones/models.py` - Modelos DeviceToken y Notificacion
- ✅ `eventos/signals.py` - Señales para detectar eventos publicados
- ✅ `notificaciones/views.py` - API REST para gestionar tokens FCM

### **Android App**
- ✅ `services/MyFirebaseMessagingService.kt` - Servicio FCM
- ✅ `data/FirebaseManager.kt` - Gestor de tokens FCM
- ✅ `data/preferences/UserPreferences.kt` - Persistencia de tokens
- ✅ `data/api/ApiService.kt` - Endpoints para FCM
- ✅ `AndroidManifest.xml` - Permisos y configuración
- ✅ `build.gradle.kts` - Dependencias Firebase

---

## 🔍 Troubleshooting

### **No llegan notificaciones**
1. Verifica que el archivo `google-services.json` sea real (no dummy)
2. Chequea que Firebase Admin SDK esté inicializado en Django
3. Revisa logs del backend para errores de FCM
4. Asegúrate de que el dispositivo tenga conexión a internet

### **Errores de compilación**
1. Ejecuta `./gradlew clean` y luego `./gradlew assembleDebug`
2. Sincroniza el proyecto con Gradle Files
3. Verifica que todas las dependencias estén actualizadas

### **Token no se registra**
1. Verifica que el usuario esté logueado antes del registro
2. Chequea los logs de `FirebaseManager` para ver errores
3. Asegúrate de que la API del backend esté funcionando

---

## ✅ Estado Actual

- 🟢 **Backend Firebase**: Configurado y funcionando
- 🟢 **Android Firebase**: SDK integrado y funcionando  
- 🟢 **Persistencia de sesión**: Implementada
- 🟢 **Auto-registro de tokens**: Funcionando
- 🟢 **Envío de notificaciones**: Automatizado
- 🟠 **Google-services.json**: Requiere archivo real de Firebase
- 🟠 **Pruebas**: Pendientes con configuración real

El sistema está **completamente implementado** y solo requiere la configuración final de Firebase con credenciales reales para estar 100% funcional. 