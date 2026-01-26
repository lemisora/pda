#include "include/stack.h"
#include <stdlib.h>
#include <string.h>

stack_t* stack_create() {
    stack_t* stack = (stack_t*)malloc(sizeof(stack_t));
    if (stack) {
        stack->top = NULL;
        stack->size = 0;
    }
    return stack;
}

void stack_destroy(stack_t* stack) {
    if (!stack) return;
    
    while (!stack_is_empty(stack)) {
        char* filename = stack_pop(stack);
        free(filename);
    }
    free(stack);
}

int stack_push(stack_t* stack, const char* filename) {
    if (!stack || !filename) return -1;
    
    stack_node_t* node = (stack_node_t*)malloc(sizeof(stack_node_t));
    if (!node) return -1;
    
    node->identifier = strdup(filename);
    if (!node->identifier) {
        free(node);
        return -1;
    }
    
    node->next = stack->top;
    stack->top = node;
    stack->size++;
    
    return 0;
}

char* stack_pop(stack_t* stack) {
    if (!stack || stack_is_empty(stack)) return NULL;
    
    stack_node_t* node = stack->top;
    char* filename = node->identifier;
    
    stack->top = node->next;
    stack->size--;
    
    free(node);
    return filename;
}

char* stack_peek(stack_t* stack) {
    if (!stack || stack_is_empty(stack)) return NULL;
    return stack->top->identifier;
}

int stack_is_empty(stack_t* stack) {
    return (!stack || stack->top == NULL);
}

int stack_size(stack_t* stack) {
    return stack ? stack->size : 0;
}