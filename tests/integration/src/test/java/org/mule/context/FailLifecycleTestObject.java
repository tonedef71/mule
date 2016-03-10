/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;

public class FailLifecycleTestObject implements Initialisable, Startable
{

    private boolean failInit = false;
    private boolean failStart = false;

    @Override
    public void initialise() throws InitialisationException
    {
        if (failInit)
        {
            throw new InitialisationException(new RuntimeException(), this);
        }
    }

    @Override
    public void start() throws MuleException
    {
        if (failStart)
        {
            throw new DefaultMuleException("c'est la vie");
        }
    }

    public boolean isFailInit()
    {
        return failInit;
    }

    public void setFailInit(boolean failInit)
    {
        this.failInit = failInit;
    }

    public boolean isFailStart()
    {
        return failStart;
    }

    public void setFailStart(boolean failStart)
    {
        this.failStart = failStart;
    }
}
