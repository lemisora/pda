package com.pda.Manager;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.pda.Node.ArchivoInfo;
import com.pda.Node.NodeStatus;

public class StorageManager {
    private final Path storageDir; // ./archivos/
    private final Path replicasDir; // ./archivos/replicas/
    private final Path entradaDir; // ./archivos_entrada/

    private final int initialThreshold;
    private int currentThreshold;
    private final Map<String, ArchivoInfo> localFiles; // Archivos de este nodo

    // Solo para el líder
    private final Map<String, ArchivoInfo> globalCatalog; // Catálogo global

    /**
     * @param threshold : Umbral de máximo de archivos que cada nodo debe alojar
     * @param baseDir   : Dirección del directorio donde se almacenarán los
     *                  directorios que
     *                  alojarán los ficheros de este sistema
     */
    public StorageManager(int threshold, String baseDir) throws IOException {
        this.initialThreshold = threshold;
        this.currentThreshold = threshold;
        this.localFiles = new ConcurrentHashMap<>();
        this.globalCatalog = new ConcurrentHashMap<>();

        // Crear directorios
        this.storageDir = Paths.get(baseDir, "archivos");
        this.replicasDir = Paths.get(baseDir, "archivos", "replicas");
        this.entradaDir = Paths.get(baseDir, "archivos_entrada");

        // Mediante createDirectories se crean directorios, si existen no los
        // sobreescribe
        Files.createDirectories(storageDir);
        Files.createDirectories(replicasDir);
        Files.createDirectories(entradaDir);

        // Cargar archivos existentes al iniciar
        loadExistingFiles();
    }

    private void loadExistingFiles() throws IOException {
        // Cargar archivos normales
        try (var stream = Files.list(storageDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        localFiles.put(fileName, new ArchivoInfo(fileName, "localhost"));
                    });
        }

        // Cargar réplicas
        try (var stream = Files.list(replicasDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        localFiles.put(fileName + "_replica", new ArchivoInfo(fileName, "localhost"));
                    });
        }
    }

    public boolean canStoreFile() {
        int activeFiles = (int) localFiles.values().stream()
                .filter(info -> !info.isTempDelete())
                .count();
        return activeFiles < currentThreshold;
    }

    public boolean storeFile(String fileName, boolean isReplica) throws IOException {
        if (!canStoreFile()) {
            return false;
        }

        Path targetPath = isReplica
                ? replicasDir.resolve(fileName)
                : storageDir.resolve(fileName);

        // Crear archivo vacío (0 bytes)
        Files.createFile(targetPath);

        // Registrar en localFiles
        String key = isReplica ? fileName + "_replica" : fileName;
        localFiles.put(key, new ArchivoInfo(fileName, "localhost"));

        return true;
    }

    public List<String> listLocalFiles() {
        return localFiles.values().stream()
                .filter(info -> !info.isTempDelete())
                .map(ArchivoInfo::getFileName)
                .toList();
    }

    public int getCurrentFileCount() {
        return (int) localFiles.values().stream()
                .filter(info -> !info.isTempDelete())
                .count();
    }

    public NodeStatus getStatus(String nodeIP, int nodePort) {
        return new NodeStatus(nodeIP, nodePort, getCurrentFileCount(), currentThreshold);
    }

    public void updateThresholdIfNeeded() {
        int currentCount = getCurrentFileCount();
        if (currentCount <= initialThreshold && currentThreshold > initialThreshold) {
            System.out.println("[StorageManager] Volviendo al umbral inicial: " + initialThreshold);
            currentThreshold = initialThreshold;
        }
    }

    public void increaseThreshold(int extraSlots) {
        currentThreshold += extraSlots;
        System.out.println("[StorageManager] Umbral aumentado a: " + currentThreshold);
    }

    // === MÉTODOS SOLO PARA EL LÍDER ===

    public void registerFileInCatalog(String fileName, String ubicacion) {
        globalCatalog.computeIfAbsent(fileName, k -> new ArchivoInfo(fileName, ubicacion))
                .addUbicacion(ubicacion);
    }

    public ArchivoInfo getFileInfo(String fileName) {
        return globalCatalog.get(fileName);
    }

    public List<String> listAllFiles() {
        return globalCatalog.values().stream()
                .filter(info -> !info.isTempDelete())
                .map(ArchivoInfo::getFileName)
                .toList();
    }

    public Path getEntradaDir() {
        return entradaDir;
    }

    public boolean fileExistsInCatalog(String fileName) {
        return globalCatalog.containsKey(fileName);
    }

    public void updateNodeStatus(String nodeKey, NodeStatus status) {
        // Por ahora solo log, después implementaremos el Map
        System.out.println("[Estado Nodos] Actualizado " + nodeKey + ": " +
                status.currentFiles() + "/" + status.threshold());
    }

    public String findBestNodeForStorage() {
        // Por ahora retorna null - mejoraremos esto después
        // Cuando implementes el Map de estados, aquí buscarás el nodo con más espacio
        System.out.println("[Líder] Buscando mejor nodo... (por implementar)");
        return null;
    }

    public void moveToRejected(String fileName) throws IOException {
        // Por ahora solo log - implementaremos después
        System.out.println("[StorageManager] Archivo rechazado: " + fileName);
    }
}
