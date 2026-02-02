package com.pda.Node;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

import com.pda.Constants.Net;
import com.pda.Manager.MessageManager;

import com.pda.Manager.Security.NetFilter;

/**
 * Clase para almacenar los nodos del sistema distribuido
 */
public class Nodo {
    
    /**
     * Record para enviar datos a otros nodos
     * @param destinoHost : IP del destino
     * @param destinoPort : puerto del destino
     * @param mensaje : mensaje a enviar de tipo Mensaje (clase contenedora de datos)
     */
    public record Envio (String destinoHost, int destinoPort, Mensaje mensaje) {}
    
    private BlockingQueue<Envio> colaEnvios = new LinkedBlockingQueue<Envio>();
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    
    private String IP;
    private int port;
    private String name;
    
    // Con esta variable se puede saber si es el nodo líder
    private boolean isLeader = false;
    // private boolean running = true;
    
    /**
     * Constructor de la clase Node
     * @param ip : IP del nodo
     * @param port : puerto del nodo
     * @param name : nombre del nodo
     */
    public Nodo(String ip, int port, String name) {
        this.IP = ip;
        this.port = port;
        this.name = name;
        
        // Iniciar hilos de envio y recepción
        startSender();
        startReceiver();
    }
     
    // Hilos anónimos lambda
    /** Función para iniciar un hilo que envía peticiones a otros nodos */
    private void startSender() {
        new Thread(() -> {
            while(true) {
                try {
                    Envio envio = colaEnvios.take();
                    System.out.println("Enviando petición a " + envio.destinoHost() + ":" + envio.destinoPort());
                    sendEnvio(envio);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
     
    /** Función para iniciar un hilo que recibe peticiones de otros nodos */
    private void startReceiver() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(this.port, 50, InetAddress.getByName(Net.listenIP))) {
                System.out.println("Recibiendo peticiones en '" + Net.listenIP + ":" + this.port + "'");
                while(true) {
                    Socket clientSocket = serverSocket.accept();
                    
                    // Validar que la IP del cliente sea válida
                    System.out.println("Validando transmisor de mensaje -> " + clientSocket.getInetAddress().getHostAddress());
                    if (NetFilter.isTailscaleIP(clientSocket.getInetAddress()) || NetFilter.isLocalhost(clientSocket.getInetAddress())) {
                        executor.execute(new MessageManager(clientSocket, this));
                    } else {
                        System.err.println("Cliente no válido: " + clientSocket.getInetAddress().getHostAddress());
                        clientSocket.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
     
    /**
    * Método para enviar llamadas (datos) a otros nodos - los añade a la cola de envios
    * @param destinoHost : IP del destino
    * @param destinoPort : puerto del destino
    * @param mensaje : mensaje a enviar de tipo Mensaje (clase contenedora de datos)
    */
    public void addDataToMessageQueue(String destinoHost, int destinoPort, Mensaje mensaje) {
        colaEnvios.offer(new Envio(destinoHost, destinoPort, mensaje));
    }
    
    /** Función para enviar un mensaje a otro nodo 
    ** @param envio : objeto de tipo 'Envio' que contiene la información de destino y mensaje a enviar
    */
    private void sendEnvio(Envio envio){
        try (Socket socket = new Socket(envio.destinoHost(), envio.destinoPort())) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            
            out.writeObject(envio.mensaje());
            System.out.println("[SENDER - " + this.name + "] Enviando a " + envio.destinoHost() + ":" + envio.destinoPort());
            out.close();
        } catch (IOException e) {
            System.err.println("[SENDER - " + this.name + "] Falló envío a "+envio.destinoHost());
        }
    }
      
    // =================================================================================
    // Getters y Setters
    // =================================================================================
    public String getIP() { return IP; }

    public void setIP(String iP) { IP = iP; }

    public int getPort() { return port; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
}