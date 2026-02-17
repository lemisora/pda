package com.pda.Node;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pda.Constants.Net;
import com.pda.Enums.CommandType;
import com.pda.Manager.MessageManager;
import com.pda.Manager.JsonRpcConnectionManager;
import com.pda.Rpc.JsonRpcHandler;

import com.pda.Manager.Security.NetFilter;

/**
 * Clase para almacenar los nodos del sistema distribuido
 */
public class Nodo {

    /**
     * Record para enviar datos a otros nodos
     * 
     * @param destinoHost : IP del destino
     * @param destinoPort : puerto del destino
     * @param mensaje     : mensaje a enviar de tipo Mensaje (clase contenedora de
     *                    datos)
     */
    public record Envio(String destinoHost, int destinoPort, Mensaje mensaje) {
    }

    private BlockingQueue<Envio> colaEnvios = new LinkedBlockingQueue<Envio>();
    private ExecutorService executor = Executors.newFixedThreadPool(10);

    // Lista de IPs validas (se obtienen desde un archivo)
    private List<String> ipNodos;

    // Monitor para evitar múltiples elecciones simultáneas
    private AtomicBoolean electionInProgress = new AtomicBoolean(false);

    // Variables y constantes para el detector de fallos
    private volatile long lastHeartbeatTime = System.currentTimeMillis();
    private static final int HEARTBEAT_INTERVAL = 1000; // El líder envía cada 1s
    private static final int FAILURE_TIMEOUT = 3500; // Si pasan 3.5s, el líder murió

    private String IP;
    private int port;
    private String name;
    private int id; // Se usará para el algoritmo de bully

    // RPC
    private JsonRpcHandler rpcHandler;
    private int rpcPort;

    // Con esta variable se puede saber si es el nodo líder
    private boolean isLeader = false;
    // Con este booleano se determina si se puede elegir como candidato a líder o no
    private boolean candidateFailed = false;

    /**
     * Constructor de la clase Node
     * 
     * @param id   : Identificador numérico para el nodo
     * @param ip   : IP del nodo
     * @param port : Puerto del nodo
     * @param name : Nombre del nodo
     */
    public Nodo(int id, String ip, int port, String name) {
        this.id = id;
        this.IP = ip;
        this.port = port;
        this.name = name;

        // Cargar las IP desde un archivo
        // this.ipNodos = new ArrayList<>();
        // ToDo: Cargar las IPs válidas del sistema distribuido
        loadIPsFromFile("ips.txt");

        // Inicializar RPC
        this.rpcPort = port + 100;
        this.rpcHandler = new JsonRpcHandler();
        registerRpcMethods();
    }

    private void loadIPsFromFile(String filePath) {
        this.ipNodos = new ArrayList<>();
        System.out.println("Cargando nodos desde " + filePath);
        try {
            Path path = Paths.get(filePath);

            // Verificamos si existe
            if (!Files.exists(path)) {
                System.err.println("No se encontró 'ips.txt'. Creando archivo vacío de ejemplo...");
                Files.writeString(path, "# Agrega aquí las IPs de tus nodos (ej: 100.x.y.z)\n");
                return;
            }

            // Leer todas las líneas
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

            for (String line : lines) {
                // Limpiar espacios y comentarios
                String entry = line.split("#")[0].trim();

                if (!entry.isEmpty()) {
                    this.ipNodos.add(entry);
                    System.out.println("   -> Nodo agregado: " + entry);
                }
            }

            if (this.ipNodos.isEmpty()) {
                System.out.println("La lista de nodos está vacía. Este nodo está solo.");
            }

        } catch (IOException e) {
            System.err.println("Error leyendo configuración de red: " + e.getMessage());
        }
    }

    // ============ APARTADO DE SERVICIOS ============
    // Función general para iniciar el nodo
    public void start() throws IOException {
        // Iniciar hilos de envio y recepción
        startSender();
        startReceiver();
        startDiscover();

        startFailureDetection();
        startRpcServer();
    }

