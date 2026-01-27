#ifndef RED_H
#define RED_H

#include "protocolo.h"

// Configura los parametros de red (puertos e IP)
void configurar_red(int p_local, char *ip_vec, int p_vec);

// Envia peticion al vecino para crear archivo
void avisar_vecino(char *nombre_arch);

// Inicia el hilo del servidor en background
void arrancar_servidor(char *ruta);

int enviar_comando(char* ip_destino, int puerto_destino, paquete_t paquete_com);

#endif
