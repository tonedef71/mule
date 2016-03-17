/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.ProcessorId;
import org.mule.api.metadata.Result;
import org.mule.extension.api.metadata.NullMetadataKey;
import org.mule.metadata.api.model.MetadataType;

import java.util.List;

import org.junit.Test;

public class InputOutputMetadataTestCase extends MetadataManagerTestCase
{
    @Test
    public void getMetadataKeysWithKeyParam() throws Exception
    {
        processorId = new ProcessorId(OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);
        final Result<List<MetadataKey>> metadataKeys = metadataManager.getMetadataKeys(processorId);
        assertThat(metadataKeys.isSucess(), is(true));
        assertThat(metadataKeys.get().size(), is(3));
    }

    @Test
    public void getMetadataKeysWithoutKeyParam() throws Exception
    {
        processorId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);
        final Result<List<MetadataKey>> metadataKeys = metadataManager.getMetadataKeys(processorId);
        assertThat(metadataKeys.isSucess(), is(true));
        assertThat(metadataKeys.get().size(), is(1));
        assertThat(metadataKeys.get().get(0), instanceOf(NullMetadataKey.class));
    }


    @Test
    public void getContentMetadataWithKey() throws Exception
    {
        processorId = new ProcessorId(CONTENT_METADATA_WITH_KEY_PARAM, 0);

        final Result<MetadataType> contentMetadata = metadataManager.getContentMetadata(processorId, personKey);
        final Result<MetadataType> outputMetadata = metadataManager.getOutputMetadata(processorId, personKey);

        assertSuccess(contentMetadata, PERSON_TYPE);
        assertSuccess(outputMetadata, JAVA_OBJECT_TYPE);
    }

    @Test
    public void getOutputMetadataWithKey() throws Exception
    {
        processorId = new ProcessorId(OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final Result<MetadataType> contentMetadata = metadataManager.getContentMetadata(processorId, personKey);
        final Result<MetadataType> outputMetadata = metadataManager.getOutputMetadata(processorId, personKey);

        assertSuccess(contentMetadata, JAVA_OBJECT_TYPE);
        assertSuccess(outputMetadata, PERSON_TYPE);
    }

    @Test
    public void dynamicContentWithoutKeyParam() throws Exception
    {
        processorId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final Result<MetadataType> contentMetadata = metadataManager.getContentMetadata(processorId, nullMetadataKey);
        final Result<MetadataType> outputMetadata = metadataManager.getOutputMetadata(processorId, nullMetadataKey);

        assertSuccess(contentMetadata, PERSON_TYPE);
        assertSuccess(outputMetadata, JAVA_OBJECT_TYPE);
    }


    @Test
    public void dynamicOutputWithoutKeyParam() throws Exception
    {
        processorId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final Result<MetadataType> contentMetadata = metadataManager.getContentMetadata(processorId, nullMetadataKey);
        final Result<MetadataType> outputMetadata = metadataManager.getOutputMetadata(processorId, nullMetadataKey);

        assertSuccess(outputMetadata, PERSON_TYPE);
        assertSuccess(contentMetadata, JAVA_OBJECT_TYPE);
    }
}
