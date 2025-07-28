# Requirements Document

## Introduction

Esta funcionalidad implementará un rediseño completo de la aplicación AppAquanqa para usar exclusivamente modo oscuro, eliminando el modo claro actual. El nuevo tema utilizará el color `#1F2937` como color base de fondo y texto blanco para garantizar una excelente legibilidad y una experiencia visual moderna y consistente.

## Requirements

### Requirement 1

**User Story:** Como usuario de AppAquanqa, quiero que toda la aplicación use un tema oscuro consistente con el color `#1F2937` como fondo principal, para tener una experiencia visual moderna y cómoda para mis ojos.

#### Acceptance Criteria

1. WHEN la aplicación se inicia THEN todos los fragments deben mostrar el fondo con color `#1F2937`
2. WHEN navego entre diferentes pantallas THEN el tema oscuro debe mantenerse consistente en toda la aplicación
3. WHEN veo cualquier texto THEN debe ser de color blanco o colores claros para garantizar legibilidad sobre el fondo oscuro
4. WHEN interactúo con elementos de la UI THEN deben tener colores apropiados para el tema oscuro

### Requirement 2

**User Story:** Como usuario, quiero que todos los fragments (profile, chatbot, login, etc.) tengan una apariencia consistente con el nuevo tema oscuro, para que la experiencia sea uniforme en toda la aplicación.

#### Acceptance Criteria

1. WHEN accedo al fragment de profile THEN debe usar el fondo `#1F2937` y texto blanco
2. WHEN uso el chatbot THEN debe tener el tema oscuro aplicado consistentemente
3. WHEN veo la pantalla de login THEN debe seguir el mismo esquema de colores oscuros
4. WHEN navego por el drawer menu THEN debe estar adaptado al tema oscuro
5. WHEN veo cards o elementos de UI THEN deben usar colores apropiados para el tema oscuro

### Requirement 3

**User Story:** Como usuario, quiero que los colores de Aquanqa (azules y verdes) se adapten correctamente al tema oscuro, para mantener la identidad visual de la marca mientras uso el modo oscuro.

#### Acceptance Criteria

1. WHEN veo elementos con colores de marca THEN deben ser versiones adaptadas para tema oscuro
2. WHEN interactúo con botones THEN deben usar colores Aquanqa compatibles con fondo oscuro
3. WHEN veo iconos THEN deben tener colores que contrasten bien con el fondo `#1F2937`
4. WHEN veo elementos destacados THEN deben usar colores que mantengan la identidad Aquanqa

### Requirement 4

**User Story:** Como usuario, quiero que todos los elementos interactivos (botones, cards, inputs) tengan una apariencia moderna y consistente con el tema oscuro, para una experiencia de usuario óptima.

#### Acceptance Criteria

1. WHEN veo cards THEN deben tener fondos ligeramente más claros que `#1F2937` para crear jerarquía visual
2. WHEN interactúo con botones THEN deben tener estados hover/pressed apropiados para tema oscuro
3. WHEN uso inputs de texto THEN deben tener fondos y bordes visibles sobre el fondo oscuro
4. WHEN veo separadores o líneas THEN deben ser visibles pero sutiles en el tema oscuro

### Requirement 5

**User Story:** Como desarrollador, quiero que el sistema de colores esté centralizado y bien organizado, para facilitar el mantenimiento y futuras actualizaciones del tema.

#### Acceptance Criteria

1. WHEN se definen colores THEN deben estar centralizados en colors.xml
2. WHEN se crean nuevos elementos THEN deben usar los colores definidos del sistema
3. WHEN se necesite modificar un color THEN debe poder hacerse desde un lugar central
4. WHEN se agreguen nuevos fragments THEN deben seguir automáticamente el tema oscuro establecido