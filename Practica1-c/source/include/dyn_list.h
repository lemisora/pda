#ifndef DYN_LIST_H
#define DYN_LIST_H

#include <stddef.h>

// Tipo para crear listas
typedef struct {
    void **items;       // Arreglo de elementos de tipo genérico (void)
    size_t size;        // Número de elementos en la lista
    size_t capacity;    // Espacio reservado en memoria
} List;

List* list_create (size_t initial_capacity);

//  Función para añadir un elemento a la lista
int list_add(List *list, void *item);
// Función para obtener un elemento de la lista mediante su índice
void* list_get(List *list, size_t index);
// Función para liberar memoria
void list_free(List *list);

// Función para liberar memoria con una función específica para liberar tipos específicos
void list_destroy(List* list, void (*free_func)(void*));

// Función para obtener el tamaño de la lista
size_t list_size(List *list);

#endif