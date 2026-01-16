import cmd
import socket

from colorama import Fore, Style, init
from common import rec_msg, send_msg

# Inicializar colores
init(autoreset=True)

# Clase contenedora de las configuraciones de la shell
class DistrubutedShell(cmd.Cmd):
    intro = (
        Fore.CYAN
        + "Bienvenido a la Shell del Sistema Distribuido. Escribe help para ver comandos.\n"
    )
    prompt = f"{Fore.GREEN}user@host{Style.RESET_ALL}:{Fore.BLUE}~{Style.RESET_ALL}$ "

    def __init__(self, master_ip, master_port=5000):
        super().__init__()
        self.master_addr = (master_ip, master_port)
        self.worker_port = 5001  # Puerto donde escuchan los workers
        # Obtenemos el umbral del maestro al iniciar
        self.threshold = self._fetch_threshold()
        if self.threshold is None:
            print(f"{Fore.RED}[CONFIG] No se pudo obtener el umbral del maestro.")
        print(f"{Fore.YELLOW}[CONFIG] Umbral detectado: {self.threshold}")

    def _fetch_threshold(self):
        try:
            with self._connect_master() as master:
                send_msg(master, {"action": "GET_CONFIG"})
                res = rec_msg(master)
                return res.get(
                    "threshold"
                )
        except Exception:
            return None

    # Función para conectar al servidor a 'master'
    def _connect_master(self):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(self.master_addr)
        return sock

    # Muestra los archivos en el almacenamiento distribuido
    def do_ls(self, arg):
        try:
            with self._connect_master() as master:
                send_msg(master, {"action": "LIST_FILES"})
                res = rec_msg(master)
                if res["status"] == "success":
                    files = res.get("files", [])
                    if not files:
                        print("Directorio vacío.")
                    else:
                        print(f"{Fore.YELLOW}" + "  ".join(files))
        except Exception as e:
            print(f"{Fore.RED}Error al conectar con el maestro: {e}")

    # Crea un archivo con el nombre dado como argumento
    def do_touch(self, filename):
        if not filename:
            print("Uso: touch <nombre_archivo>")
            return

        try:
            # Preguntar al Maestro dónde guardar
            with self._connect_master() as master:
                send_msg(
                    master, {"action": "GET_WRITE_NODE", "filename": filename}
                )
                res = rec_msg(master)

                if res["status"] != "success":
                    print(f"{Fore.RED}El Maestro no asignó un nodo.")
                    return

                target_worker_ip = res["ip"]

            # Conectar al Worker designado y crear el archivo
            print(f"[*] Maestro asignó el nodo: {target_worker_ip}. Creando...")
            worker_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            worker_sock.connect((target_worker_ip, self.worker_port))
            send_msg(worker_sock, {"action": "TOUCH", "filename": filename})
            worker_res = rec_msg(worker_sock)
            worker_sock.close()

            # Confirmar escritura y actualizar contador correspondiente al worker escrito
            if worker_res and worker_res["status"] == "success":
                with self._connect_master() as master:
                    send_msg(
                        master,
                        {
                            "action": "CONFIRM_TOUCH",
                            "filename": filename,
                            "worker_ip": target_worker_ip,
                        },
                    )

        except Exception as e:
            print(f"{Fore.RED}Error durante la operación touch: {e}")

    # Función para salir de la shell
    def do_exit(self, arg):
        print("Saliendo...")
        return True

    # Ejecuta el comando para ver el estado de cada nodo
    def do_status(self, arg):
        try:
            with self._connect_master() as master:
                send_msg(master, {"action": "GET_STATUS"})
                res = rec_msg(master)

                if res["status"] == "success":
                    nodes = res.get("nodes", {})
                    total = res.get("total_archivos", 0)

                    print(f"\n{Fore.CYAN}ESTADO DEL SISTEMA DISTRIBUIDO")
                    print(f"Total de archivos registrados: {total}")
                    print(
                        f"{'IP del Worker':<15} | {'Archivos':<10} | {'Estado'}"
                    )
                    print("-" * 45)

                    if not nodes:
                        print("No hay nodos registrados.")
                    else:
                        for ip, data in nodes.items():
                            count = data["file_count"]
                            # Si llega al umbral se reporta en color rojo
                            color = (
                                Fore.RED
                                if count >= self.threshold
                                else Fore.YELLOW
                            )
                            print(
                                f"{ip:<15} | {color}{count:<10}{Style.RESET_ALL} | {data['status']}"
                            )
                    print()
        except Exception as e:
            print(f"{Fore.RED}Error al obtener estado: {e}")


if __name__ == "__main__":
    # Cambia esto por la IP de Tailscale de tu computadora Maestro
    IP_MAESTRO = "100.107.126.50"
    shell = DistrubutedShell(IP_MAESTRO)
    shell.cmdloop()
