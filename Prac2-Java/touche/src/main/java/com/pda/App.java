package com.pda;

import com.pda.Node.Nodo;
import com.pda.Node.Mensaje;
import com.pda.Enums.*;
import com.pda.Constants.*;

public class App {
    /** Función para procesar argumentos de la línea de comandos */
    /*TODO: Implementar esta función */
    static void processArgs( String[] args ) {
        switch(args.length) {
            case 0:
                System.out.println("Hola mundo");
                break;
            case 1:
                System.out.println("Hola mundo " + args[0]);
                break;
            default:
                System.out.println("Hola mundo " + args[0] + " " + args[1]);
                break;
        }
    }

    public static void main( String[] args ) {
        // processArgs( args );
        Nodo n1 = new Nodo(Net.localhost, 8000, "node1");
        // Nodo n2 = new Nodo(Net.localhost, 8001, "node2");
        
        for (int i = 0; i < 10; i++) {
            n1.addDataToMessageQueue(Net.localhost, 8001, new Mensaje(CommandType.WRITE, "node1",  "Hola mundo - "+i));
            // n2.addDataToMessageQueue(Net.localhost, 8000, new Mensaje(CommandType.WRITE, "node2",  "Hola mundo - "+i));
        }
    }
}
