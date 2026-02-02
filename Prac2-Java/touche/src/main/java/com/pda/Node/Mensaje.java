package com.pda.Node;

import com.pda.Enums.CommandType;
import java.io.Serializable;

/**
 * Clase para almacenar los mensajes que se envían entre nodos
 */
public class Mensaje implements Serializable{
    private final CommandType command;
    private final String senderName;
    private final String data;
    
    public Mensaje(CommandType command, String senderName, String data) {
        this.command = command;
        this.senderName = senderName;
        this.data = data;
    }

    public CommandType getCommand() {
        return command;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return "Mensaje [tipo='" + command + "', sender='" + senderName + "', content='" + data + "']";
    }
    
}