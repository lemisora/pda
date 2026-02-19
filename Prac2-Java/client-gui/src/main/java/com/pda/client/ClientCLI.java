package com.pda.client;

import com.pda.Enums.CommandType;
import com.pda.Node.Mensaje;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ClientCLI {

    private String targetIP = "127.0.0.1";
    private int targetPort = 8080;

    private int myPort;
    private ServerSocket listenerSocket;
    private volatile boolean running = true; // Use volatile for thread visibility

    public static void main(String[] args) {
        new ClientCLI().start(args);
    }

    public void start(String[] args) {
        if (args.length >= 2) {
            targetIP = args[0];
            targetPort = Integer.parseInt(args[1]);
        }

        System.out.println("=== Cliente CLI DFS - Touché ===");
        System.out.println("Configurado hacia nodo: " + targetIP + ":" + targetPort);

        // Iniciar listener en background
        initListener();

        // Loop principal de comandos
        try (Scanner scanner = new Scanner(System.in)) {
            printHelp();
            while (running) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();

                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\s+", 2);
                String command = parts[0].toLowerCase();
                String arg = parts.length > 1 ? parts[1] : "";

                switch (command) {
                    case "ls":
                    case "list":
                        sendListRequest();
                        break;
                    case "upload":
                    case "put":
                        if (arg.isEmpty()) {
                            System.out.println("Uso: upload <nombre_archivo>");
                        } else {
                            sendStoreRequest(arg);
                        }
                        break;
                    case "connect":
                        handleConnect(arg);
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "exit":
                    case "quit":
                        running = false;
                        System.out.println("Saliendo...");
                        break;
                    default:
                        System.out.println("Comando desconocido. Escribe 'help' para ver opciones.");
                }
            }
        }

        // Cerrar recursos
        stopListener();
        System.exit(0);
    }

    private void initListener() {
        new Thread(() -> {
            try {
                listenerSocket = new ServerSocket(0); // Puerto aleatorio
                myPort = listenerSocket.getLocalPort();
                System.out.println("[Sistema] Escuchando respuestas en puerto local: " + myPort);

                while (running) {
                    try {
                        Socket socket = listenerSocket.accept();
                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                        Mensaje response = (Mensaje) in.readObject();

                        handleIncomingMessage(response);

                        socket.close();
                    } catch (IOException | ClassNotFoundException e) {
                        if (running)
                            System.err.println("[Listener Error]: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("No se pudo iniciar el listener: " + e.getMessage());
            }
        }).start();
    }

    private void stopListener() {
        running = false;
        try {
            if (listenerSocket != null && !listenerSocket.isClosed()) {
                listenerSocket.close();
            }
        } catch (IOException e) {
            // Ignorar
        }
    }

    private void handleIncomingMessage(Mensaje mensaje) {
        // Imprimir saltos de línea para no romper el prompt "> " visualmente
        System.out.println("\n");
        System.out.println("<<< RESPUESTA RECIBIDA (" + mensaje.getCommand() + ") <<<");

        switch (mensaje.getCommand()) {
            case LIST_RESPONSE -> {
                System.out.println("--- ARCHIVOS DISPONIBLES ---");
                String[] files = mensaje.getData().split(", ");
                if (mensaje.getData().isEmpty()) {
                    System.out.println("(Ninguno)");
                } else {
                    for (String f : files) {
                        System.out.println("  - " + f);
                    }
                }
                System.out.println("----------------------------");
            }
            case STORE_ASSIGNED -> {
                System.out.println("[OK] Solicitud aceptada por el lider.");
                System.out.println("[INFO] Enviando confirmacion de almacenamiento...");

                // En un sistema real, aquí enviaríamos los bytes del archivo.
                // Simulamos que ya "guardamos" el archivo localmente y confirmamos al líder.

                Mensaje confirmacion = new Mensaje(
                        CommandType.STORE_CONFIRMED,
                        777, "client-cli", "127.0.0.1", myPort,
                        mensaje.getData() // Nombre del archivo
                );

                // Responder al remitente (el líder)
                sendDirectMessage(mensaje.getSenderHost(), mensaje.getSenderPort(), confirmacion);
            }
            case STORE_REJECTED -> System.out.println("[ERROR] El archivo fue rechazado (¿Duplicado?).");
            case REPLICA_CONFIRMED -> System.out.println("[INFO] Confirmacion de replica recibida.");
            default -> System.out.println("Mensaje: " + mensaje.toString());
        }
        System.out.print("> "); // Restaurar prompt
    }

    private void sendDirectMessage(String ip, int port, Mensaje mensaje) {
        try (Socket socket = new Socket(ip, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(mensaje);
        } catch (IOException e) {
            System.err.println("❌ Error enviando confirmación a " + ip + ":" + port);
        }
    }

    private void sendListRequest() {
        System.out.println("Solicitando lista...");
        Mensaje request = new Mensaje(
                CommandType.LIST_REQUEST,
                777, "cli-user", "127.0.0.1", myPort,
                "LIST");
        sendFireAndForget(request);
    }

    private void sendStoreRequest(String fileName) {
        // Simulamos envío de solicitud de guardado
        System.out.println("Solicitando guardar: '" + fileName + "' ...");
        Mensaje request = new Mensaje(
                CommandType.STORE_REQUEST,
                777, "cli-user", "127.0.0.1", myPort,
                fileName);
        sendFireAndForget(request);
    }

    private void handleConnect(String arg) {
        String[] parts = arg.split(":");
        if (parts.length == 2) {
            try {
                targetIP = parts[0];
                targetPort = Integer.parseInt(parts[1]);
                System.out.println("Objetivo cambiado a " + targetIP + ":" + targetPort);
            } catch (NumberFormatException e) {
                System.out.println("Error: El puerto debe ser un número entero.");
            }
        } else {
            System.out.println("Uso: connect <IP>:<PUERTO> (ej: connect 127.0.0.1:8081)");
        }
    }

    private void sendFireAndForget(Mensaje mensaje) {
        if (myPort == 0) {
            System.out.println("Wait: Listener no iniciado todavía...");
            return;
        }

        try (Socket socket = new Socket(targetIP, targetPort);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(mensaje);
            // System.out.println("[DEBUG] Enviado " + mensaje.getCommand());
        } catch (IOException e) {
            System.err.println("❌ Error conectando con " + targetIP + ":" + targetPort + " -> " + e.getMessage());
        }
    }

    private void printHelp() {
        System.out.println("\nComandos disponibles:");
        System.out.println("  ls, list             : Listar archivos en el sistema");
        System.out.println("  upload <nombre>      : Subir (solicitar guardar) un archivo");
        System.out.println("  connect <ip>:<port>  : Cambiar nodo objetivo");
        System.out.println("  help                 : Ver esta ayuda");
        System.out.println("  exit, quit           : Salir");
    }
}
