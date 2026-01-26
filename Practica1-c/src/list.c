#include "include/list.h"
#include <stdlib.h>
#include <string.h>

// Inicialización
list_t* list_create(int initial_capacity) {
    list_t* list = malloc(sizeof(list_t));
    list->capacity = initial_capacity;
    list->size = 0;
    list->items = malloc(sizeof(char*) * list->capacity);
    return list;
}

void list_add(list_t* list, const char* item) {
    if (list->size == list->capacity) {
        list->capacity *= 2;
        list->items = realloc(list->items, sizeof(char*) * list->capacity);
    }
    list->items[list->size] = strdup(item); // Copiamos la IP
    list->size++;
}

char* list_get(list_t* list, int index) {
    if (index >= 0 && index < list->size) {
        return list->items[index];
    }
    return NULL;
}

void list_destroy(list_t* list) {
    for (int i = 0; i < list->size; i++) {
        free(list->items[i]);
    }
    free(list);
}