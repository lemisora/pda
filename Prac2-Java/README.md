# Proyecto Maven PDA (Práctica 2)

Este proyecto ha sido convertido a una estructura Maven estándar.

## Estructura del Proyecto

- `src/main/java/com/distribuida`: Código fuente Java.
- `pom.xml`: Configuración de Maven.

## Requisitos

- Java 17 o superior.
- Maven 3.6 o superior.

## Compilación y Ejecución

### Usando Maven (Recomendado)

Para compilar el proyecto:
```bash
mvn clean package
```

Para ejecutar el servidor:
```bash
mvn exec:java -Dexec.mainClass="com.distribuida.ServidorArchivos"
```

Para ejecutar el cliente:
```bash
mvn exec:java -Dexec.mainClass="com.distribuida.GestorArchivosCliente"
```

### Sin Maven (Manual)

Si no tienes Maven instalado, puedes compilar manualmente:
```bash
javac -d bin src/main/java/com/distribuida/*.java
```

Y ejecutar:
```bash
java -cp bin com.distribuida.ServidorArchivos
java -cp bin com.distribuida.GestorArchivosCliente
```
