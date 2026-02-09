#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>

#define PORT 8080
#define BUFFER_SIZE 8192

// Función simple para sumar dos números extraídos del XML
int add(int a, int b) {
    return a + b;
}

void handle_client(int client_socket) {
    char buffer[BUFFER_SIZE];
    int read_size = read(client_socket, buffer, BUFFER_SIZE - 1);
    if (read_size < 0) {
        perror("Error al leer del socket");
        return;
    }
    buffer[read_size] = '\0';

    printf("Solicitud recibida:\n%s\n", buffer);

    // Buscar los parámetros en el XML
    // Buscamos <i4> o <int> tags.
    int a = 0, b = 0;
    char *p1 = strstr(buffer, "<i4>");
    if (!p1) p1 = strstr(buffer, "<int>");
    
    if (p1) {
        a = atoi(p1 + 4); // Saltar <i4>
        char *p2 = strstr(p1 + 1, "<i4>");
        if (!p2) p2 = strstr(p1 + 1, "<int>");
        if (p2) {
            b = atoi(p2 + 4);
        }
    }

    int result = add(a, b);
    printf("Ejecutando add(%d, %d) = %d\n", a, b, result);

    // Construir respuesta XML-RPC
    char response_body[BUFFER_SIZE];
    snprintf(response_body, sizeof(response_body),
             "<?xml version=\"1.0\"?>\r\n"
             "<methodResponse>\r\n"
             "  <params>\r\n"
             "    <param>\r\n"
             "      <value><i4>%d</i4></value>\r\n"
             "    </param>\r\n"
             "  </params>\r\n"
             "</methodResponse>\r\n", result);

    char response_header[BUFFER_SIZE];
    snprintf(response_header, sizeof(response_header),
             "HTTP/1.1 200 OK\r\n"
             "Content-Type: text/xml\r\n"
             "Content-Length: %ld\r\n"
             "Connection: close\r\n"
             "\r\n", strlen(response_body));

    write(client_socket, response_header, strlen(response_header));
    write(client_socket, response_body, strlen(response_body));

    close(client_socket);
}

int main() {
    int server_fd, new_socket;
    struct sockaddr_in address;
    int addrlen = sizeof(address);

    // Crear socket
    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("Fallo al crear socket");
        exit(EXIT_FAILURE);
    }

    // Configurar dirección
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(PORT);

    // Vincular socket
    if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0) {
        perror("Fallo en bind");
        exit(EXIT_FAILURE);
    }

    // Escuchar
    if (listen(server_fd, 3) < 0) {
        perror("Fallo en listen");
        exit(EXIT_FAILURE);
    }

    printf("Servidor XML-RPC escuchando en el puerto %d...\n", PORT);

    while (1) {
        if ((new_socket = accept(server_fd, (struct sockaddr *)&address, (socklen_t*)&addrlen)) < 0) {
            perror("Fallo en accept");
            exit(EXIT_FAILURE);
        }
        handle_client(new_socket);
    }

    return 0;
}
