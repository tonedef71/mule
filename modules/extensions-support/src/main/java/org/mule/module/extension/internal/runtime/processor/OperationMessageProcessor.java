/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.processor;

import static org.mule.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extension.internal.util.IntrospectionUtils.isVoid;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.metadata.FailureType;
import org.mule.api.metadata.MetadataAware;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.api.metadata.Result;
import org.mule.api.processor.MessageProcessor;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.RuntimeExtensionModel;
import org.mule.extension.api.introspection.RuntimeOperationModel;
import org.mule.extension.api.metadata.MetadataContext;
import org.mule.extension.api.metadata.MetadataResolvingException;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.OperationContext;
import org.mule.extension.api.runtime.OperationExecutor;
import org.mule.internal.connection.ConnectionManagerAdapter;
import org.mule.metadata.api.model.MetadataType;
import org.mule.module.extension.internal.runtime.DefaultExecutionMediator;
import org.mule.module.extension.internal.runtime.DefaultOperationContext;
import org.mule.module.extension.internal.runtime.ExecutionMediator;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.config.DynamicConfigurationProvider;
import org.mule.module.extension.internal.runtime.metadata.DefaultMetadataContext;
import org.mule.module.extension.internal.runtime.metadata.MetadataMediator;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.util.StringUtils;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MessageProcessor} capable of executing extension operations.
 * <p>
 * It obtains a configuration instance, evaluate all the operation parameters
 * and executes a {@link RuntimeOperationModel} by using a {@link #operationExecutor}. This message processor is capable
 * of serving the execution of any {@link OperationModel} of any {@link RuntimeExtensionModel}.
 * <p>
 * A {@link #operationExecutor} is obtained by invoking {@link RuntimeOperationModel#getExecutor()}. That instance
 * will be use to serve all invokations of {@link #process(MuleEvent)} on {@code this} instance but
 * will not be shared with other instances of {@link OperationMessageProcessor}. All the {@link Lifecycle}
 * events that {@code this} instance receives will be propagated to the {@link #operationExecutor}.
 * <p>
 * The {@link #operationExecutor} is executed directly but by the means of a {@link DefaultExecutionMediator}
 *
 * @since 3.7.0
 */
public final class OperationMessageProcessor implements MessageProcessor, MuleContextAware, Lifecycle, MetadataAware
{

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationMessageProcessor.class);

    private final RuntimeExtensionModel extensionModel;
    private final String configurationProviderName;
    private final RuntimeOperationModel operationModel;
    private final ResolverSet resolverSet;
    private final ExtensionManager extensionManager;
    private final String target;

    private ExecutionMediator executionMediator;
    private ReturnDelegate returnDelegate;
    private MuleContext muleContext;
    private OperationExecutor operationExecutor;
    private MetadataMediator metadataMediator;

    @Inject
    private ConnectionManagerAdapter connectionManager;

    public OperationMessageProcessor(RuntimeExtensionModel extensionModel,
                                     RuntimeOperationModel operationModel,
                                     String configurationProviderName,
                                     String target,
                                     ResolverSet resolverSet,
                                     ExtensionManager extensionManager)
    {
        this.extensionModel = extensionModel;
        this.operationModel = operationModel;
        this.configurationProviderName = configurationProviderName;
        this.resolverSet = resolverSet;
        this.extensionManager = extensionManager;
        this.target = target;
        this.metadataMediator = new MetadataMediator(operationModel);
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        ConfigurationInstance<Object> configuration = getConfiguration(event);
        OperationContextAdapter operationContext = createOperationContext(configuration, event);
        Object result = executeOperation(operationContext, event);

        return returnDelegate.asReturnValue(result, operationContext);
    }

    private Object executeOperation(OperationContext operationContext, MuleEvent event) throws MuleException
    {
        try
        {
            return executionMediator.execute(operationExecutor, operationContext);
        }
        catch (Throwable e)
        {
            throw new MessagingException(createStaticMessage(e.getMessage()), event, e, this);
        }
    }

    private ConfigurationInstance<Object> getConfiguration(MuleEvent event)
    {
        return StringUtils.isBlank(configurationProviderName) ? extensionManager.getConfiguration(extensionModel, event)
                                                              : extensionManager.getConfiguration(configurationProviderName, event);
    }

    private OperationContextAdapter createOperationContext(ConfigurationInstance<Object> configuration, MuleEvent event) throws MuleException
    {
        return new DefaultOperationContext(configuration, resolverSet.resolve(event), operationModel, event);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        returnDelegate = createReturnDelegate();
        operationExecutor = operationModel.getExecutor().createExecutor();
        executionMediator = new DefaultExecutionMediator(extensionModel, operationModel, connectionManager);
        initialiseIfNeeded(operationExecutor, true, muleContext);
    }

    private ReturnDelegate createReturnDelegate()
    {
        if (isVoid(operationModel))
        {
            return VoidReturnDelegate.INSTANCE;
        }

        return StringUtils.isBlank(target) ? new ValueReturnDelegate(muleContext) : new TargetReturnDelegate(target, muleContext);
    }

    @Override
    public void start() throws MuleException
    {
        startIfNeeded(operationExecutor);
    }

    @Override
    public void stop() throws MuleException
    {
        stopIfNeeded(operationExecutor);
    }

    @Override
    public void dispose()
    {
        disposeIfNeeded(operationExecutor, LOGGER);
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public Result<List<MetadataKey>> getMetadataKeys() throws MetadataResolvingException
    {
        return metadataMediator.getMetadataKeys(getMetadataContext());
    }

    @Override
    public Result<MetadataType> getContentMetadata(MetadataKey key) throws MetadataResolvingException
    {
        return metadataMediator.getContentMetadata(getMetadataContext(), key);
    }

    @Override
    public Result<MetadataType> getOutputMetadata(MetadataKey key) throws MetadataResolvingException
    {
        return metadataMediator.getOutputMetadata(getMetadataContext(), key);
    }

    @Override
    public Result<OperationMetadataDescriptor> getMetadata() throws MetadataResolvingException
    {
        return metadataMediator.getMetadata();
    }

    @Override
    public Result<OperationMetadataDescriptor> getMetadata( MetadataKey key) throws MetadataResolvingException
    {
        return metadataMediator.getMetadata(getMetadataContext(), key);
    }

    private MetadataContext getMetadataContext() throws MetadataResolvingException
    {
        //FIXME bug when the configuration is accesed by the extensionModel, but the implicit config is dynamic
        if (!StringUtils.isBlank(configurationProviderName) &&
            muleContext.getRegistry().get(configurationProviderName) instanceof DynamicConfigurationProvider)
        {
            throw new MetadataResolvingException("Configuration used for Metadata fetch cannot be dynamic", FailureType.INVALID_CONFIGURATION);
        }

        ConfigurationInstance<Object> configuration = getConfiguration(getInitialiserEvent(muleContext));
        return new DefaultMetadataContext(configuration, connectionManager);
    }

}
