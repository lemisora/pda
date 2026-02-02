package com.distribuida;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java com.distribuida.Main <puertoLocal> <ipSiguiente> <puertoSiguiente> [archivoInicial]");
            System.exit(1);
        }

        int puertoLocal = Integer.parseInt(args[0]);
        String ipSiguiente = args[1];
        int puertoSiguiente = Integer.parseInt(args[2]);
        String archivoInicial = (args.length >= 4) ? args[3] : null;

        Nodo nodo = new Nodo(puertoLocal, ipSiguiente, puertoSiguiente);
        nodo.iniciar();

        // Si se pasó un archivo por argumento (modo activo/test)
        if (archivoInicial != null) {
            try {
                // Pequeña pausa para que el servidor arranque
                Thread.sleep(1000); 
                System.out.println("\n[Main] Iniciando inyección manual de archivo: " + archivoInicial);
                nodo.procesarArchivo(archivoInicial);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // Mantener vivo el main si es necesario, aunque el hilo del servidor ya lo hace.
        // Opcional: un shell interactivo para probar
        if (archivoInicial == null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                System.out.println("\n--- Consola Interactiva ---");
                System.out.println("Escribe un nombre de archivo para crear (o 'exit'):");
                String input;
                while ((input = br.readLine()) != null) {
                    if ("exit".equalsIgnoreCase(input)) break;
                    nodo.procesarArchivo(input);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}