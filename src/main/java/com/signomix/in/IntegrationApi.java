/**
 * Copyright (C) Grzegorz Skorupa 2018.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.in;

import org.cricketmsf.Adapter;
import java.util.HashMap;
import java.util.Map;
import org.cricketmsf.Kernel;
import org.cricketmsf.RequestObject;
import org.cricketmsf.event.ProcedureCall;
import org.cricketmsf.in.http.HttpAdapterIface;
import org.cricketmsf.in.http.HttpPortedAdapter;
import org.cricketmsf.in.openapi.Operation;
import org.cricketmsf.out.dispatcher.DispatcherIface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public class IntegrationApi extends HttpPortedAdapter implements HttpAdapterIface, Adapter {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationApi.class);
    private boolean dumpRequest = false;

    /**
     * This method is executed while adapter is instantiated during the service
     * start. It's used to configure the adapter according to the configuration.
     *
     * @param properties map of properties readed from the configuration file
     * @param adapterName name of the adapter set in the configuration file (can
     * be different from the interface and class name.
     */
    @Override
    public void loadProperties(HashMap<String, String> properties, String adapterName) {
        super.loadProperties(properties, adapterName);
        setContext(properties.get("context"));
        logger.info("\tcontext: {}", getContext());
        setExtendedResponse(properties.getOrDefault("extended-response", "false"));
        logger.info("\textended-response: ", isExtendedResponse());
        setDateFormat(properties.get("date-format"));
        logger.info("\tdate-format: ", getDateFormat());
        dumpRequest = "true".equalsIgnoreCase(properties.getOrDefault("dump-request", "false"));
        logger.info("\tdump-request: ", dumpRequest);
        properties.put("dump-request", "" + dumpRequest);
    }

    @Override
    protected ProcedureCall preprocess(RequestObject request) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
