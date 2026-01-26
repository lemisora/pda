#ifndef PROTOCOLO_H
#define PROTOCOLO_H

#define MAXIMO 50
#define MAX_ARCHIVOS 5

typedef enum  { 
    REG_WORKER = 0,
    LIST_FILES = 1,
    GET_STATUS = 2,
    SOLICITAR_WORKER = 3,
    CONFIRM_WORKER = 4,
} action_t;

typedef struct {
    action_t accion;
    char msg[128];  // Dato flexible, puede ser IP, o nombre_archivo
    int valor;      // Valor numérico flexible
} paquete_t;

typedef struct {
    char accion[10];   
    char nombre[MAXIMO]; 
} Datos;

#endif
