/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.MimeTypes;

import javax.activation.MimeTypeParseException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class AddAttachmentTransformerTestCase extends AbstractMuleTestCase
{
    public static final String ENCODING = "encoding";
    public static final String EMPTY_STRING_KEY = "";
    public static final String PLAIN_STRING_KEY = "someText";
    public static final String PLAIN_STRING_VALUE = "someValue";
    public static final String EXPRESSION = "#[string:someValue]";
    public static final String EXPRESSION_VALUE = "expressionValueResult";
    public static final String NULL_EXPRESSION = "#[string:someValueNull]";
    public static final String CUSTOM_ENCODING = UTF_8.name();
    public static final String TEST_ATTACHMENT = "testAttachment";

    private final ArgumentCaptor<DataType> dataTypeCaptor = ArgumentCaptor.forClass(DataType.class);

    @Mock
    private MuleEvent mockEvent;
    @Mock
    private MuleContext mockMuleContext;
    @Mock
    private MuleMessage mockMessage;
    @Mock
    private MuleMessage mockOuterMessage;
    @Mock
    private ExpressionManager mockExpressionManager;

    private AddAttachmentTransformer addAttachmentTransformer;

    @Before
    public void setUpTest() throws MimeTypeParseException
    {
        this.addAttachmentTransformer = new AddAttachmentTransformer();
        this.addAttachmentTransformer.setEncoding(null);
        this.addAttachmentTransformer.setMimeType(null);
        this.addAttachmentTransformer.setMuleContext(mockMuleContext);

        when(mockMessage.getInvocationProperty(EMPTY_STRING_KEY)).thenReturn(mockMessage);
        when(mockMessage.getInvocationProperty(PLAIN_STRING_KEY)).thenReturn(mockOuterMessage);

        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    }

    @Test
    public void testAddAttachmentWithMessageVariableName() throws Exception, InitialisationException, TransformerException, MimeTypeParseException
    {
        mockMessage.setInvocationProperty(PLAIN_STRING_KEY, mockOuterMessage, DataType.OBJECT_DATA_TYPE);
        verify(mockMessage).setInvocationProperty(argThat(equalTo(PLAIN_STRING_KEY)), argThat(equalTo(mockOuterMessage)), dataTypeCaptor.capture());
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(Object.class, MimeTypes.ANY, null));

        this.addAttachmentTransformer.setAttachmentName(TEST_ATTACHMENT);
        this.addAttachmentTransformer.setValue(PLAIN_STRING_VALUE);
        this.addAttachmentTransformer.setContentType(MimeTypes.TEXT);
        this.addAttachmentTransformer.setMessageVariableName(PLAIN_STRING_KEY);
        this.addAttachmentTransformer.setEncoding(CUSTOM_ENCODING);
        this.addAttachmentTransformer.setMimeType(MimeTypes.TEXT);

//        final TypedValue typedValue = new TypedValue(null, DataType.OBJECT_DATA_TYPE);
//        when(mockExpressionManager.evaluateTyped(EXPRESSION, mockMessage)).thenReturn(typedValue);

        this.addAttachmentTransformer.initialise();
        this.addAttachmentTransformer.transform(mockMessage, ENCODING);

        verify(mockMessage, never()).addOutboundAttachment(argThat(equalTo(TEST_ATTACHMENT)), argThat(equalTo(PLAIN_STRING_VALUE)), argThat(equalTo(MimeTypes.TEXT)));
        verify(mockOuterMessage).addOutboundAttachment(argThat(equalTo(TEST_ATTACHMENT)), argThat(equalTo(PLAIN_STRING_VALUE)), argThat(equalTo(MimeTypes.TEXT)));
    }

    @Test
    public void testAddAttachmentWithEmptyMessageVariableName() throws Exception, InitialisationException, TransformerException, MimeTypeParseException
    {
        this.addAttachmentTransformer.setAttachmentName(TEST_ATTACHMENT);
        this.addAttachmentTransformer.setValue(PLAIN_STRING_VALUE);
        this.addAttachmentTransformer.setContentType(MimeTypes.TEXT);
        this.addAttachmentTransformer.setMessageVariableName(EMPTY_STRING_KEY);
        this.addAttachmentTransformer.setEncoding(CUSTOM_ENCODING);
        this.addAttachmentTransformer.setMimeType(MimeTypes.TEXT);

//        final TypedValue typedValue = new TypedValue(null, DataType.OBJECT_DATA_TYPE);
//        when(mockExpressionManager.evaluateTyped(EXPRESSION, mockMessage)).thenReturn(typedValue);

        this.addAttachmentTransformer.initialise();
        this.addAttachmentTransformer.transform(mockMessage, ENCODING);

        verify(mockMessage).addOutboundAttachment(argThat(equalTo(TEST_ATTACHMENT)), argThat(equalTo(PLAIN_STRING_VALUE)), argThat(equalTo(MimeTypes.TEXT)));
        verify(mockOuterMessage, never()).addOutboundAttachment(argThat(equalTo(TEST_ATTACHMENT)), argThat(equalTo(PLAIN_STRING_VALUE)), argThat(equalTo(MimeTypes.TEXT)));
    }

    @Test
    public void testAddAttachmentWithNullMessageVariableName() throws Exception, InitialisationException, TransformerException, MimeTypeParseException
    {
        this.addAttachmentTransformer.setAttachmentName(TEST_ATTACHMENT);
        this.addAttachmentTransformer.setValue(PLAIN_STRING_VALUE);
        this.addAttachmentTransformer.setContentType(MimeTypes.TEXT);
        this.addAttachmentTransformer.setMessageVariableName(null);
        this.addAttachmentTransformer.setEncoding(CUSTOM_ENCODING);
        this.addAttachmentTransformer.setMimeType(MimeTypes.TEXT);

//        final TypedValue typedValue = new TypedValue(null, DataType.OBJECT_DATA_TYPE);
//        when(mockExpressionManager.evaluateTyped(EXPRESSION, mockMessage)).thenReturn(typedValue);

        this.addAttachmentTransformer.initialise();
        this.addAttachmentTransformer.transform(mockMessage, ENCODING);

        verify(mockMessage).addOutboundAttachment(argThat(equalTo(TEST_ATTACHMENT)), argThat(equalTo(PLAIN_STRING_VALUE)), argThat(equalTo(MimeTypes.TEXT)));
        verify(mockOuterMessage, never()).addOutboundAttachment(argThat(equalTo(TEST_ATTACHMENT)), argThat(equalTo(PLAIN_STRING_VALUE)), argThat(equalTo(MimeTypes.TEXT)));
    }
}
