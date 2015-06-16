/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig;

import org.mule.api.context.MuleContextAware;

public class DummyComponent
{
    private MuleContextAware property;

    public MuleContextAware getProperty()
    {
        return property;
    }

    public void setProperty(MuleContextAware dummy)
    {
        this.property = dummy;
    }

}