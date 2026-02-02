package com.pda.Manager;

import java.io.ObjectInputStream;
import java.net.Socket;

import com.pda.Node.Nodo;
import com.pda.Node.Mensaje;

public class MessageManager implements Runnable {
    private Socket socket;
    private final Nodo nodo;
    
    public MessageManager(Socket socket, Nodo nodo) {
        this.socket = socket;
        this.nodo = nodo;
    }
    
    @Override
    public void run() {
        try (
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        ) {
            // Desearilizar mensaje recibido
            Mensaje mensaje = (Mensaje) in.readObject();
            
            // Procesar mensaje
            procesarMensaje(mensaje);
            
        } catch (Exception e) {
            System.err.println("Error al procesar el mensaje: " + e.getMessage());
        }
    }
    
    /** Función para procesar el contenido del mensaje recibido */
    public void procesarMensaje(Mensaje mensaje) {
        System.out.println("[Nodo '" + nodo.getName() + "'] Mensaje recibido: " + mensaje.toString());
    }
}