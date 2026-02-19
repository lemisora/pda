package com.pda;

import com.pda.Node.Nodo;
import com.pda.Constants.Net;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "touche", mixinStandardHelpOptions = true, version = "1.0", description = "Iniciar un nodo para un sistema distribuido simple.")

public class App implements Runnable {
    @Option(names = { "-I", "--IP" }, description = "IP del Nodo")
    private String ip = Net.localhost;

    @Option(names = { "-p", "--port" }, description = "Puerto del Nodo")
    private int port = Net.listenPort;

    @Option(names = { "-i", "--id" }, description = "ID del Nodo, por defecto es 1.")
    private int id = 1;

    @Option(names = { "-n", "--name" }, description = "Nombre del Nodo")
    private String nombre = "nodo-";

    @Override
    public void run() {
        // System.out.println(CommandLine.Help.Ansi.AUTO.string(
        // "@|bold,green Touché|@ - Iniciando un nodo para un sistema distribuido
        // simple.\n" + ip + "!|@ @|red,underline (Error simulado)|@"
        // ));
        // System.out.println("Valores de los argumentos recibidos: IP= " + ip + " |
        // Puerto= " + port + " | Nombre= " + nombre);

        Nodo n1 = new Nodo(id, ip, port, nombre);
        // Nodo n2 = new Nodo(ip, port+1, "node2");

        try {
            n1.start();
            // n2.start();

            // for (int i = 0; i < 10; i++) {
            // n1.addDataToMessageQueue(ip, n2.getPort(), new Mensaje(CommandType.WRITE,
            // nombre, "Hola mundo - "+i));
            // n2.addDataToMessageQueue(ip, n1.getPort(), new Mensaje(CommandType.WRITE,
            // "node2", "Hola mundo - "+i));
            // }

            System.out.println("Nodo iniciado. Presiona Ctrl+C para salir.");
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("Aplicación interrumpida.");
        } catch (Exception e) {
            System.err.println("Error al iniciar los servicios del Nodo " + n1.getName() + ": " + e.getMessage());
        }
    }

    /** Función para procesar argumentos de la línea de comandos */
    // static void processArgs( String[] args ) {
    // switch(args.length) {
    // default:
    // int i = 1;
    // for (String arg : args) {
    // System.out.println("Argumento "+i+": " + arg + " | Tipo de argumento: " +
    // arg.getClass());
    // i++;
    // }
    // break;
    // }
    // System.exit(1);
    // }

    // Suponiendo argumentos de la línea de comandos con la siguiente forma:
    // nombre ejecutable [ip] [puerto] [nombre del nodo]
    public static void main(String[] args) {
        new CommandLine(new App()).execute(args);
    }
}
