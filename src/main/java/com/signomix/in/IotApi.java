/**
 * Copyright (C) Grzegorz Skorupa 2021.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.in;

import java.util.HashMap;
import org.cricketmsf.RequestObject;
import org.cricketmsf.api.ResponseCode;
import org.cricketmsf.event.ProcedureCall;
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
        return null;
    }

}
