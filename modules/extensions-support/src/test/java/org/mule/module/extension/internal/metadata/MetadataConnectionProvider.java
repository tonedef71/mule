/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata;

import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionValidationResult;

public class MetadataConnectionProvider implements ConnectionProvider<MetadataExtension, MetadataConnection>
{

    @Override
    public MetadataConnection connect(MetadataExtension metadataExtension) throws ConnectionException
    {
        return new MetadataConnection();
    }

    @Override
    public void disconnect(MetadataConnection metadataConnection)
    {

    }

    @Override
    public ConnectionValidationResult validate(MetadataConnection metadataConnection)
    {
        return ConnectionValidationResult.success();
    }

    @Override
    public ConnectionHandlingStrategy<MetadataConnection> getHandlingStrategy(ConnectionHandlingStrategyFactory<MetadataExtension, MetadataConnection> handlingStrategyFactory)
    {
        return handlingStrategyFactory.cached();
    }
}
