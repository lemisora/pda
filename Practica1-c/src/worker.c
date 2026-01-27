#include "include/protocolo.h"
#include "include/almacenamiento.h"
#include "include/master.h"
#include "include/worker.h"
#include "include/red.h"
#include <arpa/inet.h>
#include <sys/socket.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

char* ip_master;
char* ip_worker;
int puerto_master;
int puerto_worker;

char* directorio;

int init_worker(char* dir, 
                char* ip_w,
                int w_port,
                char* ip_m, 
                int m_port
){
    directorio = strdup(dir);
    if (directorio == NULL) {
        fprintf(stderr, "No se pudo iniciar el worker en %s\n", dir);
        return 1;
    }
    ip_master = strdup(ip_m);
    ip_worker = strdup(ip_w);
    puerto_master = m_port;
    puerto_worker = w_port;
    return 0;
}

int worker(char* dir, 
           char* ip_w,
           int puerto_w,
           char* ip_m, 
           int puerto_m
){
    if (init_worker(dir, ip_w, puerto_w, ip_m, puerto_m)) return 1;
    
    paquete_t registro = {
        .accion = REG_WORKER,
        .valor = contar_cosas(directorio)
    };
    
    strcpy(registro.msg, ip_worker);
    
    // Enviar el registro al master
    printf("[WORKER] Registrando en maestro %s:%d...\n", ip_master, puerto_master);
    if (enviar_comando(ip_master, puerto_master, registro) == 1) {
        printf("[WORKER] Error al registrar en el maestro\n");
        return 1;
    }
    
    printf("Worker iniciado en %s\n", directorio);
    
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
    
    struct sockaddr_in direccion;
    direccion.sin_family = AF_INET;
    direccion.sin_addr.s_addr = INADDR_ANY;
    direccion.sin_port = htons(puerto_worker);
    
    if (bind(server_fd, (struct sockaddr *)&direccion, sizeof(direccion)) < 0) {
        perror("Error en bind del worker");
        return 1;
    }
    
    listen(server_fd, 3);
    printf("Worker escuchando peticiones en puerto %d...\n", puerto_worker);
    
    while(1) {
        int client_sock = accept(server_fd, NULL, NULL);
        if (client_sock >= 0) {
            recibir_comando(client_sock);
            close(client_sock); 
        }
    }
    
    // while(1){
        // getchar();
        // int client_sock = accept(server_fd, NULL, NULL);
        //     if (client_sock >= 0) {
        //         recibir_comando(client_sock);
        //         close(client_sock); // Cerramos tras atender la petición de la shell
        //     }
    // }
    free(directorio);
    free(ip_master);
    free(ip_worker);
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
