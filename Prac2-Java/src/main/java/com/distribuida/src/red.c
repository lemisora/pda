#include "red.h"
#include "protocolo.h"
#include "almacenamiento.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <pthread.h>

// Variables privadas del modulo
static int mi_puerto = 0;
static int puerto_vecino = 0;
static char ip_vecino[20] = "127.0.0.1";

void configurar_red(int p_local, char *ip_vec, int p_vec) {
    mi_puerto = p_local;
    strcpy(ip_vecino, ip_vec);
    puerto_vecino = p_vec;
}

// ---------- Parte del cliente (mandar) ----------
void avisar_vecino(char *nombre_arch) {
    printf("Conectando con vecino %s:%d para '%s'...\n", ip_vecino, puerto_vecino, nombre_arch);
    
    int soc = socket(AF_INET, SOCK_STREAM, 0);
    if (soc < 0) { 
        perror("error al crear socket"); 
        return; 
    }

    struct sockaddr_in dir_vecino;
    dir_vecino.sin_family = AF_INET;
    dir_vecino.sin_port = htons(puerto_vecino);
    
    // poner la ip bien
    if(inet_pton(AF_INET, ip_vecino, &dir_vecino.sin_addr) <= 0) {
        printf("Error en la ip del vecino\n");
        return;
    }

    // intentar conectar
    if (connect(soc, (struct sockaddr *)&dir_vecino, sizeof(dir_vecino)) < 0) {
        printf("Fallo al conectar con %s:%d... ", ip_vecino, puerto_vecino);
        perror("CAUSA REAL DEL ERROR"); 
        return;
    }

    Datos d;
    strcpy(d.accion, "CREAR");
    strcpy(d.nombre, nombre_arch);
    
    send(soc, &d, sizeof(d), 0);
    printf("Ya le avise.\n");
    close(soc);
}

// ---------- Parte del servidor (recibir) ----------
void *ciclo_servidor(void *arg) {
    int s_servidor, s_cliente;
    struct sockaddr_in direccion;
    int opt = 1;
    socklen_t largo_dir = sizeof(direccion);
    char *ruta = (char*)arg;

    printf("Servidor listo en puerto %d...\n", mi_puerto);

    s_servidor = socket(AF_INET, SOCK_STREAM, 0);
    if (s_servidor == 0) {
        perror("fallo socket server");
        exit(1);
    }

    // para que no marque error de puerto ocupado
    if (setsockopt(s_servidor, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT, &opt, sizeof(opt))) {
        perror("error opciones");
        exit(1);
    }

    direccion.sin_family = AF_INET;
    direccion.sin_addr.s_addr = INADDR_ANY;
    direccion.sin_port = htons(mi_puerto);

    if (bind(s_servidor, (struct sockaddr *)&direccion, sizeof(direccion)) < 0) {
        perror("fallo bind");
        exit(1);
    }

    if (listen(s_servidor, 3) < 0) {
        perror("fallo listen");
        exit(1);
    }

    while(1) {
        // esperar a que alguien se conecte
        s_cliente = accept(s_servidor, (struct sockaddr *)&direccion, &largo_dir);
        if (s_cliente < 0) {
            perror("error accept");
            continue;
        }
        
        Datos llega;
        read(s_cliente, &llega, sizeof(llega));
        printf("Me llego peticion para: %s\n", llega.nombre);

        // chequeo si quepo (usando funciones de almacenamiento)
        int cuantos = contar_cosas(ruta);
        if (cuantos < MAX_ARCHIVOS) {
            hacer_archivo(ruta, llega.nombre);
        } else {
            printf("Estoy lleno (%d). Se lo paso al otro...\n", cuantos);
            avisar_vecino(llega.nombre);
        }
        close(s_cliente);
    }
}

void arrancar_servidor(char *ruta){
    pthread_t id_hilo;
    pthread_create(&id_hilo, NULL, ciclo_servidor, (void*)ruta);
}
