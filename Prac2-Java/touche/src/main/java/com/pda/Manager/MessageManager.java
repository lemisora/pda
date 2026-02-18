package com.pda.Manager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
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

        if (mensaje.getCommand() != CommandType.HEARTBEAT)
            System.out.println("[Nodo '" + nodo.getName() + "'] Mensaje recibido: " + mensaje.toString());
        switch (mensaje.getCommand()) {
            // En este caso se ha iniciado una elección
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
                // En caso de seguir siendo líder por errores de red renunciar acá
                if (nodo.isLeader() && mensaje.getSenderId() > nodo.getId()) {
                    System.out.println("[CORRECCIÓN - Nodo (" + nodo.getName()
                            + ")] Recibí ALIVE tardío de un mayor. Dejo de ser líder.");
                    nodo.setLeader(false);
                }
            }
            case HEARTBEAT -> {

                if (nodo.isLeader() && mensaje.getSenderId() > nodo.getId()) {
                    System.err.println("[Nodo ( " + nodo.getName() + ") ] Detecté un líder con mayor ID: " + "( "
                            + mensaje.getSenderId() + "). Renunciando a puesto líder.");
                    nodo.setLeader(false);
                    nodo.setCandidateFailed(false);
                    break;
                }
                nodo.updateLastHeartbeat();
            }
            case NEW_LEADER -> {
                // NO procesar mi propio mensaje
                String senderKey = mensaje.getSenderHost() + ":" + mensaje.getSenderPort();
                if (senderKey.equals(nodo.getNodeKey())) {
                    return; // Ignorar silenciosamente
                }
                // Hay un nuevo líder oficial. Actualizo mi estado.
                System.out.println(
                        "Nuevo Líder reconocido: " + mensaje.getSenderName() + " (ID: " + mensaje.getSenderId() + ")");
                nodo.setLeader(false);
                nodo.setCandidateFailed(true); // Ya no intento ser líder

                nodo.updateLastHeartbeat();

                nodo.setLeaderNodeKey(senderKey);
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
        // Encontrar mejor nodo (por ahora, asignar al que pidió si tiene espacio)
        // TODO: Mejorar para buscar realmente el mejor nodo
        String targetIP = requesterIP;
        int targetPort = requesterPort;

        Mensaje assignment = new Mensaje(
                CommandType.STORE_ASSIGNED,
                nodo.getId(), nodo.getName(), nodo.getIP(), nodo.getPort(),
                fileName);

        nodo.addDataToMessageQueue(targetIP, targetPort, assignment);
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

        // TODO: Guardar en un Map<String, NodeStatus> para tener estado de todos los
        // nodos
    }

    private void handleListRequest(Mensaje mensaje) {
        List<String> files;

        if (nodo.isLeader()) {
            // Líder responde con catálogo global
            files = nodo.getStorageManager().listAllFiles();
        } else {
            // Nodo normal responde con sus archivos locales
            files = nodo.getStorageManager().listLocalFiles();
        }

        String fileList = String.join(", ", files);

        Mensaje response = new Mensaje(
                CommandType.LIST_RESPONSE,
                nodo.getId(), nodo.getName(), nodo.getIP(), nodo.getPort(),
                fileList);

        nodo.addDataToMessageQueue(mensaje.getSenderHost(), mensaje.getSenderPort(), response);
    }

    private void handleListResponse(Mensaje mensaje) {
        System.out.println("\n=== ARCHIVOS DISPONIBLES ===");
        System.out.println(mensaje.getData());
        System.out.println("============================\n");
    }
}
