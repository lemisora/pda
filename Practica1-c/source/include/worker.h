#ifndef WORKER_H
#define WORKER_H

// Estructura para pasar datos al hilo de latidos
typedef struct {
    char master_ip[20];
    int master_port;
    char my_ip[20];
    int my_port;
} HeartbeatConfig;

int start_worker_node(char* ip_master, int port_master, char* mi_ip, int mi_puerto, char* dir_data);

#endif