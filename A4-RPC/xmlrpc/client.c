#include <stdio.h>
#include <xmlrpc-c/base.h>
#include <xmlrpc-c/client.h>

#define NAME "XML-RPC C Client"
#define VERSION "1.0"

// Tu función de input mejorada
void pedir_input(int* num1, int* num2) {
    while (1) {
        printf("Ingrese dos números separados por espacios: ");
        if (scanf("%d %d", num1, num2) == 2) {
            break;
        } else {
            printf("Entrada inválida.\n");
            int c;
            while ((c = getchar()) != '\n' && c != EOF);
        }
    }
}

int main(int argc, char **argv) {
    xmlrpc_env env;
    xmlrpc_value *resultP;
    int num1, num2, suma;
    const char * const serverUrl = "http://localhost:8080/RPC2";
    const char * const methodName = "add";

    // Inicializar entorno y cliente
    xmlrpc_env_init(&env);
    xmlrpc_client_init2(&env, XMLRPC_CLIENT_NO_FLAGS, NAME, VERSION, NULL, 0);

    pedir_input(&num1, &num2);

    printf("Llamando a %s con %d y %d...\n", methodName, num1, num2);

    // HACER LA LLAMADA
    // "(ii)" indica que enviamos dos enteros
    resultP = xmlrpc_client_call(&env, serverUrl, methodName, "(ii)", num1, num2);

    if (env.fault_occurred) {
        fprintf(stderr, "Error en la llamada XML-RPC: %s\n", env.fault_string);
    } else {
        // Leer el resultado
        xmlrpc_read_int(&env, resultP, &suma);
        if (env.fault_occurred) {
            fprintf(stderr, "Error leyendo respuesta: %s\n", env.fault_string);
        } else {
            printf("Respuesta del servidor: %d\n", suma);
        }
    }

    // Limpieza
    xmlrpc_DECREF(resultP);
    xmlrpc_env_clean(&env);
    xmlrpc_client_cleanup();

    return 0;
}