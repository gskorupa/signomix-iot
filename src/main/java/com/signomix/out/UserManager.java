package com.signomix.out;

import java.util.HashMap;
import org.cricketmsf.Adapter;
import org.cricketmsf.out.OutboundAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author greg
 */
public class UserManager extends OutboundAdapter implements Adapter, UserManagerIface {
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);
    private HashMap<String, String> users = null;

    @Override
    public boolean addUser(String name) {
        users.put(name, name);
        return true;
    }

    @Override
    public boolean isRegistered(String name) {
        return users.containsKey(name);
    }

    @Override
    public void loadProperties(HashMap<String, String> properties, String adapterName) {
        super.loadProperties(properties, adapterName);
        users = new HashMap<>();
    }

    @Override
    public String getGreeting(String userName, String friendName) {
        if (friendName.isBlank()) {
            return String.format("Hello %1s!", userName);
        } else {
            return String.format("Hello %1s! Greetings from %2s.", friendName, userName);
        }
    }
    
    @Override
    public void clear(){
        users.clear();
    }

}
