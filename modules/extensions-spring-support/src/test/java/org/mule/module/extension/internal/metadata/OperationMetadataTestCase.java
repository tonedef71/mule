/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.module.extension.internal.runtime.metadata.MetadataMediator.RETURN_PARAM_NAME;
import org.mule.api.metadata.ProcessorId;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.api.metadata.descriptor.ParameterMetadataDescriptor;

import java.util.Iterator;

import org.junit.Test;

public class OperationMetadataTestCase extends MetadataManagerTestCase
{
    @Test
    public void dynamicOperationMetadata() throws Exception
    {
        processorId =  new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();
        final Iterator<ParameterMetadataDescriptor> paramsIt = metadataDescriptor.getParametersMetadata().iterator();

        assertExpectedParam(metadataDescriptor.getOutputMetadata(), RETURN_PARAM_NAME, PERSON_TYPE, true);

        assertExpectedParam(paramsIt.next(), "type", String.class, false);
        assertExpectedParam(paramsIt.next(), "content", PERSON_TYPE, true);
    }

    @Test
    public void staticOperationMetadata() throws Exception
    {
        processorId =  new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationStaticMetadata();
        final Iterator<ParameterMetadataDescriptor> paramsIt = metadataDescriptor.getParametersMetadata().iterator();

        assertExpectedParam(metadataDescriptor.getOutputMetadata(), RETURN_PARAM_NAME, JAVA_OBJECT_TYPE, false);

        assertExpectedParam(paramsIt.next(), "type", String.class, false);
        assertExpectedParam(paramsIt.next(), "content", JAVA_OBJECT_TYPE, false);
    }

    @Test
    public void dynamicOutputWithoutContentParam() throws Exception
    {
        // Resolver for content and output type, no @Content param, resolves only output, with keys and KeyParam
        processorId = new ProcessorId(OUTPUT_ONLY_WITHOUT_CONTENT_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();
        final Iterator<ParameterMetadataDescriptor> paramsIt = metadataDescriptor.getParametersMetadata().iterator();

        assertExpectedParam(metadataDescriptor.getOutputMetadata(), RETURN_PARAM_NAME, PERSON_TYPE, true);

        assertExpectedParam(paramsIt.next(), "type", String.class, false);
        assertThat(paramsIt.hasNext(), is(false));
    }

    @Test
    public void dynamicContentWithoutOutput() throws Exception
    {
        // Resolver for content and output type, no return type, resolves only @Content, with key and KeyParam
        processorId = new ProcessorId(CONTENT_ONLY_IGNORES_OUTPUT, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();
        final Iterator<ParameterMetadataDescriptor> paramsIt = metadataDescriptor.getParametersMetadata().iterator();

        assertExpectedParam(metadataDescriptor.getOutputMetadata(), RETURN_PARAM_NAME, void.class, false);

        assertExpectedParam(paramsIt.next(), "type", String.class, false);
        assertExpectedParam(paramsIt.next(), "content", PERSON_TYPE, true);
    }

    @Test
    public void operationOutputWithoutKeyParam() throws Exception
    {
        processorId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();
        final Iterator<ParameterMetadataDescriptor> paramsIt = metadataDescriptor.getParametersMetadata().iterator();

        assertExpectedParam(metadataDescriptor.getOutputMetadata(), RETURN_PARAM_NAME, PERSON_TYPE, true);

        assertExpectedParam(paramsIt.next(), "content", Object.class, false);
        assertThat(paramsIt.hasNext(), is(false));
    }

    @Test
    public void contentAndOutputMetadataWithoutKeyParam() throws Exception
    {
        processorId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();
        final Iterator<ParameterMetadataDescriptor> paramsIt = metadataDescriptor.getParametersMetadata().iterator();

        assertExpectedParam(metadataDescriptor.getOutputMetadata(), RETURN_PARAM_NAME, PERSON_TYPE, true);

        assertExpectedParam(paramsIt.next(), "content", PERSON_TYPE, true);
        assertThat(paramsIt.hasNext(), is(false));
    }

    @Test
    public void contentMetadataWithoutKeysWithKeyParam() throws Exception
    {
        processorId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEYS_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();
        final Iterator<ParameterMetadataDescriptor> paramsIt = metadataDescriptor.getParametersMetadata().iterator();

        assertExpectedParam(metadataDescriptor.getOutputMetadata(), RETURN_PARAM_NAME, void.class, false);

        assertExpectedParam(paramsIt.next(), "type", String.class, false);
        assertExpectedParam(paramsIt.next(), "content", PERSON_TYPE, true);
    }

    @Test
    public void outputMetadataWithoutKeysWithKeyParam() throws Exception
    {
        processorId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEYS_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final OperationMetadataDescriptor metadataDescriptor = getOperationDynamicMetadata();
        final Iterator<ParameterMetadataDescriptor> paramsIt = metadataDescriptor.getParametersMetadata().iterator();

        assertExpectedParam(metadataDescriptor.getOutputMetadata(), RETURN_PARAM_NAME, PERSON_TYPE, true);

        assertExpectedParam(paramsIt.next(), "type", String.class, false);
        assertThat(paramsIt.hasNext(), is(false));
    }
}
