/**
 * Copyright (C) Grzegorz Skorupa 2021.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.in;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;
import com.signomix.events.NewDataEvent;
import com.signomix.iot.IotData;
import com.signomix.iot.generic.IotData2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.cricketmsf.RequestObject;
import org.cricketmsf.api.ResponseCode;
import org.cricketmsf.event.ProcedureCall;
import org.cricketmsf.event.Procedures;
import org.cricketmsf.in.http.HttpPortedAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IotApi extends HttpPortedAdapter {

    private static final Logger logger = LoggerFactory.getLogger(IotApi.class);
    private boolean authorizationRequired = false;
    private boolean ignoreServiceResponseCode = false;

    public IotApi() {
        super();
    }

    @Override
    public void loadProperties(HashMap<String, String> properties, String adapterName) {
        super.loadProperties(properties, adapterName);
        setContext(properties.get("context"));
        authorizationRequired = Boolean.parseBoolean(properties.get("authorization-required"));
        setProperty("authorization-required", "" + authorizationRequired);
        ignoreServiceResponseCode = Boolean.parseBoolean(properties.get("overwrite-resp-code"));
        setProperty("overwrite-resp-code", "" + ignoreServiceResponseCode);
    }

    /**
     * Transforming request data to Event type required by the service adapter.
     *
     * @param request
     * @param rootEventId
     * @return Wrapper object for the event and the port procedure name.
     */
    protected ProcedureCall preprocess(RequestObject request) {
        // validation and translation 
        String method = request.method;
        if ("POST".equalsIgnoreCase(method)) {
            return preprocessPost(request);
        } else if ("OPTIONS".equalsIgnoreCase(method)) {
            return ProcedureCall.toRespond(200, "OK");
        } else {
            return ProcedureCall.toRespond(ResponseCode.METHOD_NOT_ALLOWED, "error");
        }
    }

    private ProcedureCall preprocessPost(RequestObject request) {
        String errorMessage = "";
        ProcedureCall result = null;
        IotData2 iotData = null;
        String dataString = request.body;
        String jsonString = null;
        boolean authProblem = false;

        String authKey = request.headers.getFirst("Authorization");
        if (authorizationRequired && (null == authKey || authKey.isBlank())) {
            errorMessage = "no authorization header";
            authProblem = true;
        } else {
            if (dataString == null) {
                dataString = ((String) request.parameters.getOrDefault("data", "")).trim();
            }
            if (dataString.isEmpty()) {
                iotData = parseIotData(request.parameters);
                dataString = buildParamString(request.parameters);
            } else {
                StringBuilder sb = new StringBuilder(dataString.trim());
                sb.insert(1, "{\"@type\":\"com.signomix.iot.IotData2\",");
                jsonString = sb.toString();
                try {
                    iotData = (IotData2) JsonReader.jsonToJava(jsonString);
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    logger.warn("deserialization problem from {} {}", request.clientIp, jsonString);
                }
            }
        }

        if (errorMessage.isEmpty()) {
            iotData.prepareIotValues();
            NewDataEvent ev = new NewDataEvent().data(new IotData(iotData)
                    .authRequired(authorizationRequired)
                    .authKey(request.headers.getFirst("Authorization"))
                    .serializedData(null != jsonString ? jsonString : dataString));
            result = ProcedureCall.toForward(ev, Procedures.DEFAULT, ignoreServiceResponseCode ? 200 : 0);
        } else {
            if (authProblem) {
                if (ignoreServiceResponseCode) {
                    result = ProcedureCall.toRespond(200, errorMessage);
                } else {
                    result = ProcedureCall.toRespond(401, errorMessage);
                }
            } else {
                if (ignoreServiceResponseCode) {
                    result = ProcedureCall.toRespond(200, errorMessage);
                } else {
                    result = ProcedureCall.toRespond(400, errorMessage);
                }
            }
        }
        return result;
    }

    private IotData2 parseIotData(Map<String, Object> parameters) {
        IotData2 data = new IotData2();
        data.dev_eui = null;
        data.timestamp = "" + System.currentTimeMillis();
        data.payload_fields = new ArrayList<>();
        HashMap<String, String> map;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            if ("eui".equalsIgnoreCase(key)) {
                data.dev_eui = value;
                System.out.println("dev_eui:" + data.dev_eui);
            } else if ("timestamp".equalsIgnoreCase(key)) {
                data.timestamp = value;
            } else if ("authkey".equalsIgnoreCase(key)) {
                data.authKey = value;
            } else if ("clienttitle".equalsIgnoreCase(key)) {
                data.clientname = value;
            } else {
                map = new HashMap<>();
                map.put("name", key);
                map.put("value", value);
                data.payload_fields.add(map);
                System.out.println(key + ":" + value);
            }
            System.out.println("timestamp:" + data.timestamp);
        }
        if (null == data.dev_eui || data.payload_fields.isEmpty()) {
            System.out.println("ERROR: " + data.dev_eui + "," + data.payload_fields);
            return null;
        }
        data.normalize();
        return data;
    }

    private String buildParamString(Map<String, Object> parameters) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            result.append(entry.getKey()).append("=").append((String) entry.getValue()).append("\r\n");
        }
        return result.toString();
    }

    private IotData2 processObject(JsonObject o) {
        IotData2 data = new IotData2();
        return data;
    }

}
