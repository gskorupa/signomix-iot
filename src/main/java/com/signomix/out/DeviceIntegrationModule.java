/**
 * Copyright (C) Grzegorz Skorupa 2018.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.out;

import com.signomix.IntegrationService;
//import com.signomix.in.http.KpnApi;
import com.signomix.iot.IotData;
import com.signomix.iot.generic.IotData2;
import com.signomix.events.ActuatorEvent;
import com.signomix.events.ScriptingProblem;
//import com.signomix.iot.lora.LoRaData;
import org.cricketmsf.Kernel;
import com.signomix.out.iot.Device;
import com.signomix.out.iot.ThingsDataException;
import com.signomix.out.iot.ThingsDataIface;
//import com.signomix.iot.kpn.KPNData;
import com.signomix.out.db.ActuatorCommandsDBIface;
import com.signomix.out.iot.ChannelData;
import com.signomix.out.script.ScriptingAdapterIface;
import com.signomix.util.HexTool;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.attribute.HashAttributeSet;
import org.cricketmsf.api.ResponseCode;
import org.cricketmsf.api.StandardResult;
import org.cricketmsf.microsite.out.user.UserAdapterIface;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public class DeviceIntegrationModule {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DeviceIntegrationModule.class);

    private static DeviceIntegrationModule logic;

    public static DeviceIntegrationModule getInstance() {
        if (logic == null) {
            logic = new DeviceIntegrationModule();
        }
        return logic;
    }
    public Object processGenericRequest(IotData data) {
        ScriptingAdapterIface scriptingAdapter;
        scriptingAdapter=((IntegrationService)Kernel.getInstance()).getScriptingAdapter();
        ThingsDataIface thingsAdapter;
        thingsAdapter = ((IntegrationService)Kernel.getInstance()).getThingsAdapter();
        UserAdapterIface userAdapter;
        userAdapter=((IntegrationService)Kernel.getInstance()).getUserAdapter();
        ActuatorCommandsDBIface actuatorCommandsDB;
        actuatorCommandsDB=((IntegrationService)Kernel.getInstance()).getActuatorCommandsDB();
                
        IotData2 iotData = data.getIotData();
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.CREATED);
        result.setData("OK");
        boolean htmlClient = false;
        String clientAppTitle = data.getClientName();
        if (null != clientAppTitle && !clientAppTitle.isEmpty()) {
            result.setHeader("Content-type", "text/html");
            htmlClient = true;
        }
        Device device = getDeviceChecked(data, IotData.GENERIC, thingsAdapter);
        if (null == device) {
            //result.setData(authMessage);
            return result;
        }

        try {
            //after successful authorization
            thingsAdapter.updateHealthStatus(device.getEUI(), System.currentTimeMillis(), -1, "", "");
        } catch (ThingsDataException ex) {
            Logger.getLogger(DeviceIntegrationModule.class.getName()).log(Level.SEVERE, null, ex);
        }
        ArrayList<ChannelData> inputList = decodePayload(iotData, scriptingAdapter, clientAppTitle, clientAppTitle, clientAppTitle);
        ArrayList<ArrayList> outputList;
        String dataString = data.getSerializedData();
        try {
            Object[] processingResult
                    = DataProcessor.processValues(inputList, device, scriptingAdapter,
                            iotData.getReceivedPackageTimestamp(), iotData.getLatitude(),
                            iotData.getLongitude(), iotData.getAltitude(), dataString, "");
            outputList = (ArrayList<ArrayList>) processingResult[0];
            for (int i = 0; i < outputList.size(); i++) {
                thingsAdapter.putData(device.getUserID(), device.getEUI(), device.getProject(), device.getState(), fixValues(device, outputList.get(i)));
            }
            if (device.getState().compareTo((Double) processingResult[1]) != 0) {
                System.out.println("DEVICE STATE " + device.getState() + " " + (Double) processingResult[1]);
                thingsAdapter.updateDeviceState(device.getEUI(), (Double) processingResult[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("processGenericRequest()", e.getMessage());
            fireEvent(2, device, e.getMessage());
        }

        ActuatorEvent command = ActuatorModule.getInstance().getCommand(device.getEUI(), actuatorCommandsDB);
        if (null != command) {
            String commandPayload = (String) command.getData().get("payload");
            if ((Boolean)command.getData().get("hexCommand")) {
                String rawCmd = new String(Base64.getEncoder().encode(HexTool.hexStringToByteArray(commandPayload)));
                result.setPayload(rawCmd.getBytes());
                //TODO: odpowiedź jeśli dane z formularza
            } else {
                result.setPayload(commandPayload.getBytes());
                //TODO: odpowiedź jeśli dane z formularza
            }
            ActuatorModule.getInstance().archiveCommand(command, actuatorCommandsDB);
        }

        if (htmlClient) {
            result.setCode(ResponseCode.OK);
            result.setPayload(buildResultData(htmlClient, true, clientAppTitle, "Data saved.").getBytes());
        }

        return result;
    }

    private Device getDeviceChecked(IotData data, int expectedType, ThingsDataIface thingsAdapter) {
        if (data.isAuthRequired() && (data.getAuthKey() == null || data.getAuthKey().isEmpty())) {
            logger.warn("Authorization is required");
            return null;
        }
        Device device;
        Device gateway;
        try {
            device = thingsAdapter.getDevice(data.getDeviceEUI());
            gateway = thingsAdapter.getDevice(data.getGatewayEUI());
            if (null == device) {
                logger.warn("Device {} is not registered", data.getDeviceEUI());
                return null;
            }
        } catch (ThingsDataException ex) {
            logger.warn(ex.getMessage());
            return null;
        }
        if (data.isAuthRequired()) {
            String secret;
            if (gateway == null) {
                secret = device.getKey();
            } else {
                secret = gateway.getKey();
            }
            try {
                if (!data.getAuthKey().equals(secret)) {
                    logger.warn("Authorization key don't match for {}",device.getEUI());
                    return null;
                }
            } catch (Exception ex) { //catch (UserException ex) {
                logger.warn(ex.getMessage());
                return null;
            }
        }
        switch (expectedType) {
            case IotData.GENERIC:
                if (!device.getType().startsWith("GENERIC")) {
                    logger.warn("Device " + data.getDeviceEUI() + " type is not valid");
                    return null;
                }
                break;
            case IotData.CHIRPSTACK:
                if (!device.getType().startsWith("LORA")) {
                    logger.warn("Device " + data.getDeviceEUI() + " type is not valid");
                    return null;
                }
                break;
            case IotData.TTN:
                if (!device.getType().startsWith("TTN")) {
                    logger.warn("Device " + data.getDeviceEUI() + " type is not valid");
                    return null;
                }
                break;
            case IotData.KPN:
                if (!device.getType().startsWith("KPN")) {
                    logger.warn("Device " + data.getDeviceEUI() + " type is not valid");
                    return null;
                }
                break;
        }
        if (!device.isActive()) {
            //return "device is not active";
            return null;
        }
        return device;
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

    private ArrayList<ChannelData> decodePayload(IotData2 data, ScriptingAdapterIface scriptingAdapter, String encoderCode, String deviceID, String userID) {
        if (!data.getDataList().isEmpty()) {
            return data.getDataList();
        }
        ArrayList<ChannelData> values = new ArrayList<>();
        if (data.getPayloadFieldNames() == null || data.getPayloadFieldNames().length == 0) {
            if (null != data.getPayload()) {
                byte[] decodedPayload = Base64.getDecoder().decode(data.getPayload().getBytes());
                try {
                    values = scriptingAdapter.decodeData(decodedPayload, encoderCode, deviceID, data.getTimestamp(), userID);
                } catch (Exception e) {
                    logger.warn("prepareTtnValues for device " + deviceID, e.getMessage());
                    fireEvent(1, deviceID, userID, e.getMessage());
                    return null;
                }
            }
        }
        return values;
    }

    
    /*
    public Object processLoRaRequest(Event event, ThingsDataIface thingsAdapter, UserAdapterIface userAdapter, LoRaApi loraApi) {
        ScriptingAdapterIface scriptingAdapter;
        scriptingAdapter=((IntegrationService)Kernel.getInstance()).getScriptingAdapter();
        
        RequestObject request = event.getRequest();
        StandardResult result = new StandardResult();
        result.setCode(HttpAdapter.SC_CREATED);
        result.setData("OK");
        boolean debugMode = "true".equalsIgnoreCase(request.headers.getFirst("X-debug"));
        try {
            String authKey = request.headers.getFirst("Authorization");
            if (authKey == null || authKey.isEmpty()) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Authorization is required"));
                if (debugMode) {
                    result.setCode(HttpAdapter.SC_UNAUTHORIZED);
                    result.setData("Authorization header not found");
                }
                return result;
            }
            String jsonString = request.body;

            jsonString
                    = "{\"@type\":\"com.signomix.iot.lora.LoRaData\","
                    + jsonString.substring(jsonString.indexOf("{") + 1);
            LoRaData data = null;
            try {

                data = (LoRaData) JsonReader.jsonToJava(jsonString);
                data.normalize();
            } catch (Exception e) {
                Kernel.getInstance().dispatchEvent(Event.logSevere(this.getClass().getSimpleName(), "deserialization problem: incompatible format " + jsonString));
                e.printStackTrace();
            }
            if (data == null) {
                //TODO: send warning to the service admin about deserialization error
                result.setCode(HttpAdapter.SC_BAD_REQUEST);
                result.setData("deserialization problem");
                return result;
            }

            // save value and timestamp in device's channel witch name is the same as the field name
            boolean isRegistered = false;
            Device device;
            try {
                //device = thingsAdapter.getDeviceChecked(data.getUserId(), data.getDeviceId());
                device = thingsAdapter.getDevice(data.getDevEUI());
                isRegistered = (null != device);
                if (!isRegistered) {
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Device " + data.getDevEUI() + " is not registered"));
                    result.setCode(HttpAdapter.SC_NOT_FOUND);
                    result.setData("device not found");
                    return result;
                }
                if ("VIRTUAL".equals(device.getType()) || device.getType().startsWith("TTN")) {
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Device " + data.getDevEUI() + " type is not valid"));
                    result.setCode(HttpAdapter.SC_BAD_REQUEST);
                    result.setData("device type not valid");
                    return result;
                }

            } catch (ThingsDataException ex) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), ex.getMessage()));
                result.setCode(HttpAdapter.SC_NOT_FOUND);
                result.setData("device not found");
                return result;
            }

            //String secret = device.getKey();
            String secret = userAdapter.get(device.getUserID()).getConfirmString();
            String applicationSecret;
            boolean authorized = false;

            if (secret != null && !secret.isEmpty()) {
                authorized = authKey.equals(secret);
            }
            if (!authorized) {
                try {
                    User user = userAdapter.get(data.getApplicationID());
                    if (user != null) {
                        applicationSecret = user.getConfirmString();
                        authorized = authKey.equals(applicationSecret);
                    }
                } catch (UserException ex) {
                }
            }
            if (!authorized) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Data request from device " + device.getEUI() + " not authorized"));
                result.setCode(HttpAdapter.SC_FORBIDDEN);
                result.setData("not authorized");
                return result;
            }
            //after successful authorization
            if (!device.isActive()) {
                result.setCode(HttpAdapter.SC_UNAVAILABLE);
                result.setData("device is not active");
                return result;
            }

            //check frame counter
            if (device.isCheckFrames()) {
                if (device.getLastFrame() == data.getfCnt()) {
                    //drop request
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this, "duplicated frame " + data.getfCnt() + " for " + device.getEUI()));
                    result.setCode(HttpAdapter.SC_OK);
                    result.setData("OK");
                    return result;
                }
            }
            thingsAdapter.updateHealthStatus(device.getEUI(), System.currentTimeMillis(), data.getfCnt(), "", "");
            ArrayList<ChannelData> inputList = prepareLoRaValues(data, scriptingAdapter, device.getEncoderUnescaped(), device.getEUI(), device.getUserID());

            ArrayList<ArrayList> outputList;
            try {
                Object[] processingResult = DataProcessor.processValues(inputList, device, scriptingAdapter,
                        data.getReceivedPackageTimestamp(), data.getLatitude(),
                        data.getLongitude(), data.getAltitude(), "", "");
                outputList = (ArrayList<ArrayList>) processingResult[0];
                for (int i = 0; i < outputList.size(); i++) {
                    thingsAdapter.putData(device.getUserID(), device.getEUI(), device.getProject(), device.getState(), fixValues(device, outputList.get(i)));
                }
                if (device.getState().compareTo((Double) processingResult[1]) != 0) {
                    System.out.println("DEVICE STATE " + device.getState() + " " + (Double) processingResult[1]);
                    thingsAdapter.updateDeviceState(device.getEUI(), (Double) processingResult[1]);
                }
            } catch (Exception e) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName() + ".processLoraRequest()", e.getMessage()));
                fireEvent(2, device, e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public Object processTtnRequest(Event event, ThingsDataIface thingsAdapter, UserAdapterIface userAdapter, ScriptingAdapterIface scriptingAdapter, TtnApi ttnApi) {
        //TODO: Authorization
        RequestObject request = event.getRequest();
        boolean authorizationRequired = !("false".equalsIgnoreCase(ttnApi.getProperty("authorization-required")));
        StandardResult result = new StandardResult();
        result.setCode(HttpAdapter.SC_CREATED);
        result.setData("OK");
        String authKey = request.headers.getFirst("Authorization");

        if (authorizationRequired && (authKey == null || authKey.isEmpty())) {
            Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Authorization is required"));
            return result;
        }

        String jsonString = request.body;
        jsonString
                = "{\"@type\":\"com.signomix.iot.TtnData\","
                + jsonString.substring(jsonString.indexOf("{") + 1);
        TtnData data = null;
        try {

            data = (TtnData) JsonReader.jsonToJava(jsonString);
            data.normalize();
        } catch (Exception e) {
            Kernel.getInstance().dispatchEvent(Event.logSevere(this.getClass().getSimpleName(), "deserialization problem: incompatible format " + jsonString));
            e.printStackTrace();
            //TODO: send warning to the service admin about deserialization error
        }
        if (data == null) {
            //we don't send error code to TTN
            return result;
        }

        Device device;
        try {
            device = thingsAdapter.getDevice(data.getDeviceEUI());
            if (null == device) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Device " + data.getDeviceEUI() + " is not registered"));
                return result;
            }
        } catch (ThingsDataException ex) {
            Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), ex.getMessage()));
            return result;
        }
        if (authorizationRequired) {
            try {
                if (!authKey.equals(device.getKey())) {
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Authorization key don't match for " + device.getEUI()));
                    return result;
                }
            } catch (Exception ex) { //catch (UserException ex) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), ex.getMessage()));
                return result;
            }
        }

        if (!device.isActive()) {
            result.setData("device is not active");
            return result;
        }

        try {
            if (!device.getType().startsWith("TTN")) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Device " + data.getDeviceEUI() + " type is not valid"));
                return result;
            }
            if (device.isCheckFrames()) {
                if (device.getLastFrame() == data.getFrameCounter()) {
                    //drop request
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this, "duplicated frame " + data.getFrameCounter() + " for " + device.getEUI()));
                    result.setCode(HttpAdapter.SC_OK);
                    result.setData("OK");
                    return result;
                }
            }
            thingsAdapter.updateHealthStatus(device.getEUI(), System.currentTimeMillis(), data.getFrameCounter(), data.getDownlink(), data.getDeviceID());

        } catch (ThingsDataException ex) {
            Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), ex.getMessage()));
            return result;
        }
        try {
            ArrayList<ChannelData> inputList = prepareTtnValues(data, scriptingAdapter, device.getEncoderUnescaped(), device.getEUI(), device.getUserID());
            ArrayList<ArrayList> outputList;
            try {
                Object[] processingResult = DataProcessor.processValues(inputList,
                        device,
                        scriptingAdapter,
                        data.getReceivedPackageTimestamp(),
                        data.getLatitude(),
                        data.getLongitude(),
                        data.getAltitude(),
                        request.body, "");
                outputList = (ArrayList<ArrayList>) processingResult[0];
                for (int i = 0; i < outputList.size(); i++) {
                    thingsAdapter.putData(device.getUserID(), device.getEUI(), device.getProject(), device.getState(), fixValues(device, outputList.get(i)));
                }
                if (device.getState().compareTo((Double) processingResult[1]) != 0) {
                    System.out.println("DEVICE STATE " + device.getState() + " " + (Double) processingResult[1]);
                    thingsAdapter.updateDeviceState(device.getEUI(), (Double) processingResult[1]);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName() + ".processTtnRequest()", e.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public Object processChirpstackRequest(IotData data, ThingsDataIface thingsAdapter, UserAdapterIface userAdapter, ScriptingAdapterIface scriptingAdapter, TtnApi ttnApi) {
        Uplink uplink = data.getChirpstackData();
        StandardResult result = new StandardResult();
        result.setCode(HttpAdapter.SC_CREATED);
        result.setData("OK");

        Device device = getDeviceChecked(data, IotData.CHIRPSTACK, thingsAdapter);
        if (null == device) {
            //result.setData(authMessage);
            return result;
        }
        try {
            if (device.isCheckFrames()) {
                if (device.getLastFrame() == uplink.getfCnt()) {
                    //drop request
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this, "duplicated frame " + uplink.getfCnt() + " for " + device.getEUI()));
                    result.setCode(HttpAdapter.SC_OK);
                    result.setData("OK");
                    return result;
                }
            }
            String downlink = "";
            thingsAdapter.updateHealthStatus(device.getEUI(), System.currentTimeMillis(), uplink.getfCnt(), downlink, uplink.getDeviceID());
        } catch (ThingsDataException ex) {
            Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), ex.getMessage()));
            return result;
        }

        try {
            ArrayList<ChannelData> inputList = prepareChirpstackValues(uplink, scriptingAdapter, device.getEncoderUnescaped(), device.getEUI(), device.getUserID());
            ArrayList<ArrayList> outputList;
            try {
                Object[] processingResult = DataProcessor.processValues(
                        inputList,
                        device,
                        scriptingAdapter,
                        uplink.getReceivedPackageTimestamp(),
                        uplink.getLatitude(),
                        uplink.getLongitude(),
                        uplink.getAltitude(),
                        data.getSerializedData(),
                        "");
                outputList = (ArrayList<ArrayList>) processingResult[0];
                for (int i = 0; i < outputList.size(); i++) {
                    thingsAdapter.putData(device.getUserID(), device.getEUI(), device.getProject(), device.getState(), fixValues(device, outputList.get(i)));
                }
                if (device.getState().compareTo((Double) processingResult[1]) != 0) {
                    System.out.println("DEVICE STATE " + device.getState() + " " + (Double) processingResult[1]);
                    thingsAdapter.updateDeviceState(device.getEUI(), (Double) processingResult[1]);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName() + ".processTtnRequest()", e.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    public Object processKpnRequest(Event event, ThingsDataIface thingsAdapter, UserAdapterIface userAdapter, ScriptingAdapterIface scriptingAdapter, KpnApi kpnApi) {
        //TODO: Authorization
        RequestObject request = event.getRequest();
        StandardResult result = new StandardResult();
        result.setCode(HttpAdapter.SC_CREATED);
        result.setData("OK");
        boolean debugMode = "true".equalsIgnoreCase(request.headers.getFirst("X-debug"));
        try {
            String authKey = null;

            String jsonString = request.body;
            jsonString
                    = "{\"@type\":\"com.signomix.iot.kpn.KPNData\","
                    + jsonString.substring(jsonString.indexOf("{") + 1);
            KPNData data = null;
            try {

                data = (KPNData) JsonReader.jsonToJava(jsonString);
                data.normalize();
            } catch (Exception e) {
                Kernel.getInstance().dispatchEvent(Event.logSevere(this.getClass().getSimpleName(), "deserialization problem: incompatible format " + jsonString));
                e.printStackTrace();
            }
            if (data == null) {
                //TODO: send warning to the service admin about deserialization error
                result.setCode(HttpAdapter.SC_BAD_REQUEST);
                result.setData("deserialization problem");
                return result;
            }

            // save value and timestamp in device's channel witch name is the same as the field name
            boolean isRegistered = false;
            Device device;
            try {
                //device = thingsAdapter.getDeviceChecked(data.getUserId(), data.getDeviceId());
                device = thingsAdapter.getDevice(data.getDeviceEUI());
                isRegistered = (null != device);
                if (!isRegistered) {
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Device " + data.getDeviceEUI() + " is not registered"));
                    result.setCode(HttpAdapter.SC_NOT_FOUND);
                    result.setData("device not found");
                    return result;
                }
                if (!device.getType().equalsIgnoreCase("KPN")) {
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Device " + data.getDeviceEUI() + " type is not valid"));
                    result.setCode(HttpAdapter.SC_BAD_REQUEST);
                    result.setData("device type not valid");
                    return result;
                }

            } catch (ThingsDataException ex) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), ex.getMessage()));
                result.setCode(HttpAdapter.SC_NOT_FOUND);
                result.setData("device not found");
                return result;
            }

            //String secret = device.getKey();
            String secret = userAdapter.get(device.getUserID()).getConfirmString();
            String applicationSecret;
            boolean authorized = true;

            //TODO: authorization
            if (!authorized) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Data request from device " + device.getEUI() + " not authorized"));
                result.setCode(HttpAdapter.SC_FORBIDDEN);
                result.setData("not authorized");
                return result;
            }
            //after successful authorization

            if (!device.isActive()) {
                result.setCode(HttpAdapter.SC_UNAVAILABLE);
                result.setData("device is not active");
                return result;
            }

            //TODO: check frame counter
            thingsAdapter.updateHealthStatus(device.getEUI(), System.currentTimeMillis(), 0, "", "");
            ArrayList<ChannelData> inputList = prepareKpnValues(data, scriptingAdapter, device.getEncoderUnescaped(), device.getEUI(), device.getUserID());
            ArrayList<ArrayList> outputList;
            try {
                Object[] processingResult = DataProcessor.processValues(inputList, device, scriptingAdapter,
                        data.getReceivedPackageTimestamp(), data.getLatitude(),
                        data.getLongitude(), data.getAltitude(), request.body, "");
                outputList = (ArrayList<ArrayList>) processingResult[0];
                for (int i = 0; i < outputList.size(); i++) {
                    thingsAdapter.putData(device.getUserID(), device.getEUI(), device.getProject(), device.getState(), fixValues(device, outputList.get(i)));
                }
                if (device.getState().compareTo((Double) processingResult[1]) != 0) {
                    System.out.println("DEVICE STATE " + device.getState() + " " + (Double) processingResult[1]);
                    thingsAdapter.updateDeviceState(device.getEUI(), (Double) processingResult[1]);
                }
            } catch (Exception e) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName() + ".processKpnRequest()", e.getMessage()));
                fireEvent(2, device, e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public Object processRawRequest(Event event, ThingsDataIface thingsAdapter, UserAdapterIface userAdapter, ScriptingAdapterIface scriptingAdapter, IntegrationApi rawApi, ActuatorCommandsDBIface actuatorCommandsDB) {
        //TODO: Authorization
        RequestObject request = event.getRequest();
        //TODO: kpnApi

        StandardResult result = new StandardResult();
        result.setCode(HttpAdapter.SC_CREATED);
        result.setData("OK");
        boolean debugMode = "true".equalsIgnoreCase(request.headers.getFirst("X-debug"));
        try {
            String authKey = null;
            String deviceEUI;
            //TODO: header should be configurable
            deviceEUI = request.headers.getFirst(rawApi.getProperty("header-name"));

            if (deviceEUI == null) {
                //TODO: send warning to the service admin about deserialization error
                result.setCode(HttpAdapter.SC_BAD_REQUEST);
                result.setData("deserialization problem");
                return result;
            }
            deviceEUI = deviceEUI.toUpperCase();

            // save value and timestamp in device's channel witch name is the same as the field name
            boolean isRegistered = false;
            Device device;
            try {
                //device = thingsAdapter.getDeviceChecked(data.getUserId(), data.getDeviceId());
                device = thingsAdapter.getDevice(deviceEUI);
                isRegistered = (null != device);
                if (!isRegistered) {
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Device " + deviceEUI + " is not registered"));
                    result.setCode(HttpAdapter.SC_NOT_FOUND);
                    result.setData("device not found");
                    return result;
                }
                if (!device.getType().equalsIgnoreCase("GENERIC")) {
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Device " + deviceEUI + " type is not valid"));
                    result.setCode(HttpAdapter.SC_BAD_REQUEST);
                    result.setData("device type not valid");
                    return result;
                }

            } catch (ThingsDataException ex) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), ex.getMessage()));
                result.setCode(HttpAdapter.SC_NOT_FOUND);
                result.setData("device not found");
                return result;
            }

            //String secret = device.getKey();
            String secret = userAdapter.get(device.getUserID()).getConfirmString();
            String applicationSecret;
            boolean authorized = true;

            //TODO: authorization
            if (!authorized) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "Data request from device " + device.getEUI() + " not authorized"));
                result.setCode(HttpAdapter.SC_FORBIDDEN);
                result.setData("not authorized");
                return result;
            }
            //after successful authorization
            if (!device.isActive()) {
                result.setCode(HttpAdapter.SC_UNAVAILABLE);
                result.setData("device is not active");
                return result;
            }

            thingsAdapter.updateHealthStatus(device.getEUI(), System.currentTimeMillis(), 0, "", "");
            ArrayList<ChannelData> finalValues = null;
            try {
                finalValues = DataProcessor.processRawValues(request.body, device, scriptingAdapter, System.currentTimeMillis());
            } catch (Exception e) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName() + ".processRawRequest()", e.getMessage()));
                fireEvent(2, device, e.getMessage());
            }
            thingsAdapter.putData(device.getUserID(), device.getEUI(), device.getProject(), device.getState(), fixValues(device, finalValues));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private ArrayList<ChannelData> prepareTtnValues(TtnData data, ScriptingAdapterIface scriptingAdapter, String encoderCode, String deviceID, String userID) {
        ArrayList<ChannelData> values = new ArrayList<>();
        if (data.getPayloadFieldNames() == null || data.getPayloadFieldNames().length == 0) {
            if (null != data.getPayload()) {
                byte[] decodedPayload = Base64.getDecoder().decode(data.getPayload().getBytes());
                try {
                    values = scriptingAdapter.decodeData(decodedPayload, encoderCode, deviceID, data.getTimestamp(), userID);
                } catch (Exception e) {
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName() + ".prepareTtnValues for device " + deviceID, e.getMessage()));
                    fireEvent(1, deviceID, userID, e.getMessage());
                    return null;
                }
            }
        } else {
            TtnData processedData = new TtnData(data);
            // handling Cayenne LPP
            ArrayList<String> toExpand = new ArrayList<>();
            Iterator it = data.getPayloadFields().keySet().iterator();
            Object payloadField;
            String fieldName;
            while (it.hasNext()) {
                fieldName = (String) it.next();
                payloadField = data.getPayloadFields().get(fieldName);
                if (payloadField instanceof com.cedarsoftware.util.io.JsonObject) {
                    toExpand.add(fieldName);
                } else {
                    // nothing to do
                }
            }
            toExpand.forEach(name -> {
                com.cedarsoftware.util.io.JsonObject j = (com.cedarsoftware.util.io.JsonObject) data.getPayloadFields().get(name);
                Iterator it2 = j.keySet().iterator();
                String key;
                while (it2.hasNext()) {
                    key = (String) it2.next();
                    processedData.putField(name + "_" + key, j.get(key));
                }
            });
            toExpand.forEach(name -> {
                processedData.removeField(name);
            });
            // Cayenne LPP - end
            for (String payloadFieldName : processedData.getPayloadFieldNames()) {
                ChannelData mval = new ChannelData();
                mval.setDeviceEUI(processedData.getDeviceEUI());
                mval.setName(payloadFieldName.toLowerCase());
                mval.setValue(processedData.getDoubleValue(payloadFieldName));
                mval.setStringValue(processedData.getStringValue(payloadFieldName));
                if (data.getTimeField() != null) {
                    mval.setTimestamp(data.getTimeField().toEpochMilli());
                } else {
                    mval.setTimestamp(data.getTimestamp());
                }
                values.add(mval);
            }
        }
        return values;
    }

    private ArrayList<ChannelData> prepareChirpstackValues(Uplink data, ScriptingAdapterIface scriptingAdapter, String encoderCode, String deviceID, String userID) {
        ArrayList<ChannelData> values = new ArrayList<>();
        if (data.getPayloadFieldNames() == null || data.getPayloadFieldNames().length == 0) {
            if (null != data.getPayload()) {
                byte[] decodedPayload = Base64.getDecoder().decode(data.getPayload().getBytes());
                try {
                    values = scriptingAdapter.decodeData(decodedPayload, encoderCode, deviceID, data.getTimestamp(), userID);
                } catch (Exception e) {
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName() + ".prepareTtnValues for device " + deviceID, e.getMessage()));
                    fireEvent(1, deviceID, userID, e.getMessage());
                    return null;
                }
            }
        } else {
            for (String payloadFieldName : data.getPayloadFieldNames()) {
                ChannelData mval = new ChannelData();
                mval.setDeviceEUI(data.getDeviceEUI());
                mval.setName(payloadFieldName.toLowerCase());
                mval.setValue(data.getDoubleValue(payloadFieldName));
                mval.setStringValue(data.getStringValue(payloadFieldName));
                if (data.getTimeField() != null) {
                    mval.setTimestamp(data.getTimeField().toEpochMilli());
                } else {
                    mval.setTimestamp(data.getTimestamp());
                }
                values.add(mval);
            }
        }
        return values;
    }

    private ArrayList<ChannelData> prepareLoRaValues(LoRaData data, ScriptingAdapterIface scriptingAdapter, String encoderCode, String deviceID, String userID) {
        byte[] encodedPayload = Base64.getDecoder().decode(data.getData().getBytes());
        //przekształcamy tablicę bajtów na listę obiektów ChannelData
        ArrayList<ChannelData> values;
        try {
            values = scriptingAdapter.decodeData(encodedPayload, encoderCode, deviceID, data.getTimestamp(), userID);
        } catch (Exception e) {
            Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName() + ".prepareLoRaValues for device " + deviceID, e.getMessage()));
            fireEvent(1, deviceID, userID, e.getMessage());
            return null;
        }
        return values;
    }

    private ArrayList<ChannelData> prepareKpnValues(KPNData data, ScriptingAdapterIface scriptingAdapter, String encoderCode, String deviceID, String userID) {
        //przekształcamy hexadecimal payload na listę obiektów ChannelData
        ArrayList<ChannelData> values;
        try {
            values = scriptingAdapter.decodeHexData(data.getPayload(), encoderCode, deviceID, data.getTimestamp(), userID);
        } catch (Exception e) {
            Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName() + ".prepareLoRaValues for device " + deviceID, e.getMessage()));
            fireEvent(1, deviceID, userID, e.getMessage());
            return null;
        }
        return values;
    }

*/    
    public void writeVirtualData(ThingsDataIface thingsAdapter, ScriptingAdapterIface scriptingAdapter, Device device, ArrayList<ChannelData> values) {
        try {
            long now = System.currentTimeMillis();
            if (!device.getEUI().equalsIgnoreCase((String) Kernel.getInstance().getProperties().get("monitoring_device"))) {
                logger.debug("virtual data to {} {}",device.getEUI(),(String) Kernel.getInstance().getProperties().get("monitoring_device"));
                thingsAdapter.updateHealthStatus(device.getEUI(), now, 0/*new frame count*/, "", "");
            }
            ArrayList<ArrayList> outputList;
            try {
                Object[] processingResult = DataProcessor.processValues(values, device, scriptingAdapter,
                        now, device.getLatitude(), device.getLongitude(), device.getAltitude(), "", "");
                outputList = (ArrayList<ArrayList>) processingResult[0];
                for (int i = 0; i < outputList.size(); i++) {
                    thingsAdapter.putData(device.getUserID(), device.getEUI(), device.getProject(), device.getState(), fixValues(device, outputList.get(i)));
                }
                if (device.getState().compareTo((Double) processingResult[1]) != 0) {
                    System.out.println("DEVICE STATE " + device.getState() + " " + (Double) processingResult[1]);
                }
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void fireEvent(int source, Device device, String message) {
        fireEvent(source, device.getUserID(), device.getEUI(), message);
    }

    private void fireEvent(int source, String userID, String deviceEUI, String message) {
        ScriptingProblem ev = new ScriptingProblem();
        HashMap<String,Object>data=new HashMap<>();
        data.put("origin",userID + "\t" + deviceEUI);
        if (source == 1) {
            data.put("payload","Decoder script (0): " + message);
        } else {
            data.put("payload","Data processor script (0): " + message);
        }
        Kernel.getInstance().dispatchEvent(ev.data(data));
    }

    String buildResultData(boolean html, boolean isSuccess, String title, String text) {
        if (!html) {
            return text;
        }
        String err = isSuccess ? "" : "ERROR<br>";
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='text-align: center;'><h1>")
                .append(title)
                .append("</h1><p>")
                .append(err)
                .append(text)
                .append("</p><button type='button' onclick='window.history.go(-1); return false;'>")
                .append("OK")
                .append("</button></body></html>");
        return sb.toString();
    }

}
