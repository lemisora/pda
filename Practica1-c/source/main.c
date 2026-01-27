#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "master.h"
#include "worker.h"

void print_help(char* prog_name) {
    printf("Uso del Sistema Distribuido:\n");
    printf("  Modo Maestro:\n");
    printf("    %s master <puerto_escucha> [threshold]\n", prog_name);
    printf("    Ejemplo: %s master 8000 5\n\n", prog_name);
    
    printf("  Modo Worker:\n");
    printf("    %s worker <ip_master> <puerto_master> <mi_ip_publica> <mi_puerto>\n", prog_name);
    printf("    Ejemplo: %s worker 192.168.1.50 8000 192.168.1.51 8001\n", prog_name);
}

int main(int argc, char** argv){
    if (argc < 2) {
        print_help(argv[0]);
        return EXIT_FAILURE;
    }
    
    char* modo = argv[1];
    
    if (strcmp(modo, "master") == 0) {
        if (argc < 3) {
            fprintf(stderr, "Error: Faltan argumentos para modo master.\n");
            return EXIT_FAILURE;
        }
            
        int port = atoi(argv[2]);
        int threshold = (argc >= 4) ? atoi(argv[3]) : 5; // Default 5
    
        printf("[INIT] Arrancando MASTER en puerto %d (Threshold: %d)\n", port, threshold);
            
        return start_master_node(port, threshold);
    } 
        
    // MODO WORKER
    else if (strcmp(modo, "worker") == 0) {
        if (argc < 6) {
            fprintf(stderr, "Error: Faltan argumentos para modo worker.\n");
            print_help(argv[0]);
            return EXIT_FAILURE;
        }
        
        char* ip_master = argv[2];
        int port_master = atoi(argv[3]);
        char* mi_ip     = argv[4];
        int mi_puerto   = atoi(argv[5]);
        char* dir_data  = "./archivos";
    
        printf("[INIT] Arrancando WORKER en %s:%d\n", mi_ip, mi_puerto);
        printf("[INIT] Conectando a Master en %s:%d\n", ip_master, port_master);
    
        return start_worker_node(ip_master, port_master, mi_ip, mi_puerto, dir_data);
    } else {
        fprintf(stderr, "Modo desconocido: '%s'\n", modo);
        print_help(argv[0]);
        return EXIT_FAILURE;
    }
    return EXIT_SUCCESS;
}