/**
* Copyright (C) Grzegorz Skorupa 2018.
* Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
*/
package com.signomix.out.iot;

import java.util.List;
import org.cricketmsf.event.Event;

/**
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public interface ThingsDataIface {
    public void init(String helperName) throws ThingsDataException;   
    public boolean isAuthorized(String userIF, String deviceEUI) throws ThingsDataException;
    public boolean isGroupAuthorized(String userIF, String groupEUI) throws ThingsDataException;
    public void putDevice(String userID, Device device) throws ThingsDataException;
    public void modifyDevice(String userID, Device device) throws ThingsDataException;
    public void updateHealthStatus(String id, long lastSeen, long frameCounter, String downlink, String deviceID) throws ThingsDataException;
    public void updateAlertStatus(String id, int status) throws ThingsDataException;
    public void updateDeviceState(String id, Double state) throws ThingsDataException;
    public Device getDevice(String userId, String deviceEUI, boolean withShared) throws ThingsDataException;
    public Device getDevice(String deviceEUI) throws ThingsDataException;
    public List<Device> getUserDevices(String userID, boolean withShared) throws ThingsDataException;
    public List<Device> getGroupDevices(String userID, String group) throws ThingsDataException;
    public int getUserDevicesCount(String userID) throws ThingsDataException;
    public void removeDevice(String deviceEUI) throws ThingsDataException;
    public void removeAllDevices(String userId) throws ThingsDataException;    
    public List<Device> getInactiveDevices() throws ThingsDataException;
    public List <DeviceGroup> getUserGroups(String userID) throws ThingsDataException;
    public DeviceGroup getGroup(String groupEUI) throws ThingsDataException;
    public DeviceGroup getGroup(String userId, String groupEUI) throws ThingsDataException;
    public void putGroup(String userID, DeviceGroup group) throws ThingsDataException;
    public void modifyGroup(String userID, DeviceGroup group) throws ThingsDataException;
    public void removeGroup(String userID, String groupEUI) throws ThingsDataException;
    public List<DeviceTemplate> getTemplates() throws ThingsDataException;
}
