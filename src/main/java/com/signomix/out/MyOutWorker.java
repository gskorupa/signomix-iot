package com.signomix.out;

import org.cricketmsf.Adapter;
import org.cricketmsf.out.OutboundAdapter;

/**
 *
 * @author greg
 */
public class MyOutWorker extends OutboundAdapter implements Adapter, MyOutIface{

    @Override
    public void printOut(String data) {
        System.out.println("Hi, I'm MyOutWorker");
    }
    
}
