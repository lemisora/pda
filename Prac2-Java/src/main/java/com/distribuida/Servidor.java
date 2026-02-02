package com.distribuida;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor implements Runnable {
    private Nodo nodo;
    private int puerto;

    public Servidor(Nodo nodo, int puerto) {
        this.nodo = nodo;
        this.puerto = puerto;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("[Servidor] Escuchando en puerto " + puerto);
            
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                    
                    String nombreArchivo = in.readLine();
                    if (nombreArchivo != null && !nombreArchivo.isEmpty()) {
                        nodo.procesarArchivo(nombreArchivo);
                    }
                    
                } catch (Exception e) {
                    System.err.println("[Servidor] Error procesando petición: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[Servidor] Error fatal al iniciar: " + e.getMessage());
        }
    }
}