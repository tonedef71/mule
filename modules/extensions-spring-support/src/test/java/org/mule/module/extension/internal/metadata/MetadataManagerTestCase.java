/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.MetadataKeysBuilder;
import org.mule.api.metadata.ProcessorId;
import org.mule.api.metadata.Result;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.extension.api.metadata.NullMetadataKey;

import org.junit.Before;

public abstract class MetadataManagerTestCase extends MetadataExtensionFunctionalTestCase
{

    protected static final String PERSON_TYPE = "PERSON.type";
    protected static final String JAVA_OBJECT_TYPE = "JAVA_OBJECT.type";
    protected static final String FIRST_PROCESSOR_INDEX = "0";

    protected static final String CONTENT_METADATA_WITH_KEY_PARAM = "contentMetadataWithKeyParam";
    protected static final String OUTPUT_METADATA_WITH_KEY_PARAM = "outputMetadataWithKeyParam";
    protected static final String CONTENT_AND_OUTPUT_METADATA_WITH_KEY_PARAM = "contentAndOutputMetadataWithKeyParam";
    protected static final String OUTPUT_ONLY_WITHOUT_CONTENT_PARAM = "outputOnlyWithoutContentParam";
    protected static final String CONTENT_ONLY_IGNORES_OUTPUT = "contentOnlyIgnoresOutput";
    protected static final String CONTENT_METADATA_WITHOUT_KEY_PARAM = "contentMetadataWithoutKeyParam";
    protected static final String OUTPUT_METADATA_WITHOUT_KEY_PARAM = "outputMetadataWithoutKeyParam";
    protected static final String CONTENT_AND_OUTPUT_METADATA_WITHOUT_KEY_PARAM = "contentAndOutputMetadataWithoutKeyParam";
    protected static final String CONTENT_METADATA_WITHOUT_KEYS_WITH_KEY_PARAM = "contentMetadataWithoutKeysWithKeyParam";
    protected static final String OUTPUT_METADATA_WITHOUT_KEYS_WITH_KEY_PARAM = "outputMetadataWithoutKeysWithKeyParam";
    protected final NullMetadataKey nullMetadataKey = new NullMetadataKey();
    protected MetadataKey personKey;
    protected ProcessorId processorId;

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {MetadataExtension.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "metadata-tests.xml";
    }

    @Before
    public void setKey() throws Exception
    {
        this.personKey = MetadataKeysBuilder.createKeys().addKey(MetadataConnection.PERSON).build().get(0);
    }

    protected OperationMetadataDescriptor getOperationDynamicMetadata()
    {
        Result<OperationMetadataDescriptor> operationMetadata = metadataManager.getOperationMetadata(processorId, personKey);
        assertThat(operationMetadata.isSucess(), is(true));

        return operationMetadata.get();
    }

    protected OperationMetadataDescriptor getOperationStaticMetadata()
    {
        Result<OperationMetadataDescriptor> operationMetadata = metadataManager.getOperationMetadata(processorId);
        assertThat(operationMetadata.isSucess(), is(true));

        return operationMetadata.get();
    }
}
