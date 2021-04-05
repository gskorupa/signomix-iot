package com.signomix;

import com.signomix.events.ActuatorEvent;
import com.signomix.events.NewDataEvent;
import com.signomix.events.NewNotification;
import com.signomix.events.NotificationType;
import com.signomix.iot.DeviceStatus;
import com.signomix.iot.IotData;
import com.signomix.out.ActuatorModule;
import com.signomix.out.DeviceIntegrationModule;
import com.signomix.out.iot.ThingsDataException;
import java.util.logging.Level;
import org.cricketmsf.annotation.EventHook;
import org.cricketmsf.api.StandardResult;
import org.cricketmsf.microsite.out.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO: Unable to find procedure@method 0@null for event class com.signomix.events.ScriptingProblem 
/**
 *
 * @author greg
 */
public class IntegrationServiceRouter {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationServiceRouter.class);

    private IntegrationService service;

    public IntegrationServiceRouter(IntegrationService service) {
        this.service = service;
    }

    @EventHook(className = "com.signomix.events.NewDataEvent")
    public Object handleNewData(NewDataEvent event) {
        IotData data = (IotData) event.getData();
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

    @EventHook(className = "com.signomix.events.NewNotification")
    public Object handleNewData(NewNotification event) {
        String origin[];
        String tmps = (String) event.getData().get("origin");
        origin = tmps.split("\t");
        if (origin.length < 2) {
            logger.warn("event origin not properly set: {}", event.getOrigin());
            return null;
        }
        String nodeName = origin[1];
        String eventType = (String) event.getData().get("type");
        if (NotificationType.DEVICE_LOST.equals(eventType)) {
            try {
                // this event can appear several times: for device owner + all team members
                // TODO: the event should be directed for "system" user and only this particulrar
                // event instance should modify alert status
                service.getThingsAdapter().updateAlertStatus(nodeName, DeviceStatus.FAILURE);
                // see also:
                // ThingsDataIface.updateHealthStatus()
                // DeviceManagementModule.checkStatus()
            } catch (ThingsDataException ex) {
                logger.error(ex.getMessage());
            }
        }
        return null;
    }

}
