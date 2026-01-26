#include "include/master.h"
#include "include/list.h"
#include <stdio.h>
#include <stdlib.h>
#include "include/protocolo.h"
#include "sys/socket.h"
#include <unistd.h>
#include <string.h>

void* master_global_ptr = NULL;

// Struct para almacenar el estado del maestro
typedef struct status {
    int threshold;
    list_t* workers;
    list_t* files;
} status_t;


void init_master(status_t* master_status, int threshold){
    master_status->threshold = threshold;
    master_status->workers = list_create(4);
    master_status->files = list_create(20);

    // Verificar creación exitosa
    if (!master_status->workers || !master_status->files) {
        fprintf(stderr, "Error: No se pudieron inicializar las listas de estado\n");
    }
}

void print_status_master(status_t master_status){
    printf("\n- Estado del master - \n"
            "\tTotal archivos: %d\n"
            "\tTotal workers: %d\n"
            , master_status.files->size
            , master_status.workers->size
    );
}

int master(int threshold){
    // status_t* master_status = (status_t*)malloc(sizeof(status_t));
    // if(master_status == NULL){
    //     printf("Error al inicializar el maestro\n");
    //     return 1;
    // }
    
    status_t master_status;

    init_master(&master_status, threshold);
    master_global_ptr = &master_status;

    printf("\nMaestro iniciado actualmente con: \n"
            "\tUmbral: %d\n"
            "\tTotal archivos: %d\n"
            "\tTotal workers: %d\n"
            , master_status.threshold
            , master_status.files->size
            , master_status.workers->size
    );

    while(1){
        sleep(1);
        print_status_master(master_status);
    }
    // Liberar memoria al salir
    master_global_ptr = NULL;
    if (master_status.workers) list_destroy(master_status.workers);
    if (master_status.files) list_destroy(master_status.files);
    return 0;
}

// Lógica para obtener el worker adecuado
char* obtener_ip_destino(status_t* master) {
    if (master->workers->size == 0) return NULL;

    // Determinamos el índice del worker basándonos en el total de archivos creados
    int total_creados = master->files->size;
    int index_worker = total_creados / master->threshold;

    // Si el índice calculado supera la cantidad de workers, 
    // usamos el último disponible para evitar desbordamientos.
    if (index_worker >= master->workers->size) {
        index_worker = master->workers->size - 1;
    }

    return list_get(master->workers, index_worker);
}

// Mostrar archivos creados en el sistema distribuido
void mostrar_archivos_maestro(status_t* master) {
    printf("Archivos en el sistema: ");
    for (int i = 0; i < master->files->size; i++) {
        printf("%s  ", list_get(master->files, i));
    }
    printf("\n");
}

void procesar_solicitud_cliente(int socket_cliente, status_t* master_status){
    paquete_t paquete;
    if (recv(socket_cliente, &paquete, sizeof(paquete_t), 0) <= 0) return;
    
    switch (paquete.accion) {
        case REG_WORKER:
            // paquete.nombre contiene la IP del worker
            list_add(master_status->workers, paquete.msg);
            printf("[MASTER] Nodo %s registrado.\n", paquete.msg);
            break;
        case SOLICITAR_WORKER: {
            char* ip_asignada = obtener_ip_destino(master_status);
            if(ip_asignada == NULL) {
                fprintf(stderr, "[MASTER] No hay workers disponibles\n");
                break;
            }
            
            // Preparamos respuesta
            paquete_t respuesta;
            respuesta.accion = SOLICITAR_WORKER;
            
            strcpy(respuesta.msg, ip_asignada ? ip_asignada : "NULL");
                
            send(socket_cliente, &respuesta, sizeof(paquete_t), 0);
            break;
        }
        case CONFIRM_WORKER:
            // Añadimos a la lista de archivos
            list_add(master_status->files, paquete.msg);
            printf("[MASTER] Archivo '%s' confirmado en sistema.\n", paquete.msg);
            break;
    
        default:
            printf("[MASTER] Acción desconocida: %d\n", paquete.accion);
        }
}
