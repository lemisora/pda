#ifndef SHELL_H
#define SHELL_H

/**
 * Inicia el bucle principal de la Shell interactiva.
 * * @param master_ip   La dirección IP del Nodo Maestro (ej. "100.x.y.z" o "127.0.0.1")
 * @param master_port El puerto donde escucha el Maestro (ej. 8000)
 */
void start_shell(char* master_ip, int master_port);

#endif // SHELL_H