package com.pda.Rpc;

import java.util.List;

public class JsonRpcRequest {
    private String jsonrpc = "2.0";
    private String method;
    private List<Object> params;
    private Object id;

    public JsonRpcRequest(String method, List<Object> params, Object id) {
        this.method = method;
        this.params = params;
        this.id = id;
    }

    // Getters y Setters
    public String getJsonrpc() {
        return jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }
}
