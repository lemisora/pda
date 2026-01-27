#include <stdio.h>
#include <stdlib.h>
#include "include/dyn_list.h"

List* list_create (size_t initial_capacity){
    List *list = malloc(sizeof(List));
    if (!list) return NULL;
    
    list->capacity = initial_capacity > 0 ? initial_capacity : 5;
    list->size = 0;
    
    list -> items = malloc(list->capacity * sizeof(void*));
    return list;
}

void list_add(List *list, void *item){
    if (list->size >= list->capacity){
        list->capacity *= 2;
        void **temp = realloc(list->items, list->capacity * sizeof(void*));
        if(!temp) {
            fprintf(stderr, "Error en list_add: no se pudo reservar memoria\n");
            exit(EXIT_FAILURE);
        }
        list->items = temp;
    }
    list->items[list->size++] = item;
}

void* list_get(List *list, size_t index){
    if(index >= list->size) return NULL;
    return list->items[index];
}

void list_free(List *list){
    free(list->items);
    free(list);
}