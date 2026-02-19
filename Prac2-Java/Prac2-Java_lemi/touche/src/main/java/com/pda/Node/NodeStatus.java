package com.pda.Node;

import java.io.Serializable;

public record NodeStatus(
        String nodeIP,
        int nodePort,
        int currentFiles,
        int threshold,
        int availableSlots
) implements Serializable {

    /** Record que almacena el estado de cada nodo
     * @param nodeIP : IP del nodo reportado
     * @param nodePort : Puerto del nodo reportado
     * @param currentFiles : número total de archivos alojados en el nodo al momento.
     * @param threshold : Umbral de máximo de archivos (capacidad) que debe almacenar un nodo*/
    public NodeStatus(String nodeIP, int nodePort, int currentFiles, int threshold) {
        this(nodeIP, nodePort, currentFiles, threshold, threshold - currentFiles);
    }

    /**Función para verificar que aún exista espacio disponible*/
    public boolean hasSpace() {
        return availableSlots > 0;
    }

    /**Función de alerta para verificar si el almacenamiento disponible es poco*/
    public boolean isAlmostFull() {
        return availableSlots <= 1;
    }
}