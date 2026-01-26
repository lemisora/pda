#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include "protocolo.h"
#include "almacenamiento.h"
#include "red.h"

int main(int argc, char *argv[]) {
    // Validar argumentos
    if (argc < 4) {
        printf("Faltan argumentos: ./touche <mi_puerto> <ip_vecino> <puerto_vecino> [archivo_opcional]\n");
        printf("Ejemplo: ./touche 8000 192.168.100.40 8000\n");
        return 1;
    }

    int mi_puerto = atoi(argv[1]);
    char ip_vecino[20];
    strcpy(ip_vecino, argv[2]);
    int puerto_vecino = atoi(argv[3]);
    
    // Configurar modulo de red
    configurar_red(mi_puerto, ip_vecino, puerto_vecino);

    char *carpeta = "./archivos";
    char *archivo_usuario = NULL;
    
    if (argc >= 5) archivo_usuario = argv[4];

    // Preparar almacenamiento local
    hacer_carpeta(carpeta);
    
    // Arrancar servidor en hilo aparte
    arrancar_servidor(carpeta); 
    sleep(1); 

    if (archivo_usuario != NULL) {
        
        // --- LOGICA DE MODOS ---
        
        // MODO 1: GENERADOR AUTOMATICO (MAESTRO)
        if (strcmp(archivo_usuario, "1") == 0) {
            printf("\n[MODO 1 ACTIVADO] Empezando a crear archivos a lo loco...\n");
            
            // Empezamos a contar desde lo que ya tenga
            int contador_global = contar_cosas(carpeta);

            while(1) {
                char *fake_name = inventar_nombre(contador_global);
                
                int locales = contar_cosas(carpeta);
                
                if (locales < MAX_ARCHIVOS) {
                    hacer_archivo(carpeta, fake_name);
                    sleep(1); 
                } else {
                    printf("FULL! Intentando mandar %s a %s\n", fake_name, ip_vecino);
                    avisar_vecino(fake_name);
                    sleep(3); 
                }
                
                free(fake_name);
                contador_global++; 
            }
        }
        
        // MODO 0: PASIVO (ESCLAVO)
        else if (strcmp(archivo_usuario, "0") == 0) {
            printf("\n[MODO 0 ACTIVADO] Modo silencioso. Solo escucho a los demas.\n");
        }

        // MODO NOMBRE DE ARCHIVO (touch normal)
        else {
            int n = contar_cosas(carpeta);
            printf("Tengo %d archivos. Piden crear especificamente: %s\n", n, archivo_usuario);
            
            if(n < MAX_ARCHIVOS){
                hacer_archivo(carpeta, archivo_usuario);
            } else {
                printf("Ya no caben. Le digo al vecino (%s)...\n", ip_vecino);
                avisar_vecino(archivo_usuario);
            }
        }

    } else {
        printf("No pusiste modo (0 o 1). Asumo modo 0 (Pasivo) por seguridad.\n");
    }

    // Loop principal para mantener vivo el proceso
    while(1) sleep(10);
    return 0;
}
