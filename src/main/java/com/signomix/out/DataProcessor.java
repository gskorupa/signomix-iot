/**
 * Copyright (C) Grzegorz Skorupa 2018.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.out;

import com.signomix.events.ActuatorEvent;
import com.signomix.out.iot.ChannelData;
import com.signomix.out.iot.Device;
import com.signomix.out.script.ScriptAdapterException;
import com.signomix.out.script.ScriptResult;
import com.signomix.out.script.ScriptingAdapterIface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.cricketmsf.Kernel;
import com.signomix.events.NewActuatorCommand;
import com.signomix.events.NewNotification;
import com.signomix.events.NewVirtualData;

/**
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public class DataProcessor {

//    public static ArrayList<ArrayList> processValues(ArrayList<ChannelData> listOfValues, Device device, ScriptingAdapterIface scriptingAdapter, long dataTimestamp,
//            Double latitude, Double longitude, Double altitude) throws Exception {
    public static Object[] processValues(
            ArrayList<ChannelData> listOfValues,
            Device device,
            ScriptingAdapterIface scriptingAdapter,
            long dataTimestamp,
            Double latitude,
            Double longitude,
            Double altitude,
            String requestData,
            String command
    ) throws Exception {
        ScriptResult scriptResult = null;
        try {
            scriptResult = scriptingAdapter.processData(
                    listOfValues,
                    device.getCodeUnescaped(),
                    device.getEUI(),
                    device.getUserID(),
                    dataTimestamp,
                    latitude,
                    longitude,
                    altitude,
                    device.getState(),
                    device.getAlertStatus(),
                    device.getLatitude(),
                    device.getLongitude(),
                    device.getAltitude(),
                    command,
                    requestData);
        } catch (ScriptAdapterException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
        if (scriptResult == null) {
            throw new Exception("preprocessor script returns null result");
        }
        ArrayList<ArrayList> finalValues = scriptResult.getOutput();
        ArrayList<NewActuatorCommand> commands = scriptResult.getCommands();
        ArrayList<NewNotification> notifications = scriptResult.getNotifications();
        HashMap<String, String> recipients;
        //commands
        for (int i = 0; i < commands.size(); i++) {
            Kernel.getInstance().dispatchEvent(commands.get(i));
        }
        //notifications
        for (int i = 0; i < notifications.size(); i++) {
            recipients = new HashMap<>();
            recipients.put(device.getUserID(), "");
            if (device.getTeam() != null) {
                String[] r = device.getTeam().split(",");
                for (int j = 0; j < r.length; j++) {
                    if (!r[j].isEmpty()) {
                        recipients.put(r[j], "");
                    }
                }
            }
            Iterator itr = recipients.keySet().iterator();
            while (itr.hasNext()) {
                NewNotification newEvent = new NewNotification().data(notifications.get(i).getData());
                Kernel.getInstance().dispatchEvent(newEvent);
            }
        }

        //data events
        HashMap<String, ArrayList> dataEvents = scriptResult.getDataEvents();
        ArrayList<NewVirtualData> eventList;
        for (String deviceName : dataEvents.keySet()) {
            eventList = dataEvents.get(deviceName);
            NewVirtualData newEvent;
            if (eventList.size() > 0) {
                newEvent = new NewVirtualData().data(eventList.get(0).getData());
                //newEvent.setOrigin(device.getUserID());
                String payload = "";
                for (int i = 0; i < eventList.size(); i++) {
                    payload = payload + ";" + eventList.get(i).getData().get("payload");
                }
                payload = payload.substring(1);
                newEvent.getData().put("payload",payload);
                Kernel.getInstance().dispatchEvent(newEvent);
            }
        }
        Object[] result = {finalValues, scriptResult.getDeviceState()};
        return result;
    }

    /*
    public static ArrayList<ChannelData> processRawValues(String requestBody, Device device, ScriptingAdapterIface scriptingAdapter, long dataTimestamp) throws Exception {
        ScriptResult scriptResult = null;
        try {
            scriptResult = scriptingAdapter.processRawData(requestBody, device.getCodeUnescaped(), device.getEUI(), device.getUserID(), dataTimestamp);
        } catch (ScriptAdapterException e) {
            throw new Exception(e.getMessage());
        }
        if (scriptResult == null) {
            throw new Exception("preprocessor script returns null result");
        }
        ArrayList<ChannelData> finalValues = scriptResult.getMeasures();
        ArrayList<NewActuatorCommand> events = scriptResult.getCommands();
        //Event ev;
        HashMap<String, String> recipients;
        for (int i = 0; i < events.size(); i++) {
            if (Event.CATEGORY_GENERIC.equals(events.get(i).getCategory())) {
                Event newEvent = events.get(i).clone();
                newEvent.setOrigin(device.getEUI());
                Kernel.getInstance().dispatchEvent(newEvent);
            } else {
                recipients = new HashMap<>();
                recipients.put(device.getUserID(), "");
                if (device.getTeam() != null) {
                    String[] r = device.getTeam().split(",");
                    for (int j = 0; j < r.length; j++) {
                        if (!r[j].isEmpty()) {
                            recipients.put(r[j], "");
                        }
                    }
                }
                Iterator itr = recipients.keySet().iterator();
                while (itr.hasNext()) {
                    Event newEvent = events.get(i).clone();
                    newEvent.setOrigin(itr.next() + "\t" + device.getEUI());
                    Kernel.getInstance().dispatchEvent(newEvent);
                }
            }
        }
        //data commands
        HashMap<String, ArrayList> dataEvents = scriptResult.getDataEvents();
        ArrayList<Event> eventList;
        for (String deviceName : dataEvents.keySet()) {
            eventList = dataEvents.get(deviceName);
            Event newEvent;
            if (eventList.size() > 0) {
                newEvent = eventList.get(0).clone();
                newEvent.setOrigin(device.getUserID());
                String payload = "";
                for (int i = 0; i < eventList.size(); i++) {
                    payload = payload + ";" + eventList.get(i).getPayload();
                }
                payload = payload.substring(1);
                newEvent.setPayload(payload);
                Kernel.getInstance().dispatchEvent(newEvent);
            }
        }
        return finalValues;
    }
*/
}
