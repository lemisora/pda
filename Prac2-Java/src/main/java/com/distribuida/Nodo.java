package com.distribuida;

import java.io.File;
import java.io.IOException;

public class Nodo {
    private int puertoLocal;
    private String ipSiguiente;
    private int puertoSiguiente;
    private String directorio;
    private final int LIMITE_ARCHIVOS = 5;

    public Nodo(int puertoLocal, String ipSiguiente, int puertoSiguiente) {
        this.puertoLocal = puertoLocal;
        this.ipSiguiente = ipSiguiente;
        this.puertoSiguiente = puertoSiguiente;
        this.directorio = "archivos_" + puertoLocal;
        
        File dir = new File(directorio);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    public void iniciar() {
        // Arrancamos el servidor en un hilo aparte
        Servidor servidor = new Servidor(this, puertoLocal);
        new Thread(servidor).start();
        System.out.println("[Nodo] Iniciado en puerto " + puertoLocal + ". Directorio: " + directorio);
        System.out.println("[Nodo] Vecino siguiente: " + ipSiguiente + ":" + puertoSiguiente);
    }

    public synchronized void procesarArchivo(String nombre) {
        int cantidadActual = contarArchivos();
        
        System.out.println("[Nodo " + puertoLocal + "] Petición procesar: " + nombre + " (Total actual: " + cantidadActual + ")");

        if (cantidadActual < LIMITE_ARCHIVOS) {
            crearArchivoLocal(nombre);
        } else {
            System.out.println("[Nodo " + puertoLocal + "] LLENO. Enviando a vecino...");
            Cliente.enviarArchivo(ipSiguiente, puertoSiguiente, nombre);
        }
    }

    private int contarArchivos() {
        File dir = new File(directorio);
        String[] files = dir.list();
        return (files != null) ? files.length : 0;
    }

    private void crearArchivoLocal(String nombre) {
        try {
            File f = new File(directorio + "/" + nombre);
            if (f.createNewFile()) {
                System.out.println("[Nodo " + puertoLocal + "] Archivo CREADO: " + nombre);
            } else {
                System.out.println("[Nodo " + puertoLocal + "] El archivo ya existe: " + nombre);
            }
        } catch (IOException e) {
            System.err.println("[Nodo " + puertoLocal + "] Error al crear archivo: " + e.getMessage());
        }
    }
}
