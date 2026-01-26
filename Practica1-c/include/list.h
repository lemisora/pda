#ifndef LIST_H
#define LIST_H

typedef struct {
    char** items;    // Arreglo de strings (IPs de workers)
    int capacity;    // Espacio total reservado
    int size;        // Elementos usados actualmente
} list_t;

list_t* list_create(int initial_capacity);

void list_add(list_t* list, const char* item);

char* list_get(list_t* list, int index);

void list_destroy(list_t* list);

#endif