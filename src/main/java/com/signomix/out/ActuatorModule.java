/*
* Copyright (C) Grzegorz Skorupa 2018.
* Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.out;

import com.signomix.events.ActuatorEvent;
import com.signomix.in.ActuatorApi;
import com.signomix.out.db.ActuatorCommandsDBIface;
import com.signomix.out.iot.ChannelData;
import org.cricketmsf.api.ResponseCode;
import org.cricketmsf.event.Event;
import org.cricketmsf.RequestObject;
import org.cricketmsf.api.StandardResult;
import com.signomix.out.iot.Device;
import com.signomix.out.iot.ThingsDataException;
import com.signomix.out.iot.ThingsDataIface;
import com.signomix.out.notification.CommandWebHookIface;
import com.signomix.out.script.ScriptingAdapterIface;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import org.cricketmsf.Kernel;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public class ActuatorModule {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ActuatorModule.class);
    private static ActuatorModule logic;

    public static ActuatorModule getInstance() {
        if (logic == null) {
            logic = new ActuatorModule();
        }
        return logic;
    }

    public Object processRequest(
            ActuatorEvent event,
            ActuatorApi actuatorApi,
            ThingsDataIface thingsAdapter,
            ActuatorCommandsDBIface actuatorCommandsDB,
            ScriptingAdapterIface scriptingAdapter
    ) {
        StandardResult result=new StandardResult();
        RequestObject request=(RequestObject)event.getData().get("request");
        Device device = (Device)event.getData().get("device");
        long userID =(Long)event.getData().get("userID");
        boolean hexPayload=(Boolean)event.getData().get("hexPayload");
        switch (request.method.toUpperCase()) {
            case "GET":
                result = processGet(device.getEUI(), actuatorCommandsDB);
                break;
            case "POST":
                result = processPost(device, request, hexPayload, actuatorCommandsDB, thingsAdapter, scriptingAdapter);
                break;
            default:
                result.setCode(ResponseCode.METHOD_NOT_ALLOWED);
        }
        return result;
    }

    private StandardResult processGet(String deviceEUI, ActuatorCommandsDBIface actuatorCommandsDB) {
        StandardResult result = new StandardResult();
        try {
            result.setData(actuatorCommandsDB.getFirstCommand(deviceEUI));
        } catch (ThingsDataException ex) {
            logger.error(ex.getMessage());
        }
        return result;
    }

    private StandardResult processPost(
            Device device,
            RequestObject request,
            boolean hexPayload,
            ActuatorCommandsDBIface actuatorCommandsDB,
            ThingsDataIface thingsAdapter,
            ScriptingAdapterIface scriptingAdapter) {
        StandardResult result = new StandardResult();

        ActuatorEvent event = new ActuatorEvent();
        HashMap<String,Object> data=new HashMap<>();
        data.put("origin","@" + device.getEUI());
        data.put("hexPayload",hexPayload);
        /*
        leading "#" - overwrite previous command if still not send, 
        leading "&" - send command after previously registered
         */
        data.put("payload", "#" + request.body.trim());
        event.data(data);
        Kernel.getInstance().dispatchEvent(event);
        return result;
    }

    /*
    public void processCommand(
            Event event,
            boolean hexagonalRepresentation,
            ActuatorCommandsDBIface actuatorCommandsDB,
            ThingsDataIface thingsAdapter,
            ScriptingAdapterIface scriptingAdapter
    ) {
        String[] devices = event.getOrigin().split("@");
        String sourceEUI = devices[0];
        String deviceEUI = null;
        if (devices.length > 1) {
            deviceEUI = devices[1];
        }
        String payload = (String) event.getPayload();
        boolean done = false;
        Device sourceDevice = null;
        Device device = null;
        try {
            if (!sourceEUI.isEmpty()) {
                sourceDevice = thingsAdapter.getDevice(sourceEUI);
            }
            device = thingsAdapter.getDevice(deviceEUI);
            if (device == null) {
                logger.warn("device " + deviceEUI + " not found");
                return;
            }
            if (device.getType().equals(Device.VIRTUAL)) {
                if (null != sourceDevice && !sourceDevice.getType().equals(Device.VIRTUAL)) {
                    done = sendToVirtual(device, payload.substring(1), thingsAdapter, scriptingAdapter);
                } else if (null == sourceDevice && event.getType().equals(IotEvent.PLATFORM_MONITORING)) {
                    done = sendToVirtual(device, payload, thingsAdapter, scriptingAdapter);
                } else {
                    logger.warn("blocked command from virtual to virtual device");
                    done = true;
                }

            } else if (device.getType().equals(Device.TTN)) {
                done = sendToTtn(device, payload.substring(1), hexagonalRepresentation);
            } else if (device.getType().equals(Device.LORA)) {
                //TODO: not implemented
                done = true;
            } else if (device.getType().equals(Device.KPN)) {
                //TODO: not implemented
                done = true;
            } else if (device.getType().equals(Device.GENERIC) || device.getType().equals(Device.GATEWAY)) {
                // Nothing to do. Command will be included in the response for the next device data transfer.
                done = false;
            } else if (device.getType().equals(Device.EXTERNAL)) {
                done = sendToWebhook(device, payload.substring(1), hexagonalRepresentation);
            }
            if (done) {
                actuatorCommandsDB.putCommandLog(event.getOrigin(), event);
            } else {
                actuatorCommandsDB.putDeviceCommand(event.getOrigin(), event);
            }
            logger.info("processCommand " + event.getOrigin() + ":" + event.getPayload());
        } catch (ThingsDataException e) {
            logger.warn(e.getMessage());
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }

    }
    */

    /**
     * Redirect command to virtual device.
     *
     * @param device
     * @param command
     * @param thingsAdapter
     * @param scriptingAdapter
     * @return
     */
    private boolean sendToVirtual(Device device, String command, ThingsDataIface thingsAdapter, ScriptingAdapterIface scriptingAdapter) {
        try {
            String cmd = new String(Base64.getDecoder().decode(command));
            long now = System.currentTimeMillis();
            thingsAdapter.updateHealthStatus(device.getEUI(), now, 0/*new frame count*/, "", "");
            ArrayList<ArrayList> outputList;
            try {
                Object[] processingResult = DataProcessor.processValues(new ArrayList(), device, scriptingAdapter,
                        now, device.getLatitude(), device.getLongitude(), device.getAltitude(), "", cmd);
                outputList = (ArrayList<ArrayList>) processingResult[0];
                for (int i = 0; i < outputList.size(); i++) {
                    thingsAdapter.putData(device.getUserID(), device.getEUI(), device.getProject(), device.getState(), fixValues(device, outputList.get(i)));
                }
                if (device.getState().compareTo((Double) processingResult[1]) != 0) {
                    thingsAdapter.updateDeviceState(device.getEUI(), (Double) processingResult[1]);
                }
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //end
        return true;
    }

    /*
    private boolean sendToTtn(Device device, String payload, boolean hexRepresentation) {
        return sendToTtn(device, payload, hexRepresentation, null);
    }

    private boolean sendToTtn(Device device, String payload, boolean hexRepresentation, String forceUrl) {
        //payload to jest String będący tablicą bajtów zapisanych w formacie hex
        //odpowiednie encodowanie jest w TtnDownlinkMessage
        String deviceID = device.getDeviceID();
        String downlink;
        if (null == forceUrl) {
            downlink = device.getDownlink();
        } else {
            downlink = forceUrl;
        }
        HashMap<String, Object> args = new HashMap<>();
        args.put(JsonWriter.TYPE, false);
        args.put(JsonWriter.SKIP_NULL_FIELDS, true);
        TtnDownlinkMessage message;
        if (hexRepresentation) {
            message = new TtnDownlinkMessage(deviceID, payload, false, 1);
        } else {
            logger.error(".sendToTtn()", "plain text commands not implemented");
            return false;
        }
        String requestBody = JsonWriter.objectToJson(message, args);
        HttpClient client = new HttpClient();
        Request request = new Request()
                .setUrl(downlink)
                .setMethod("POST").setProperty("Content-Type", "application/json")
                .setData(requestBody);
        Result result;
        try {
            result = client.send(request);
        } catch (AdapterException ex) {
            return false;
        }
        return 200 == result.getCode();
    }
*/
    private boolean sendToWebhook(Device device, String payload, boolean hexRepresentation) {
        CommandWebHookIface webhookSender = (CommandWebHookIface) Kernel.getInstance().getAdaptersMap().get("CommandWebHook");
        if (null == webhookSender) {
            logger.warn("CommandWebHook adaper not configured");
            return false;
        } else {
            return webhookSender.send(device, payload, hexRepresentation);
        }
    }

    public ActuatorEvent getCommand(String deviceEUI, ActuatorCommandsDBIface actuatorCommandsDB) {
        ActuatorEvent result = null;
        if (deviceEUI != null) {
            try {
                result = (ActuatorEvent) actuatorCommandsDB.getFirstCommand(deviceEUI);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
        }
        return result;
    }

    public String archiveCommand(ActuatorEvent command, ActuatorCommandsDBIface actuatorCommandsDB) {
        String result = "";
        if (command != null) {
            long id=(Long)command.getData().get("id");
            String origin=(String)command.getData().get("origin");
            try {
                actuatorCommandsDB.removeCommand(id);
                actuatorCommandsDB.putCommandLog(origin, command);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
        }
        return result;
    }

    ArrayList<ChannelData> fixValues(Device device, ArrayList<ChannelData> values) {
        ArrayList<ChannelData> fixedList = new ArrayList<>();
        if (values != null && values.size() > 0) {
            for (ChannelData value : values) {
                if (device.getChannels().containsKey(value.getName())) {
                    fixedList.add(value);
                }
            }
        }
        return fixedList;
    }

}
