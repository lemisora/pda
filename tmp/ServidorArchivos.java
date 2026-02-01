import java.io.*;
import java.net.*;

public class ServidorArchivos {
    private int puerto;
    private String directorioBase = "archivos_remotos";

    public ServidorArchivos(int puerto) {
        this.puerto = puerto;
        File dir = new File(directorioBase);
        if (!dir.exists()) dir.mkdir();
    }

    public void iniciar() {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor Distribuido escuchando en puerto " + puerto);
            
            while (true) {
                try (Socket socket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    
                    String nombreArchivo = in.readLine();
                    if (nombreArchivo != null) {
                        crearArchivo(nombreArchivo);
                        System.out.println("Petición recibida: Archivo '" + nombreArchivo + "' creado exitosamente.");
                    }
                } catch (IOException e) {
                    System.err.println("Error al procesar petición: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    private void crearArchivo(String nombre) throws IOException {
        File file = new File(directorioBase + "/" + nombre);
        if (file.createNewFile()) {
            try (FileWriter fw = new FileWriter(file)) {
                fw.write("Contenido generado por el sistema distribuido.");
            }
        }
    }

    public static void main(String[] args) {
        new ServidorArchivos(5000).iniciar();
    }
}