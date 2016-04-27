/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.AttributeEvaluator;

/**
 *
 */
public abstract class AbstractReassignableMessageTransformer extends AbstractMessageTransformer
{
  private AttributeEvaluator messageVariableNameSourceEvaluator;

  public AbstractReassignableMessageTransformer() {
    super();
    registerSourceType(DataTypeFactory.OBJECT);
    setReturnDataType(DataTypeFactory.OBJECT);
  }

  @Override
  public void initialise() throws InitialisationException
  {
    super.initialise();
    this.messageVariableNameSourceEvaluator.initialize(muleContext.getExpressionManager());
  }

  /**
   * Transform the message
   */
  public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
  {
    MuleMessage src = message;

    final Object messageVariableValue = (null != this.messageVariableNameSourceEvaluator) ? this.messageVariableNameSourceEvaluator.resolveValue(message) : null;
    final String messageVariableName = (null != messageVariableValue) ? messageVariableValue.toString() : null;
    if (StringUtils.isNotBlank(messageVariableName))
    {
      final Object value = message.getInvocationProperty(messageVariableName);
      if (MuleMessage.class.isAssignableFrom(value.getClass()))
      {
        src = (MuleMessage)value;
      }
      else {
        final Throwable cause = new IllegalArgumentException(String.format("Value of 'messageVariableName' [%s] does not reference a MuleMessage", messageVariableName));
        throw new TransformerException(this, cause);
      }
    }

    final Object retVal = this.transformReassignableMessage(src, outputEncoding);
    return retVal;
  }

  @Override
  public Object clone() throws CloneNotSupportedException
  {
    final AbstractReassignableMessageTransformer clone = (AbstractReassignableMessageTransformer) super.clone();
    clone.setMessageVariableName(this.messageVariableNameSourceEvaluator.getRawValue());
    return clone;
  }

  public void setMessageVariableName(String messageVariableName)
  {
    this.messageVariableNameSourceEvaluator = new AttributeEvaluator(messageVariableName);
  }

  public abstract Object transformReassignableMessage(MuleMessage message, String outputEncoding) throws TransformerException;
}
