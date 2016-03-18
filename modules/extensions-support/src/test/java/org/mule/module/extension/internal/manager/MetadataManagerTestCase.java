/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.metadata.FailureType;
import org.mule.api.metadata.MetadataAware;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.MuleMetadataManager;
import org.mule.api.metadata.ProcessorId;
import org.mule.api.metadata.Result;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.MuleRegistry;
import org.mule.construct.Flow;
import org.mule.metadata.api.model.MetadataType;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MetadataManagerTestCase
{
    private final String FLOW_NAME = "testFlow";
    private final String PROCESSOR_INDEX = "0";
    private final ProcessorId processorId = new ProcessorId(FLOW_NAME, PROCESSOR_INDEX);

    @Mock
    private MuleContext muleContext;

    @Mock
    private MuleEvent event;

    @Mock
    private MuleRegistry muleRegistry;

    @Mock(extraInterfaces = FlowConstruct.class)
    private Flow flow;

    @Mock(extraInterfaces = MessageProcessor.class)
    private MetadataAware messageProcessor;

    @Mock
    private Result successResult;

    @Mock
    private Result failureResult;

    private MuleMetadataManager metadataManager;

    @Before
    public void setupMock()
    {
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        when(muleRegistry.lookupFlowConstruct(FLOW_NAME)).thenReturn(flow);
        when(flow.getMessageProcessors()).thenReturn(Arrays.<MessageProcessor>asList((MessageProcessor)messageProcessor));

        when(successResult.isSucess()).thenReturn(true);
        when(successResult.getFailureType()).thenReturn(FailureType.NONE);

        when(failureResult.isSucess()).thenReturn(false);
        when(failureResult.getFailureType()).thenReturn(FailureType.UNKNOWN);

        metadataManager = new MuleMetadataManager();
        metadataManager.setMuleContext(muleContext);
    }

    @Test
    public void getMetadataKeys() throws Exception
    {
        when(successResult.get()).thenReturn(Arrays.asList(mock(MetadataKey.class)));

        when(messageProcessor.getMetadataKeys()).thenReturn(successResult);

        MuleMetadataManager metadataManager = new MuleMetadataManager();
        metadataManager.setMuleContext(muleContext);

        Result<List<MetadataKey>> keys = metadataManager.getMetadataKeys(new ProcessorId(FLOW_NAME, PROCESSOR_INDEX));

        verifyFoundMessageProcessor();
        verify(messageProcessor).getMetadataKeys();
        assertThat(keys.isSucess(), is(true));
        assertThat(keys.get().size(), is(1));
    }

    @Test
    public void getContentMetadata() throws Exception
    {
        MetadataKey keyMock = mock(MetadataKey.class);
        MetadataType typeMock = mock(MetadataType.class);

        when(successResult.get()).thenReturn(typeMock);

        when(messageProcessor.getContentMetadata(keyMock)).thenReturn(successResult);
        Result<MetadataType> type = metadataManager.getContentMetadata(processorId, keyMock);

        verifyFoundMessageProcessor();
        verify(messageProcessor).getContentMetadata(keyMock);
        assertThat(type.isSucess(), is(true));
        assertThat(type.get(), is(typeMock));
    }

    @Test
    public void getOutputMetadata() throws Exception
    {
        MetadataKey keyMock = mock(MetadataKey.class);
        MetadataType typeMock = mock(MetadataType.class);

        when(successResult.get()).thenReturn(typeMock);

        when(messageProcessor.getOutputMetadata(keyMock)).thenReturn(successResult);
        Result<MetadataType> type = metadataManager.getOutputMetadata(processorId, keyMock);

        verifyFoundMessageProcessor();
        verify(messageProcessor).getOutputMetadata(keyMock);
        assertThat(type.isSucess(), is(true));
        assertThat(type.get(), is(typeMock));
    }

    @Test
    public void flowIsNotMetadataAware() throws Exception
    {
        MessageProcessor staticMessageProcessor = mock(MessageProcessor.class);
        when(flow.getMessageProcessors()).thenReturn(Arrays.<MessageProcessor>asList(staticMessageProcessor));

        MetadataKey keyMock = mock(MetadataKey.class);

        MuleMetadataManager metadataManager = new MuleMetadataManager();
        metadataManager.setMuleContext(muleContext);

        Result<List<MetadataKey>> keys = metadataManager.getMetadataKeys(processorId);
        Result<MetadataType> contentType = metadataManager.getContentMetadata(processorId, keyMock);
        Result<MetadataType> outputType = metadataManager.getOutputMetadata(processorId, keyMock);

        assertThat(keys.isSucess(), is(false));
        assertThat(contentType.isSucess(), is(false));
        assertThat(outputType.isSucess(), is(false));

        assertThat(outputType.getFailureType().getName(), is(FailureType.RESOURCE_UNAVAILABLE.getName()));
    }

    private void verifyFoundMessageProcessor() throws MuleException
    {
        verify(muleContext).getRegistry();
        verify(muleRegistry).lookupFlowConstruct(FLOW_NAME);
        verify(flow).getMessageProcessors();
    }
}
