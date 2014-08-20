/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.schedulers.config;

import org.mule.module.springconfig.handlers.AbstractMuleNamespaceHandler;
import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;
import org.mule.modules.schedulers.cron.CronSchedulerFactory;


public class SchedulersNamespaceHandler extends AbstractMuleNamespaceHandler
{

    @Override
    public void init()
    {
        registerBeanDefinitionParser("cron-scheduler", new ChildDefinitionParser("schedulerFactory", CronSchedulerFactory.class));
    }
}