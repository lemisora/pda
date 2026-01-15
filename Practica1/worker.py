import json
import os
import socket
import threading
from pathlib import Path

from common import rec_msg, send_msg

# Configuración local
STORAGE_DIR = Path("./archivos")
MASTER_IP = "100.107.126.50"  # IP de Tailscale del Maestro
MASTER_PORT = 5000
WORKER_PORT = 5001

def get_local_file_count():
    """Cuenta archivos físicos en el directorio de almacenamiento."""
    if not STORAGE_DIR.exists():
        STORAGE_DIR.mkdir(parents=True)
    # Contamos solo archivos, ignorando carpetas
    return len([f for f in STORAGE_DIR.iterdir() if f.is_file()])

def handle_shell_request(conn, addr):
    """Maneja las peticiones de creación de archivos enviadas por la Shell."""
    try:
        data = rec_msg(conn)
        if data and data.get("action") == "TOUCH":
            filename = data.get("filename")
            file_path = STORAGE_DIR / filename
            
            # Operación física: Crear el archivo vacío
            file_path.touch()
            print(f"[+] Archivo creado: {file_path}")
            
            send_msg(conn, {"status": "success"})
    except Exception as e:
        print(f"[!] Error al procesar touch: {e}")
    finally:
        conn.close()

def start_worker():
    # 1. Preparar almacenamiento
    initial_count = get_local_file_count()
    print(f"[*] Almacenamiento listo en '{STORAGE_DIR}'. Conteo inicial: {initial_count}")

    # 2. Registrarse con el Maestro
    try:
        master_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        master_sock.connect((MASTER_IP, MASTER_PORT))
        send_msg(master_sock, {
            "action": "REGISTER_WORKER",
            "count": initial_count
        })
        master_sock.close()
        print("[*] Registro exitoso con el Maestro.")
    except Exception as e:
        print(f"[!] No se pudo contactar al Maestro: {e}")
        return

    # 3. Escuchar peticiones de la Shell (Cliente)
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("0.0.0.0", WORKER_PORT))
    server.listen()
    print(f"[*] Worker escuchando peticiones en el puerto {WORKER_PORT}...")

    while True:
        conn, addr = server.accept()
        thread = threading.Thread(target=handle_shell_request, args=(conn, addr))
        thread.start()

if __name__ == "__main__":
    start_worker()