/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.transformer.types.MimeTypes.APPLICATION_XML;
import org.mule.PropertyScope;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.metadata.DataType;
import org.mule.api.metadata.SimpleDataType;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.MimeTypes;
import org.mule.transformer.types.TypedValue;

import javax.activation.MimeTypeParseException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class AddPropertyTransformerTestCase extends AbstractMuleTestCase
{
    public static final String ENCODING = "encoding";
    public static final String PLAIN_STRING_KEY = "someText";
    public static final String PLAIN_STRING_VALUE = "someValue";
    public static final String EXPRESSION = "#[string:someValue]";
    public static final String EXPRESSION_VALUE = "expressionValueResult";
    public static final String NULL_EXPRESSION = "#[string:someValueNull]";
    public static final String CUSTOM_ENCODING = UTF_8.name();

    private MuleEvent mockEvent = mock(MuleEvent.class);
    private MuleMessage mockMessage = mock(MuleMessage.class, RETURNS_DEEP_STUBS);
    private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    private ExpressionManager mockExpressionManager = mock(ExpressionManager.class);
    private AbstractAddVariablePropertyTransformer addPropertyTransformer = new AddPropertyTransformer();
    private PropertyScope scope = PropertyScope.OUTBOUND;
    private final ArgumentCaptor<DataType> dataTypeCaptor = ArgumentCaptor.forClass(DataType.class);

    @Before
    public void setUpTest() throws MimeTypeParseException
    {
        addPropertyTransformer.setEncoding(null);
        addPropertyTransformer.setMimeType(null);

        when(mockEvent.getMessage()).thenReturn(mockMessage);
        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
        when(mockExpressionManager.parse(anyString(), Mockito.any(MuleEvent.class))).thenAnswer(
            new Answer<String>()
            {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable
                {

                    return (String) invocation.getArguments()[0];
                }
            });
        when(mockExpressionManager.evaluate(EXPRESSION, mockEvent)).thenReturn(EXPRESSION_VALUE);
        TypedValue typedValue = new TypedValue(EXPRESSION_VALUE, DataTypeFactory.STRING);
        when(mockExpressionManager.evaluateTyped(EXPRESSION, mockEvent)).thenReturn(typedValue);
        addPropertyTransformer.setMuleContext(mockMuleContext);
        when(mockMessage.getDataType()).thenReturn(new SimpleDataType(String.class));
    }

    @Test
    public void testAddVariable() throws InitialisationException, TransformerException
    {
        addPropertyTransformer.setIdentifier(PLAIN_STRING_KEY);
        addPropertyTransformer.setValue(PLAIN_STRING_VALUE);
        addPropertyTransformer.initialise();
        addPropertyTransformer.transform(mockEvent, ENCODING);

        verify(mockMessage).setOutboundProperty(argThat(equalTo(PLAIN_STRING_KEY)), argThat(equalTo(PLAIN_STRING_VALUE)), dataTypeCaptor.capture());
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void testAddVariableWithExpressionValue() throws InitialisationException, TransformerException
    {
        addPropertyTransformer.setIdentifier(PLAIN_STRING_KEY);
        addPropertyTransformer.setValue(EXPRESSION);
        addPropertyTransformer.initialise();
        addPropertyTransformer.transform(mockEvent, ENCODING);

        verify(mockMessage).setOutboundProperty(argThat(equalTo(PLAIN_STRING_KEY)), argThat(equalTo(EXPRESSION_VALUE)), dataTypeCaptor.capture());
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void testAddVariableWithExpressionKey() throws InitialisationException, TransformerException
    {
        addPropertyTransformer.setIdentifier(EXPRESSION);
        addPropertyTransformer.setValue(PLAIN_STRING_VALUE);
        addPropertyTransformer.initialise();
        addPropertyTransformer.transform(mockEvent, ENCODING);

        verify(mockMessage).setOutboundProperty(argThat(equalTo(EXPRESSION_VALUE)), argThat(equalTo(PLAIN_STRING_VALUE)), dataTypeCaptor.capture());
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void testAddVariableWithEncoding() throws InitialisationException, TransformerException
    {
        addPropertyTransformer.setIdentifier(PLAIN_STRING_KEY);
        addPropertyTransformer.setValue(PLAIN_STRING_VALUE);
        addPropertyTransformer.initialise();
        addPropertyTransformer.setEncoding(CUSTOM_ENCODING);
        addPropertyTransformer.transform(mockEvent, ENCODING);

        verify(mockMessage).setOutboundProperty(argThat(equalTo(PLAIN_STRING_KEY)), argThat(equalTo(PLAIN_STRING_VALUE)), dataTypeCaptor.capture());
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, CUSTOM_ENCODING));
    }

    @Test
    public void testAddVariableWithMimeType() throws InitialisationException, TransformerException, MimeTypeParseException
    {
        addPropertyTransformer.setIdentifier(PLAIN_STRING_KEY);
        addPropertyTransformer.setValue(PLAIN_STRING_VALUE);
        addPropertyTransformer.initialise();
        addPropertyTransformer.setMimeType(APPLICATION_XML);
        addPropertyTransformer.transform(mockEvent, ENCODING);

        verify(mockMessage).setOutboundProperty(argThat(equalTo(PLAIN_STRING_KEY)), argThat(equalTo(PLAIN_STRING_VALUE)), dataTypeCaptor.capture());
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, APPLICATION_XML, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddVariableWithNullKey() throws InitialisationException, TransformerException
    {
        addPropertyTransformer.setIdentifier(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddVariableWithEmptyKey() throws InitialisationException, TransformerException
    {
        addPropertyTransformer.setIdentifier("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddVariableWithNullValue() throws InitialisationException, TransformerException
    {
        addPropertyTransformer.setValue(null);
    }

    @Test
    public void testAddVariableWithNullExpressionKeyResult()
        throws InitialisationException, TransformerException
    {
        addPropertyTransformer.setIdentifier(NULL_EXPRESSION);
        addPropertyTransformer.setValue(PLAIN_STRING_VALUE);
        addPropertyTransformer.initialise();
        addPropertyTransformer.transform(mockEvent, ENCODING);
        verify(mockMessage, VerificationModeFactory.times(0)).setProperty((String) isNull(), anyString(),
            Matchers.<PropertyScope> anyObject());
    }

    @Test
    public void testAddVariableWithNullExpressionValueResult()
        throws InitialisationException, TransformerException
    {
        addPropertyTransformer.setIdentifier(PLAIN_STRING_KEY);
        TypedValue typedValue = new TypedValue(null, DataType.OBJECT_DATA_TYPE);
        when(mockExpressionManager.evaluateTyped(NULL_EXPRESSION, mockEvent)).thenReturn(typedValue);
        addPropertyTransformer.setValue(NULL_EXPRESSION);
        addPropertyTransformer.initialise();
        addPropertyTransformer.transform(mockEvent, ENCODING);
        verify(mockMessage, VerificationModeFactory.times(1)).removeProperty(PLAIN_STRING_KEY, scope);
    }

    @Test
    public void testAddVariableWithNullPayloadExpressionValueResult()
            throws InitialisationException, TransformerException
    {
        addPropertyTransformer.setIdentifier(PLAIN_STRING_KEY);
        addPropertyTransformer.setValue(EXPRESSION);
        TypedValue typedValue = new TypedValue(null, DataType.OBJECT_DATA_TYPE);
        when(mockExpressionManager.evaluateTyped(EXPRESSION, mockEvent)).thenReturn(typedValue);
        addPropertyTransformer.initialise();

        addPropertyTransformer.transform(mockEvent, ENCODING);

        verify(mockMessage, VerificationModeFactory.times(1)).removeProperty(PLAIN_STRING_KEY, scope);
    }
}
