/**
 * Copyright (C) Grzegorz Skorupa 2018.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.out.db;

import com.signomix.events.ActuatorEvent;
import com.signomix.out.iot.ThingsDataException;
import java.util.List;
import org.cricketmsf.event.Event;
import org.cricketmsf.out.db.KeyValueDBIface;

/**
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public interface ActuatorCommandsDBIface extends KeyValueDBIface {

    public void putDeviceCommand(String deviceEUI, ActuatorEvent commandEvent) throws ThingsDataException;

    /**
     *
     * @param deviceEUI
     * @return
     * @throws ThingsDataException
     */
    public ActuatorEvent getFirstCommand(String deviceEUI) throws ThingsDataException;

    public ActuatorEvent previewDeviceCommand(String deviceEUI, ActuatorEvent commandEvent) throws ThingsDataException;

    public void clearAllCommands(String deviceEUI, long checkPoint) throws ThingsDataException;

    public void removeAllCommands(String deviceEUI) throws ThingsDataException;
    
    public void removeCommand(long id) throws ThingsDataException;

    public List<ActuatorEvent> getAllCommands(String deviceEUI) throws ThingsDataException;

    public void putCommandLog(String deviceEUI, ActuatorEvent commandEvent) throws ThingsDataException;

    //public Event getFirstLog(String deviceEUI) throws ThingsDataException;

    public void clearAllLogs(String deviceEUI, long checkPoint) throws ThingsDataException;

    public void removeAllLogs(String deviceEUI) throws ThingsDataException;

    public List<ActuatorEvent> getAllLogs(String deviceEUI) throws ThingsDataException;
}
