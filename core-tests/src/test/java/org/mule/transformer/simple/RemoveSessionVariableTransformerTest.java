/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.metadata.SimpleDataType;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;

@SmallTest
public class RemoveSessionVariableTransformerTest extends AbstractMuleTestCase
{
    public static final String ENCODING = "encoding";
    public static final String PLAIN_STRING_KEY = "someText";
    public static final String PLAIN_STRING_VALUE = "someValue";
    public static final String EXPRESSION = "#[string:someValue]";
    public static final String EXPRESSION_VALUE = "expressionValueResult";
    public static final String NULL_EXPRESSION = "#[string:someValueNull]";
    public static final String NULL_EXPRESSION_VALUE = null;

    private MuleEvent mockEvent = mock(MuleEvent.class);
    private MuleMessage mockMessage = mock(MuleMessage.class, RETURNS_DEEP_STUBS);
    private MuleSession mockSession = mock(MuleSession.class, RETURNS_DEEP_STUBS);
    private MuleContext mockMuleContext = mock(MuleContext.class);
    private ExpressionManager mockExpressionManager = mock(ExpressionManager.class);
    private RemoveSessionVariableTransformer removeSessionVariableTransformer = new RemoveSessionVariableTransformer();

    @Before
    public void setUpTest()
    {
        when(mockEvent.getMessage()).thenReturn(mockMessage);
        when(mockEvent.getSession()).thenReturn(mockSession);
        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
        when(mockExpressionManager.parse(anyString(), Mockito.any(MuleEvent.class))).thenAnswer(
                invocation -> invocation.getArguments()[0]);
        when(mockExpressionManager.evaluate(EXPRESSION, mockEvent)).thenReturn(EXPRESSION_VALUE);
        removeSessionVariableTransformer.setMuleContext(mockMuleContext);
        when(mockMessage.getDataType()).thenReturn(new SimpleDataType(String.class));
    }

    @Test
    public void testRemoveVariable() throws InitialisationException, TransformerException
    {
        removeSessionVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        removeSessionVariableTransformer.initialise();
        removeSessionVariableTransformer.transform(mockEvent, ENCODING);
        verify(mockSession).removeProperty(PLAIN_STRING_KEY);
    }

    @Test
    public void testRemoveVariableUsingExpression() throws InitialisationException, TransformerException
    {
        removeSessionVariableTransformer.setIdentifier(EXPRESSION);
        removeSessionVariableTransformer.initialise();
        removeSessionVariableTransformer.transform(mockEvent, ENCODING);
        verify(mockSession).removeProperty(EXPRESSION_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveVariableNullKey() throws InitialisationException, TransformerException
    {
        removeSessionVariableTransformer.setIdentifier(null);
    }

    @Test //Don't fail.
    public void testRemoveVariableExpressionKeyNullValue() throws InitialisationException, TransformerException
    {
        removeSessionVariableTransformer.setIdentifier(NULL_EXPRESSION);
        removeSessionVariableTransformer.initialise();
        removeSessionVariableTransformer.transform(mockMessage, ENCODING);
    }

    @Test
    @Ignore
    public void testRemoveVariableWithRegexExpression() throws InitialisationException, TransformerException
    {
        when(mockEvent.getFlowVariableNames()).thenReturn(new HashSet<String>(Arrays.asList("MULE_ID","MULE_CORRELATION_ID","SomeVar","MULE_GROUP_ID")));
        removeSessionVariableTransformer.setIdentifier("MULE_(.*)");
        removeSessionVariableTransformer.initialise();
        removeSessionVariableTransformer.transform(mockMessage, ENCODING);
        verify(mockSession).removeProperty("MULE_ID");
        verify(mockSession).removeProperty("MULE_CORRELATION_ID");
        verify(mockSession).removeProperty("MULE_GROUP_ID");
        verify(mockSession, VerificationModeFactory.times(0)).removeProperty("SomeVar");
    }
}
