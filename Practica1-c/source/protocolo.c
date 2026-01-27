#include "include/protocolo.h"
#include <sys/socket.h>
#include <stdio.h>

int send_packet(int socket, paquete_t* p) {
    size_t total_sent = 0;
    size_t bytes_left = sizeof(paquete_t);
    char* buffer = (char*)p;

    while (total_sent < sizeof(paquete_t)) {
        ssize_t sent = send(socket, buffer + total_sent, bytes_left, 0);
        if (sent == -1) {
            perror("Error enviando paquete");
            return -1;
        }
        total_sent += sent;
        bytes_left -= sent;
    }
    return 0; // Éxito
}

int recv_packet(int socket, paquete_t* p) {
    size_t total_received = 0;
    size_t bytes_left = sizeof(paquete_t);
    char* buffer = (char*)p;

    // Bucle para asegurar que recibimos la estructura COMPLETA
    while (total_received < sizeof(paquete_t)) {
        ssize_t received = recv(socket, buffer + total_received, bytes_left, 0);
        
        // Si received es 0, el otro lado cerró la conexión
        if (received <= 0) {
            return -1; 
        }
        
        total_received += received;
        bytes_left -= received;
    }
    return 0; // Éxito
}