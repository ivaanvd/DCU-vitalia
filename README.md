# Vitalia - Asistente Robótico para Personas Mayores

Vitalia es una solución integral para el robot **Sanbot (Qihan)** diseñada para mejorar la calidad de vida de personas mayores en entornos asistenciales o domésticos.

## 📂 Contenido del repositorio
* `/app`: Código fuente de la aplicación móvil y del sistema robótico.
* `/docs/documento_diseno_vitalia.pdf`: Memoria completa con la investigación de usuarios, arquitectura de la información y validación del sistema.

---

## 🏗️ Arquitectura y Flavors

El proyecto implementa un sistema multi-flavor para facilitar el desarrollo:
*   **`robot`**: Versión productiva. Utiliza el SDK de Qihan para controlar el hardware real (sensores, motores, voz).
*   **`tablet`**: Versión de desarrollo. Simula el comportamiento del robot en dispositivos estándar para pruebas de interfaz y flujo.

## 📂 Organización de Módulos

### 🔵 Actividades y Recordatorios
Módulos principales para la gestión del día a día. Utilizan un patrón de **Wizard** (asistente por pasos) para la entrada de datos.
*   **Gestores**: `ActividadDialogManager` y `RecordatorioDialogManager` desacoplan la creación de diálogos de la Activity principal.
*   **Ayudantes**: `WizardHelper` centraliza la actualización visual de los pasos y resaltados.
*   **Persistencia**: `ActividadRepository` gestiona el almacenamiento local y la sincronización con el `AlarmScheduler`.

### 🎮 Juegos Cognitivos
Minijuegos diseñados para la estimulación mental:
*   **Bingo**: Juego grupal con cartones generados dinámicamente.
*   **Rosco/Pasapalabra**: Definiciones y palabras por letra.
*   **Refranes**: Completar frases populares.
*   **Busca y Encuentra**: Encuentra el objeto en la pantalla.

### ⏰ Sistema de Alertas (`alarmas`)
Garantiza que el usuario no olvide sus tareas:
*   **Scheduler**: Programa alarmas exactas en el sistema Android.
*   **Receiver**: Captura el evento del sistema incluso si la app está cerrada.
*   **Popup**: Interfaz de alta prioridad que "despierta" al robot para anunciar el aviso.

---

## 🎨 Investigación, Requisitos y Diseño Centrado en el Usuario (UX)

Antes de la codificación, el ecosistema de Vitalia se consolidó a través de una metodología completa de diseño orientada a producto (detallada en el documento PDF dentro de la carpeta `/docs`):

### 1. Investigación Basada en Datos (Data-Driven UX Research)
* Recopilación y análisis cuantitativo de las necesidades del público objetivo y cuidadores mediante encuestas estructuradas.
* Procesamiento de datos y métricas empíricas para fundamentar las decisiones de diseño, asegurar la aceptación tecnológica y mitigar las barreras de fricción digital en la tercera edad.

### 2. Ingeniería de Requisitos y Modelado de Comportamiento
* Definición formal de la funcionalidad principal, viabilidad técnica de la plataforma y selección de componentes de hardware/software.
* Modelado conceptual de usuarios y flujos lógicos mediante herramientas de la industria como *User Journey Maps*, arquetipos de *Personas* y escenarios complejos de interacción humano-robot.

### 3. Arquitectura de la Información y Accesibilidad
* Estructuración lógica del árbol de contenidos y de las pantallas de navegación del asistente.
* Diseño de interfaces guiadas por principios de usabilidad, contraste adaptado y flujos conversacionales de baja carga cognitiva.

---

## 📊 Diagramas de Arquitectura

### 🧬 Estructura de Clases
Este diagrama muestra la relación jerárquica y de composición entre los componentes principales del módulo de actividades.

```mermaid
classDiagram
    class BaseActivity {
        <<Abstract>>
        #SpeechManager speechManager
        #hablarEnMain(String)
        #escuchar()
        +onCabezaTocada()*
    }
    
    class MainActivity {
        -iniciarAppRobot()
        +navegarAActividades()
    }
    
    class ActividadesActivity {
        -ActividadRepository repo
        -ActividadDialogManager dialogManager
        +actualizarProgresoYResaltadoAct(Campo)
    }
    
    class ActividadDialogManager {
        -ActividadesActivity activity
        +mostrarDialogoAnadir(Actividad)
        +mostrarDialogoDetalle(Actividad)
    }
    
    class ActividadWizardHelper {
        -AlertDialog dialog
        +actualizarProgresoYResaltado(Campo)
    }
    
    class ActividadRepository {
        -Context context
        +getAll() List
        +add(Actividad)
        +update(Actividad)
    }

    BaseActivity <|-- MainActivity
    BaseActivity <|-- ActividadesActivity
    ActividadesActivity *-- ActividadDialogManager
    ActividadesActivity *-- ActividadRepository
    ActividadDialogManager ..> ActividadWizardHelper
    ActividadRepository ..> Actividad
```

### 🗣️ Secuencia: Creación de Actividad (Asistente de Voz)
Describe cómo el sistema coordina la voz, los sensores físicos y el procesamiento de lenguaje natural.

```mermaid
sequenceDiagram
    participant U as Usuario
    participant AC as ActividadesActivity
    participant DM as ActividadDialogManager
    participant WH as ActividadWizardHelper
    participant R as Robot (BaseActivity)
    participant VP as VozParser

    AC->>DM: mostrarDialogoAnadir(null)
    DM->>WH: inicializarWizard()
    AC->>R: hablar("¿Qué tipo de actividad?")
    U->>R: Toca cabeza (Sensor físico)
    R->>U: Escucha (STT)
    Note over U,R: El usuario dice "Gimnasia"
    R->>VP: parsearVoz("Gimnasia")
    VP-->>AC: Tipo: GIMNASIA
    AC->>WH: actualizarProgreso(HORA)
    AC->>R: hablar("¿A qué hora?")
```

### 🔔 Secuencia: Disparo de Alerta Programada
Muestra el flujo desde que el sistema Android lanza la alarma hasta que el robot interactúa con el usuario.

```mermaid
sequenceDiagram
    participant AS as AlarmManager (OS)
    participant AR as ActividadAlarmReceiver
    participant REP as ActividadRepository
    participant POP as ActividadPopupActivity
    participant R as Robot (BaseActivity)

    AS->>AR: onReceive()
    AR->>REP: getById(id)
    REP-->>AR: Datos de la actividad
    AR->>POP: startActivity(Popup)
    POP->>R: hablar("Hola, es hora de comenzar la actividad: Gimnasia")
    R->>R: Muestra emoción (Alegre)
```

---

## 🛠️ Especificaciones Técnicas
*   **Android SDK**: API 21+ (Optimizado para Android 6.0/7.1 en Sanbot).
*   **SDK Robótico**: Qihan Sanbot Open SDK.
*   **Localización**: Totalmente en español (ES-es).
*   **Patrones**: Repository Pattern, Delegation (Helpers), State Machine (Wizard).

> **Nota**: Algunos paquetes de bajo nivel (control de motores) pueden encontrarse bajo el path `sandbotapp` debido a herencia de versiones previas del SDK.

---
*Vitalia - Cuidando con tecnología y empatía.*
