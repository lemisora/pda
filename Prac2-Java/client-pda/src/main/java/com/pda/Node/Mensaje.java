package com.pda.Node;

import com.pda.Enums.CommandType;
import java.io.Serializable;

/**
 * Clase para almacenar los mensajes que se envían entre nodos
 */
public class Mensaje implements Serializable {
    private final CommandType command;
    private final int senderId;
    private final String senderName;
    private final String senderHost;
    private final int senderPort;
    private final String data;

    public Mensaje(CommandType command,
            int senderId,
            String senderName,
            String senderHost,
            int senderPort,
            String data) {
        this.command = command;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderHost = senderHost;
        this.senderPort = senderPort;
        this.data = data;
    }

    public CommandType getCommand() {
        return command;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderHost() {
        return senderHost;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Mensaje [tipo='" + command + "', sender='" + senderName + "', content='" + data + "']";
    }
}
