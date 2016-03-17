/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata;

import org.mule.extension.api.annotation.metadata.MetadataScope;
import org.mule.extension.api.annotation.param.Connection;
import org.mule.extension.api.annotation.param.metadata.Content;
import org.mule.extension.api.annotation.param.metadata.MetadataKeyParam;
import org.mule.module.extension.internal.metadata.resolver.ContentResolverWithoutKeyResolver;
import org.mule.module.extension.internal.metadata.resolver.MetadataResolverThrowsResolvingException;
import org.mule.module.extension.internal.metadata.resolver.MetadataResolverThrowsRuntimeException;

public class MetadataFailureOperations
{

    // MetadataResolver throws MetadataResolvingException
    @MetadataScope(MetadataResolverThrowsResolvingException.class)
    public Object failWithResolvingException(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    // With keys resolver and without KeyParam
    @MetadataScope(ContentResolverWithoutKeyResolver.class)
    public void keyParamWithoutKeyResolver(@Connection MetadataConnection connection, @MetadataKeyParam String type)
    {
        // Nothing to do here
    }

    // Resolver for content and output type
    // With keys and KeyParam
    @MetadataScope(MetadataResolverThrowsRuntimeException.class)
    public Object failWithRuntimeException(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

}
