import cmd
import socket
import sys

from colorama import Fore, Style, init
from common import rec_msg, send_msg

# Inicializar colores
init(autoreset=True)

class DistrubutedShell(cmd.Cmd):
    intro = Fore.CYAN + "Bienvenido a la Shell del Sistema Distribuido. Escribe help para ver comandos.\n"
    prompt = f"{Fore.GREEN}nixos@dist-fs{Style.RESET_ALL}:{Fore.BLUE}~{Style.RESET_ALL}$ "

    def __init__(self, master_ip, master_port=5000):
        super().__init__()
        self.master_addr = (master_ip, master_port)
        self.worker_port = 5001 # Puerto donde escuchan los workers

    def _connect_master(self):
        """Helper para conectar al maestro rápidamente."""
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(self.master_addr)
        return sock

    def do_ls(self, arg):
        """Muestra la lista de archivos registrados en el sistema global."""
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

    def do_touch(self, filename):
        """Crea un archivo vacío siguiendo la regla del umbral."""
        if not filename:
            print("Uso: touch <nombre_archivo>")
            return

        try:
            # 1. Preguntar al Maestro dónde guardar
            with self._connect_master() as master:
                send_msg(master, {"action": "GET_WRITE_NODE", "filename": filename})
                res = rec_msg(master)

                if res["status"] != "success":
                    print(f"{Fore.RED}El Maestro no asignó un nodo.")
                    return

                target_worker_ip = res["ip"]

            # 2. Conectar al Worker designado y crear el archivo
            print(f"[*] Maestro asignó el nodo: {target_worker_ip}. Creando...")
            worker_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            worker_sock.connect((target_worker_ip, self.worker_port))
            send_msg(worker_sock, {"action": "TOUCH", "filename": filename})
            worker_res = rec_msg(worker_sock)
            worker_sock.close()

            # 3. Confirmar al Maestro que el archivo existe físicamente
            if worker_res and worker_res["status"] == "success":
                with self._connect_master() as master:
                    send_msg(master, {
                        "action": "CONFIRM_TOUCH",
                        "filename": filename,
                        "worker_ip": target_worker_ip
                    })
                print(f"{Fore.GREEN}Archivo '{filename}' creado exitosamente.")

        except Exception as e:
            print(f"{Fore.RED}Error durante la operación touch: {e}")

    def do_exit(self, arg):
        """Cierra la shell."""
        print("Saliendo...")
        return True

    def do_status(self, arg):
        """Muestra el estado de carga (IP y conteo) de cada nodo worker."""
        try:
            with self._connect_master() as master:
                send_msg(master, {"action": "GET_STATUS"})
                res = rec_msg(master)

                if res["status"] == "success":
                    nodes = res.get("nodes", {})
                    total = res.get("total_archivos", 0)

                    print(f"\n{Fore.CYAN}ESTADO DEL SISTEMA DISTRIBUIDO")
                    print(f"Total de archivos registrados: {total}")
                    print(f"{'IP del Worker':<15} | {'Archivos':<10} | {'Estado'}")
                    print("-" * 45)

                    if not nodes:
                        print("No hay nodos registrados.")
                    else:
                        for ip, data in nodes.items():
                            count = data['file_count']
                            # Si llega al umbral (20), lo ponemos en rojo
                            color = Fore.RED if count >= 20 else Fore.YELLOW
                            print(f"{ip:<15} | {color}{count:<10}{Style.RESET_ALL} | {data['status']}")
                    print()
        except Exception as e:
            print(f"{Fore.RED}Error al obtener estado: {e}")

    def do_EOF(self, arg):
        return True

if __name__ == "__main__":
    # Cambia esto por la IP de Tailscale de tu computadora Maestro
    IP_MAESTRO = "100.107.126.50"
    shell = DistrubutedShell(IP_MAESTRO)
    shell.cmdloop()
