/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import org.mule.api.MuleEvent;
import org.mule.transformer.types.TypedValue;

import java.io.Serializable;

/**
 * Propagates data type for session vars used for enrichment target
 */
public class SessionVarEnricherDataTypePropagator extends AbstractVariableEnricherDataTypePropagator
{

    public SessionVarEnricherDataTypePropagator()
    {
        super("sessionVars");
    }

    protected void addVariable(MuleEvent event, TypedValue typedValue, String propertyName)
    {
        event.getSession().setProperty(propertyName, (Serializable) typedValue.getValue(), typedValue.getDataType());
    }

    protected boolean containsVariable(MuleEvent event, String propertyName)
    {
        return event.getSession().getPropertyNamesAsSet().contains(propertyName);
    }

}
