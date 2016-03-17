/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata.resolver;

import org.mule.api.metadata.FailureType;
import org.mule.api.metadata.MetadataKey;
import org.mule.extension.api.metadata.MetadataContext;
import org.mule.extension.api.metadata.MetadataResolver;
import org.mule.extension.api.metadata.MetadataResolvingException;
import org.mule.metadata.api.model.MetadataType;

import java.util.List;

public class MetadataResolverThrowsResolvingException implements MetadataResolver
{

    public List<MetadataKey> getMetadataKeys(MetadataContext context) throws MetadataResolvingException
    {
        throw new MetadataResolvingException("Failing keys retriever", FailureType.CONNECTION_FAILURE);
    }

    public MetadataType getContentMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException
    {
        throw new MetadataResolvingException("Failing keys retriever", FailureType.CONNECTION_FAILURE);
    }

    public MetadataType getOutputMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException
    {
        throw new MetadataResolvingException("Failing keys retriever", FailureType.CONNECTION_FAILURE);
    }

}
