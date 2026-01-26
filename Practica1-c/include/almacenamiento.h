#ifndef ALMACENAMIENTO_H
#define ALMACENAMIENTO_H

// Genera un nombre de archivo automatico "archivoN.txt"
char* inventar_nombre(int n);

// Crea el directorio si no existe
int hacer_carpeta(char *d);

// Cuenta archivos regulares en la ruta
int contar_cosas(char *ruta);

// Crea un archivo vacio en la ruta
int hacer_archivo(char *ruta, char *nombre);

#endif
