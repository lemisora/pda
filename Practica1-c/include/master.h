#ifndef MASTER_H
#define MASTER_H

typedef struct status status_t;

int master(int threshold);
void procesar_solicitud_cliente(int socket, status_t* master_global_ptr);

#endif // MASTER_H