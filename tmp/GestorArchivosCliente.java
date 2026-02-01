import java.io.*;
import java.net.*;

public class GestorArchivosCliente {
    private final String RUTA_LOCAL = "archivos_locales";
    private final int LIMITE = 5;
    private String ipRemota;
    private int puertoRemoto;

    public GestorArchivosCliente(String ipRemota, int puertoRemoto) {
        this.ipRemota = ipRemota;
        this.puertoRemoto = puertoRemoto;
        File dir = new File(RUTA_LOCAL);
        if (!dir.exists()) dir.mkdir();
    }

    public void procesarCreacion(String nombreArchivo) {
        int totalArchivos = contarArchivosLocales();

        if (totalArchivos < LIMITE) {
            crearLocal(nombreArchivo);
        } else {
            System.out.println("Límite local alcanzado (" + LIMITE + "). Redirigiendo a nodo remoto...");
            enviarANodoRemoto(nombreArchivo);
        }
    }

    private int contarArchivosLocales() {
        return new File(RUTA_LOCAL).list().length;
    }

    private void crearLocal(String nombre) {
        try {
            File f = new File(RUTA_LOCAL + "/" + nombre);
            if (f.createNewFile()) {
                System.out.println("[Local] Archivo creado: " + nombre);
            }
        } catch (IOException e) {
            System.err.println("Error local: " + e.getMessage());
        }
    }

    private void enviarANodoRemoto(String nombre) {
        try (Socket socket = new Socket(ipRemota, puertoRemoto);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            out.println(nombre);
            System.out.println("[Remoto] Orden de creación enviada a " + ipRemota);
            
        } catch (IOException e) {
            System.err.println("[Error de Red] No se pudo conectar con el nodo remoto: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        
        GestorArchivosCliente cliente = new GestorArchivosCliente("127.0.0.1", 5000);
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("Ingrese nombre del archivo (o 'salir'): ");
                String nombre = br.readLine();
                if (nombre.equalsIgnoreCase("salir")) break;
                cliente.procesarCreacion(nombre);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


