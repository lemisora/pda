#include <stdio.h>

#include <xmlrpc-c/base.h>
#include <xmlrpc-c/server.h>
#include <xmlrpc-c/server_abyss.h>

// Función de suma con la forma de xmlrpc-c
static xmlrpc_value *
sample_add(xmlrpc_env * const envP,
           xmlrpc_value * const paramArrayP,
           void * const serverInfo) {

    xmlrpc_int32 x, y, z;

    // Parseamos los argumentos: "(ii)" significa que esperamos dos enteros
    xmlrpc_decompose_value(envP, paramArrayP, "(ii)", &x, &y);
    if (envP->fault_occurred) return NULL;

    printf("Solicitud recibida: Sumar %d + %d\n", x, y);
    z = x + y;

    // Retornamos el resultado construido como entero (i)
    return xmlrpc_build_value(envP, "i", z);
}

int main(int argc, char **argv) {
    xmlrpc_registry * registryP;
    xmlrpc_env env;

    xmlrpc_env_init(&env);

    // Crear registro de métodos
    registryP = xmlrpc_registry_new(&env);
    if (env.fault_occurred) {
        printf("Error creando registro: %s\n", env.fault_string);
        return 1;
    }

    // Registrar la función "add"
    xmlrpc_registry_add_method(
        &env, registryP, NULL, "add", &sample_add, NULL);
    if (env.fault_occurred) {
        printf("Error registrando método: %s\n", env.fault_string);
        return 1;
    }

    // Iniciar el servidor HTTP Abyss en puerto 8080
    printf("Servidor XML-RPC escuchando en puerto 8080...\n");

    xmlrpc_server_abyss_parms serverparms;
    serverparms.config_file_name = NULL;
    serverparms.registryP        = registryP;
    serverparms.port_number      = 8080;
    serverparms.log_file_name    = "/dev/stdout";

    xmlrpc_server_abyss(&env, &serverparms, XMLRPC_APSIZE(log_file_name));
    
    if (env.fault_occurred) {
        printf("Error en el servidor: %s\n", env.fault_string);
        return 1;
    }

    return 0;
}
