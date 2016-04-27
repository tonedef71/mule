/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

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
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.MimeTypes;

import javax.activation.MimeTypeParseException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class AbstractReassignableMessageTransformerTestCase {
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

  private AbstractReassignableMessageTransformer reassignableMessageTransformer;

  @Before
  public void setUpTest() throws MimeTypeParseException
  {
    this.reassignableMessageTransformer = new AbstractReassignableMessageTransformer() {
      @Override
      public Object transformReassignableMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        return message;
      }
    };
    this.reassignableMessageTransformer.setEncoding(null);
    this.reassignableMessageTransformer.setMimeType(null);
    this.reassignableMessageTransformer.setMuleContext(mockMuleContext);

    when(mockMessage.getInvocationProperty(EMPTY_STRING_KEY)).thenReturn(mockMessage);
    when(mockMessage.getInvocationProperty(PLAIN_STRING_KEY)).thenReturn(PLAIN_STRING_VALUE);

    when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
  }

  @Test(expected = TransformerException.class)
  public void testMessageVariableNameThatDoesNotReferenceMuleMessage() throws Exception
  {
    mockMessage.setInvocationProperty(PLAIN_STRING_KEY, PLAIN_STRING_VALUE, DataType.STRING_DATA_TYPE);
    verify(mockMessage).setInvocationProperty(argThat(equalTo(PLAIN_STRING_KEY)), argThat(equalTo(PLAIN_STRING_VALUE)), dataTypeCaptor.capture());
    assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, null));

    this.reassignableMessageTransformer.setMessageVariableName(PLAIN_STRING_KEY);
    this.reassignableMessageTransformer.setEncoding(CUSTOM_ENCODING);
    this.reassignableMessageTransformer.setMimeType(MimeTypes.TEXT);
    this.reassignableMessageTransformer.initialise();
    this.reassignableMessageTransformer.transform(mockMessage, ENCODING);
  }
}
