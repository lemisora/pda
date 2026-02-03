package com.pda.Enums;

public enum CommandType {
    WRITE,  // Orden para escribir en el almacenamiento de un nodo
    ALIVE,   // Orden para verificar si un nodo está activo (y como respuesta de la elección)
    HELLO,  // Orden para votar por nuevo líder (inicia elección)
    NEW_LEADER,  // Orden para mandar el mensaje de que se es el nuevo líder
    HEARTBEAT   // Orden para enviar estado de funcionamiento del líder a los demás
}