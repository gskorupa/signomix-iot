package com.signomix;

import com.signomix.events.ActuatorEvent;
import com.signomix.events.NewDataEvent;
import com.signomix.iot.IotData;
import com.signomix.out.ActuatorModule;
import com.signomix.out.DeviceIntegrationModule;
import org.cricketmsf.annotation.EventHook;
import org.cricketmsf.api.StandardResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author greg
 */
public class IntegrationServiceRouter {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationServiceRouter.class);
    
    private IntegrationService service;
    
    public IntegrationServiceRouter(IntegrationService service){
        this.service=service;
    }

    @EventHook(className = "com.signomix.events.NewDataEvent")
    public Object handleNewData(NewDataEvent event) {
        IotData data = event.getData();
        StandardResult result = new StandardResult();
        try {
            return DeviceIntegrationModule.getInstance().processGenericRequest(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @EventHook(className = "com.signomix.events.ActuatorEvent")
    public Object actuatorHandle(ActuatorEvent event) {
        return ActuatorModule.getInstance().processRequest(
                event, 
                service.getActuatorApi(), 
                service.getThingsAdapter(), 
                service.getActuatorCommandsDB(), 
                service.getScriptingAdapter()
        );
    }



}
