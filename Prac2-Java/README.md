# Proyecto Maven PDA - Arquitectura Anillo

Este proyecto implementa un sistema distribuido con topología de **Token Ring** en Java.
Cada nodo actúa como **Cliente** y **Servidor**: crea archivos localmente hasta un límite, y luego los pasa al siguiente nodo en el anillo.

## Estructura del Proyecto

- `src/main/java/com/distribuida`:
    - `Main.java`: Punto de entrada.
    - `Nodo.java`: Lógica de negocio (almacenar vs enviar).
    - `Servidor.java`: Hilo que escucha peticiones del vecino anterior.
    - `Cliente.java`: Cliente TCP para enviar al vecino siguiente.

## Requisitos

- Java 17 o superior.
- Maven 3.6 o superior (opcional).

## Compilación

### Opción 1: Maven
```bash
mvn clean package
```

### Opción 2: Manual (Javac)
```bash
javac -d bin src/main/java/com/distribuida/*.java
```

## Ejecución (Simulación de Anillo)

Para probar el anillo, abre 3 terminales y ejecuta los siguientes comandos en orden:

**Terminal 1 (Nodo A - Puerto 8080):**
Este nodo enviará al 8081.
```bash
# Sintaixs: <puertoLocal> <ipSiguiente> <puertoSiguiente> [archivoInicial]
java -cp bin com.distribuida.Main 8080 127.0.0.1 8081
```

**Terminal 2 (Nodo B - Puerto 8081):**
Este nodo enviará al 8082.
```bash
java -cp bin com.distribuida.Main 8081 127.0.0.1 8082
```

**Terminal 3 (Nodo C - Puerto 8082):**
Este nodo enviará de vuelta al 8080 (Cerrando el anillo).
```bash
java -cp bin com.distribuida.Main 8082 127.0.0.1 8080
```

### Prueba de Funcionamiento
En cualquiera de las consolas, escribe un nombre de archivo (ej: `prueba.txt`) y presiona Enter.
1. El nodo intentará crearlo.
2. Si llega a su límite (default: 5), verás un mensaje: `LLENO. Enviando a vecino...`.
3. El siguiente nodo recibirá la petición y lo creará o pasará.
