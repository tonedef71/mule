/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.processor;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.metadata.DataType;
import org.mule.api.metadata.FailureType;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.api.metadata.Result;
import org.mule.api.temporary.MuleMessage;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.ExceptionEnricherFactory;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.RuntimeConfigurationModel;
import org.mule.extension.api.introspection.RuntimeExtensionModel;
import org.mule.extension.api.introspection.RuntimeOperationModel;
import org.mule.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.OperationContext;
import org.mule.extension.api.runtime.OperationExecutor;
import org.mule.extension.api.runtime.OperationExecutorFactory;
import org.mule.internal.connection.ConnectionManagerAdapter;
import org.mule.internal.connection.ConnectionProviderWrapper;
import org.mule.internal.connection.DefaultConnectionManager;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.StringType;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.exception.NullExceptionEnricher;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extension.metadata.NoConfigMetadataResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OperationMessageProcessorTestCase extends AbstractMuleContextTestCase
{

    private static final String CONFIG_NAME = "config";
    private static final String TARGET_VAR = "myFlowVar";

    @Mock
    private RuntimeExtensionModel extensionModel;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private RuntimeConfigurationModel configurationModel;

    @Mock
    private RuntimeOperationModel operationModel;

    @Mock
    private ExtensionManager extensionManager;

    @Mock
    private ConnectionManagerAdapter connectionManagerAdapter;

    @Mock
    private OperationExecutorFactory operationExecutorFactory;

    @Mock(extraInterfaces = {Lifecycle.class, MuleContextAware.class})
    private OperationExecutor operationExecutor;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ResolverSet resolverSet;

    @Mock
    private ResolverSetResult parameters;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent event;

    @Mock
    private ConfigurationInstance<Object> configurationInstance;

    @Mock
    private Object configuration;

    @Mock
    private ExceptionEnricherFactory exceptionEnricherFactory;

    @Mock
    private MetadataResolverFactory metadataResolverFactory;

    @Mock
    private Result successResult;

    @Mock
    private Result failureResult;

    @Mock
    private ConnectionProviderWrapper connectionProviderWrapper;

    private OperationMessageProcessor messageProcessor;
    private String configurationName = CONFIG_NAME;
    private String target = EMPTY;
    private DefaultConnectionManager connectionManager;

    @Before
    public void before() throws Exception
    {
        configureMockEvent(event);

        when(operationModel.getReturnType()).thenReturn(toMetadataType(String.class));
        when(operationModel.getExecutor()).thenReturn(operationExecutorFactory);
        when(operationExecutorFactory.createExecutor()).thenReturn(operationExecutor);

        when(operationModel.getExceptionEnricherFactory()).thenReturn(Optional.of(exceptionEnricherFactory));
        when(exceptionEnricherFactory.createEnricher()).thenReturn(new NullExceptionEnricher());

        when(operationModel.getMetadataResolverFactory()).thenReturn(metadataResolverFactory);
        when(metadataResolverFactory.getResolver()).thenReturn(new NoConfigMetadataResolver());

        when(successResult.isSucess()).thenReturn(true);
        when(successResult.getFailureType()).thenReturn(FailureType.NONE);

        when(failureResult.isSucess()).thenReturn(false);
        when(failureResult.getFailureType()).thenReturn(FailureType.UNKNOWN);

        when(resolverSet.resolve(event)).thenReturn(parameters);

        when(configurationInstance.getName()).thenReturn(CONFIG_NAME);
        when(configurationInstance.getModel()).thenReturn(configurationModel);
        when(configurationInstance.getValue()).thenReturn(configuration);

        connectionManager = new DefaultConnectionManager(muleContext);
        connectionManager.initialise();
        when(connectionProviderWrapper.getRetryPolicyTemplate()).thenReturn(connectionManager.getDefaultRetryPolicyTemplate());
        when(configurationInstance.getConnectionProvider()).thenReturn(Optional.of(connectionProviderWrapper));

        when(extensionManager.getConfiguration(CONFIG_NAME, event)).thenReturn(configurationInstance);
        when(extensionManager.getConfiguration(extensionModel, event)).thenReturn(configurationInstance);


        messageProcessor = createOperationMessageProcessor();
    }

    @Test
    public void operationExecutorIsInvoked() throws Exception
    {
        messageProcessor.process(event);
        verify(operationExecutor).execute(any(OperationContext.class));
    }

    @Test
    public void operationContextIsWellFormed() throws Exception
    {
        ArgumentCaptor<OperationContext> operationContextCaptor = ArgumentCaptor.forClass(OperationContext.class);
        messageProcessor.process(event);

        verify(operationExecutor).execute(operationContextCaptor.capture());
        OperationContext operationContext = operationContextCaptor.getValue();

        assertThat(operationContext, is(instanceOf(OperationContextAdapter.class)));
        OperationContextAdapter operationContextAdapter = (OperationContextAdapter) operationContext;

        assertThat(operationContextAdapter.getEvent(), is(sameInstance(event)));
        assertThat(operationContextAdapter.getConfiguration().getValue(), is(sameInstance(configuration)));
    }

    @Test
    public void operationReturnsMuleMessageWichKeepsNoValues() throws Exception
    {
        Object payload = new Object();
        DataType dataType = mock(DataType.class);
        Serializable attributes = mock(Serializable.class);

        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(new DefaultMuleMessage(payload, dataType, attributes));

        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);

        messageProcessor.process(event);

        verify(event).setMessage(captor.capture());
        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(attributes)));
        assertThat(message.getDataType(), is(sameInstance(dataType)));
    }

    @Test
    public void operationReturnsMuleMessageOnTarget() throws Exception
    {
        target = TARGET_VAR;
        messageProcessor = createOperationMessageProcessor();

        Object payload = new Object();
        DataType dataType = mock(DataType.class);
        Serializable attributes = mock(Serializable.class);

        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(new DefaultMuleMessage(payload, dataType, attributes));

        messageProcessor.process(event);

        verify(event, never()).setMessage(any(org.mule.api.MuleMessage.class));
        verify(event.getMessage(), never()).setPayload(any(Object.class));

        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);
        verify(event).setFlowVariable(same(TARGET_VAR), captor.capture());
        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(attributes)));
        assertThat(message.getDataType(), is(sameInstance(dataType)));
    }

    @Test
    public void operationReturnsMuleMessageButKeepsAttributes() throws Exception
    {
        Object payload = new Object();
        DataType dataType = mock(DataType.class);
        Serializable oldAttributes = mock(Serializable.class);

        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(new DefaultMuleMessage(payload, dataType));
        when(event.getMessage()).thenReturn(new DefaultMuleMessage("", mock(DataType.class), oldAttributes));
        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);

        messageProcessor.process(event);

        verify(event).setMessage(captor.capture());
        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(oldAttributes)));
        assertThat(message.getDataType(), is(sameInstance(dataType)));
    }

    @Test
    public void operationReturnsMuleMessageThatOnlySpecifiesPayload() throws Exception
    {
        Object payload = "hello world!";
        Serializable oldAttributes = mock(Serializable.class);

        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(new DefaultMuleMessage(payload));
        when(event.getMessage()).thenReturn(new DefaultMuleMessage("", mock(DataType.class), oldAttributes));
        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);

        messageProcessor.process(event);

        verify(event).setMessage(captor.capture());
        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(oldAttributes)));
        assertThat(message.getDataType().getType().equals(String.class), is(true));
    }

    @Test
    public void operationReturnsMuleMessageWithPayloadAndAttributes() throws Exception
    {
        Object payload = "hello world!";
        Serializable attributes = mock(Serializable.class);

        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(new DefaultMuleMessage(payload, attributes));
        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);

        messageProcessor.process(event);

        verify(event).setMessage(captor.capture());
        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));

        assertThat(message.getPayload(), is(sameInstance(payload)));
        assertThat(message.getAttributes(), is(sameInstance(attributes)));
        assertThat(message.getDataType().getType().equals(String.class), is(true));
    }

    @Test
    public void operationReturnsPayloadValue() throws Exception
    {
        Object value = new Object();
        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(value);

        messageProcessor.process(event);

        ArgumentCaptor<org.mule.api.MuleMessage> captor = ArgumentCaptor.forClass(org.mule.api.MuleMessage.class);
        verify(event).setMessage(captor.capture());

        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));
        assertThat(message.getPayload(), is(sameInstance(value)));
    }

    @Test
    public void operationReturnsPayloadValueWithTarget() throws Exception
    {
        target = TARGET_VAR;
        messageProcessor = createOperationMessageProcessor();

        Object value = new Object();
        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(value);

        messageProcessor.process(event);

        verify(event, never()).setMessage(any(org.mule.api.MuleMessage.class));
        verify(event.getMessage(), never()).setPayload(any(Object.class));

        ArgumentCaptor<DefaultMuleMessage> captor = ArgumentCaptor.forClass(DefaultMuleMessage.class);
        verify(event).setFlowVariable(same(TARGET_VAR), captor.capture());

        MuleMessage message = captor.getValue();
        assertThat(message, is(notNullValue()));
        assertThat(message.getPayload(), is(sameInstance(value)));
    }

    @Test
    public void operationIsVoid() throws Exception
    {
        when(operationModel.getReturnType()).thenReturn(toMetadataType(void.class));
        messageProcessor = createOperationMessageProcessor();

        when(operationExecutor.execute(any(OperationContext.class))).thenReturn(null);
        assertThat(messageProcessor.process(event), is(sameInstance(event)));
        verify(event, never()).setMessage(any(org.mule.api.MuleMessage.class));
    }

    @Test
    public void executesWithDefaultConfig() throws Exception
    {
        configurationName = null;
        messageProcessor = createOperationMessageProcessor();

        Object defaultConfigInstance = new Object();
        when(configurationInstance.getValue()).thenReturn(defaultConfigInstance);
        when(extensionManager.getConfiguration(extensionModel, event)).thenReturn(configurationInstance);

        ArgumentCaptor<OperationContext> operationContextCaptor = ArgumentCaptor.forClass(OperationContext.class);
        messageProcessor.process(event);
        verify(operationExecutor).execute(operationContextCaptor.capture());

        OperationContext operationContext = operationContextCaptor.getValue();

        assertThat(operationContext, is(instanceOf(OperationContextAdapter.class)));
        assertThat(operationContext.getConfiguration().getValue(), is(sameInstance(defaultConfigInstance)));
    }

    @Test
    public void initialise() throws Exception
    {
        verify((MuleContextAware) operationExecutor).setMuleContext(muleContext);
        verify((Initialisable) operationExecutor).initialise();
    }

    @Test
    public void start() throws Exception
    {
        messageProcessor.start();
        verify((Startable) operationExecutor).start();
    }

    @Test
    public void stop() throws Exception
    {
        messageProcessor.stop();
        verify((Stoppable) operationExecutor).stop();
    }

    @Test
    public void dispose() throws Exception
    {
        messageProcessor.dispose();
        verify((Disposable) operationExecutor).dispose();
    }

    @Test
    public void getMetadataKeys() throws Exception
    {
        ParameterModel contentMock = mock(ParameterModel.class);
        when(operationModel.getMetadataKeyParameter()).thenReturn(Optional.of(contentMock));

        Result<List<MetadataKey>> metadataKeys = messageProcessor.getMetadataKeys();

        verify(operationModel).getMetadataResolverFactory();
        verify(metadataResolverFactory).getResolver();

        assertThat(metadataKeys.isSucess(), is(true));
        assertThat(metadataKeys.get().size(), is(2));

        assertThat(metadataKeys.get().get(0).getId(), equalTo(NoConfigMetadataResolver.KeyIds.BOOLEAN.name()));
        assertThat(metadataKeys.get().get(1).getId(), equalTo(NoConfigMetadataResolver.KeyIds.STRING.name()));
    }

    @Test
    public void getContentMetadata() throws Exception
    {
        ParameterModel contentMock = mock(ParameterModel.class);
        when(operationModel.getContentParameter()).thenReturn(Optional.of(contentMock));
        when(operationModel.hasDynamicContentType()).thenReturn(true);

        MetadataKey keyMock = mock(MetadataKey.class);
        when(keyMock.getId()).thenReturn(NoConfigMetadataResolver.KeyIds.STRING.name());

        Result<MetadataType> contentMetadata = messageProcessor.getContentMetadata(keyMock);

        verify(operationModel).getMetadataResolverFactory();
        verify(metadataResolverFactory).getResolver();

        assertThat(contentMetadata.isSucess(), is(true));
        assertThat(contentMetadata.get().getMetadataFormat().getId(), equalTo(StringType.class.getSimpleName()));
    }

    @Test
    public void getOutputMetadata() throws Exception
    {
        MetadataKey keyMock = mock(MetadataKey.class);
        when(keyMock.getId()).thenReturn(NoConfigMetadataResolver.KeyIds.BOOLEAN.name());
        when(operationModel.hasDynamicOutputType()).thenReturn(true);

        Result<MetadataType> outputMetadata = messageProcessor.getOutputMetadata(keyMock);

        verify(operationModel).getMetadataResolverFactory();
        verify(metadataResolverFactory).getResolver();

        assertThat(outputMetadata.isSucess(), is(true));
        assertThat(outputMetadata.get().getMetadataFormat().getId(), equalTo(NoConfigMetadataResolver.BOOLEAN_TYPE_RESULT_ID));
    }

    @Test
    public void getContentMetadataDefaultToJavaType() throws Exception
    {
        ParameterModel contentMock = mock(ParameterModel.class);
        when(operationModel.getContentParameter()).thenReturn(Optional.of(contentMock));
        when(operationModel.hasDynamicContentType()).thenReturn(false);

        MetadataType typeMock = mock(MetadataType.class);
        when(typeMock.getDescription()).thenReturn(Optional.of("mockedType"));
        when(contentMock.getType()).thenReturn(typeMock);

        Result<MetadataType> contentMetadata = messageProcessor.getContentMetadata(mock(MetadataKey.class));
        verify(operationModel, never()).getMetadataResolverFactory();
        verify(metadataResolverFactory, never()).getResolver();

        assertThat(contentMetadata.isSucess(), is(true));
    }

    @Test
    public void getContentMetadataMissingContent() throws Exception
    {
        when(operationModel.getContentParameter()).thenReturn(Optional.empty());

        Result<MetadataType> contentMetadata = messageProcessor.getContentMetadata(mock(MetadataKey.class));
        verify(operationModel, never()).getMetadataResolverFactory();
        verify(metadataResolverFactory, never()).getResolver();

        assertThat(contentMetadata.isSucess(), is(false));
        assertThat(contentMetadata.getFailureType(), is(FailureType.NO_DYNAMIC_TYPE_AVAILABLE));
    }

    @Test
    public void getOutputMetadataDefaultToJavaType() throws Exception
    {
        MetadataKey nullKeyMock = mock(MetadataKey.class);
        when(nullKeyMock.getId()).thenReturn("");

        MetadataType typeMock = mock(MetadataType.class);
        when(typeMock.getDescription()).thenReturn(Optional.of("mockedType"));
        when(operationModel.getReturnType()).thenReturn(typeMock);
        when(operationModel.hasDynamicOutputType()).thenReturn(false);

        Result<MetadataType> outputMetadata = messageProcessor.getOutputMetadata(nullKeyMock);

        assertThat(outputMetadata.isSucess(), is(true));
        assertThat(outputMetadata.get().getDescription(), is(typeMock.getDescription()));
    }


    @Test
    public void getOperationStaticMetadata() throws Exception
    {
        // output mock
        MetadataType typeMock = mock(MetadataType.class);
        when(typeMock.getDescription()).thenReturn(Optional.of("mockedType"));
        when(operationModel.getReturnType()).thenReturn(typeMock);

        // content mock
        ParameterModel contentMock = mock(ParameterModel.class);
        when(contentMock.getType()).thenReturn(typeMock);
        when(operationModel.getParameterModels()).thenReturn(Collections.singletonList(contentMock));

        Result<OperationMetadataDescriptor> metadata = messageProcessor.getMetadata();

        verify(operationModel, never()).getMetadataResolverFactory();
        verify(operationModel, never()).hasDynamicContentType();
        verify(operationModel, never()).hasDynamicOutputType();

        assertThat(metadata.isSucess(), is(true));
        assertThat(metadata.get().getOutputMetadata().getType().getDescription().get(), equalTo("mockedType"));
        assertThat(metadata.get().getParametersMetadata().size(), is(1));
        assertThat(metadata.get().getParametersMetadata().get(0).getType(), is(typeMock));
    }


    @Test
    public void getOperationDynamicMetadata() throws Exception
    {
        MetadataType typeMock = mock(MetadataType.class);
        when(typeMock.getDescription()).thenReturn(Optional.of("mockedType"));
        when(operationModel.getReturnType()).thenReturn(typeMock);
        when(operationModel.hasDynamicOutputType()).thenReturn(false);

        ParameterModel contentMock = mock(ParameterModel.class);
        when(operationModel.getContentParameter()).thenReturn(Optional.of(contentMock));
        when(operationModel.hasDynamicContentType()).thenReturn(true);

        MetadataKey keyMock = mock(MetadataKey.class);
        when(keyMock.getId()).thenReturn(NoConfigMetadataResolver.KeyIds.STRING.name());
        Result<OperationMetadataDescriptor> metadata = messageProcessor.getMetadata(keyMock);

        assertThat(metadata.isSucess(), is(true));
        assertThat(metadata.get().getOutputMetadata().getType(), is(typeMock));

        assertThat(metadata.get().getParametersMetadata().size(), is(1));
        ParameterMetadataDescriptor content = metadata.get().getParametersMetadata().get(0);
        assertThat(content.getType().getMetadataFormat().getId(), equalTo(StringType.class.getSimpleName()));
    }

    private OperationMessageProcessor createOperationMessageProcessor() throws Exception
    {
        OperationMessageProcessor messageProcessor = new OperationMessageProcessor(extensionModel, operationModel, configurationName, target, resolverSet, extensionManager);
        messageProcessor.setMuleContext(muleContext);
        muleContext.getInjector().inject(connectionManager);
        messageProcessor.initialise();
        return messageProcessor;
    }

    private MuleEvent configureMockEvent(MuleEvent mockEvent)
    {
        when(mockEvent.getMessage().getEncoding()).thenReturn(Charset.defaultCharset().name());
        return mockEvent;
    }
}
