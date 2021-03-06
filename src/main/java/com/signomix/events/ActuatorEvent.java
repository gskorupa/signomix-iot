
package com.signomix.events;

import com.signomix.iot.IotData;
import java.util.HashMap;
import org.cricketmsf.event.Event;

public class ActuatorEvent extends Event {
    
    private HashMap<String,Object> data=null;

    public ActuatorEvent() {
        super();
    }
    
    public ActuatorEvent data(HashMap<String,Object> newData){
        this.data=newData;
        return this;
    }

    /**
     * @return the data
     */
    @Override
    public HashMap<String,Object> getData() {
        return data;
    }
    
}
