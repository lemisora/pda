#include "include/protocolo.h"
#include "include/almacenamiento.h"
#include <sys/socket.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

char* directorio;

int worker(char* dir){
    directorio = strdup(dir);
    if (directorio == NULL) {
        fprintf(stderr, "No se pudo iniciar el worker en %s\n", dir);
        return 1;
    }
    
    // Falta la acción de registrar el worker en el master
    
    printf("Worker iniciado en %s\n", directorio);
    while(1){
        getchar();
        // int client_sock = accept(server_fd, NULL, NULL);
        //     if (client_sock >= 0) {
        //         recibir_comando(client_sock);
        //         close(client_sock); // Cerramos tras atender la petición de la shell
        //     }
    }
    free(directorio);
    return 0;
}

void recibir_comando(int socket_shell){
    paquete_t paquete;
    if(recv(socket_shell, &paquete, sizeof(paquete_t), 0) <= 0) return;

    if(paquete.accion == WRITE_FILE){
        // 'paquete.msg' contiene el nombre del archivo a crear
        printf("[WORKER] Creando archivo físico: %s\n", paquete.msg);
        int resultado = hacer_archivo(directorio, paquete.msg);
        
        // RESPUESTA AL CLIENTE
        paquete_t respuesta;
        respuesta.accion = WRITE_FILE;
        respuesta.valor = (resultado == 0) ? 1 : 0; // 1 éxito, 0 error
        send(socket_shell, &respuesta, sizeof(paquete_t), 0);
    }
}
