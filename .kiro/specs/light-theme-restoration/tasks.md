# Lista de Tareas - Restauración del Tema Claro

## Tareas de Implementación

- [x] 1. Limpiar y simplificar el archivo colors.xml


  - Eliminar todos los colores con prefijo "dark_"
  - Crear nueva paleta de colores claros simplificada
  - Definir colores corporativos AquaNQA (#1E88E5, #1565C0, #42A5F5)
  - Definir colores de fondo claros (#FFFFFF, #F8F9FA)
  - Definir colores de texto oscuros (#212529, #6C757D, #ADB5BD)
  - Definir colores de bordes claros (#DEE2E6, #CED4DA, #E9ECEF)
  - Definir colores de estado (success, warning, error)
  - _Requirements: 1.1, 2.1, 6.1, 6.2_

- [x] 2. Actualizar styles.xml para tema claro únicamente


  - Eliminar todos los estilos relacionados con tema oscuro
  - Crear AppTheme.Light como tema principal
  - Definir estilos base para cards, botones e inputs
  - Configurar colores de Material Components para tema claro
  - Actualizar estilos de ProfileDivider para tema claro
  - _Requirements: 1.2, 2.2, 7.1_

- [x] 3. Actualizar MainActivity y componentes globales


  - Aplicar AppTheme.Light en AndroidManifest.xml
  - Configurar StatusBar y NavigationBar para tema claro
  - Actualizar Toolbar con colores corporativos
  - Configurar colores globales de la aplicación
  - _Requirements: 3.3, 5.1, 5.2, 5.4_

- [x] 4. Actualizar NavigationDrawer para tema claro


  - Cambiar fondo del drawer a blanco (#FFFFFF)
  - Actualizar header con azul corporativo
  - Configurar items del menú con texto oscuro
  - Actualizar iconos con azul corporativo
  - Configurar estados selected con fondo claro
  - _Requirements: 4.6, 5.3, 7.2_

- [x] 5. Restaurar ProfileFragment a tema claro




  - Cambiar fondo principal a blanco (#FFFFFF)
  - Actualizar cards con fondos blancos y sombras sutiles
  - Cambiar textos a colores oscuros para contraste
  - Actualizar iconos con azul corporativo
  - Configurar foto de perfil con borde azul
  - Actualizar botón de acción con azul corporativo
  - _Requirements: 4.1, 6.2, 7.1, 7.2_

- [x] 6. Restaurar EditProfileFragment a tema claro


  - Cambiar fondo principal a blanco (#FFFFFF)
  - Actualizar header card con fondo blanco
  - Configurar TextInputLayout con bordes claros y focus azul
  - Actualizar iconos de campos con azul corporativo
  - Configurar botones con estilos claros (outline y filled)
  - Actualizar sección de firma con fondo blanco
  - _Requirements: 4.2, 6.2, 7.1, 7.2_

- [x] 7. Actualizar ProfileFieldHelper para tema claro


  - Modificar configuración automática para usar colores claros
  - Actualizar iconos para usar azul corporativo
  - Configurar textos con colores oscuros para contraste
  - Actualizar divisores con colores claros
  - _Requirements: 6.2, 7.1, 8.1_

- [ ] 8. Actualizar fragment de chatbot para tema claro
  - Cambiar fondo principal a blanco (#FFFFFF)
  - Configurar mensajes de usuario con fondo azul claro
  - Configurar mensajes de bot con fondo gris muy claro
  - Actualizar área de input con fondo blanco y bordes claros
  - Configurar botón de envío con azul corporativo
  - Actualizar sugerencias con fondos claros
  - _Requirements: 4.3, 6.1, 7.2_

- [ ] 9. Actualizar fragment de login para tema claro
  - Cambiar fondo principal a blanco (#FFFFFF)
  - Configurar logo/header con azul corporativo
  - Actualizar campos de entrada con fondos blancos y bordes claros
  - Configurar labels con colores de texto secundario
  - Actualizar botones con estilos primary y secondary claros
  - Configurar mensajes de error con color rojo apropiado
  - _Requirements: 4.4, 6.1, 7.2_

- [ ] 10. Actualizar todos los demás fragments para tema claro
  - Identificar todos los fragments restantes en la aplicación
  - Aplicar sistemáticamente la paleta de colores claros
  - Actualizar fondos, textos, botones e iconos
  - Verificar consistencia visual en cada fragment
  - _Requirements: 4.5, 6.1, 6.2, 7.2_

- [ ] 11. Limpiar drawables y recursos de tema oscuro


  - Eliminar todos los drawables específicos de tema oscuro
  - Actualizar selectores de color para usar solo colores claros
  - Limpiar recursos no utilizados del tema oscuro
  - Verificar que no queden referencias a recursos oscuros
  - _Requirements: 1.3, 8.3, 8.4_

- [x] 12. Actualizar item_profile_field.xml para tema claro




  - Cambiar colores de texto a oscuros (#212529, #6C757D)
  - Actualizar tint de iconos a azul corporativo
  - Configurar actionIcon con colores claros apropiados
  - _Requirements: 6.2, 7.1, 8.2_

- [ ] 13. Verificar y limpiar código Kotlin
  - Buscar y eliminar referencias a colores oscuros en código Kotlin
  - Actualizar cualquier configuración programática de colores
  - Limpiar imports no utilizados relacionados con tema oscuro
  - _Requirements: 8.1, 8.5_

- [ ] 14. Compilar y probar la aplicación


  - Ejecutar compilación completa para verificar ausencia de errores
  - Verificar que no existan warnings sobre recursos no utilizados
  - Probar navegación entre todos los fragments
  - Validar consistencia visual en toda la aplicación
  - _Requirements: 1.4, 8.4, 8.5_

- [ ] 15. Pruebas de contraste y accesibilidad
  - Verificar que todos los textos cumplan WCAG 2.1 AA
  - Probar legibilidad en diferentes tamaños de pantalla
  - Validar navegación con herramientas de accesibilidad
  - Verificar contraste de colores en todos los elementos
  - _Requirements: 6.1, 6.2, 7.1_

- [ ] 16. Validación final de consistencia
  - Revisar que todos los fragments usen la misma paleta
  - Verificar que elementos similares tengan el mismo estilo
  - Validar tipografías consistentes en toda la aplicación
  - Confirmar espaciados y esquinas redondeadas uniformes
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_