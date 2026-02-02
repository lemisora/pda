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
    }
}