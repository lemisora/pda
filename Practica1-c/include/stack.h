#ifndef STACK_H
#define STACK_H

typedef struct stack_node {
    char* identifier;
    struct stack_node* next;
} stack_node_t;

typedef struct {
    stack_node_t* top;
    int size;
} stack_t;

// Operaciones de la pila
stack_t* stack_create();
void stack_destroy(stack_t* stack);
int stack_push(stack_t* stack, const char* filename);
char* stack_pop(stack_t* stack);
char* stack_peek(stack_t* stack);
int stack_is_empty(stack_t* stack);
int stack_size(stack_t* stack);

#endif