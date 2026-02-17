package com.pda.client;

import com.pda.Enums.CommandType;
import com.pda.Node.Mensaje;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

//import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientController {

    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private ComboBox<CommandType> commandCombo;
    @FXML
    private TextField dataField;
    @FXML
    private TextArea logArea;

    @FXML
    public void initialize() {
        commandCombo.getItems().setAll(CommandType.values());
        commandCombo.setValue(CommandType.HELLO);
        log("Client initialized.");
    }

    @FXML
    private void handleSend() {
        String host = hostField.getText();
        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            log("Invalid port number.");
            return;
        }

        CommandType type = commandCombo.getValue();
        String data = dataField.getText();

        new Thread(() -> sendRequest(host, port, type, data)).start();
    }

    private void sendRequest(String host, int port, CommandType type, String data) {
        log("Sending " + type + " to " + host + ":" + port + "...");

        // ID 999 para identificar al cliente
        Mensaje mensaje = new Mensaje(type, 999, "ClientApp", "localhost", 0, data);

        try (Socket socket = new Socket(host, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(mensaje);
            log("Message sent successfully.");

            // Opcional: Si el servidor envía respuesta sincrónica, leerla aquí.
            // Pero touché usa asíncrono (envía a otro socket).
            // Para simplificar, asumimos fire-and-forget o lectura básica si hay listener.

        } catch (Exception e) {
            log("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void log(String message) {
        javafx.application.Platform.runLater(() -> logArea.appendText(message + "\n"));
    }
}
