import json
import socket
import threading
from datetime import datetime
from typing import Dict, List, Optional

from common import (  # Asumiendo las funciones de utilidad previas
    rec_msg,
    send_msg,
)


class StorageManager:
    def __init__(self, threshold: int):
        self.lock = threading.Lock()
        self.threshold = threshold
        self.metadata: Dict[
            str, str
        ] = {}  # Estructura esperada 'nombre_archivo' e 'IP_Worker'
        self.nodes: Dict[
            str, Dict
        ] = {}  # Estructura esperada 'IP_Worker' y 'detalles_nodo'

    def register_worker(self, ip: str, count: int):
        with self.lock:
            self.nodes[ip] = {
                "file_count": count,
                "status": "online",
                "last_update": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            }
            print(f"Worker {ip} registrado con {count} archivos")

    def get_target_node(self) -> Optional[str]:
        with self.lock:
            # Buscamos el primer nodo que no supere el umbral
            for ip, data in self.nodes.items():
                if data["file_count"] < self.threshold:
                    return ip

            # Fallback: Si todos están llenos, podrías devolver el último
            # o manejar un error de 'Almacenamiento Lleno'
            return list(self.nodes.keys())[-1] if self.nodes else None

    def add_file_record(self, filename: str, worker_ip: str):
        with self.lock:
            self.metadata[filename] = worker_ip
            if worker_ip in self.nodes:
                self.nodes[worker_ip]["file_count"] += 1
                self.nodes[worker_ip]["last_update"] = datetime.now().strftime(
                    "%Y-%m-%d %H:%M:%S"
                )

    def get_all_files(self) -> List[str]:
        with self.lock:
            return list(self.metadata.keys())


def handle_client(conn, addr, manager):
    """Maneja la comunicación individual con cada socket."""
    print(f"[+] Nueva conexión desde {addr}")
    try:
        while True:
            data = rec_msg(conn)
            if not data:
                break

            action = data.get("action")
            response = {"status": "error", "message": "Acción desconocida"}

            # --- Dispatcher de Acciones ---
            if action == "REGISTER_WORKER":
                manager.register_worker(addr[0], data["count"])
                response = {"status": "success"}

            elif action == "GET_WRITE_NODE":
                target_ip = manager.get_target_node()
                response = {"status": "success", "ip": target_ip}

            elif action == "CONFIRM_TOUCH":
                target_ip = data.get("worker_ip")
                manager.add_file_record(data["filename"], target_ip)
                response = {"status": "success"}

            elif action == "LIST_FILES":
                files = manager.get_all_files()
                response = {"status": "success", "files": files}

            elif action == "GET_STATUS":
                # manager.nodes ya contiene la IP, el conteo y el estado
                response = {
                    "status": "success",
                    "nodes": manager.nodes,
                    "total_archivos": len(manager.metadata),
                }

            send_msg(conn, response)
    except Exception as e:
        print(f"[!] Error con {addr}: {e}")
    finally:
        conn.close()


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
