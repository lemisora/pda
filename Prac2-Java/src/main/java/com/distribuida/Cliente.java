package com.distribuida;

import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {
    
    public static void enviarArchivo(String ip, int puerto, String nombreArchivo) {
        try (Socket socket = new Socket(ip, puerto);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            out.println(nombreArchivo);
            System.out.println("[Cliente] Orden de creación enviada a " + ip + ":" + puerto + " para: " + nombreArchivo);
            
        } catch (Exception e) {
            System.err.println("[Cliente] Error al conectar con vecino: " + e.getMessage());
        }
    }
}
