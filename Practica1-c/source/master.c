#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "include/dyn_list.h"
#include "include/master.h"

void Manager_Init(StorageManager* self, int threshold) {
    self->threshold = threshold;
    // Iniciamos listas dinámicas
    self->workers = list_create(5); 
    self->files = list_create(20);
    pthread_mutex_init(&self->lock, NULL);
}

void Manager_Destroy(StorageManager* self) {
    // Para workers: Pasamos 'free' estándar porque son structs reservados con malloc
    list_destroy(self->workers, free);
    
    // Para files: Pasamos 'free' porque son strings reservados con strdup
    list_destroy(self->files, free);
    
    pthread_mutex_destroy(&self->lock);
}

void Manager_RegisterWorker(StorageManager* self, char* ip, int port) {
    pthread_mutex_lock(&self->lock);
    
    // Verificar si existe (Iterando la lista genérica)
    WorkerNode* existing = NULL;
    for (size_t i = 0; i < list_size(self->workers); i++) {
        WorkerNode* node = (WorkerNode*)list_get(self->workers, i);
        if (strcmp(node->ip, ip) == 0 && node->port == port) {
            existing = node;
            break;
        }
    }

    if (existing) {
        existing->is_online = 1; // Reactivar
        printf("[MANAGER] Worker reconectado: %s\n", ip);
    } else {
        // Crear nuevo nodo en el Heap
        WorkerNode* new_node = malloc(sizeof(WorkerNode));
        if (new_node) {
            strcpy(new_node->ip, ip);
            new_node->port = port;
            new_node->file_count = 0;
            new_node->is_online = 1;
            
            // Agregamos a la lista genérica
            list_add(self->workers, new_node);
            printf("[MANAGER] Worker registrado: %s\n", ip);
        }
    }
    
    pthread_mutex_unlock(&self->lock);
}

char* Manager_GetTargetWorker(StorageManager* self) {
    pthread_mutex_lock(&self->lock);
    char* selected_ip = NULL;
    size_t count = list_size(self->workers);

    // Iterar buscando espacio
    for (size_t i = 0; i < count; i++) {
        WorkerNode* w = (WorkerNode*)list_get(self->workers, i);
        
        if (w->is_online && w->file_count < self->threshold) {
            selected_ip = w->ip; // Solo copiamos el puntero (cuidado lifetime)
            break;
        }
    }
    
    // Fallback al último
    if (!selected_ip && count > 0) {
        WorkerNode* last = (WorkerNode*)list_get(self->workers, count - 1);
        selected_ip = last->ip;
    }

    pthread_mutex_unlock(&self->lock);
    return selected_ip;
}

void Manager_AddFileRecord(StorageManager* self, char* filename, char* worker_ip) {
    pthread_mutex_lock(&self->lock);
    
    // Guardamos copia del nombre (strdup es vital aquí)
    list_add(self->files, strdup(filename));
    
    // Actualizar conteo del worker
    for (size_t i = 0; i < list_size(self->workers); i++) {
        WorkerNode* w = (WorkerNode*)list_get(self->workers, i);
        if (strcmp(w->ip, worker_ip) == 0) {
            w->file_count++;
            break;
        }
    }
    
    pthread_mutex_unlock(&self->lock);
}