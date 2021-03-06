package com.signomix.out;

/**
 *
 * @author greg
 */
public interface UserManagerIface {
    
    public boolean addUser(String name);
    public boolean isRegistered(String name);
    public String getGreeting(String userName, String friendName);
    public void clear();
}
