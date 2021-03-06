
package com.signomix.events;

import com.signomix.iot.IotData;
import org.cricketmsf.event.Event;

public class NewDataEvent extends Event {
    
    private IotData data=null;

    public NewDataEvent() {
        super();
    }
    
    public NewDataEvent data(IotData newData){
        this.data=newData;
        return this;
    }

    /**
     * @return the data
     */
    @Override
    public IotData getData() {
        return data;
    }
    
}
