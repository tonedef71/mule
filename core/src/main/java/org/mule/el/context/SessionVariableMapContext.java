/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import org.mule.api.MuleSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SessionVariableMapContext extends AbstractMapContext<String, Object>
{

    private MuleSession session;

    public SessionVariableMapContext(MuleSession session)
    {
        this.session = session;
    }

    @Override
    public Object get(Object key)
    {
        if (!(key instanceof String))
        {
            return null;
        }
        return session.getProperty(((String) key));
    }

    @Override
    public Object put(String key, Object value)
    {
        Object previousValue = get(key);
        session.setProperty(key, value);
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
        session.removeProperty(((String) key));
        return previousValue;
    }

    @Override
    public Set<String> keySet()
    {
        return session.getPropertyNamesAsSet();
    }

    @Override
    public void clear()
    {
        session.clearProperties();
    }

    @Override
    public String toString()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String key : session.getPropertyNamesAsSet())
        {
            Object value = session.getProperty(key);
            map.put(key, value);
        }
        return map.toString();
    }
}
