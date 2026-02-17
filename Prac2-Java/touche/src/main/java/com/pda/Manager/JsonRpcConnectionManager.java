package com.pda.Manager;

import com.pda.Rpc.JsonRpcHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class JsonRpcConnectionManager implements Runnable {
    private Socket socket;
    private JsonRpcHandler handler;

    public JsonRpcConnectionManager(Socket socket, JsonRpcHandler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);) {
            String line;
            // Lee línea por línea (asumiendo que cada petición RPC es una línea por
            // simplicidad)
            while ((line = in.readLine()) != null) {
                String response = handler.handleRequest(line);
                out.println(response);
            }
        } catch (IOException e) {
            System.err.println("Error en conexión RPC: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
