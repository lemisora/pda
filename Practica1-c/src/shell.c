#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <readline/readline.h>
#include <readline/history.h>

#include "include/shell.h"

// Función para procesar un comando
void procesar_comando(char* linea) {
    // Copiamos la línea para no modificar el original con strtok
    char* linea_copia = strdup(linea);
    char* comando = strtok(linea_copia, " \t\n");

    if (!comando) {
        free(linea_copia);
        return;
    }

    if (strcmp(comando, "ls") == 0) {
        printf("Ejecutando LS...\n");
        // Aquí iría la lógica para pedir los archivos al maestro
    } else if (strcmp(comando, "touch") == 0) {
        char* archivo = strtok(NULL, " \t\n");
        if (archivo) {
            printf("Ejecutando TOUCH para %s...\n", archivo);
            // Aquí la lógica para crear el archivo
        } else {
            printf("Uso: touch <nombre_archivo>\n");
        }
    } else if (strcmp(comando, "status") == 0) {
        printf("Ejecutando STATUS...\n");
        // Aquí la lógica para pedir el estado de los nodos
    } else if (strcmp(comando, "exit") == 0) {
        printf("Saliendo...\n");
        free(linea_copia);
        free(linea); // Liberamos la línea original de readline
        exit(0);
    } else {
        printf("Comando desconocido: %s\n", comando);
    }

    free(linea_copia);
}

void start_shell() {
    char* linea;
    const char* prompt = "dist-shell> ";

    printf("Bienvenido a la Shell del Sistema Distribuido.\n");

    while (1) {
        linea = readline(prompt);

        // EOF (Ctrl+D)
        if (!linea) {
            printf("\nSaliendo...\n");
            break;
        }

        // Añadir al historial si no está vacía
        if (linea && *linea) {
            add_history(linea);
            procesar_comando(linea);
        }

        // readline() devuelve memoria que debemos liberar
        free(linea);
    }
}

int main() {
    start_shell();
    return 0;
}
