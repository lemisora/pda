project('xmlrpc-demo', 'c')

# Dependencias separadas para cliente y servidor
# xmlrpc_client: Para hacer peticiones
# xmlrpc_server_abyss: Incluye el servidor HTTP "Abyss" integrado
client_dep = dependency('xmlrpc_client', required : true)
server_dep = dependency('xmlrpc_server_abyss', required : true)
util_dep   = dependency('xmlrpc_util', required : true)

executable('server', 'server.c', dependencies : [server_dep, util_dep])
executable('client', 'client.c', dependencies : [client_dep, util_dep])