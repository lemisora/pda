#ifndef MASTER_H
#define MASTER_H

#include "dyn_list.h"
#include <pthread.h>

// Estructura para representar un Worker individual
typedef struct {
    char ip[20];
    int port;
    int file_count;      // Cuántos archivos tiene este worker
    int is_online;       // 1 = activo, 0 = caído
} WorkerNode;

// Para controlar el almacenamiento en cada nodo
typedef struct {
    int threshold;       // Umbral por nodo
    List* workers;       // (WorkerNode*) Array de workers
    int worker_count;    // Cantidad actual de workers registrados
    
    List* files;       // (char*) Lista de nombres de archivos (metadata global)
    
    pthread_mutex_t lock; // MUTEX: para evitar condiciones de carrera
} StorageManager;

//======== Métodos del StorageManager ===========

// Constructor: Inicializa memoria y mutex
void Manager_Init(StorageManager* self, int threshold);

// Destructor: Libera listas y mutex
void Manager_Destroy(StorageManager* self);

// Registra un worker nuevo o actualiza uno existente
void Manager_RegisterWorker(StorageManager* self, char* ip, int port);

// Algoritmo de balanceo: Retorna la IP del worker ideal para escribir
// Retorna NULL si no hay espacio
WorkerNode* Manager_GetTargetWorker(StorageManager* self);

// Confirma que un archivo se creó exitosamente
void Manager_AddFileRecord(StorageManager* self, char* filename, char* worker_ip, int worker_port);

// Imprime el estado (tu print_status_master mejorado)
void Manager_PrintStatus(StorageManager* self);

// Función principal del hilo del servidor (punto de entrada)
int start_master_node(int port, int threshold);

#endif