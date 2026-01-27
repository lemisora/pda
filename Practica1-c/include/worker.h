#ifndef WORKER_H
#define WORKER_H

int worker(char* dir, 
           char* ip_w,
           int puerto_w,
           char* ip_m, 
           int puerto_m
);

void recibir_comando(int socket_shell);

#endif