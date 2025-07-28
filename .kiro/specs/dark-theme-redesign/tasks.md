# Implementation Plan - Rediseño del Tema Oscuro

- [x] 1. Actualizar paleta de colores base


  - Reemplazar todos los colores del tema oscuro en colors.xml con la nueva paleta minimalista
  - Eliminar colores no aprobados y mantener solo: negro (#000000), gris navegación (#1f2937), textos (blanco y #D1D5DB), colores corporativos y de estado
  - _Requirements: 1.1, 6.1, 6.2_

- [x] 2. Actualizar estilos de navegación



  - Modificar estilos de BottomNavigationView para usar fondo #1f2937
  - Actualizar estilos de NavigationDrawer para usar fondo #1f2937
  - Configurar estilos de Toolbar/AppBar para usar fondo #1f2937
  - _Requirements: 2.1, 2.2, 2.3_


- [ ] 3. Actualizar estilos de componentes Material Design
  - Modificar estilos de MaterialCardView para usar fondo negro
  - Actualizar estilos de botones para usar colores corporativos
  - Configurar estilos de TextInputLayout para tema negro
  - Actualizar estilos de otros componentes (Chips, ProgressBar, etc.)
  - _Requirements: 8.1, 8.2, 8.3, 4.1, 4.2_



- [ ] 4. Actualizar layouts de fragments principales
  - Modificar fragment_home.xml para usar fondo negro
  - Actualizar fragment_beneficios.xml para usar fondo negro y textos apropiados


  - Modificar fragment_anuncios.xml para usar fondo negro
  - _Requirements: 1.2, 7.1, 7.2_



- [ ] 5. Actualizar layouts de fragments de perfil
  - Modificar fragment_profile.xml para usar nueva paleta de colores
  - Actualizar fragment_edit_profile.xml para usar fondo negro y elementos apropiados


  - _Requirements: 1.2, 7.1, 7.2_

- [ ] 6. Actualizar layout de fragment de chatbot
  - Modificar fragment_chatbot.xml para usar fondo negro


  - Actualizar layouts de items de mensajes para nueva paleta
  - _Requirements: 1.2, 7.1, 7.2_

- [x] 7. Actualizar layouts de navegación

  - Modificar activity_main.xml para elementos de navegación con #1f2937
  - Actualizar app_bar_main.xml para usar fondo #1f2937
  - Modificar nav_header_main.xml para usar fondo #1f2937
  - _Requirements: 2.1, 2.2, 2.3_




- [ ] 8. Actualizar layouts de items y componentes
  - Modificar item_anuncio.xml para usar fondo negro y textos apropiados
  - Actualizar otros layouts de items para nueva paleta
  - Configurar elementos interactivos con colores corporativos
  - _Requirements: 1.3, 3.1, 3.2, 4.3_

- [ ] 9. Actualizar configuración de accesibilidad
  - Modificar AccessibilityConfig.kt para usar nueva paleta validada
  - Actualizar RuntimeAccessibilityChecker.kt para validar nuevos colores
  - Configurar validaciones de contraste para nueva paleta
  - _Requirements: 6.3, 8.4_

- [ ] 10. Validar y probar implementación
  - Ejecutar pruebas de accesibilidad con nueva paleta
  - Verificar contraste WCAG AA en todos los componentes
  - Probar navegación entre fragments con nuevo tema
  - Validar que no se usen colores fuera de la paleta aprobada
  - _Requirements: 7.3, 8.4_