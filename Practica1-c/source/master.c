#include "include/master.h"
#include "include/protocolo.h"
#include "include/dyn_list.h"
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <pthread.h>
#include <stdio.h>

// Puntero global para que los hilos accedan al Manager
static StorageManager global_manager;

// ==== Implementación de Métodos del Manager ====

void Manager_Init(StorageManager* self, int threshold) {
    self->threshold = threshold;
    // Iniciamos listas dinámicas
    self->workers = list_create(5); 
    self->files = list_create(20);
    
    if (pthread_mutex_init(&self->lock, NULL) != 0) {
        perror("Fallo al crear mutex");
        exit(1);
    }
    printf("[MANAGER] Inicializado con umbral: %d\n", threshold);
}

void Manager_Destroy(StorageManager* self) {
    // Liberar memoria de workers y files
    list_destroy(self->workers, free);
    list_destroy(self->files, free);
    pthread_mutex_destroy(&self->lock);
}

void Manager_RegisterWorker(StorageManager* self, char* ip, int port) {
    pthread_mutex_lock(&self->lock);
    
    WorkerNode* existing = NULL;
    // Iteramos usando la lista dinámica
    for (size_t i = 0; i < list_size(self->workers); i++) {
        WorkerNode* node = (WorkerNode*)list_get(self->workers, i);
        if (strcmp(node->ip, ip) == 0 && node->port == port) {
            existing = node;
            break;
        }
    }

    if (existing) {
        existing->is_online = 1;
        printf("[MANAGER] Worker reconectado: %s:%d\n", ip, port);
    } else {
        WorkerNode* new_node = malloc(sizeof(WorkerNode));
        if (new_node) {
            strcpy(new_node->ip, ip);
            new_node->port = port;
            new_node->file_count = 0;
            new_node->is_online = 1;
            list_add(self->workers, new_node);
            printf("[MANAGER] Nuevo Worker registrado: %s:%d\n", ip, port);
        }
    }
    
    pthread_mutex_unlock(&self->lock);
}

char* Manager_GetTargetWorker(StorageManager* self) {
    pthread_mutex_lock(&self->lock);
    char* selected_ip = NULL;
    size_t count = list_size(self->workers);

    for (size_t i = 0; i < count; i++) {
        WorkerNode* w = (WorkerNode*)list_get(self->workers, i);
        if (w->is_online && w->file_count < self->threshold) {
            selected_ip = w->ip;
            break;
        }
    }
    
    if (!selected_ip && count > 0) {
        WorkerNode* last = (WorkerNode*)list_get(self->workers, count - 1);
        selected_ip = last->ip;
    }

    pthread_mutex_unlock(&self->lock);
    return selected_ip; 
}

void Manager_AddFileRecord(StorageManager* self, char* filename, char* worker_ip) {
    pthread_mutex_lock(&self->lock);
    
    list_add(self->files, strdup(filename));
    
    // Buscar worker para incrementar contador
    // Nota: Si worker_ip es "UNK" o no se encuentra, solo agregamos el archivo a la lista global
    if (worker_ip) {
        for (size_t i = 0; i < list_size(self->workers); i++) {
            WorkerNode* w = (WorkerNode*)list_get(self->workers, i);
            if (strcmp(w->ip, worker_ip) == 0) {
                w->file_count++;
                break;
            }
        }
    }
    
    printf("[MANAGER] Archivo '%s' registrado.\n", filename);
    pthread_mutex_unlock(&self->lock);
}

void Manager_PrintStatus(StorageManager* self) {
    pthread_mutex_lock(&self->lock);
    printf("\n--- ESTADO DEL SISTEMA ---\n");
    printf("Total Archivos: %zu\n", list_size(self->files));
    size_t w_count = list_size(self->workers);
    printf("Workers Registrados: %zu\n", w_count);
    
    for(size_t i=0; i < w_count; i++) {
        WorkerNode* w = (WorkerNode*)list_get(self->workers, i);
        printf("  Worker [%s:%d] -> %d/%d archivos\n", 
            w->ip, w->port, w->file_count, self->threshold);
    }
    printf("--------------------------\n");
    pthread_mutex_unlock(&self->lock);
}

// --- Manejo de Red ---

