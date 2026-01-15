import json


# Función mediante la que se enviarán los mensajes
def send_msg(sock, data):
    # Convertir diccionario a cadena JSON y a bytes
    msg = json.dumps(data).encode('utf-8')
    sock.sendall(msg)
    
# Función para recibir los mensajes
def rec_msg(sock):
    # Recibir el mensaje en bloques de 4 kB
    raw_msg = sock.recv(4096)
    if not raw_msg:
        return None
    return json.loads(raw_msg.decode('utf-8'))