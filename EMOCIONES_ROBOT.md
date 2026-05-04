# 😊 GUÍA DE EMOCIONES DEL ROBOT SANBOT

## 📚 Referencia del Manual (Sección 3.7.3)

El robot Sanbot tiene acceso a múltiples emociones que se pueden usar en el juego.

---

## 🎭 EMOCIONES DISPONIBLES (Enum EmotionsType)

### **POSITIVAS - Para Aciertos** ✅
```
SMILE       😊 Sonrisa - Alegría básica
PRISE       🤩 Orgullo - Sensación de logro (RECOMENDADA PARA ACIERTOS)
LAUGHTER    😂 Risa - Diversión, más intensidad
KISS        😘 Beso - Afecto
```

### **NEGATIVAS - Para Errores** ❌
```
CRY         😢 Llora - Tristeza (RECOMENDADA PARA ERRORES)
GRIEVANCE   😔 Queja - Decepción menor
ABUSE       😠 Abuso - Enojo (NO RECOMENDADO, puede asustar)
ANGRY       😡 Enfado - Ira (NO RECOMENDADO)
```

### **NEUTRAL/OTRAS**
```
NORMAL      😐 Neutral - Cara normal
QUESTION    🤔 Pregunta - Confusión, duda
FAINT       😵 Desmayo - Sorpresa extrema
SNICKER     🙃 Burla - Risa burlona (evitar)
```

### **ESPECIALES** 🎪
```
SURPRISE    😲 Sorpresa - Shock positivo
WHISTLE     😏 Silbido - Admiración
GOODBYE     👋 Adiós - Despedida
SHY         🙈 Tímido - Vergüenza leve
SWEAT       😅 Sudor - Nerviosismo
PICKNOSE    🙍 Hurgarse - Burla
SLEEP       😴 Sueño - Aburrimiento
ARROGANCE   😏 Arrogancia - Prepotencia
```

---

## 🎯 RECOMENDACIONES PARA EL JUEGO

### **PARA ACIERTOS** ✅ (Primera opción → Segunda opción → Etc.)
```java
// Opción 1 - Segura y clara
mostrarEmocion("PRISE");  // Orgullo/Felicidad

// Opción 2 - Más intensidad
mostrarEmocion("SMILE");  // Sonrisa

// Opción 3 - Muy entusiasta
mostrarEmocion("LAUGHTER");  // Risa
```

### **PARA ERRORES** ❌ (Primera opción → Segunda opción → Etc.)
```java
// Opción 1 - Empática y no cruel
mostrarEmocion("CRY");  // Triste (como "yo también estoy triste")

// Opción 2 - Menos intensa
mostrarEmocion("GRIEVANCE");  // Queja/decepción

// EVITAR - Pueden asustar o frustrar
// mostrarEmocion("ANGRY");     ❌ Demasiado agresivo
// mostrarEmocion("ABUSE");     ❌ Muy fuerte para mayores
// mostrarEmocion("SNICKER");   ❌ Burlón - poco solidario
```

---

## 📖 EJEMPLOS DE USO EN CÓDIGO

### Acierto Positivo
```java
if (esCorrect) {
    aciertos++;
    mostrarEmocion("PRISE");               // Robot orgulloso
    moverBrazos("LEVANTAR_BRAZO", "AMBOS"); // Celebración
    hablarOSimular("¡Correcto! Excelente");
}
```

### Error Empático
```java
else {
    mostrarEmocion("CRY");  // Robot triste (solidaridad)
    hablarOSimular("Lástima, te acercabas");
    // No burlarse
}
```

### Sorpresa (Para evento especial)
```java
if (aciertos == preguntasDelTema.size()) {
    mostrarEmocion("SURPRISE");
    hablarOSimular("¡Has acertado todas!");
}
```

---

## ⚠️ GUÍA DE MEJOR PRÁCTICA

### ✅ HACER
- Usar **PRISE** para aciertos → refuerzo positivo
- Usar **CRY** para errores → validar emoción del usuario
- Cambiar emociones según **contexto del juego**
- Combinar emociones con **voz apropiada**
- Usar **brazos** para intensificar emoción

### ❌ NO HACER
- Usar **ANGRY** o **ABUSE** regularmente (asusta)
- Burlar al usuario con **SNICKER** (desmoralizante)
- Alternar emociones constantemente (confunde)
- Mostrar emoción sin voz de apoyo
- Usar emociones incompatibles con el mensaje

