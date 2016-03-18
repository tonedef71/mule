/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.metadata;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.api.source.MessageSource;
import org.mule.construct.Flow;
import org.mule.extension.api.metadata.MetadataResolvingException;
import org.mule.metadata.api.model.MetadataType;
import org.mule.util.metadata.ResultFactory;

import java.util.List;

public class MuleMetadataManager implements MetadataManager, MuleContextAware
{

    public static final String MANAGER_REGISTRY_ID = "core.metadata.manager.1";
    private static final String EXCEPTION_RESOLVING_OPERATION_METADATA = "An exception occurred while resolving Operation %s metadata";
    private static final String PROCESSOR_NOT_METADATA_AWARE = "Operation is not MetadataAware, no information available";
    private static final String EXCEPTION_RESOLVING_METADATA_KEYS = "An exception occurred while resolving Operation MetadataKeys";
    private static final String EXCEPTION_RESOLVING_CONTENT_METADATA = "An exception occurred while resolving Content metadata";
    private static final String EXCEPTION_RESOLVING_OUTPUT_METADATA = "An exception occurred while resolving Output metadata";
    private static final String SOURCE_NOT_FOUND = "Flow doesn't contain a message source";
    private static final String PROCESSOR_NOT_FOUND = "Processor doesn't exist in the given index [%s]";

    private MuleContext muleContext;

    @Override
    public Result<List<MetadataKey>> getMetadataKeys(ProcessId processId)
    {
        return exceptionHandledMetadataFetch(processId, MetadataAware::getMetadataKeys, EXCEPTION_RESOLVING_METADATA_KEYS);
    }

    @Override
    public Result<MetadataType> getContentMetadata(ProcessId processId, MetadataKey key)
    {
        return exceptionHandledMetadataFetch(processId, processor -> processor.getContentMetadata(key), EXCEPTION_RESOLVING_CONTENT_METADATA);
    }

    @Override
    public Result<MetadataType> getOutputMetadata(ProcessId processId, MetadataKey key)
    {
        return exceptionHandledMetadataFetch(processId, processor -> processor.getOutputMetadata(key), EXCEPTION_RESOLVING_OUTPUT_METADATA);
    }

    @Override
    public Result<OperationMetadataDescriptor> getOperationMetadata(ProcessId processId, MetadataKey key)
    {
        return exceptionHandledMetadataFetch(processId, processor -> processor.getMetadata(key),
                                             String.format(EXCEPTION_RESOLVING_OPERATION_METADATA, processId));
    }

    @Override
    public Result<OperationMetadataDescriptor> getOperationMetadata(ProcessId processId)
    {
        return exceptionHandledMetadataFetch(processId, MetadataAware::getMetadata,
                                             String.format(EXCEPTION_RESOLVING_OPERATION_METADATA, processId));
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }


    private <T> Result<T> exceptionHandledMetadataFetch(ProcessId processId, MetadataDelegate<T> metadataSupplier, String failureMessage)
    {
        try
        {
            return metadataSupplier.get(findMetadataAwareExecutable(processId));
        }
        catch (InvalidExecutableIdException e)
        {
            return ResultFactory.failure(null, e.getMessage(), e);
        }
        catch (Exception e)
        {
            return ResultFactory.failure(null, failureMessage, e);
        }
    }

    private MetadataAware findMetadataAwareExecutable(ProcessId processId) throws InvalidExecutableIdException
    {
        //FIXME MULE-9496 : Use flow paths to obtain Processors
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(processId.getFlowName());
        if (flow == null)
        {
            throw new InvalidExecutableIdException(String.format(PROCESSOR_NOT_FOUND, processId.getExecutablePath()));
        }
        try
        {
            if (!processId.getExecutablePath().equals("-1"))
            {
                try
                {
                    return ((MetadataAware) flow.getMessageProcessors().get(Integer.parseInt(processId.getExecutablePath())));
                }
                catch (IndexOutOfBoundsException | NumberFormatException e)
                {
                    throw new InvalidExecutableIdException(String.format(PROCESSOR_NOT_FOUND, processId.getExecutablePath()), e);
                }
            }
            else
            {
                final MessageSource messageSource = flow.getMessageSource();
                if (messageSource == null)
                {
                    throw new InvalidExecutableIdException(SOURCE_NOT_FOUND);
                }
                return (MetadataAware) messageSource;
            }
        }
        catch (ClassCastException e)
        {
            throw new InvalidExecutableIdException(PROCESSOR_NOT_METADATA_AWARE, e);
        }
    }

    private interface MetadataDelegate<T>
    {
        Result<T> get(MetadataAware processor) throws MetadataResolvingException;
    }

}
