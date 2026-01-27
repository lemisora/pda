#ifndef PROTOCOLO_H
#define PROTOCOLO_H

// Definición de acciones del protocolo
typedef enum {
    REG_WORKER = 0,
    LIST_FILES = 1,
    GET_STATUS = 2,
    SOLICITAR_WORKER = 3,
    CONFIRM_WORKER = 4,
    WRITE_FILE = 5,
    ALIVE = 6,
    RESPUESTA_OK = 200,
    RESPUESTA_ERR = 500
} action_t;

// Estructura fija de comunicación (136 bytes aprox)
typedef struct {
    action_t accion;
    char msg[128]; // IP, Nombre de archivo, o Mensaje error
    int valor;     // Puerto, Tamaño, etc.
} paquete_t;

// Funciones "Wrapper" para enviar/recibir de forma segura
int send_packet(int socket, paquete_t* p);
int recv_packet(int socket, paquete_t* p);

#endif