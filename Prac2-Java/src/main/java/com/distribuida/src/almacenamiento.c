#include "almacenamiento.h"
#include "protocolo.h"
#include <stdio.h>
#include <dirent.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

char* inventar_nombre(int n){
    char *texto = (char*) malloc(MAXIMO * sizeof(char));
    sprintf(texto, "archivo%d.txt", n);
    return texto;
}

int hacer_carpeta(char *d){
    // 0777 para permisos rwx
    mkdir(d, 0777);
    return 1;
}

int contar_cosas(char *ruta){
    int contador = 0;
    DIR *d = opendir(ruta);

    if(d == NULL){
        hacer_carpeta(ruta);
        d = opendir(ruta);
    }

    if(d == NULL) return -1;

    struct dirent *entrada;
    while ((entrada = readdir(d)) != NULL) {
        // checar que sea archivo normal
        if(entrada->d_type == DT_REG){
            contador++;
        }
    }
    closedir(d);
    return contador;
}

int hacer_archivo(char *ruta, char *nombre){
    printf("Creando el archivo %s en %s\n", nombre, ruta);
    char *completo = (char*) malloc(100);
    strcpy(completo, ruta);
    strcat(completo, "/");
    strcat(completo, nombre);
    
    FILE *archivo = fopen(completo, "w");
    if(archivo) fclose(archivo);
    
    printf("Listo.\n");
    free(completo);
    return 0;
}