---

## 🎬 SECUENCIAS RECOMENDADAS

### Acierto Perfecto
```
1. Muestra PRISE (0ms)
2. Levanta brazos (100ms)
3. Dice en voz alta "¡Correcto!"
4. Espera 2000ms
5. Baja brazos y siguiente pregunta
```

### Error Cercano
```
1. Muestra CRY (0ms)
2. Dice "Lástima, te acercabas"
3. Espera 2000ms
4. Siguiente pregunta
```

### Juego Completado (Pleno)
```
1. Muestra SURPRISE (0ms)
2. Levanta brazos con LAUGHTER
3. Dice "¡Pleno! ¡Excelente trabajo!"
4. Baila/gira cabeza
5. Pantalla de resultados
```

---

## 📊 COMPATIBILIDAD EMOCIONES + ACCIONES

| Emoción | + Brazos | + Cabeza | + Voz | Nivel |
|---------|---------|---------|-------|-------|
| PRISE | ✅ Si | ✅ Si | ✅ Si | 🟢 |
| SMILE | ⚠️ Opcional | ✅ Si | ✅ Si | 🟢 |
| CRY | ⚠️ No | ⚠️ Abajo | ✅ Si | 🟢 |
| LAUGHTER | ✅ Si | ✅ Si | ✅ Si | 🟡 |
| SURPRISE | ✅ Si | ✅ Arriba | ✅ Si | 🟡 |
| GRIEVANCE | ⚠️ No | ⚠️ Abajo | ✅ Si | 🟢 |
| ANGRY | ❌ No | ❌ No | ⚠️ Sí | 🔴 |

---

## 🔄 ACTUALIZAR EMOCIÓN DURANTE SESIÓN

```java
public class JuegoMiniRoscoActivity extends BaseActivity {
    
    private String emocionActual = "NORMAL";
    
    // Cambiar emoción cuando quieras
    private void cambiarEmocionAleatoria() {
        String[] emociones = {"SMILE", "PRISE", "LAUGHTER"};
        int random = new Random().nextInt(emociones.length);
        mostrarEmocion(emociones[random]);
    }
    
    // O cambiar según condición
    private void mostrarEmocionSegunResultado(boolean es_correcto) {
        emocionActual = es_correcto ? "PRISE" : "CRY";
        mostrarEmocion(emocionActual);
    }
}
```

---

## 🎓 NOTAS PEDAGÓGICAS

### Por qué PRISE para aciertos:
- ✅ Transmite **orgullo compartido**
- ✅ No es "burla" del usuario
- ✅ Comunica que el robot **valida el éxito**
- ✅ Diferencia clara respecto a fallo

### Por qué CRY para errores:
- ✅ **Empatía**: El robot está triste CONTIGO
- ✅ No es **culpabilización**: Dice "yo también"
- ✅ **Emocional sin ser cruel**
- ✅ Más empática que "ANGRY" o "ABUSE"

### Evitar ANGRY o ABUSE:
- ❌ Pueden **asustar** a personas mayores
- ❌ Pueden causar **frustración** o **abandono**
- ❌ No son apropiados para **contexto lúdico**
- ❌ Pueden ser **percibidos como castigo**

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

En el archivo `JuegoMiniRoscoActivity.java`:

- [x] Importar BaseActivity
- [x] Llamar a `mostrarEmocion()` en aciertos: PRISE
- [x] Llamar a `mostrarEmocion()` en errores: CRY
- [x] Llamar a `moverBrazos()` en aciertos
- [x] Llamar a `hablarOSimular()` para feedback
- [ ] Agregar más emociones para eventos especiales
- [ ] Testear con personas reales >65 años
- [ ] Obtener feedback sobre timing emocional
- [ ] Ajustar según reacciones reales

---

## 📞 SOPORTE

Si las emociones no funcionan:
1. Verificar que el robot está **conectado**
2. Verificar que `systemControl != null` en BaseActivity
3. Revisar que `mostrarEmocion()` recibe string válido
4. Probar con emociones "SMILE" (más universales)
5. Consultar logs: `adb logcat | grep SystemControl`

---

**Guía realizada basada en:** Sanbot OpenSDK Documentation V1.1.8 (Manual adjunto)  
**Óptimo para:** Personas mayores, contextos educativos lúdicos.
