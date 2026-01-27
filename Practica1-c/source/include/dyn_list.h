#ifndef DYN_LIST_H
#define DYN_LIST_H

#include <stdlib.h>

// Tipo para crear listas
typedef struct {
    void **items;       // Arreglo de elementos de tipo genérico (void)
    size_t size;        // Número de elementos en la lista
    size_t capacity;    // Espacio reservado en memoria
} List;

List* list_create (size_t initial_capacity);

//  Función para añadir un elemento a la lista
void list_add(List *list, void *item);
// Función para obtener un elemento de la lista mediante su índice
void* list_get(List *list, size_t index);
// Función para liberar memoria
void list_free(List *list);

#endif