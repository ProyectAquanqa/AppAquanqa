# Design Document - Rediseño del Tema Oscuro

## Overview

Este documento describe el rediseño completo del tema oscuro de la aplicación AquanQA, implementando un esquema de colores minimalista y contrastante basado en negro puro como color principal, con elementos de navegación en gris oscuro y acentos en los colores corporativos de la marca.

## Architecture

### Paleta de Colores Aprobada

#### Colores Base
- **Negro Principal**: `#000000` - Para todos los fondos principales
- **Gris Navegación**: `#1f2937` - Para elementos de navegación únicamente

#### Colores de Texto
- **Texto Primario**: `#FFFFFF` - Para texto principal
- **Texto Secundario**: `#D1D5DB` - Para texto de menor jerarquía

#### Colores Corporativos (Acciones e Interacciones)
- **Azul AquanQA**: `#3B82F6` - Para botones primarios y elementos interactivos
- **Verde AquanQA**: `#10B981` - Para elementos de éxito y acciones secundarias

#### Colores de Estado
- **Éxito**: `#10B981` - Estados positivos
- **Error**: `#EF4444` - Estados de error
- **Advertencia**: `#F59E0B` - Estados de advertencia

### Jerarquía Visual

```
┌─ Navegación (#1f2937)
│  ├─ BottomNavigationView
│  ├─ NavigationDrawer
│  └─ Toolbar/AppBar
│
├─ Contenido Principal (#000000)
│  ├─ Fragments
│  ├─ Cards
│  └─ Contenedores
│
├─ Texto
│  ├─ Primario (#FFFFFF)
│  └─ Secundario (#D1D5DB)
│
└─ Elementos Interactivos
   ├─ Acciones Primarias (#3B82F6)
   ├─ Acciones Secundarias (#10B981)
   └─ Estados (Error: #EF4444, Warning: #F59E0B, Success: #10B981)
```

## Components and Interfaces

### 1. Sistema de Colores

#### colors.xml - Nueva Estructura
```xml
<!-- Colores Base del Tema Oscuro -->
<color name="dark_background_primary">#000000</color>
<color name="dark_background_navigation">#1f2937</color>

<!-- Colores de Texto -->
<color name="dark_text_primary">#FFFFFF</color>
<color name="dark_text_secondary">#D1D5DB</color>

<!-- Colores Corporativos -->
<color name="dark_aquanqa_blue">#3B82F6</color>
<color name="dark_aquanqa_green">#10B981</color>

<!-- Colores de Estado -->
<color name="dark_success">#10B981</color>
<color name="dark_error">#EF4444</color>
<color name="dark_warning">#F59E0B</color>
```

### 2. Componentes de Navegación

#### BottomNavigationView
- **Fondo**: `#1f2937`
- **Iconos Activos**: `#3B82F6`
- **Iconos Inactivos**: `#D1D5DB`
- **Texto**: `#D1D5DB`

#### NavigationDrawer
- **Fondo**: `#1f2937`
- **Items Activos**: `#3B82F6`
- **Items Inactivos**: `#D1D5DB`
- **Texto**: `#FFFFFF`

#### Toolbar/AppBar
- **Fondo**: `#1f2937`
- **Título**: `#FFFFFF`
- **Iconos**: `#FFFFFF`

### 3. Componentes de Contenido

#### Fragments
- **Fondo**: `#000000`
- **Contenedores**: `#000000`

#### MaterialCardView
- **Fondo**: `#000000`
- **Borde**: `#D1D5DB` (sutil)
- **Elevación**: Sombra sutil

#### TextViews
- **Títulos**: `#FFFFFF`
- **Contenido**: `#FFFFFF`
- **Subtítulos**: `#D1D5DB`
- **Metadatos**: `#D1D5DB`

### 4. Componentes Interactivos

#### Buttons
- **Primarios**: Fondo `#3B82F6`, Texto `#FFFFFF`
- **Secundarios**: Borde `#10B981`, Texto `#10B981`
- **Texto**: Color `#3B82F6`

#### TextInputLayout
- **Fondo**: `#000000`
- **Borde**: `#D1D5DB`
- **Borde Activo**: `#3B82F6`
- **Texto**: `#FFFFFF`
- **Hint**: `#D1D5DB`

#### Chips
- **Fondo**: `#000000`
- **Borde**: `#D1D5DB`
- **Texto**: `#FFFFFF`
- **Seleccionado**: Fondo `#3B82F6`

### 5. Estados y Feedback

#### Estados de Carga
- **ProgressBar**: `#3B82F6`
- **Fondo Track**: `#D1D5DB`

#### Estados de Error/Éxito
- **Error**: `#EF4444`
- **Éxito**: `#10B981`
- **Advertencia**: `#F59E0B`

## Data Models

### ColorScheme
```kotlin
data class DarkColorScheme(
    val backgroundPrimary: String = "#000000",
    val backgroundNavigation: String = "#1f2937",
    val textPrimary: String = "#FFFFFF",
    val textSecondary: String = "#D1D5DB",
    val aquanqaBlue: String = "#3B82F6",
    val aquanqaGreen: String = "#10B981",
    val success: String = "#10B981",
    val error: String = "#EF4444",
    val warning: String = "#F59E0B"
)
```

## Error Handling

### Validación de Colores
- Verificar que todos los colores cumplan con WCAG AA para contraste
- Validar que no se usen colores fuera de la paleta aprobada
- Alertas en desarrollo si se detectan colores no aprobados

### Fallbacks
- Si un color no está definido, usar el color base correspondiente
- Logging de colores faltantes en modo debug

## Testing Strategy

### Pruebas Visuales
1. **Contraste**: Verificar que todos los textos cumplan WCAG AA
2. **Consistencia**: Validar que todos los fragments usen la paleta correcta
3. **Navegación**: Confirmar que elementos de navegación usen `#1f2937`
4. **Contenido**: Verificar que contenido principal use `#000000`

### Pruebas Automatizadas
1. **AccessibilityValidator**: Validar contraste de colores
2. **ColorSchemeTest**: Verificar que solo se usen colores aprobados
3. **ComponentTest**: Validar que componentes usen colores correctos

### Casos de Prueba
1. Navegación entre todos los fragments
2. Interacción con botones y elementos interactivos
3. Estados de error, éxito y advertencia
4. Modo claro/oscuro (si aplica)

## Implementation Notes

### Orden de Implementación
1. Actualizar `colors.xml` con nueva paleta
2. Actualizar `styles.xml` con nuevos estilos
3. Actualizar layouts de navegación
4. Actualizar layouts de fragments
5. Actualizar componentes Material Design
6. Validar y probar

### Consideraciones Especiales
- Mantener accesibilidad en todos los cambios
- Preservar funcionalidad existente
- Documentar cambios para futuros desarrolladores
- Crear guía de colores para referencia

### Archivos a Modificar
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/styles.xml`
- Todos los layouts de fragments
- Layouts de navegación
- Componentes Material Design personalizados