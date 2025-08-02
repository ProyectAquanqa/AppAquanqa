# Implementation Plan

- [x] 1. Eliminar archivos del fragment de beneficios





  - Eliminar BeneficiosFragment.kt y BeneficiosViewModel.kt del directorio ui/beneficios
  - Eliminar el directorio completo ui/beneficios
  - _Requirements: 1.1_

- [x] 2. Eliminar recursos de layout y drawables de beneficios





  - Eliminar fragment_beneficios.xml del directorio res/layout
  - Eliminar ic_benefits_filled.xml e ic_benefits_outline.xml del directorio res/drawable
  - Eliminar selector_benefits.xml del directorio res/drawable
  - _Requirements: 1.2_

- [x] 3. Crear estructura base del fragment de lunch





  - Crear directorio ui/lunch
  - Crear LunchFragment.kt que herede de BaseFragment con ViewBinding
  - Implementar métodos básicos getViewBinding() y setupObservers()
  - _Requirements: 2.1_

- [x] 4. Implementar LunchViewModel con funcionalidad básica




  - Crear LunchViewModel.kt que herede de ViewModel
  - Implementar LiveData para texto de lunch con valor inicial
  - Exponer el LiveData como propiedad pública
  - _Requirements: 2.2_

- [x] 5. Crear layout XML para el fragment de lunch





  - Crear fragment_lunch.xml con ConstraintLayout como raíz
  - Agregar TextView con ID text_lunch para mostrar contenido
  - Aplicar estilos consistentes con otros fragments (colores, fuentes, márgenes)
  - _Requirements: 2.3_

- [x] 6. Diseñar iconos de lunch para navegación





  - Crear ic_lunch_filled.xml con diseño de icono sólido relacionado con comida
  - Crear ic_lunch_outline.xml con diseño de icono outline relacionado con comida
  - Crear selector_lunch.xml que alterne entre los dos iconos según estado
  - _Requirements: 3.1, 3.2_

- [x] 7. Actualizar strings de recursos





  - Eliminar title_beneficios de strings.xml
  - Agregar title_lunch con valor "Lunch" a strings.xml
  - _Requirements: 1.5, 3.3_

- [x] 8. Actualizar navegación en mobile_navigation.xml





  - Eliminar fragment navigation_beneficios
  - Agregar fragment navigation_lunch con referencia a LunchFragment
  - Configurar label y layout correctos para el nuevo fragment
  - _Requirements: 1.3, 2.4_

- [x] 9. Actualizar menú de navegación inferior





  - Eliminar item navigation_beneficios de bottom_nav_menu.xml
  - Agregar item navigation_lunch con icono selector_lunch y título title_lunch
  - Mantener el mismo orden de items en el menú
  - _Requirements: 1.4, 2.5_

- [x] 10. Actualizar MainActivity para usar nueva navegación





  - Reemplazar R.id.navigation_beneficios con R.id.navigation_lunch en appBarConfiguration
  - Verificar que no haya otras referencias a beneficios en MainActivity
  - _Requirements: 3.4_
-

- [x] 11. Limpiar referencias en CategoryAdapter si existen




  - Revisar CategoryAdapter.kt para referencias a ic_benefits_filled
  - Actualizar o eliminar referencias según sea necesario
  - Asegurar que no haya referencias rotas después del cambio
  - _Requirements: 4.2_

- [x] 12. Compilar y verificar la aplicación





  - Ejecutar build del proyecto para verificar que no hay errores de compilación
  - Verificar que todas las referencias a beneficios han sido eliminadas
  - Confirmar que el fragment de lunch aparece correctamente en la navegación
  - _Requirements: 4.3, 4.4_