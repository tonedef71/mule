/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.api.MuleEvent;
import org.mule.api.metadata.FailureType;
import org.mule.api.metadata.MetadataManager;
import org.mule.api.metadata.MuleMetadataManager;
import org.mule.api.metadata.Result;
import org.mule.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.JavaTypeLoader;
import org.mule.metadata.utils.MetadataTypeWriter;
import org.mule.util.ClassUtils;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Before;

public abstract class MetadataExtensionFunctionalTestCase extends ExtensionFunctionalTestCase
{

    protected MuleEvent event;
    protected MetadataManager metadataManager;
    protected JavaTypeLoader typeLoader;

    @Before
    public void setUp() throws Exception
    {
        event = getTestEvent("");
        metadataManager = new MuleMetadataManager();
        ((MuleMetadataManager) metadataManager).setMuleContext(muleContext);
        typeLoader = new JavaTypeLoader(Thread.currentThread().getContextClassLoader());
    }

    protected void assertEqualsType(MetadataType metadataType, Class<?> classType) throws IOException
    {
        assertThat(metadataType, is(typeLoader.load(classType)));
    }

    protected void assertEqualsType(MetadataType metadataType, String expectedFileName) throws IOException
    {
        final String actual = new MetadataTypeWriter().toString(metadataType);
        final URL resource = ClassUtils.getResource(expectedFileName, getClass());

        if (resource == null)
        {
            fail(String.format("The expectedFileName [%s] doesn't exist in your project resources", expectedFileName));
        }

        final String expected = IOUtils.toString(resource);
        assertThat(actual, equalToIgnoringWhiteSpace(expected));
    }

    protected void assertSuccess(Result<MetadataType> result, String expectedFileType) throws IOException
    {
        assertThat(result.isSucess(), is(true));
        assertEqualsType(result.get(), expectedFileType);
    }

    protected void assertFailure(Result<?> result, String msgContains, FailureType failureType, String traceContains) throws IOException
    {
        assertThat(result.isSucess(), is(false));
        assertThat(result.getFailureType(), is(failureType));

        if(!StringUtils.isBlank(msgContains))
        {
            assertThat(result.getMessage(), containsString(msgContains));
        }

        if(!StringUtils.isBlank(traceContains))
        {
            assertThat(result.getStacktrace(), containsString(traceContains));
        }
    }

    protected void assertExpectedParam(ParameterMetadataDescriptor param, String name, Class<?> type, boolean isDynamic) throws IOException
    {
        MatcherAssert.assertThat(param.getName(), is(name));
        MatcherAssert.assertThat(param.hasDynamicType(), is(isDynamic));
        assertEqualsType(param.getType(), type);
    }

    protected void assertExpectedParam(ParameterMetadataDescriptor param, String name, String typeFile, boolean isDynamic) throws IOException
    {
        MatcherAssert.assertThat(param.getName(), is(name));
        MatcherAssert.assertThat(param.hasDynamicType(), is(isDynamic));
        assertEqualsType(param.getType(), typeFile);
    }

}