    /**
     * Función para elección de líder
     * 
     * @param remoteNodeID : ID del nodo remoto
     */
    public void bullyElectionVote(int remoteNodeID) {
        // En Bully, si alguien con ID mayor me responde, él manda.
        if (remoteNodeID > this.id) {
            System.out.println("[ELECCIÓN] El nodo " + remoteNodeID + " es mayor. Me retiro de la elección.");
            setCandidateFailed(true);
            setLeader(false);
        }
    }

    /** Función para convertirse en líder */
    private void becomeLeader() {
        this.isLeader = true;
        this.candidateFailed = false; // Reiniciar estado
        System.out.println("[LIDER] ¡Soy el nuevo líder! (ID: " + this.id + ")");

        // Avisar a los demás
        broadcast(CommandType.NEW_LEADER, "¡Soy el nuevo Líder!");

        // Iniciar proceso de envío de latidos
        startLeaderHeartbeat();
    }

    /**
     * Función para iniciar el proceso de búsqueda de nodos en Red (y encontrar un
     * nuevo líder, si solo hay un nodo entonces el líder es el mismo nodo)
     */
    private void startDiscover() {
        if (electionInProgress.getAndSet(true)) {
            System.out.println("[DISCOVER] Elección en proceso.");
            return;
        }

        // Se reinicia para que este nodo sea elegible como candidato
        this.candidateFailed = false;

        new Thread(() -> {
            System.out.println("[DISCOVER] Buscando otros nodos...");

            broadcast(CommandType.HELLO, "Voten por un líder.");

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.getMessage();
            }

            if (!candidateFailed && !isLeader) {
                becomeLeader();
            } else {
                System.out.println("[Nodo " + this.name + "] Me mantengo como seguidor.");
            }

            // Se libera el monitor
            electionInProgress.set(false);
            System.out.println("[DISCOVER] Fin de proceso de elección.");

        }).start();
    }

    // Hilos anónimos lambda
    /** Función para iniciar un hilo que envía peticiones a otros nodos */
    private void startSender() {
        new Thread(() -> {
            System.out.println("Iniciando hilo de envío de comandos...");
            while (true) {
                try {
                    Envio envio = colaEnvios.take();
                    sendEnvio(envio);
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    // Thread.currentThread().interrupt();
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
                while (true) {
                    Socket clientSocket = serverSocket.accept();

                    // Validar que la IP del cliente sea válida
                    // System.out.println("Validando transmisor de mensaje -> " +
                    // clientSocket.getInetAddress().getHostAddress());
                    if (NetFilter.isTailscaleIP(clientSocket.getInetAddress())
                            || NetFilter.isLocalhost(clientSocket.getInetAddress())) {
                        executor.execute(new MessageManager(clientSocket, this));
                    } else {
                        System.err.println("Cliente no válido: " + clientSocket.getInetAddress().getHostAddress());
                        clientSocket.close();
                    }
                    // executor.execute(new MessageManager(clientSocket, this));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Función que ejecuta el líder para avisar a todos los nodos que sigue activo
     */
    private void startLeaderHeartbeat() {
        new Thread(() -> {
            System.out.println(
                    "[Heartbeat Service] Iniciando servicio para informar a los otros Nodos de mi funcionamiento.");
            while (isLeader) {
                try {
                    broadcast(CommandType.HEARTBEAT, "Estoy funcionando");
                    Thread.sleep(HEARTBEAT_INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("[Heartbeat Service] Deteniendo servicio, el líder ha cambiado.");
        }).start();
    }

    /**
     * Función que se ejecuta para detectar fallos en la conexión entre los nodos
     * con el Nodo Líder
     */
    private void startFailureDetection() {
        new Thread(() -> {
            System.out.println("[Detector Service] Iniciando servicio que vigila el estado actual del líder.");
            while (true) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL);
                    if (isLeader)
                        continue;
                    long deltaHeartbeatTime = System.currentTimeMillis() - lastHeartbeatTime;

                    if (deltaHeartbeatTime > FAILURE_TIMEOUT) {
                        System.err.println(
                                "[Detector Service] El líder no responde desde hace " + deltaHeartbeatTime + " ms.");
                        System.out.println("[Detector Service] Iniciando una nueva elección de líder.");

                        // Se actualiza la última vez que se hizo un heartbeat para que no haya un bucle
                        lastHeartbeatTime = System.currentTimeMillis();
                        startDiscover();
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }).start();
    }

    /**
     * Función para actualizar el tiempo en el que se mandó el último latido por
     * parte del nodo Líder
     */
    public void updateLastHeartbeat() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    /**
     * Función para enviar llamadas (datos) a otros nodos - los añade a la cola de
     * envíos
     * 
     * @param destinoHost : IP del destino
     * @param destinoPort : puerto del destino
     * @param mensaje     : mensaje a enviar de tipo Mensaje (clase contenedora de
     *                    datos)
     */
    public void addDataToMessageQueue(String destinoHost, int destinoPort, Mensaje mensaje) {
        colaEnvios.offer(new Envio(destinoHost, destinoPort, mensaje));
    }

    /**
     * Función para enviar un mensaje a otro nodo
     ** 
     * @param envio : objeto de tipo 'Envio' que contiene la información de destino
     *              y mensaje a enviar
     */
    private void sendEnvio(Envio envio) {
        try (Socket socket = new Socket(envio.destinoHost(), envio.destinoPort())) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("Enviando petición a " + envio.destinoHost() + ":" + envio.destinoPort());
            out.writeObject(envio.mensaje());
            // System.out.println("[SENDER - " + this.name + "] Enviando a " +
            // envio.destinoHost() + ":" + envio.destinoPort());
            out.close();
        } catch (IOException e) {
            // System.err.println("[SENDER - " + this.name + "] Falló envío a
            // "+envio.destinoHost());
        }
    }

    private void broadcast(CommandType type, String data) {
        // Usamos la lista cargada desde el archivo
        for (String targetNode : this.ipNodos) {

            String targetHost;
            int targetPort;

            // Lógica para soportar formato IP:PUERTO o solo IP
            if (targetNode.contains(":")) {
                String[] parts = targetNode.split(":");
                targetHost = parts[0];
                targetPort = Integer.parseInt(parts[1]);
            } else {
                targetHost = targetNode;
                // Si no especifican puerto en el txt, asumimos que usan el mismo puerto que yo
                // (Arquitectura simétrica típica en Tailscale/Prod)
                targetPort = this.port;
            }

            // Evitar enviarme a mí mismo
            // Verificamos IP y Puerto por si estamos en localhost probando puertos
            // distintos
            if (targetHost.equals(this.IP) && targetPort == this.port) {
                continue;
            }

            // Construimos el mensaje CON MI IP Y PUERTO de retorno
            Mensaje msj = new Mensaje(type, this.id, this.name, this.IP, this.port, data);
            addDataToMessageQueue(targetHost, targetPort, msj);
        }
    }

    // ============ RPC SERVICES ============
    private void registerRpcMethods() {
        rpcHandler.registerMethod("ping", (req) -> "pong");
        rpcHandler.registerMethod("info", (req) -> {
            return "Node: " + name + " | ID: " + id + " | Leader: " + isLeader;
        });
    }

    private void startRpcServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(this.rpcPort)) {
                System.out.println("[RPC] Servidor iniciado en puerto " + this.rpcPort);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new JsonRpcConnectionManager(clientSocket, rpcHandler)).start();
                }
            } catch (IOException e) {
                System.err.println("[RPC] Error iniciando servidor: " + e.getMessage());
            }
        }).start();
    }

    // =================================================================================
    // Getters y Setters
    // =================================================================================
    public String getIP() {
        return IP;
    }

    public int getId() {
        return id;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public void setLeader(boolean leader) {
        isLeader = leader;
    }

    public void setCandidateFailed(boolean candidateFailed) {
        this.candidateFailed = candidateFailed;
    }

    public void setIP(String iP) {
        IP = iP;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}