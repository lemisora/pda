package com.pda.Rpc;

public class JsonRpcResponse {
    private String jsonrpc = "2.0";
    private Object result;
    private Object error;
    private Object id;

    public JsonRpcResponse(Object result, Object error, Object id) {
        this.result = result;
        this.error = error;
        this.id = id;
    }

    // Constructor de éxito
    public static JsonRpcResponse success(Object result, Object id) {
        return new JsonRpcResponse(result, null, id);
    }

    // Constructor de error
    public static JsonRpcResponse error(Object error, Object id) {
        return new JsonRpcResponse(null, error, id);
    }

    // Getters y Setters
    public String getJsonrpc() {
        return jsonrpc;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }
}
