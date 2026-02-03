package com.pda.Manager;

import java.io.ObjectInputStream;
import java.net.Socket;

import com.pda.Node.Nodo;
import com.pda.Node.Mensaje;
import com.pda.Enums.CommandType;

public class MessageManager implements Runnable {
    private Socket socket;
    private final Nodo nodo;
    
    /**
     * Constructor de la clase MessageManager
     * @param socket : socket de conexión con el cliente
     * @param nodo : nodo al que corresponde este gestor de mensajes
     */
    public MessageManager(Socket socket, Nodo nodo) {
        this.socket = socket;
        this.nodo = nodo;
    }
    
    @Override
    public void run() {
        try (
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        ) {
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
     * @param mensaje : mensaje recibido de tipo Mensaje (clase contenedora de datos)
    */
    public void procesarMensaje(Mensaje mensaje) {
        System.out.println("[Nodo '" + nodo.getName() + "'] Mensaje recibido: " + mensaje.toString());
        switch (mensaje.getCommand()) {
            // En este caso se ha iniciado una elección
            case HELLO -> {
                // Si MI id es MAYOR que el del remitente, le respondo "ALIVE" (para callarlo)
                if (nodo.getId() > mensaje.getSenderId()) {
                    System.out.println("Recibí HELLO de " + mensaje.getSenderId() + ". Soy mayor (" + nodo.getId() + "), le respondo ALIVE.");

                    Mensaje respuesta = new Mensaje(
                            CommandType.ALIVE,
                            nodo.getId(),
                            nodo.getName(),
                            nodo.getIP(),
                            nodo.getPort(),
                            "Detener elección, soy mayor. Nodo: " + nodo.getName() + "| id : " + nodo.getId()
                    );

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
                System.out.println("Nuevo Líder reconocido: " + mensaje.getSenderName() + " (ID: " + mensaje.getSenderId() + ")");
                nodo.setLeader(false);
                nodo.setCandidateFailed(true); // Ya no intento ser líder

                nodo.updateLastHeartbeat();
            }
        }
    }
}