void* handle_client(void* arg) {
    int sock = *(int*)arg;
    free(arg);
    
    paquete_t paquete;
    if (recv_packet(sock, &paquete) == 0) { // Usamos recv_packet seguro
        
        paquete_t respuesta;
        memset(&respuesta, 0, sizeof(paquete_t));
        
        switch (paquete.accion) {
            case REG_WORKER:
                Manager_RegisterWorker(&global_manager, paquete.msg, paquete.valor);
                respuesta.accion = RESPUESTA_OK;
                send_packet(sock, &respuesta);
                break;
                
            case SOLICITAR_WORKER: {
                char* target = Manager_GetTargetWorker(&global_manager);
                if (target) {
                    respuesta.accion = RESPUESTA_OK;
                    strcpy(respuesta.msg, target);
                    // Opcional: Enviar puerto si lo tuviéramos a mano, o dejar que Shell use default
                } else {
                    respuesta.accion = RESPUESTA_ERR;
                    strcpy(respuesta.msg, "Full/No workers");
                }
                send_packet(sock, &respuesta);
                break;
            }
            
            case CONFIRM_WORKER:
                // El mensaje viene formato "nombre_archivo|ip_worker"
                // Usamos strtok para separar (modifica el string in-place)
                char* nombre_archivo = strtok(paquete.msg, "|");
                char* ip_worker = strtok(NULL, "|");
            
                if (nombre_archivo && ip_worker) {
                    Manager_AddFileRecord(&global_manager, nombre_archivo, ip_worker);
                } else {
                    // Fallback por si llega el formato antiguo
                    Manager_AddFileRecord(&global_manager, paquete.msg, NULL);
                }
                break;
                
            case LIST_FILES:
                pthread_mutex_lock(&global_manager.lock);
                int total = (int)list_size(global_manager.files);
                
                respuesta.accion = LIST_FILES;
                respuesta.valor = total;
                send_packet(sock, &respuesta);
                
                for(int i=0; i<total; i++) {
                    paquete_t p_file;
                    p_file.accion = LIST_FILES;
                    strcpy(p_file.msg, (char*)list_get(global_manager.files, i));
                    send_packet(sock, &p_file);
                }
                pthread_mutex_unlock(&global_manager.lock);
                break;
            
            case GET_STATUS:
                // Responder ACK para que la shell no se quede colgada
                // (O implementar envío completo de estado aquí)
                Manager_PrintStatus(&global_manager); // Imprime en servidor
                respuesta.accion = RESPUESTA_OK;
                strcpy(respuesta.msg, "Ver logs servidor");
                send_packet(sock, &respuesta);
                break;

            default:
                printf("Accion desconocida: %d\n", paquete.accion);
        }
    }
    
    close(sock);
    return NULL;
}

void* status_monitor(void* arg) {
    while(1) {
        sleep(10);
        Manager_PrintStatus(&global_manager);
    }
    return NULL;
}

// Función para inicializar el nodo maestro
int start_master_node(int port, int threshold) {
    Manager_Init(&global_manager, threshold);

    int server_fd;
    struct sockaddr_in address;
    int opt = 1;

    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("Socket failed");
        exit(EXIT_FAILURE);
    }

    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt))) {
        perror("setsockopt");
        exit(EXIT_FAILURE);
    }

    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(port);

    if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0) {
        perror("Bind failed");
        exit(EXIT_FAILURE);
    }

    if (listen(server_fd, 10) < 0) {
        perror("Listen");
        exit(EXIT_FAILURE);
    }

    printf("[MASTER] Escuchando en puerto %d...\n", port);

    pthread_t monitor_thread;
    pthread_create(&monitor_thread, NULL, status_monitor, NULL);

    while (1) {
        struct sockaddr_in client_addr;
        socklen_t addrlen = sizeof(client_addr);
        int* new_sock = malloc(sizeof(int));
        
        *new_sock = accept(server_fd, (struct sockaddr *)&client_addr, &addrlen);
        
        if (*new_sock < 0) {
            perror("Accept");
            free(new_sock);
            continue;
        }

        pthread_t thread_id;
        if (pthread_create(&thread_id, NULL, handle_client, (void*)new_sock) != 0) {
            perror("Thread create");
            free(new_sock);
        }
        pthread_detach(thread_id);
    }
    
    Manager_Destroy(&global_manager);
    return 0;
}