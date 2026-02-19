package com.pda.Enums;

public enum CommandType {
    WRITE, // Orden para escribir en el almacenamiento de un nodo
    ALIVE, // Orden para verificar si un nodo está activo (y como respuesta de la elección)
    HELLO, // Orden para votar por nuevo líder (inicia elección)
    NEW_LEADER, // Orden para mandar el mensaje de que se es el nuevo líder
    HEARTBEAT, // Orden para enviar estado de funcionamiento del líder a los demás

    // Comandos para StorageManager y asuntos relacionados al almacenamiento
    STORE_REQUEST, // Nodo pide al líder guardar archivo
    STORE_ASSIGNED, // Líder asigna dónde guardar
    STORE_REJECTED, // Nodo rechaza guardar archivo
    STORE_CONFIRMED, // Nodo confirma que guardó el archivo
    REPLICATE_FILE, // Orden para replicar en otro nodo
    REPLICA_CONFIRMED, // Confirmación de réplica exitosa

    NODE_STATUS_UPDATE, // Nodo reporta su estado al líder (PUSH)

    LIST_REQUEST, // Pedir lista de archivos del sistema
    LIST_RESPONSE // Respuesta con la lista
}