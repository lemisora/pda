#include <stdio.h>
#include <dirent.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <pthread.h>

#define MAXIMO 50

/* 
   Tarea: Touche distribuido
   Alumno: Kirbi Xavier Huerta Salinas
*/

// Para mandar los datos
typedef struct {
    char accion[10];   
    char nombre[MAXIMO]; 
} Datos;

// Globales
int mi_puerto = 0;
int puerto_vecino = 0;
char ip_vecino[20] = "127.0.0.1";

// Funciones que uso abajo
int contar_cosas(char *ruta);
int hacer_archivo(char *ruta, char *nom);

// ---------- Parte del cliente (mandar) ----------
void avisar_vecino(char *nombre_arch) {
    printf("Conectando con vecino %s:%d para '%s'...\n", ip_vecino, puerto_vecino, nombre_arch);
    
    int soc = socket(AF_INET, SOCK_STREAM, 0);
    if (soc < 0) { 
        perror("error al crear socket"); 
        return; 
    }

    struct sockaddr_in dir_vecino;
    dir_vecino.sin_family = AF_INET;
    dir_vecino.sin_port = htons(puerto_vecino);
    
    // poner la ip bien
    if(inet_pton(AF_INET, ip_vecino, &dir_vecino.sin_addr) <= 0) {
        printf("Error en la ip del vecino\n");
        return;
    }

    // intentar conectar
    if (connect(soc, (struct sockaddr *)&dir_vecino, sizeof(dir_vecino)) < 0) {
        printf("Fallo al conectar con %s:%d... ", ip_vecino, puerto_vecino);
        perror("CAUSA REAL DEL ERROR"); // Esto nos dirá si es "Connection Refused" o "Timeout"
        return;
    }

    Datos d;
    strcpy(d.accion, "CREAR");
    strcpy(d.nombre, nombre_arch);
    
    send(soc, &d, sizeof(d), 0);
    printf("Ya le avise.\n");
    close(soc);
}

// ---------- Parte del servidor (recibir) ----------
void *ciclo_servidor(void *arg) {
    int s_servidor, s_cliente;
    struct sockaddr_in direccion;
    int opt = 1;
    int largo_dir = sizeof(direccion);
    char *ruta = (char*)arg;

    printf("Servidor listo en puerto %d...\n", mi_puerto);

    s_servidor = socket(AF_INET, SOCK_STREAM, 0);
    if (s_servidor == 0) {
        perror("fallo socket server");
        exit(1);
    }

    // para que no marque error de puerto ocupado
    if (setsockopt(s_servidor, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT, &opt, sizeof(opt))) {
        perror("error opciones");
        exit(1);
    }

    direccion.sin_family = AF_INET;
    direccion.sin_addr.s_addr = INADDR_ANY;
    direccion.sin_port = htons(mi_puerto);

    if (bind(s_servidor, (struct sockaddr *)&direccion, sizeof(direccion)) < 0) {
        perror("fallo bind");
        exit(1);
    }

    if (listen(s_servidor, 3) < 0) {
        perror("fallo listen");
        exit(1);
    }

    while(1) {
        // esperar a que alguien se conecte
        s_cliente = accept(s_servidor, (struct sockaddr *)&direccion, (socklen_t*)&largo_dir);
        if (s_cliente < 0) {
            perror("error accept");
            continue;
        }
        
        Datos llega;
        read(s_cliente, &llega, sizeof(llega));
        printf("Me llego peticion para: %s\n", llega.nombre);

        // chequeo si quepo
        int cuantos = contar_cosas(ruta);
        if (cuantos < 20) {
            hacer_archivo(ruta, llega.nombre);
        } else {
            printf("Estoy lleno (%d). Se lo paso al otro...\n", cuantos);
            avisar_vecino(llega.nombre);
        }
        close(s_cliente);
    }
}

void arrancar_hilo(char *ruta){
    pthread_t id_hilo;
    pthread_create(&id_hilo, NULL, ciclo_servidor, (void*)ruta);
}

// ---------- Cosas de archivos ----------

char* inventar_nombre(int n){
    char *texto = (char*) malloc(MAXIMO * sizeof(char));
    sprintf(texto, "archivo%d.txt", n);
    return texto;
}

int hacer_carpeta(char *d){
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

// Principal
int main(int argc, char *argv[]) {
    // checar argumentos
    // Ahora pedimos la IP tambien: ./touche <mi_puerto> <ip_vecino> <puerto_vecino> <modo > 
    if (argc < 4) {
        printf("Faltan argumentos: ./touche <mi_puerto> <ip_vecino> <puerto_vecino> [archivo_opcional]\n");
        printf("Ejemplo: ./touche 8000 192.168.100.40 8000\n");
        return 1;
    }

    mi_puerto = atoi(argv[1]);
    strcpy(ip_vecino, argv[2]); // Copiamos la IP que nos pasen
    puerto_vecino = atoi(argv[3]);
    
    char *carpeta = "./archivos";
    char *archivo_usuario = NULL;
    
    if (argc >= 5) archivo_usuario = argv[4];

    hacer_carpeta(carpeta);
    
    // prendo el servidor
    arrancar_hilo(carpeta); 
    sleep(1); 

    if (archivo_usuario != NULL) {
        
        // --- LOGICA DE MODOS ---
        
        // MODO 1: GENERADOR AUTOMATICO (MAESTRO)
        if (strcmp(archivo_usuario, "1") == 0) {
            printf("\n[MODO 1 ACTIVADO] Empezando a crear archivos a lo loco...\n");
            
            // Empezamos a contar desde lo que ya tenga (para no sobrescribir)
            int contador_global = contar_cosas(carpeta);

            while(1) {
                // Generamos nombre basado en el contador GLOBAL 
                // (no importa si se fua a otro lado, el siguiente debe ser +1)
                char *fake_name = inventar_nombre(contador_global);
                
                // Verificamos espacio LOCAL
                int locales = contar_cosas(carpeta);
                
                if (locales < 20) {
                    hacer_archivo(carpeta, fake_name);
                    sleep(1); 
                } else {
                    printf("FULL! Intentando mandar %s a %s\n", fake_name, ip_vecino);
                    avisar_vecino(fake_name);
                    sleep(3); 
                }
                
                free(fake_name);
                contador_global++; // IMPORTANTE: Siempre sube, para pida 20, 21, 22...
            }
        }
        
        // MODO 0: PASIVO (ESCLAVO)
        else if (strcmp(archivo_usuario, "0") == 0) {
            printf("\n[MODO 0 ACTIVADO] Modo silencioso. Solo escucho a los demas.\n");
            // No hacemos nada aqui, solo esperamos en el while(sleep) del final
        }

        // MODO NOMBRE DE ARCHIVO (touch normal)
        else {
            int n = contar_cosas(carpeta);
            printf("Tengo %d archivos. Piden crear especificamente: %s\n", n, archivo_usuario);
            
            if(n < 20){
                hacer_archivo(carpeta, archivo_usuario);
            } else {
                printf("Ya no caben. Le digo al vecino (%s)...\n", ip_vecino);
                avisar_vecino(archivo_usuario);
            }
        }

    } else {
        // Si no ponen nada
        printf("No pusiste modo (0 o 1). Asumo modo 0 (Pasivo) por seguridad.\n");
    }

    // para que no se cierre
    while(1) sleep(10);
    return 0;
}