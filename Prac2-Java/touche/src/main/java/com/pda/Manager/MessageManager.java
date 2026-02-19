package com.pda.Manager;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.pda.Node.Nodo;
import com.pda.Node.Mensaje;
import com.pda.Enums.CommandType;

public class MessageManager implements Runnable {
    private Socket socket;
    private final Nodo nodo;

    /**
     * Constructor de la clase MessageManager
     * 
     * @param socket : socket de conexión con el cliente
     * @param nodo   : nodo al que corresponde este gestor de mensajes
     */
    public MessageManager(Socket socket, Nodo nodo) {
        this.socket = socket;
        this.nodo = nodo;
    }

    @Override
    public void run() {
        try (
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());) {
            // Deserializar mensaje recibido
            Mensaje mensaje = (Mensaje) in.readObject();

            // Procesar mensaje
            procesarMensaje(mensaje);

        } catch (Exception e) {
            System.err.println("Error al procesar el mensaje: " + e.getMessage());
        }
    }

    /**
     * Función para procesar el contenido del mensaje recibido
     * 
     * @param mensaje : mensaje recibido de tipo Mensaje (clase contenedora de
     *                datos)
     */
    public void procesarMensaje(Mensaje mensaje) {
        // Para poner menos mensajes de Debug (excluir a los de heartbeat)
        if (mensaje.getCommand() != CommandType.HEARTBEAT)
            System.out.println("[Nodo '" + nodo.getName() + "'] Mensaje recibido: " + mensaje.toString());
        switch (mensaje.getCommand()) {
            // En este caso se ha iniciado una elección
            case WRITE -> {
                System.out.println("Mensaje de escritura recibido: " + mensaje.getData());
            }
            case HELLO -> {
                // Si MI id es MAYOR que el del remitente, le respondo "ALIVE" (para callarlo)
                if (nodo.getId() > mensaje.getSenderId()) {
                    System.out.println("Recibí HELLO de " + mensaje.getSenderId() + ". Soy mayor (" + nodo.getId()
                            + "), le respondo ALIVE.");

                    Mensaje respuesta = new Mensaje(
                            CommandType.ALIVE,
                            nodo.getId(),
                            nodo.getName(),
                            nodo.getIP(),
                            nodo.getPort(),
                            "Detener elección, soy mayor. Nodo: " + nodo.getName() + "| id : " + nodo.getId());

                    // Respondemos directamente al host y puerto que venía en el mensaje
                    nodo.addDataToMessageQueue(mensaje.getSenderHost(), mensaje.getSenderPort(), respuesta);
                }
            }
            // Verificar si puedo ser líder y entrar en la elección
            case ALIVE -> {
                nodo.bullyElectionVote(mensaje.getSenderId());
            }
            case HEARTBEAT -> {
                nodo.updateLastHeartbeat();
            }
            case NEW_LEADER -> {
                // Hay un nuevo líder oficial. Actualizo mi estado.
                System.out.println(
                        "Nuevo Líder reconocido: " + mensaje.getSenderName() + " (ID: " + mensaje.getSenderId() + ")");
                nodo.setLeader(false);
                nodo.setCandidateFailed(true); // Ya no intento ser líder

                nodo.updateLastHeartbeat();
            }
            case STORE_REQUEST -> handleStoreRequest(mensaje);
            case STORE_ASSIGNED -> handleStoreAssigned(mensaje);
            case STORE_CONFIRMED -> handleStoreConfirmed(mensaje);
            case STORE_REJECTED -> handleStoreRejected(mensaje);
            case REPLICATE_FILE -> handleReplicateFile(mensaje);
            case REPLICA_CONFIRMED -> handleReplicaConfirmed(mensaje);
            case NODE_STATUS_UPDATE -> handleNodeStatusUpdate(mensaje);
            case LIST_REQUEST -> handleListRequest(mensaje);
            case LIST_RESPONSE -> handleListResponse(mensaje);
        }
    }

    // === HANDLERS PARA STORAGE ===

    private void handleStoreRequest(Mensaje mensaje) {
        // Solo el líder procesa esto
        if (!nodo.isLeader())
            return;

        String fileName = mensaje.getData();
        System.out.println("[Líder] Recibida petición para guardar: " + fileName);

        // TODO: Encontrar nodo con espacio y asignar
        // Por ahora respuesta simple
        assignFileToNode(fileName, mensaje.getSenderHost(), mensaje.getSenderPort());
    }

    private void assignFileToNode(String fileName, String requesterIP, int requesterPort) {
        System.out.println("[Debug] assignFileToNode -> Buscando duplicados...");
        // Verificar si el nodo ya tiene el archivo asignado
        if (nodo.getStorageManager().fileExistsInCatalog(fileName)) {
            System.out.println("[Líder] Archivo duplicado rechazado: " + fileName);

            Mensaje rejection = new Mensaje(
                    CommandType.STORE_REJECTED,
                    nodo.getId(), nodo.getName(), nodo.getIP(), nodo.getPort(),
                    fileName);
            nodo.addDataToMessageQueue(requesterIP, requesterPort, rejection);
            return;
        }

        System.out.println("[Debug] Archivo nuevo. Asignando a " + requesterIP + ":" + requesterPort);
        // Encontrar mejor nodo (por ahora, asignar al que pidió si tiene espacio)
        // TODO: Mejorar para buscar realmente el mejor nodo
        String targetIP = requesterIP;
        int targetPort = requesterPort;

        Mensaje assignment = new Mensaje(
                CommandType.STORE_ASSIGNED,
                nodo.getId(), nodo.getName(), nodo.getIP(), nodo.getPort(),
                fileName);

        nodo.addDataToMessageQueue(targetIP, targetPort, assignment);
        System.out.println("[Debug] Mensaje STORE_ASSIGNED encolado para " + targetIP + ":" + targetPort);
    }

    private void handleStoreAssigned(Mensaje mensaje) {
        String fileName = mensaje.getData();
        System.out.println("[Storage] Líder me asignó guardar: " + fileName);

        // Guardar archivo
        try {
            // Buscar en archivos_entrada O en archivos_pendientes
            Path sourcePath = nodo.getStorageManager().getEntradaDir().resolve(fileName);

            if (nodo.getStorageManager().storeFile(fileName, false)) {
                // Solo borrar si existe
                if (Files.exists(sourcePath)) {
                    Files.delete(sourcePath);
                }

                // Limpiar del Set
                nodo.removeFileFromProcess(fileName);

                // Confirmar al líder
                Mensaje confirmacion = new Mensaje(
                        CommandType.STORE_CONFIRMED,
                        nodo.getId(), nodo.getName(), nodo.getIP(), nodo.getPort(),
                        fileName);
                nodo.addDataToMessageQueue(mensaje.getSenderHost(), mensaje.getSenderPort(), confirmacion);
            }
        } catch (IOException e) {
            System.err.println("Error guardando archivo: " + e.getMessage());
            nodo.removeFileFromProcess(fileName); // Limpiar si falla
        }
    }

    private void handleStoreConfirmed(Mensaje mensaje) {
        if (!nodo.isLeader())
            return;

        String fileName = mensaje.getData();
        System.out.println("[Líder] Confirmado almacenamiento de: " + fileName);

        // Registrar en catálogo global
        nodo.getStorageManager().registerFileInCatalog(fileName, mensaje.getSenderHost());
    }

    private void handleStoreRejected(Mensaje mensaje) {
        String fileName = mensaje.getData();
        System.out.println("[Storage] Archivo rechazado por duplicado: " + fileName);

        try {
            nodo.getStorageManager().moveToRejected(fileName);

            // Limpiar del Set de archivos en proceso
            nodo.removeFileFromProcess(fileName);
        } catch (IOException e) {
            System.err.println("Error moviendo archivo rechazado: " + e.getMessage());
        }
    }

    private void handleReplicateFile(Mensaje mensaje) {
        String fileName = mensaje.getData();

        // Solo replicar si tengo espacio y NO soy el nodo origen
        if (nodo.getStorageManager().canStoreFile() &&
                !mensaje.getSenderHost().equals(nodo.getIP())) {

            try {
                if (nodo.getStorageManager().storeFile(fileName, true)) {
                    System.out.println("[Replication] Réplica creada para: " + fileName);

                    // Confirmar réplica
                    Mensaje confirmacion = new Mensaje(
                            CommandType.REPLICA_CONFIRMED,
                            nodo.getId(), nodo.getName(), nodo.getIP(), nodo.getPort(),
                            fileName);
                    nodo.addDataToMessageQueue(mensaje.getSenderHost(), mensaje.getSenderPort(), confirmacion);
                }
            } catch (IOException e) {
                System.err.println("Error creando réplica: " + e.getMessage());
            }
        }
    }

    private void handleReplicaConfirmed(Mensaje mensaje) {
        String fileName = mensaje.getData();
        System.out.println("[Replication] Confirmada réplica de '" + fileName + "' en " + mensaje.getSenderHost());

        // Si soy líder, actualizar catálogo
        if (nodo.isLeader()) {
            nodo.getStorageManager().registerFileInCatalog(fileName, mensaje.getSenderHost());
        }
    }

    private void handleNodeStatusUpdate(Mensaje mensaje) {
        if (!nodo.isLeader())
            return;

        String statusData = mensaje.getData(); // Formato: "3/5"
        System.out.println("[Líder] Estado actualizado de " + mensaje.getSenderName() + ": " + statusData);
    }

    private void handleListRequest(Mensaje mensaje) {
        System.out.println("[Debug] Procesando LIST_REQUEST de " + mensaje.getSenderName() + " ("
                + mensaje.getSenderHost() + ":" + mensaje.getSenderPort() + ")");

        List<String> files;
        if (nodo.isLeader()) {
            files = nodo.getStorageManager().listAllFiles();
            System.out.println("[Debug] Soy líder. Archivos globales: " + files.size());
        } else {
            files = nodo.getStorageManager().listLocalFiles();
            System.out.println("[Debug] Soy seguidor. Archivos locales: " + files.size());
        }

        String fileList = String.join(", ", files);

        Mensaje response = new Mensaje(
                CommandType.LIST_RESPONSE,
                nodo.getId(), nodo.getName(), nodo.getIP(), nodo.getPort(),
                fileList);

        nodo.addDataToMessageQueue(mensaje.getSenderHost(), mensaje.getSenderPort(), response);
        System.out.println(
                "[Debug] LIST_RESPONSE encolado para " + mensaje.getSenderHost() + ":" + mensaje.getSenderPort());
    }

    private void handleListResponse(Mensaje mensaje) {
        System.out.println("\n=== ARCHIVOS DISPONIBLES ===");
        System.out.println(mensaje.getData());
        System.out.println("============================\n");
    }
}