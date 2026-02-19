package com.pda.Node;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ArchivoInfo {
    private String fileName;
    private List<String> ubicaciones; // IPs donde está el archivo
    private boolean tempDelete;
    private Instant createdTimestamp;
    private Instant modifiedTimestamp;

    /**
     * @param fileName  : Nombre del archivo creado
     * @param ubicacion : IP que almacenará el archivo
     */
    public ArchivoInfo(String fileName, String ubicacion) {
        this.fileName = fileName;
        this.ubicaciones = new ArrayList<>();
        this.ubicaciones.add(ubicacion);
        this.tempDelete = false;
        this.createdTimestamp = Instant.now();
        this.modifiedTimestamp = this.createdTimestamp;
    }

    // Para réplicas múltiples simultáneas
    /**
     * @param fileName    : Nombre del archivo creado
     * @param ubicaciones : IPs que almacenarán el archivo (original y réplicas)
     */
    public ArchivoInfo(String fileName, List<String> ubicaciones) {
        this.fileName = fileName;
        this.ubicaciones = new ArrayList<>(ubicaciones);
        this.tempDelete = false;
        this.createdTimestamp = Instant.now();
        this.modifiedTimestamp = this.createdTimestamp;
    }

    /**
     * Función para añadir una réplica en tiempo de ejecución
     * 
     * @param ip : IP del nodo que lo almacenará
     */
    public void addUbicacion(String ip) {
        if (!ubicaciones.contains(ip)) {
            ubicaciones.add(ip);
            this.modifiedTimestamp = Instant.now();
        }
    }

    /**
     * Función para eliminar una réplica o un archivo original del sistema
     * 
     * @param ip : IP del nodo a liberar de carga
     */
    public void removeUbicacion(String ip) {
        ubicaciones.remove(ip);
        this.modifiedTimestamp = Instant.now();
    }

    /** Función para eliminar (soft delete) */
    public void markAsDeleted() {
        this.tempDelete = true;
        this.modifiedTimestamp = Instant.now();
    }

    /** Función para recuperación de archivo eliminado por error */
    public void recoverFromDelete() {
        this.tempDelete = false;
        this.modifiedTimestamp = Instant.now();
    }

    /**
     * Función para verificar que el archivo es accesible en los nodos que lo alojan
     */
    public boolean hasAvailableLocation() {
        return !ubicaciones.isEmpty() && !tempDelete;
    }

    // Getters
    public String getFileName() {
        return fileName;
    }

    public List<String> getUbicaciones() {
        return new ArrayList<>(ubicaciones);
    }

    public boolean isTempDelete() {
        return tempDelete;
    }

    public Instant getCreatedTimestamp() {
        return createdTimestamp;
    }

    public Instant getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    @Override
    public String toString() {
        return String.format("ArchivoInfo[%s, ubicaciones=%s, deleted=%b]",
                fileName, ubicaciones, tempDelete);
    }
}
