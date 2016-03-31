/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.util.AttributeEvaluator;

/**
 *
 */
public abstract class AbstractReassignableMessageTransformer extends AbstractMessageTransformer
{
  private AttributeEvaluator messageVariableNameSourceEvaluator;

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

    final String messageVariableName = this.messageVariableNameSourceEvaluator.resolveValue(message).toString();
    if (null != messageVariableName && !messageVariableName.trim().isEmpty())
    {
      final Object value = message.getInvocationProperty(messageVariableName);
      if (value instanceof MuleMessage)
      {
        src = (MuleMessage)value;
      }
    }

    Object retVal = this.transformReassignableMessage(src, outputEncoding);
    return retVal;
  }

  @Override
  public Object clone() throws CloneNotSupportedException
  {
    final AbstractReassignableMessageTransformer clone = (AbstractReassignableMessageTransformer) super.clone();
    clone.setName(this.messageVariableNameSourceEvaluator.getRawValue());
    return clone;
  }

  public void setMessageVariableName(String messageVariableName)
  {
    this.messageVariableNameSourceEvaluator = new AttributeEvaluator(messageVariableName);
  }

  public abstract Object transformReassignableMessage(MuleMessage message, String outputEncoding) throws TransformerException;
}
