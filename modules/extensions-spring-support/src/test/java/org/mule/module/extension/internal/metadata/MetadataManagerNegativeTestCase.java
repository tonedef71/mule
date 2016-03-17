/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata;

import org.mule.api.metadata.FailureType;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.ProcessorId;
import org.mule.api.metadata.Result;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.extension.api.metadata.MetadataResolvingException;
import org.mule.metadata.api.model.MetadataType;

import java.util.List;

import org.junit.Test;

public class MetadataManagerNegativeTestCase extends MetadataManagerTestCase
{
    private static final String FAIL_WITH_RESOLVING_EXCEPTION = "failWithResolvingException";
    private static final String FAIL_WITH_RUNTIME_EXCEPTION = "failWithRuntimeException";
    private static final String KEY_PARAM_WITHOUT_KEY_RESOLVER = "keyParamWithoutKeyResolver";

    @Test
    public void getContentMetadataWhenNoContentParam() throws Exception
    {
        processorId = new ProcessorId(OUTPUT_ONLY_WITHOUT_CONTENT_PARAM, FIRST_PROCESSOR_INDEX);
        final Result<MetadataType> metadata = metadataManager.getContentMetadata(processorId, personKey);

        assertFailure(metadata, "", FailureType.NO_DYNAMIC_TYPE_AVAILABLE, "");
    }

    @Test
    public void getOutputMetadataWhenVoid() throws Exception
    {
        processorId = new ProcessorId(CONTENT_ONLY_IGNORES_OUTPUT, FIRST_PROCESSOR_INDEX);
        final Result<MetadataType> metadata = metadataManager.getOutputMetadata(processorId, personKey);

        assertFailure(metadata, "", FailureType.NO_DYNAMIC_TYPE_AVAILABLE, "");
    }

    @Test
    public void getDynamicKeysWithoutKeyResolver() throws Exception
    {
        processorId = new ProcessorId(KEY_PARAM_WITHOUT_KEY_RESOLVER, FIRST_PROCESSOR_INDEX);
        final Result<List<MetadataKey>> keys = metadataManager.getMetadataKeys(processorId);

        assertFailure(keys, "", FailureType.NO_DYNAMIC_KEY_AVAILABLE, "");
    }

    @Test
    public void getOperationMetadataWithResolvingException() throws Exception
    {
        processorId = new ProcessorId(FAIL_WITH_RESOLVING_EXCEPTION, FIRST_PROCESSOR_INDEX);
        Result<OperationMetadataDescriptor> metadata = metadataManager.getOperationMetadata(processorId, personKey);

        assertFailure(metadata, "", FailureType.UNKNOWN, MetadataResolvingException.class.getName());
    }

    @Test
    public void getOperationMetadataWithRuntimeException() throws Exception
    {
        processorId = new ProcessorId(FAIL_WITH_RUNTIME_EXCEPTION, FIRST_PROCESSOR_INDEX);
        Result<OperationMetadataDescriptor> metadata = metadataManager.getOperationMetadata(processorId, personKey);

        assertFailure(metadata, "", FailureType.UNKNOWN, MetadataResolvingException.class.getName());
    }

}
