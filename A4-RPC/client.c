#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>

#define PORT 8080
#define BUFFER_SIZE 8192

// Función para pedir dos números y almacenarlos en variables
void pedir_input(char *prompt, int* num1, int* num2) {
    while (1) {
        printf("%s", prompt);
        printf("Ingrese dos números separados por espacios: ");

        if (scanf("%d %d", num1, num2) == 2) {
            break;
        } else {
            printf("Se ha ingresado valores inválidos. Intente nuevamente ingresando solo dos números.\n");
            
            // Consumimos todo lo que quedó en la entrada hasta el salto de línea
            int c;
            while ((c = getchar()) != '\n' && c != EOF);
        }
    }
}

int main() {
    // Números a sumar
    int num1, num2;

    pedir_input("Ingrese dos números a sumar: ", &num1, &num2);

    int sock = 0;
    struct sockaddr_in serv_addr;
    char buffer[BUFFER_SIZE] = {0};

    // Crear socket
    if ((sock = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        printf("\n Error al crear socket \n");
        return -1;
    }

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(PORT);

    // Convertir dirección IP a binario
    if (inet_pton(AF_INET, "127.0.0.1", &serv_addr.sin_addr) <= 0) {
        printf("\nDirección inválida / No soportada \n");
        return -1;
    }

    // Conectar
    if (connect(sock, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) < 0) {
        printf("\nConexión fallida \n");
        return -1;
    }

    // Construir solicitud XML-RPC para llamar a 'add(5, 7)'
    // char *xml_content =
    //     "<?xml version=\"1.0\"?>\r\n"
    //     "<methodCall>\r\n"
    //     "  <methodName>add</methodName>\r\n"
    //     "  <params>\r\n"
    //     "    <param>\r\n"
    //     "      <value><i4>5</i4></value>\r\n"
    //     "    </param>\r\n"
    //     "    <param>\r\n"
    //     "      <value><i4>7</i4></value>\r\n"
    //     "    </param>\r\n"
    //     "  </params>\r\n"
    //     "</methodCall>\r\n";

    char xml_content[BUFFER_SIZE];
    snprintf(
        xml_content, sizeof(xml_content),
        "<?xml version=\"1.0\"?>\r\n"
        "<methodCall>\r\n"
        "  <methodName>add</methodName>\r\n"
        "  <params>\r\n"
        "    <param>\r\n"
        "      <value><i4>%d</i4></value>\r\n"
        "    </param>\r\n"
        "    <param>\r\n"
        "      <value><i4>%d</i4></value>\r\n"
        "    </param>\r\n"
        "  </params>\r\n"
        "</methodCall>\r\n",
        num1, num2
    );

    char request[BUFFER_SIZE];
    snprintf(request, sizeof(request),
             "POST /RPC2 HTTP/1.1\r\n"
             "Host: localhost:%d\r\n"
             "Content-Type: text/xml\r\n"
             "Content-Length: %ld\r\n"
             "User-Agent: XML-RPC-C-Client\r\n"
             "\r\n"
             "%s", PORT, strlen(xml_content), xml_content);

    printf("Enviando solicitud:\n%s\n", request);
    send(sock, request, strlen(request), 0);
    
    printf(
        "Solicitud enviada.\n"
        " ============================================================== \n"
    );

    int total_bytes = 0;
    int valread;

    // Leemos mientras haya datos y espacio en el buffer
    while ((valread = read(sock, buffer + total_bytes, BUFFER_SIZE - total_bytes - 1)) > 0) {
        total_bytes += valread;
    }
    buffer[total_bytes] = '\0'; // Null-terminate al final de todo
    
    // Imprimir respuesta
    printf("Respuesta del servidor:\n%s\n", buffer);

    // Intentar extraer el resultado (muy básico)
    char *p = strstr(buffer, "<i4>");
    if (!p) p = strstr(buffer, "<int>");
    if (p) {
        int result = atoi(p + 4);
        printf("\nResultado decodificado: %d\n", result);
    }

    close(sock);
    return 0;
}
