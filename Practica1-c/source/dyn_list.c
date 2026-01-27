#include <stdio.h>
#include <stdlib.h>

#include "include/dyn_list.h"

List* list_create (size_t initial_capacity){
    List *list = malloc(sizeof(List));
    if (!list) return NULL;
    
    list->capacity = initial_capacity > 0 ? initial_capacity : 5;
    list->size = 0;
    
    list -> items = malloc(list->capacity * sizeof(void*));
    
    // Si no se reserva bien la memoria para items
    if (!list->items) {
        free(list);
        return NULL;
    }
    
    return list;
}

int list_add(List *list, void *item){
    if (list->size >= list->capacity){
        size_t new_capacity = list->capacity * 2;
        void **temp = realloc(list->items, new_capacity * sizeof(void*));
        if(!temp) {
            fprintf(stderr, "[ERROR] No se pudo expandir la lista\n");
            return -1;
        }
        list->items = temp;
        list->capacity = new_capacity;
    }
    list->items[list->size++] = item;
    return EXIT_SUCCESS;
}

void* list_get(List *list, size_t index){
    if(index >= list->size) return NULL;
    return list->items[index];
}

void list_destroy(List *list, void (*free_func)(void*)) {
    if (!list) return;

    // Si nos pasan una función para liberar items la usamos
    if (free_func != NULL) {
        for (size_t i = 0; i < list->size; i++) {
            if (list->items[i] != NULL) {
                free_func(list->items[i]);
            }
        }
    }
    
    free(list->items);
    free(list);
}

size_t list_size(List *list) {
    return list ? list->size : 0;
}

void list_free(List *list){
    free(list->items);
    free(list);
}