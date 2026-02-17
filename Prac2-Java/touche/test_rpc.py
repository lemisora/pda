import socket
import json
import time

try:
    print("Connecting to localhost:8988...")
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(5)
    s.connect(('localhost', 8988))
    
    msg = json.dumps({"jsonrpc": "2.0", "method": "info", "id": 1}) + "\n"
    print(f"Sending: {msg.strip()}")
    s.send(msg.encode())
    
    response = s.recv(4096).decode()
    print("Response:", response)
    s.close()
except Exception as e:
    print("Error:", e)
