#include "include/worker.h"
#include "include/protocolo.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pthread.h>

// Variable global para la ruta de almacenamiento
static char STORAGE_DIR[256];

// Función auxiliar para registrarse en el Master
int registrarse_en_master(char* m_ip, int m_port, char* my_ip, int my_port) {
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) return -1;

    struct sockaddr_in serv_addr;
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(m_port);
    
    if (inet_pton(AF_INET, m_ip, &serv_addr.sin_addr) <= 0) {
        fprintf(stderr, "[ERROR] IP del Master inválida\n");
        return -1;
    }

    if (connect(sock, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) < 0) {
        perror("[ERROR] No se pudo conectar al Master");
        return -1;
    }

    // Preparamos el paquete de registro
    paquete_t p;
    p.accion = REG_WORKER;
    strcpy(p.msg, my_ip); // Importante: Le decimos al master nuestra IP pública
    p.valor = my_port;    // Y nuestro puerto de escucha
    
    send_packet(sock, &p);
    
    // Esperamos confirmación (ACK)
    if (recv_packet(sock, &p) == 0 && p.accion == RESPUESTA_OK) {
        printf("[WORKER] Registro exitoso en Master %s:%d\n", m_ip, m_port);
    } else {
        printf("[WORKER] El Master no confirmó el registro.\n");
    }

    close(sock);
    return 0;
}

// Lógica para crear el archivo físico (equivalente a `touch`)
void crear_archivo_local(char* filename) {
    char filepath[512];
    snprintf(filepath, sizeof(filepath), "%s/%s", STORAGE_DIR, filename);
    
    printf("[WORKER] Creando archivo físico: %s\n", filepath);
    
    FILE* f = fopen(filepath, "w");
    if (f) {
        fclose(f);
    } else {
        perror("[ERROR] Fallo al crear archivo");
    }
}

// Bucle principal que atiende a la Shell
void atender_peticiones_shell(int server_fd) {
    struct sockaddr_in client_addr;
    socklen_t addr_len = sizeof(client_addr);
    
    while(1) {
        int client_sock = accept(server_fd, (struct sockaddr *)&client_addr, &addr_len);
        if (client_sock < 0) {
            perror("Accept error");
            continue;
        }

        paquete_t req;
        if (recv_packet(client_sock, &req) == 0) {
            paquete_t res;
            
            if (req.accion == WRITE_FILE) {
                // Chequear bandera de advertencia (req.valor)
                if (req.valor == 1) {
                    printf("\n[WORKER WARNING] ¡Atención! Se ha superado el umbral de almacenamiento.\n");
                    printf("                 Operando en modo desbordamiento.\n");
                }
            
                // req.msg contiene el nombre del archivo
                crear_archivo_local(req.msg);
                            
                res.accion = RESPUESTA_OK;
                strcpy(res.msg, "Created");
            }
            send_packet(client_sock, &res);
        }
        close(client_sock);
    }
}

// Función del Hilo: Envía "ALIVE" periódicamente
void* thread_heartbeat(void* arg) {
    HeartbeatConfig* cfg = (HeartbeatConfig*)arg;
    
    while(1) {
        int sock = socket(AF_INET, SOCK_STREAM, 0);
        if (sock >= 0) {
            struct sockaddr_in serv_addr;
            serv_addr.sin_family = AF_INET;
            serv_addr.sin_port = htons(cfg->master_port);
            inet_pton(AF_INET, cfg->master_ip, &serv_addr.sin_addr);

            // Conexión rápida (timeout implícito del OS)
            if (connect(sock, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) == 0) {
                paquete_t p;
                p.accion = ALIVE;
                strcpy(p.msg, cfg->my_ip);
                p.valor = cfg->my_port;
                
                send_packet(sock, &p);
                // No esperamos respuesta (fire and forget)
            }
            close(sock);
        }
        
        sleep(5); // Latido cada 5 segundos
    }
    free(cfg);
    return NULL;
}

int start_worker_node(char* master_ip, int master_port, char* my_ip, int my_port, char* dir) {
    // Configurar directorio
    strcpy(STORAGE_DIR, dir);
    struct stat st = {0};
    if (stat(STORAGE_DIR, &st) == -1) {
        mkdir(STORAGE_DIR, 0777); // Crear carpeta si no existe
    }

    // Registrarse en el Master
    // Intentamos registrar antes de levantar el servidor para asegurar que el Master nos conoce
    if (registrarse_en_master(master_ip, master_port, my_ip, my_port) != 0) {
        fprintf(stderr, "[FATAL] No se pudo registrar con el Master. Abortando.\n");
        return 1;
    }

    // === LANZAR HEARTBEAT ===
        pthread_t hb_thread;
        HeartbeatConfig* cfg = malloc(sizeof(HeartbeatConfig));
        strcpy(cfg->master_ip, master_ip);
        cfg->master_port = master_port;
        strcpy(cfg->my_ip, my_ip);
        cfg->my_port = my_port;
    
        if (pthread_create(&hb_thread, NULL, thread_heartbeat, cfg) != 0) {
            perror("Error creando hilo heartbeat");
        }
        pthread_detach(hb_thread); // Que corra libre
        printf("[WORKER] Servicio de latidos iniciado.\n");
    
    // Levantar Servidor (Bind & Listen)
    int server_fd;
    struct sockaddr_in address;
    int opt = 1;

    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("Socket failed");
        exit(EXIT_FAILURE);
    }
    
    // Permitir reusar el puerto inmediatamente si se cierra mal
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY; // Escuchamos en todas las interfaces locales
    address.sin_port = htons(my_port);

    if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0) {
        perror("Bind failed");
        exit(EXIT_FAILURE);
    }

    if (listen(server_fd, 5) < 0) {
        perror("Listen failed");
        exit(EXIT_FAILURE);
    }

    printf("[WORKER] Escuchando peticiones en puerto %d... (Dir: %s)\n", my_port, STORAGE_DIR);

    // 4. Entrar al bucle de servicio
    atender_peticiones_shell(server_fd);

    return 0;
}