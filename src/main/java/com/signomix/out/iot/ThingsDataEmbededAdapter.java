/**
 * Copyright (C) Grzegorz Skorupa 2018.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.out.iot;

import com.signomix.events.PlatformMonitoringEvent;
import com.signomix.out.db.IotDatabaseIface;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import org.cricketmsf.Adapter;
import org.cricketmsf.Kernel;
import org.cricketmsf.out.OutboundAdapter;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public class ThingsDataEmbededAdapter extends OutboundAdapter implements Adapter, ThingsDataIface {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ThingsDataEmbededAdapter.class);

    private String helperAdapterName; // IoT DB
    private boolean initialized = false;
    String monitoringDeviceEui;

    @Override
    public void init(String helperName) throws ThingsDataException {
    }

    private IotDatabaseIface getIotDB() {
        return (IotDatabaseIface) Kernel.getInstance().getAdaptersMap().get(helperAdapterName);
    }

    @Override
    public void loadProperties(HashMap<String, String> properties, String adapterName) {
        helperAdapterName = properties.get("helper-name");
        logger.info("\thelper-name: " + helperAdapterName);
        try {
            init(helperAdapterName);
        } catch (ThingsDataException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        monitoringDeviceEui = (String) Kernel.getInstance().getProperties().get("monitoring_device");
    }

    @Override
    public void putDevice(String userID, Device device) throws ThingsDataException {
        if (!userID.equals(device.getUserID())) {
            throw new ThingsDataException(ThingsDataException.NOT_AUTHORIZED, "user IDs not match");
        }
        getIotDB().putDevice(device);
        //TODO: device updated event
        //getDataStorage().updateDeviceChannels(device, null);
    }

    @Override
    public void modifyDevice(String userID, Device device) throws ThingsDataException {
        Device previous = getDevice(userID, device.getEUI(), false);
        if (previous == null) {
            throw new ThingsDataException(ThingsDataException.NOT_FOUND, "device not found");
        }
        //TODO: what to do when list of channels has been changed?
        getIotDB().updateDevice(device);
        //TODO: device updated event
        //if (getDataStorage().updateDeviceChannels(device, previous) > 0) {
        //    HashMap<String, Object> data = new HashMap<>();
        //    data.put("message", "all data channels have been removed because of the device channels modification");
        //    data.put("origin", userID + "\t" + device.getEUI());
        //    ChannelsRemovedEvent event= new ChannelsRemovedEvent().data(data);
        //    Kernel.getInstance().dispatchEvent(event);
        //}
    }

    @Override
    public void updateHealthStatus(String EUI, long lastSeen, long frameCounter, String downlink, String deviceID) throws ThingsDataException {
        Device dev = getDevice(EUI);
        if (dev == null) {
            throw new ThingsDataException(ThingsDataException.NOT_FOUND, "device not found");
        } else if (null != monitoringDeviceEui) {
            logger.debug("virtual data to {} {}", EUI, monitoringDeviceEui);
            if (!EUI.equalsIgnoreCase(monitoringDeviceEui)) {
                HashMap<String, Object> data = new HashMap<>();
                String cmd = Base64.getEncoder().encodeToString("datareceived".getBytes());
                data.put("info", cmd);
                Kernel.getInstance().dispatchEvent(new PlatformMonitoringEvent().data(data));
            }
        }
        dev.setLastSeen(lastSeen);
        dev.setLastFrame(frameCounter);
        dev.setDownlink(downlink);
        dev.setAlertStatus(Device.OK);
        dev.setDeviceID(deviceID);
        getIotDB().updateDevice(dev); //TODO
    }

    @Override
    public void updateAlertStatus(String EUI, int newAlertStatus) throws ThingsDataException {
        Device dev = getDevice(EUI);
        if (dev == null) {
            throw new ThingsDataException(ThingsDataException.NOT_FOUND, "device not found");
        }
        dev.setAlertStatus(newAlertStatus);
        getIotDB().updateDevice(dev);
    }

    @Override
    public void updateDeviceState(String EUI, Double newState) throws ThingsDataException {
        Device dev = getDevice(EUI);
        if (dev == null) {
            throw new ThingsDataException(ThingsDataException.NOT_FOUND, "device not found");
        }
        dev.setState(newState);
        getIotDB().updateDevice(dev);
    }

    @Override
    public Device getDevice(String userId, String deviceEUI, boolean withShared) throws ThingsDataException {
        return getIotDB().getDevice(userId, deviceEUI, withShared);
    }

    @Override
    public Device getDevice(String deviceEUI) throws ThingsDataException {
        return getIotDB().getDevice(deviceEUI);
    }

    @Override
    public List<Device> getUserDevices(String userID, boolean withShared) throws ThingsDataException {
        return getIotDB().getUserDevices(userID, withShared);
    }

    @Override
    public List<Device> getGroupDevices(String userID, String group) throws ThingsDataException {
        return getIotDB().getGroupDevices(userID, group);
    }

    @Override
    public int getUserDevicesCount(String userID) throws ThingsDataException {
        return getIotDB().getUserDevicesCount(userID);
    }

    @Override
    public List<DeviceTemplate> getTemplates() throws ThingsDataException {
        return getIotDB().getDeviceTemplates();
    }

    @Override
    public boolean isAuthorized(String userID, String deviceEUI) throws ThingsDataException {
        return getIotDB().isAuthorized(userID, deviceEUI);
    }

    @Override
    public boolean isGroupAuthorized(String userID, String groupEUI) throws ThingsDataException {
        return getIotDB().isGroupAuthorized(userID, groupEUI);
    }

    @Override
    public void removeDevice(String deviceEUI) throws ThingsDataException {
        //TODO: chanel remob=ved event
        //removeAllChannels(deviceEUI);
        getIotDB().removeDevice(deviceEUI);
    }

    @Override
    public void removeAllDevices(String userId) throws ThingsDataException {
        getIotDB().removeAllDevices(userId);
    }

    @Override
    public List<Device> getInactiveDevices() throws ThingsDataException {
        return getIotDB().getInactiveDevices();
    }

    @Override
    public List<DeviceGroup> getUserGroups(String userID) throws ThingsDataException {
        return getIotDB().getUserGroups(userID);
    }

    @Override
    public DeviceGroup getGroup(String groupEUI) throws ThingsDataException {
        return getIotDB().getGroup(groupEUI);
    }

    @Override
    public DeviceGroup getGroup(String userId, String groupEUI) throws ThingsDataException {
        return getIotDB().getGroup(userId, groupEUI);
    }

    @Override
    public void putGroup(String userID, DeviceGroup group) throws ThingsDataException {
        if (userID.equals(group.getUserID()) || group.userIsTeamMember(userID)) {
            getIotDB().putGroup(group);
        } else {
            throw new ThingsDataException(ThingsDataException.NOT_AUTHORIZED, "user IDs not match");
        }
    }

    @Override
    public void modifyGroup(String userID, DeviceGroup group) throws ThingsDataException {
        DeviceGroup previous = getGroup(userID, group.getEUI());
        if (previous == null) {
            throw new ThingsDataException(ThingsDataException.NOT_FOUND, "group not found");
        }
        //TODO: what to do when list of channels has been changed?
        getIotDB().updateGroup(group);
    }

    @Override
    public void removeGroup(String userID, String groupEUI) throws ThingsDataException {
        DeviceGroup group = getIotDB().getGroup(userID, groupEUI);
        if (userID.equals(group.getUserID()) || group.userIsTeamMember(userID)) {
            getIotDB().removeGroup(groupEUI);
        } else {
            throw new ThingsDataException(ThingsDataException.NOT_AUTHORIZED, "user IDs not match");
        }
    }
    
}
