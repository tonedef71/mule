/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

import org.mule.lifecycle.LifecycleObject;

import java.util.Set;

public abstract class LifecyclePhaseWrapper implements LifecyclePhase
{
    protected final LifecyclePhase delegate;

    public LifecyclePhaseWrapper(LifecyclePhase delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public String getName()
    {
        return delegate.getName();
    }

    @Override
    public void addOrderedLifecycleObject(LifecycleObject lco)
    {
        delegate.addOrderedLifecycleObject(lco);
    }

    @Override
    public void removeOrderedLifecycleObject(LifecycleObject lco)
    {
        delegate.removeOrderedLifecycleObject(lco);
    }

    @Override
    public Set<LifecycleObject> getOrderedLifecycleObjects()
    {
        return delegate.getOrderedLifecycleObjects();
    }

    @Override
    public void setOrderedLifecycleObjects(Set<LifecycleObject> orderedLifecycleObjects)
    {
        delegate.setOrderedLifecycleObjects(orderedLifecycleObjects);
    }

    @Override
    public Class<?>[] getIgnoredObjectTypes()
    {
        return delegate.getIgnoredObjectTypes();
    }

    @Override
    public void setIgnoredObjectTypes(Class<?>[] ignorredObjectTypes)
    {
        delegate.setIgnoredObjectTypes(ignorredObjectTypes);
    }

    @Override
    public Class<?> getLifecycleClass()
    {
        return delegate.getLifecycleClass();
    }

    @Override
    public void setLifecycleClass(Class<?> lifecycleClass)
    {
        delegate.setLifecycleClass(lifecycleClass);
    }

    @Override
    public Set<String> getSupportedPhases()
    {
        return delegate.getSupportedPhases();
    }

    @Override
    public void setSupportedPhases(Set<String> supportedPhases)
    {
        delegate.setSupportedPhases(supportedPhases);
    }

    @Override
    public void registerSupportedPhase(String phase)
    {
        delegate.registerSupportedPhase(phase);
    }

    @Override
    public boolean isPhaseSupported(String phase)
    {
        return delegate.isPhaseSupported(phase);
    }

    @Override
    public void applyLifecycle(Object o) throws LifecycleException
    {
        delegate.applyLifecycle(o);
    }

    @Override
    public String getOppositeLifecyclePhase()
    {
        return delegate.getOppositeLifecyclePhase();
    }
}
