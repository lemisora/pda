#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include "include/master.h"
#include "include/worker.h"
#include "protocolo.h"
#include "almacenamiento.h"
#include "red.h"

void print_help() {
    printf(
        "Uso:\n"
        "    ./touche <ip_master> <puerto_master> <modo>\n"
        // "    \t- Modo: 0 (master) o 1 (worker)\n"
        "   ./touche <ip_master> <puerto_master> <ip_worker> <puerto_worker>\n"
    );
}

int main(int argc, char** argv){
    char* directorio = "./archivos";
    char* ip_master = NULL;
    char* ip_worker = NULL;
    
    int threshold = 5;
    int port_master = 0;
    int port_worker = 0;
    
    // Validación de argumentos
    // Para el modo 'master' debe ingresar los siguientes argumentos:
    // 1. La IP del master
    // 2. El puerto del master
    // 3. (Opcional: El threshold de archivos a crear)
    
    // Para el modo 'worker' debe ingresar los siguientes argumentos:
    // 1. La IP del master
    // 2. El puerto del master
    // 3. La IP del worker
    // 4. El puerto del worker
    
    switch (argc) {
        case 2:
            if (strcmp(argv[1], "--help") == 0) {
                print_help();
                return 0;
            } else {
                fprintf(stderr, "Error: Argumento desconocido. Para más información sobre como usarlo, ejecuta ./touche --help\n");
                return 1;
            }
            break;
        // Caso de que no se mande el threshold
        case 3:
            ip_master = strdup(argv[1]);
            port_master = atoi(argv[2]);
            break;
        // Caso de al 'worker' que se mande el threshold
        case 4:
            ip_master = strdup(argv[1]);
            port_master = atoi(argv[2]);
            threshold = atoi(argv[3]);
            break;
        case 5:
            ip_master = strdup(argv[1]);
            port_master = atoi(argv[2]);
            ip_worker = strdup(argv[3]);
            port_worker = atoi(argv[4]);
            break;
        default:
            fprintf(stderr, "Error: Para más información sobre el uso, ejecuta ./touche --help\n");
            break;
    }
    
    // Iniciar el master o el worker dependiendo de los argumentos cargados
    if (ip_master != NULL && ip_worker != NULL) {
        worker(directorio, ip_master, port_master, ip_worker, port_worker);
    } else if (ip_master != NULL) {
        master(threshold);
    } else {
        fprintf(stderr, "Error: Faltan argumentos. Para más información sobre como usarlo, ejecuta ./touche --help\n");
        return 1;
    }
    
    if (ip_master != NULL || ip_worker != NULL) {
        free(ip_master);
        free(ip_worker);
    }
}

// int main(int argc, char *argv[]) {
//     // Validar argumentos
//     if (argc < 4) {
//         printf("Faltan argumentos: ./touche <mi_puerto> <ip_vecino> <puerto_vecino> [archivo_opcional]\n");
//         printf("Ejemplo: ./touche 8000 192.168.100.40 8000\n");
//         return 1;
//     }

//     int mi_puerto = atoi(argv[1]);
//     char ip_vecino[20];
//     strcpy(ip_vecino, argv[2]);
//     int puerto_vecino = atoi(argv[3]);

//     // Configurar modulo de red
//     configurar_red(mi_puerto, ip_vecino, puerto_vecino);

//     char *carpeta = "./archivos";
//     char *archivo_usuario = NULL;

//     if (argc >= 5) archivo_usuario = argv[4];

//     // Preparar almacenamiento local
//     hacer_carpeta(carpeta);

//     // Arrancar servidor en hilo aparte
//     arrancar_servidor(carpeta);
//     sleep(1);

//     if (archivo_usuario != NULL) {

//         // --- LOGICA DE MODOS ---

//         // MODO 1: GENERADOR AUTOMATICO (MAESTRO)
//         if (strcmp(archivo_usuario, "1") == 0) {
//             master(MAX_ARCHIVOS);
//             // printf("\n[MODO 1 ACTIVADO] Empezando a crear archivos a lo loco...\n");

//             // Empezamos a contar desde lo que ya tenga
//             // int contador_global = contar_cosas(carpeta);

//             // while(1) {
//             //     char *fake_name = inventar_nombre(contador_global);

//             //     int locales = contar_cosas(carpeta);

//             //     if (locales < MAX_ARCHIVOS) {
//             //         hacer_archivo(carpeta, fake_name);
//             //         sleep(1);
//             //     } else {
//             //         printf("FULL! Intentando mandar %s a %s\n", fake_name, ip_vecino);
//             //         avisar_vecino(fake_name);
//             //         sleep(3);
//             //     }

//             //     free(fake_name);
//             //     contador_global++;
//             // }
//         }

//         // MODO 0: PASIVO (ESCLAVO)
//         else if (strcmp(archivo_usuario, "0") == 0) {
//             printf("\n[MODO 0 ACTIVADO] Modo silencioso. Solo escucho a los demas.\n");
//             worker(carpeta, ip_vecino, puerto_vecino, argv[2], puerto_vecino);
//         }

//         // MODO NOMBRE DE ARCHIVO (touch normal)
//         else {
//             int n = contar_cosas(carpeta);
//             printf("Tengo %d archivos. Piden crear especificamente: %s\n", n, archivo_usuario);

//             if(n < MAX_ARCHIVOS){
//                 hacer_archivo(carpeta, archivo_usuario);
//             } else {
//                 printf("Ya no caben. Le digo al vecino (%s)...\n", ip_vecino);
//                 avisar_vecino(archivo_usuario);
//             }
//         }

//     } else {
//         printf("No pusiste modo (0 o 1). Asumo modo 0 (Pasivo) por seguridad.\n");
//     }

//     // Loop principal para mantener vivo el proceso
//     while(1) sleep(10);
//     return 0;
// }


