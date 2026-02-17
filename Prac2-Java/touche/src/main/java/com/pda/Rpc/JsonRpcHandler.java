package com.pda.Rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class JsonRpcHandler {
    // Mapa de nombre_metodo -> funcion
    private Map<String, Function<JsonRpcRequest, Object>> methods = new HashMap<>();

    public void registerMethod(String name, Function<JsonRpcRequest, Object> method) {
        methods.put(name, method);
    }

    public String handleRequest(String jsonRequest) {
        try {
            JsonRpcRequest request = JsonRpcUtils.fromJson(jsonRequest, JsonRpcRequest.class);

            if (request == null || request.getMethod() == null) {
                return JsonRpcUtils.toJson(JsonRpcResponse.error("Invalid Request", null));
            }

            Function<JsonRpcRequest, Object> method = methods.get(request.getMethod());
            if (method == null) {
                return JsonRpcUtils
                        .toJson(JsonRpcResponse.error("Method not found: " + request.getMethod(), request.getId()));
            }

            try {
                Object result = method.apply(request);
                return JsonRpcUtils.toJson(JsonRpcResponse.success(result, request.getId()));
            } catch (Exception e) {
                return JsonRpcUtils.toJson(JsonRpcResponse.error("Internal Error: " + e.getMessage(), request.getId()));
            }

        } catch (Exception e) {
            return JsonRpcUtils.toJson(JsonRpcResponse.error("Parse Error", null));
        }
    }
}
