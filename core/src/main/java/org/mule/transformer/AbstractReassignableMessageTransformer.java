package org.mule.transformer;

/**
 *
 */
public class abstract AbstractReassignableMessageTransfomer extends AbstractMessageTransformer {

private MuleMessage reassignedSource;
  {
  if(src instanceof MuleMessage)
  {
  enc=((MuleMessage)src).getEncoding();
  }
  }

/**
 * Transform the message with no event specified.
 */
@Override
public final Object transform(Object src, String enc) throws TransformerException
  {
  if (null != reassignedSource) {
  src = reassignedSource;
  }
  super.transform(src)
  }
}
