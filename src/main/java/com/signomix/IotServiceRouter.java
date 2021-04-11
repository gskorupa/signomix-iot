package com.signomix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO: Unable to find procedure@method 0@null for event class com.signomix.events.ScriptingProblem 
/**
 *
 * @author greg
 */
public class IotServiceRouter {

    private static final Logger logger = LoggerFactory.getLogger(IotServiceRouter.class);

    private IotService service;

    public IotServiceRouter(IotService service) {
        this.service = service;
    }

}
