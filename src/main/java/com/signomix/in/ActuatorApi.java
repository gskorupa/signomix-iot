/**
 * Copyright (C) Grzegorz Skorupa 2018.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.in;

import com.signomix.IntegrationService;
import com.signomix.out.iot.Device;
import com.signomix.out.iot.ThingsDataException;
import com.signomix.out.iot.ThingsDataIface;
import org.cricketmsf.Adapter;
import java.util.HashMap;
import org.cricketmsf.Kernel;
import org.cricketmsf.RequestObject;
import org.cricketmsf.api.ResponseCode;
import org.cricketmsf.api.StandardResult;
import org.cricketmsf.event.ProcedureCall;
import org.cricketmsf.event.Procedures;
import org.cricketmsf.in.http.HttpPortedAdapter;
import org.cricketmsf.in.http.HttpAdapterIface;
import org.cricketmsf.livingdoc.architecture.HexagonalAdapter;
import org.cricketmsf.microsite.event.UserEvent;
import org.slf4j.LoggerFactory;

/**
 * REST API for managing actuator devices
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
@HexagonalAdapter
public class ActuatorApi extends HttpPortedAdapter implements HttpAdapterIface, Adapter {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ActuatorApi.class);
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
        //super.getServiceHooks(adapterName);
        setContext(properties.get("context"));
        logger.info("\tcontext: {}", getContext());
        setExtendedResponse(properties.getOrDefault("extended-response", "false"));
        logger.info("\textended-response: {}", isExtendedResponse());
        setDateFormat(properties.get("date-format"));
        logger.info("\tdate-format: {}",getDateFormat());
        dumpRequest = "true".equalsIgnoreCase(properties.getOrDefault("dump-request", "false"));
        logger.info("\tdump-request: {}",dumpRequest);
        properties.put("dump-request", "" + dumpRequest);
    }

    @Override
    protected ProcedureCall preprocess(RequestObject request) {
        StandardResult result = new StandardResult();
        boolean debugMode = "true".equalsIgnoreCase(request.headers.getFirst("X-debug"));
        String userID = request.headers.getFirst("X-user-id");
        Device device;
        boolean hexPayload = false;
        String eui = request.pathExt;
        if (null != eui) {
            if (eui.endsWith("/hex")) {
                eui = eui.substring(0, eui.length() - 4);
                hexPayload = true;
            }
            eui = eui.toUpperCase();
        }
        ThingsDataIface thingsAdapter=((IntegrationService)Kernel.getInstance()).getThingsAdapter();
        try {
            device = thingsAdapter.getDevice(userID, eui, false);
        } catch (ThingsDataException ex) {
            return ProcedureCall.toRespond(ResponseCode.BAD_REQUEST, ex.getMessage());
        }
        if (device == null) {
            return ProcedureCall.toRespond(ResponseCode.FORBIDDEN, "");
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
