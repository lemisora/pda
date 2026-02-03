# Mini Sistema Distribuido "Touché"

Este proyecto implementa un sistema distribuido simple en Java, donde múltiples nodos pueden comunicarse entre sí, realizar una elección de líder y detectar fallos.

## Partes Implementadas

Hasta ahora, el sistema cuenta con las siguientes características funcionales:

- **Nodos:** Se puede instanciar y ejecutar un `Nodo` que opera en una IP y puerto específicos. Cada nodo tiene un ID único, que es crucial para los algoritmos implementados.
- **Comunicación Asíncrona:** Los nodos se comunican de forma asíncrona. Cuentan con un hilo para recibir conexiones (`startReceiver`) y un sistema de cola de mensajes (`colaEnvios`) con un hilo dedicado para procesar y enviar mensajes a otros nodos (`startSender`).
- **Elección de Líder (Algoritmo Bully):**
    1.  Al iniciar, un nodo comienza un proceso de descubrimiento (`startDiscover`) para encontrar otros nodos y decidir un líder.
    2.  Envía un mensaje `HELLO` a los demás.
    3.  Si un nodo con un ID mayor responde con `ALIVE`, el nodo actual se retira de la elección.
    4.  Si después de un tiempo de espera no recibe respuesta de un nodo con ID superior, se autoproclama líder y notifica a los demás con un mensaje `NEW_LEADER`.
- **Detección de Fallos:**
    1.  El nodo líder envía periódicamente mensajes `HEARTBEAT` para notificar que sigue activo.
    2.  Los nodos seguidores monitorean estos `HEARTBEAT`. Si no reciben uno en un intervalo de tiempo definido (`FAILURE_TIMEOUT`), asumen que el líder ha caído y comienzan una nueva elección de líder.
- **Filtrado de Red Básico:** Por seguridad, el `MessageManager` solo procesa conexiones que provienen de direcciones IP de `localhost` o del rango de Tailscale (`100.64.0.0/10`), descartando el resto.
- **Interfaz de Línea de Comandos:** La aplicación utiliza la librería `picocli` para aceptar argumentos al iniciar un nodo, permitiendo configurar su IP, puerto, ID y nombre de forma sencilla.

## Jerarquía de Paquetes y Dependencias

El código fuente está organizado en los siguientes paquetes bajo `com.pda`:

- `Node`: Contiene las clases centrales del sistema.
    - `Nodo.java`: Representa un nodo en el sistema distribuido, orquestando todos los servicios (envío, recepción, elección, etc.).
    - `Mensaje.java`: Define la estructura de los datos que se intercambian entre nodos. Es una clase serializable.
- `Manager`: Clases encargadas de gestionar diferentes responsabilidades del nodo.
    - `MessageManager.java`: Procesa los mensajes entrantes y ejecuta la lógica correspondiente según el tipo de comando (`HELLO`, `ALIVE`, etc.).
    - `StorageManager.java`: (Placeholder) Diseñado para gestionar el almacenamiento, aunque su funcionalidad principal aún no está implementada.
    - `Security/NetFilter.java`: Contiene la lógica para validar las direcciones IP de las conexiones entrantes.
- `Enums`: Contiene las enumeraciones utilizadas en el proyecto.
    - `CommandType.java`: Define los tipos de mensajes que los nodos pueden intercambiar (ej. `HELLO`, `HEARTBEAT`, `NEW_LEADER`).
- `Constants`: Almacena valores constantes para la aplicación.
    - `Net.java`: Define constantes de red como `localhost` y el puerto de escucha por defecto.

### Dependencia Principal

- **Picocli:** Se utiliza para crear la interfaz de línea de comandos de una manera rápida y robusta. La dependencia está declarada en el archivo `pom.xml`.

## Ejecución

Para compilar y ejecutar el proyecto, es necesario tener instalado Maven.

### Compilar y Ejecutar con Maven

Puedes ejecutar un nodo directamente usando el plugin `exec` de Maven. El siguiente comando compila el proyecto y ejecuta la clase principal (`com.pda.App`):

```shell
mvn compile exec:java -Dexec.mainClass="com.pda.App" -Dexec.args="<argumentos>"
```

### Argumentos de Línea de Comandos

Gracias a `picocli`, puedes pasar los siguientes argumentos para configurar el nodo. Para ver la ayuda, puedes ejecutar el programa con `--help`.

- `-I, --IP <ip>`: La dirección IP que usará el nodo. Por defecto es `127.0.0.1`.
- `-p, --port <puerto>`: El puerto en el que el nodo escuchará. Por defecto es `8000`.
- `-i, --id <id>`: Un identificador numérico **único** para el nodo, usado en el algoritmo de elección. Por defecto es `1`.
- `-n, --name <nombre>`: Un nombre para identificar al nodo en los logs. Por defecto es `nodo-`.

#### Ejemplo de Ejecución

Para iniciar tres nodos en la misma máquina para una simulación, puedes abrir tres terminales diferentes y ejecutar:

**Terminal 1 (Futuro Líder por tener el ID más alto):**
```shell
mvn compile exec:java -Dexec.mainClass="com.pda.App" -Dexec.args="--id 3 --port 8002 --name nodo-gamma"
```

**Terminal 2:**
```shell
mvn compile exec:java -Dexec.mainClass="com.pda.App" -Dexec.args="--id 2 --port 8001 --name nodo-beta"
```

**Terminal 3:**
```shell
mvn compile exec:java -Dexec.mainClass="com.pda.App" -Dexec.args="--id 1 --port 8000 --name nodo-alpha"
```

Al iniciarse, los nodos comenzarán el proceso de elección, y después de unos segundos, el `nodo-gamma` (ID 3) debería anunciarse como el nuevo líder. Si detienes ese proceso, los nodos restantes (`alpha` y `beta`) iniciarán una nueva elección y `nodo-beta` (ID 2) se convertirá en el líder.
```
