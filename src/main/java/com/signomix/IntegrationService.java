/*
 * Copyright 2021 Grzegorz Skorupa <g.skorupa at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");

 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */package com.signomix;

import com.signomix.in.ActuatorApi;
import org.cricketmsf.services.MinimalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.signomix.out.db.ActuatorCommandsDBIface;
import com.signomix.out.db.IotDataStorageIface;
import com.signomix.out.db.IotDatabaseIface;
import com.signomix.out.iot.ThingsDataIface;
import com.signomix.out.script.ScriptingAdapterIface;
import org.cricketmsf.microsite.out.user.UserAdapterIface;
import org.cricketmsf.out.db.KeyValueDBIface;

public class IntegrationService extends MinimalService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);
    
    Invariants invariants = null;

    private ScriptingAdapterIface scriptingAdapter;
    private ThingsDataIface thingsAdapter;
    private UserAdapterIface userAdapter;
    
    KeyValueDBIface database = null;
    IotDatabaseIface thingsDB = null;
    IotDataStorageIface iotDataDB = null;
    private ActuatorCommandsDBIface actuatorCommandsDB;
    
    private ActuatorApi actuatorApi = null;

    public IntegrationService() {
        super();
        eventRouter = new IntegrationServiceRouter(this);
    }

    @Override
    public void getAdapters() {
        super.getAdapters();
        userAdapter = (UserAdapterIface) getAdaptersMap().get("UserAdapter");
        scriptingAdapter = (ScriptingAdapterIface) getAdaptersMap().get("ScriptingAdapter");
        thingsAdapter = (ThingsDataIface) getAdaptersMap().get("IotAdapter");
        actuatorApi = (ActuatorApi) getAdaptersMap().get("ActuatorService");
        database = (KeyValueDBIface) getRegistered("Database");
        thingsDB = (IotDatabaseIface) getAdaptersMap().get("IotDB");
        iotDataDB = (IotDataStorageIface) getAdaptersMap().get("IotDataDB");
        actuatorCommandsDB = (ActuatorCommandsDBIface) getAdaptersMap().get("ActuatorCommandsDB");
    }

    @Override
    public void runInitTasks() {
        super.runInitTasks();
        invariants = new Invariants();
        PlatformAdministrationModule.getInstance().initDatabases(database,thingsDB,iotDataDB, actuatorCommandsDB);
    }

    /**
     * @return the scriptingAdapter
     */
    public ScriptingAdapterIface getScriptingAdapter() {
        return scriptingAdapter;
    }

    /**
     * @return the thingsAdapter
     */
    public ThingsDataIface getThingsAdapter() {
        return thingsAdapter;
    }

    /**
     * @return the userAdapter
     */
    public UserAdapterIface getUserAdapter() {
        return userAdapter;
    }

    /**
     * @return the actuatorCommandsDB
     */
    public ActuatorCommandsDBIface getActuatorCommandsDB() {
        return actuatorCommandsDB;
    }

    /**
     * @return the actuatorApi
     */
    public ActuatorApi getActuatorApi() {
        return actuatorApi;
    }
    
    public IotDataStorageIface getIotDataDB(){
        return iotDataDB;
    }
    
    public IotDatabaseIface getIotDB(){
        return thingsDB;
    }
}
