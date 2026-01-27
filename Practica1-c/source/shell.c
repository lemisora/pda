#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <readline/readline.h>
#include <readline/history.h>

#include "include/shell.h"
#include "include/protocolo.h"

#define WORKER_PORT_DEFAULT 8001

// --- Helpers de Red ---

int conectar_a(const char* ip, int port) {
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) {
        perror("Error creando socket");
        return -1;
    }

    struct sockaddr_in addr;
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    if (inet_pton(AF_INET, ip, &addr.sin_addr) <= 0) {
        fprintf(stderr, "IP inválida: %s\n", ip);
        close(sock);
        return -1;
    }

    if (connect(sock, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
        fprintf(stderr, "No se pudo conectar a %s:%d\n", ip, port);
        close(sock);
        return -1;
    }

    return sock;
}

// --- Comandos (Ahora reciben contexto) ---

void cmd_ls(const char* master_ip, int master_port) {
    int sock = conectar_a(master_ip, master_port);
    if (sock < 0) return;

    paquete_t req;
    req.accion = LIST_FILES;
    send_packet(sock, &req);

    paquete_t res;
    // 1. Recibir cantidad
    if (recv_packet(sock, &res) < 0) {
        printf("Error recibiendo respuesta del Master.\n");
        close(sock);
        return;
    }

    int total_files = res.valor;
    printf("Archivos encontrados: %d\n", total_files);

    // 2. Recibir lista
    for (int i = 0; i < total_files; i++) {
        recv_packet(sock, &res);
        printf(" - %s\n", res.msg);
    }
    close(sock);
}

void cmd_touch(const char* master_ip, int master_port, char* filename) {
    if (!filename) {
        printf("Uso: touch <nombre_archivo>\n");
        return;
    }

    // PASO 1: Preguntar al Master
    int sock_master = conectar_a(master_ip, master_port);
    if (sock_master < 0) return;

    paquete_t req;
    req.accion = SOLICITAR_WORKER;
    send_packet(sock_master, &req);

    paquete_t res;
    recv_packet(sock_master, &res);
    close(sock_master);

    // Aceptamos OK u OVERFLOW como válidos (éxito operativo)
    if (res.accion != RESPUESTA_OK && res.accion != RESPUESTA_OVERFLOW) {
        printf("Error: No hay workers disponibles.\n");
        return;
    }
    
    // Detectamos si es desbordamiento internamente
    int is_overflow = (res.accion == RESPUESTA_OVERFLOW);

    char worker_ip[20];
    strcpy(worker_ip, res.msg);
    // Usamos el puerto que indique el master, o el default
    int worker_port = (res.valor > 0) ? res.valor : WORKER_PORT_DEFAULT; 

    printf("Master asignó: %s:%d. Conectando...\n", worker_ip, worker_port);

    // PASO 2: Escribir en Worker
    int sock_worker = conectar_a(worker_ip, worker_port);
    if (sock_worker < 0) return;

    req.accion = WRITE_FILE;
    strcpy(req.msg, filename);
    
    req.valor = is_overflow ? 1 : 0;
    
    send_packet(sock_worker, &req);

    recv_packet(sock_worker, &res);
    close(sock_worker);

    if (res.accion == RESPUESTA_OK) {
        printf("Archivo creado exitosamente.\n");

        // PASO 3: Confirmar al Master
        sock_master = conectar_a(master_ip, master_port);
        if (sock_master >= 0) {
            req.accion = CONFIRM_WORKER;
            
            // Formato: "nombre|ip|puerto"
            snprintf(req.msg, sizeof(req.msg), "%s|%s|%d", filename, worker_ip, worker_port);
            
            send_packet(sock_master, &req);
            close(sock_master);
            printf("Confirmación enviada al Master.\n");
        }
    } else {
        printf("Error: Fallo al crear en el worker.\n");
    }
}

void cmd_status(const char* master_ip, int master_port) {
    int sock = conectar_a(master_ip, master_port);
    if (sock < 0) return;

    paquete_t req;
    req.accion = GET_STATUS;
    send_packet(sock, &req);
    
    printf("Solicitud de estado enviada.\n");
    
    close(sock);
}

void procesar_comando(char* linea, const char* master_ip, int master_port) {
    char* linea_copia = strdup(linea);
    char* comando = strtok(linea_copia, " \t\n");

    if (!comando) {
        free(linea_copia);
        return;
    }

    if (strcmp(comando, "ls") == 0) {
        cmd_ls(master_ip, master_port);
    } 
    else if (strcmp(comando, "touch") == 0) {
        char* arg = strtok(NULL, " \t\n");
        cmd_touch(master_ip, master_port, arg);
    } 
    else if (strcmp(comando, "status") == 0) {
        cmd_status(master_ip, master_port);
    } 
    else if (strcmp(comando, "exit") == 0) {
        printf("Saliendo...\n");
        free(linea_copia);
        free(linea); 
        exit(0);
    } 
    else if (strcmp(comando, "help") == 0) {
        printf("Comandos: ls, touch <archivo>, status, exit\n");
    } 
    else {
        printf("Comando desconocido: %s\n", comando);
    }

    free(linea_copia);
}

void start_shell(char* master_ip, int master_port) {
    char* linea;
    char prompt[64];
    
    // Configuramos el prompt con los datos recibidos
    snprintf(prompt, 64, "dist-shell [%s:%d]> ", master_ip, master_port);

    printf("Bienvenido a la Shell del Sistema Distribuido.\n");
    printf("Conectado a Master en %s:%d\n", master_ip, master_port);

    while (1) {
        linea = readline(prompt);

        if (!linea) { // EOF
            printf("\n");
            break;
        }

        if (*linea) {
            add_history(linea);
            // PASAMOS EL CONTEXTO A LA SIGUIENTE CAPA
            procesar_comando(linea, master_ip, master_port);
        }
        free(linea);
    }
}

int main(int argc, char** argv) {
    char ip[20] = "127.0.0.1";
    int port = 8000;

    // Permitir configurar IP/Puerto al arrancar
    if (argc >= 2) strncpy(ip, argv[1], 19);
    if (argc >= 3) port = atoi(argv[2]);

    start_shell(ip, port);
    return 0;
}