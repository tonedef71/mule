/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import org.mule.api.MuleEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FlowVariableMapContext extends AbstractMapContext<String, Object>
{

    private MuleEvent event;

    public FlowVariableMapContext(MuleEvent event)
    {
        this.event = event;
    }

    @Override
    public Object get(Object key)
    {
        if (!(key instanceof String))
        {
            return null;
        }
        return event.getFlowVariable(((String) key));
    }

    @Override
    public Object put(String key, Object value)
    {
        Object previousValue = get(key);
        event.setFlowVariable(key, value);
        return previousValue;
    }

    @Override
    public Object remove(Object key)
    {
        if (!(key instanceof String))
        {
            return null;
        }
        Object previousValue = get(key);
        event.removeFlowVariable((String) key);
        return previousValue;
    }

    @Override
    public Set<String> keySet()
    {
        return event.getFlowVariableNames();
    }

    @Override
    public void clear()
    {
        event.clearFlowVariables();
    }

    @Override
    public String toString()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String key : event.getFlowVariableNames())
        {
            Object value = event.getFlowVariable(key);
            map.put(key, value);
        }
        return map.toString();
    }
}
