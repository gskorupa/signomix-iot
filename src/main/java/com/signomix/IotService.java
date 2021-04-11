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

import org.cricketmsf.services.MinimalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.signomix.out.db.IotDatabaseIface;
import com.signomix.out.iot.ThingsDataIface;
import org.cricketmsf.out.db.KeyValueDBIface;

public class IotService extends MinimalService {

    private static final Logger logger = LoggerFactory.getLogger(IotService.class);

    Invariants invariants = null;
    private ThingsDataIface thingsAdapter;
    KeyValueDBIface database = null;
    IotDatabaseIface thingsDB = null;

    public IotService() {
        super();
        eventRouter = new IotServiceRouter(this);
    }

    @Override
    public void getAdapters() {
        super.getAdapters();
        thingsAdapter = (ThingsDataIface) getAdaptersMap().get("IotAdapter");
        database = (KeyValueDBIface) getRegistered("database");
        thingsDB = (IotDatabaseIface) getAdaptersMap().get("IotDB");

    }

    @Override
    public void runInitTasks() {
        super.runInitTasks();
        invariants = new Invariants();
        PlatformAdministrationModule.getInstance().initDatabases(database, thingsDB);
    }

    /**
     * @return the thingsAdapter
     */
    public ThingsDataIface getThingsAdapter() {
        return thingsAdapter;
    }

    public IotDatabaseIface getIotDB() {
        return thingsDB;
    }
}
