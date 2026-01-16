import socket
import threading
from datetime import datetime
from typing import Dict, List, Optional

from common import (
    rec_msg,
    send_msg,
)


# Clase que sirve como contenedor para las configuraciones de almacenamiento del sistema distribuido
class StorageManager:
    def __init__(self, threshold: int):
        # Se usa un Lock para concurrencia
        self.lock = threading.Lock()
        # Umbral para usar otro nodo
        self.threshold = threshold
        self.metadata: Dict[
            str, str
        ] = {}  # Estructura esperada 'nombre_archivo' e 'IP_Worker'
        self.nodes: Dict[
            str, Dict
        ] = {}  # Estructura esperada 'IP_Worker' y 'detalles_nodo'

    # Función para registrar a un nodo (worker)
    def register_worker(self, ip: str, count: int):
        with self.lock:
            self.nodes[ip] = {
                "file_count": count,
                "status": "online",
                "last_update": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            }
            print(f"Worker {ip} registrado con {count} archivos")

    # Se obtiene el nodo al que escribir el archivo
    def get_target_node(self) -> Optional[str]:
        with self.lock:
            # Buscamos el primer nodo que no supere el umbral
            for ip, data in self.nodes.items():
                if data["file_count"] < self.threshold:
                    return ip

            # Si todos están llenos, se devuelve el último
            return list(self.nodes.keys())[-1] if self.nodes else None

    # Se añade un archivo al registro del almacenamiento distribuido
    def add_file_record(self, filename: str, worker_ip: str):
        with self.lock:
            self.metadata[filename] = worker_ip
            if worker_ip in self.nodes:
                self.nodes[worker_ip]["file_count"] += 1
                self.nodes[worker_ip]["last_update"] = datetime.now().strftime(
                    "%Y-%m-%d %H:%M:%S"
                )

    # Obtener el nombre de los archivos registrados en el sistema mediante los metadatos
    def get_all_files(self) -> List[str]:
        with self.lock:
            return list(self.metadata.keys())


# Función para manejar conexiones/petición con los nodos (workers)
def handle_client(conn, addr, manager):
    print(f"[+] Nueva petición desde {addr[0]}")
    try:
        while True:
            data = rec_msg(conn)
            if not data:
                break

            action = data.get("action")
            response = {"status": "error", "message": "Acción desconocida"}

            # Dispatcher de comandos (Acciones)

            # En caso de que se añada un nodo
            if action == "REGISTER_WORKER":
                manager.register_worker(addr[0], data["count"])
                response = {"status": "success"}

            # En caso de que se solicite escribir al almacenamiento de un nodo
            # (obtener el nodo específico al que escribir)
            elif action == "GET_WRITE_NODE":
                target_ip = manager.get_target_node()
                response = {"status": "success", "ip": target_ip}

            # En caso de que se necesite crear un archivo
            elif action == "CONFIRM_TOUCH":
                target_ip = data.get("worker_ip")
                manager.add_file_record(data["filename"], target_ip)
                response = {"status": "success"}

            # En caso de que se solicite listar los archivos en 'archivos/'
            elif action == "LIST_FILES":
                files = manager.get_all_files()
                response = {"status": "success", "files": files}

            # En caso de que se consulte el estado de los nodos
            elif action == "GET_STATUS":
                response = {
                    "status": "success",
                    "nodes": manager.nodes,
                    "total_archivos": len(manager.metadata),
                }
            elif action == "GET_CONFIG":
                response = {
                    "status": "success",
                    "threshold": manager.threshold,  # Enviamos el valor real del manager
                }

            send_msg(conn, response)
    except Exception as e:
        print(f"[!] Error con {addr}: {e}")
    finally:
        conn.close()


# Función para comenzar a ejecutar el nodo 'maestro'
def start_master(host="0.0.0.0", port=5000):
    manager = StorageManager(threshold=5)
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    try:
        server.bind((host, port))
        server.listen()
        print(f"[*] Maestro iniciado en {host}:{port}")
        print(f"[*] Umbral de archivos: {manager.threshold}")

        while True:
            conn, addr = server.accept()
            threading.Thread(
                target=handle_client, args=(conn, addr, manager), daemon=True
            ).start()
    except KeyboardInterrupt:
        print("\n[!] Deteniendo maestro...")
    finally:
        server.close()


if __name__ == "__main__":
    start_master()
