package com.pda.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.pda.Enums.CommandType;
import com.pda.Node.Mensaje;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class App extends Application {

    private ListView<String> fileListView;
    private TextArea logArea;
    private TextField ipField;
    private TextField portField;

    // Configuración por defecto
    private String serverIP = "127.0.0.1";
    private int serverPort = 8080;

    // Puerto de escucha del cliente
    private int clientPort = 9090;
    private ServerSocket listenerSocket;

    // ID ficticio para el cliente
    private int clientId = 999;
    private String clientName = "client-gui";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Cliente DFS - Touché");

        // Iniciar listener para respuestas
        startResponseListener();

        // === Panel de Configuración ===
        ipField = new TextField(serverIP);
        ipField.setPromptText("IP del Nodo");
        portField = new TextField(String.valueOf(serverPort));
        portField.setPromptText("Puerto");

        Button updateConfigBtn = new Button("Actualizar Conexión");
        updateConfigBtn.setOnAction(e -> updateConnectionInfo());

        HBox configBox = new HBox(10, new Label("IP:"), ipField, new Label("Puerto:"), portField, updateConfigBtn);
        configBox.setPadding(new Insets(10));
        configBox.setStyle("-fx-background-color: #e0e0e0;");

        // === Panel Principal ===
        fileListView = new ListView<>();
        fileListView.setPlaceholder(new Label("No hay archivos cargados."));

        Button refreshBtn = new Button("🔄 Actualizar Lista");
        refreshBtn.setOnAction(e -> requestFileList());

        Button uploadBtn = new Button("⬆️ Subir Archivo");
        uploadBtn.setOnAction(e -> uploadFile(primaryStage));

        HBox actionsBox = new HBox(10, refreshBtn, uploadBtn);
        actionsBox.setPadding(new Insets(10));

        // === Area de Logs ===
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setStyle("-fx-control-inner-background: #000000; -fx-text-fill: #00ff00;");

        VBox root = new VBox(10, configBox, actionsBox, new Label("Archivos en el Sistema:"), fileListView,
                new Label("Logs:"), logArea);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.show();

        log("Cliente iniciado. Escuchando respuestas en puerto " + clientPort);
        log("Listo para conectar a " + serverIP + ":" + serverPort);
    }

    private void startResponseListener() {
        new Thread(() -> {
            try {
                // Intentar encontrar puerto libre si 9090 está ocupado es complejo aquí,
                // asumiremos 9090 fijo o fallamos.
                listenerSocket = new ServerSocket(clientPort);

                while (true) {
                    Socket socket = listenerSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    try {
                        Mensaje mensaje = (Mensaje) in.readObject();
                        // Procesar en hilo de UI
                        Platform.runLater(() -> handleIncomingMessage(mensaje));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    socket.close();
                }
            } catch (IOException e) {
                Platform.runLater(
                        () -> log("Error en listener (¿puerto " + clientPort + " ocupado?): " + e.getMessage()));
            }
        }).start();
    }

    private void handleIncomingMessage(Mensaje mensaje) {
        log("<<<< Recibido: " + mensaje.getCommand());

        switch (mensaje.getCommand()) {
            case LIST_RESPONSE -> {
                String rawData = mensaje.getData();
                if (rawData == null || rawData.isEmpty()) {
                    fileListView.getItems().clear();
                    log("Lista vacía recibida.");
                    return;
                }
                String[] files = rawData.split(", ");
                fileListView.getItems().clear();
                fileListView.getItems().addAll(files);
                log("Lista de archivos actualizada (" + files.length + " archivos).");
            }
            case STORE_ASSIGNED -> log("¡Archivo asignado! El sistema lo gestionará.");
            case STORE_REJECTED -> log("Error: El archivo fue rechazado por el servidor.");
            case REPLICA_CONFIRMED -> log("Confirmación de réplica recibida.");
            default -> log("Mensaje desconocido: " + mensaje.toString());
        }
    }

    @Override
    public void stop() throws Exception {
        if (listenerSocket != null && !listenerSocket.isClosed()) {
            listenerSocket.close();
        }
        super.stop();
    }

    private void updateConnectionInfo() {
        try {
            serverIP = ipField.getText();
            serverPort = Integer.parseInt(portField.getText());
            log("Configuración actualizada: " + serverIP + ":" + serverPort);
        } catch (NumberFormatException e) {
            log("Error: El puerto debe ser un número.");
        }
    }

    private void requestFileList() {
        log(">>>> Solicitando lista de archivos...");
        sendStoreRequest(CommandType.LIST_REQUEST, "LIST");
    }

    private void uploadFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo para subir");
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            TextInputDialog dialog = new TextInputDialog(file.getName());
            dialog.setTitle("Confirmar Nombre");
            dialog.setHeaderText("Subiendo: " + file.getName());
            dialog.setContentText("Nombre en el sistema:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                log(">>>> Solicitando guardar: " + name);
                sendStoreRequest(CommandType.STORE_REQUEST, name);
            });
        }
    }

    private void sendStoreRequest(CommandType type, String data) {
        new Thread(() -> {
            try {
                // Enviamos NUESTRA IP y PUERTO para que el servidor nos responda
                Mensaje request = new Mensaje(
                        type,
                        clientId, clientName, "127.0.0.1", clientPort,
                        data);

                sendFireAndForget(request);
            } catch (Exception e) {
                Platform.runLater(() -> log("Error enviando solicitud: " + e.getMessage()));
            }
        }).start();
    }

    private void sendFireAndForget(Mensaje mensaje) {
        try (Socket socket = new Socket(serverIP, serverPort);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(mensaje);
            // log("Mensaje enviado al socket.");

        } catch (IOException e) {
            Platform.runLater(
                    () -> log("Error de conexión con nodo (" + serverIP + ":" + serverPort + "): " + e.getMessage()));
        }
    }

    private void log(String msg) {
        logArea.appendText(msg + "\n");
    }
}
