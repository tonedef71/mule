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
import org.mule.module.extension.internal.metadata.resolver.ContentAndOutputResolverWithKeyResolver;
import org.mule.module.extension.internal.metadata.resolver.ContentAndOutputResolverWithoutKeyResolver;
import org.mule.module.extension.internal.metadata.resolver.ContentResolverWithKeyResolver;
import org.mule.module.extension.internal.metadata.resolver.ContentResolverWithoutKeyResolver;
import org.mule.module.extension.internal.metadata.resolver.OutputResolverWithKeyResolver;
import org.mule.module.extension.internal.metadata.resolver.OutputResolverWithoutKeyResolver;

public class MetadataOperations
{
    // Resolver for content only, ignores Object return type
    // With keys and KeyParam
    @MetadataScope(ContentResolverWithKeyResolver.class)
    public Object contentMetadataWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    // Resolver for output only, ignores @Content Object
    // With keys and KeyParam
    @MetadataScope(OutputResolverWithKeyResolver.class)
    public Object outputMetadataWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    // Resolver for content and output type
    // With keys and KeyParam
    @MetadataScope(ContentAndOutputResolverWithKeyResolver.class)
    public Object contentAndOutputMetadataWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        return null;
    }

    // Resolver for content and output type, no @Content param, resolves only output
    // With keys and KeyParam
    @MetadataScope(ContentAndOutputResolverWithKeyResolver.class)
    public Object outputOnlyWithoutContentParam(@Connection MetadataConnection connection, @MetadataKeyParam String type)
    {
        return null;
    }

    // Resolver for content and output type, no return type, resolves only @Content
    // With key and KeyParam
    @MetadataScope(ContentAndOutputResolverWithKeyResolver.class)
    public void contentOnlyIgnoresOutput(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        // Nothing to do here
    }


    // Resolver for content only, ignores Object return type
    // Without keys and KeyParam
    @MetadataScope(ContentResolverWithoutKeyResolver.class)
    public Object contentMetadataWithoutKeyParam(@Connection MetadataConnection connection, @Content Object content)
    {
        return null;
    }

    // Resolver for output only, ignores @Content Object
    // Without keys and KeyParam
    @MetadataScope(OutputResolverWithoutKeyResolver.class)
    public Object outputMetadataWithoutKeyParam(@Connection MetadataConnection connection, @Content Object content)
    {
        return null;
    }

    // Resolver for content and output types
    // Without keys and KeyParam
    @MetadataScope(ContentAndOutputResolverWithoutKeyResolver.class)
    public Object contentAndOutputMetadataWithoutKeyParam(@Connection MetadataConnection connection, @Content Object content)
    {
        return null;
    }

    // Resolver for content only
    // Without keys
    // With KeyParam
    @MetadataScope(ContentResolverWithoutKeyResolver.class)
    public void contentMetadataWithoutKeysWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
        // Nothing to do here
    }

    // Resolver for output only
    // Without keys
    // With KeyParam
    @MetadataScope(OutputResolverWithoutKeyResolver.class)
    public Object outputMetadataWithoutKeysWithKeyParam(@Connection MetadataConnection connection, @MetadataKeyParam String type)
    {
        return null;
    }
}