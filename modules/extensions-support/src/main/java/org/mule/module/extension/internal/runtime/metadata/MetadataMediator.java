/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.metadata;

import static org.mule.api.metadata.FailureType.NO_DYNAMIC_TYPE_AVAILABLE;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.Result;
import org.mule.api.metadata.descriptor.ImmutableOperationMetadataDescriptor;
import org.mule.api.metadata.descriptor.ImmutableParameterMetadataDescriptor;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.api.temporary.MuleMessage;
import org.mule.extension.api.annotation.param.metadata.Content;
import org.mule.extension.api.annotation.param.metadata.MetadataKeyParam;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.extension.api.metadata.MetadataContext;
import org.mule.extension.api.metadata.MetadataResolver;
import org.mule.extension.api.metadata.MetadataResolvingException;
import org.mule.extension.api.metadata.NullMetadataKey;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.util.metadata.ResultFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MetadataMediator
{

    public static final String RETURN_PARAM_NAME = "output";
    public static final String MESSAGE_ATTRIBUTES_NAME = "messageAttributes";

    private static final String NO_CONTENT_PARAMETER_FOUND = "No @Content parameter found";
    private static final String NO_DYNAMIC_TYPE_AVAILABLE_DEFAULTING_TO_JAVA_TYPE = "No Dynamic Type available, defaulting to Java type";

    private final OperationModel operationModel;

    public MetadataMediator(OperationModel operationModel)
    {
        this.operationModel = operationModel;
    }

    /**
     *  Resolves the list of types for the current Operation, representing them
     *  as a list of {@link MetadataKey}.
     *
     *  If no {@link MetadataKeyParam} is present in the operation, then a {@link NullMetadataKey} is
     *  returned. Otherwise, the {@link MetadataResolver#getMetadataKeys} associated with the current Operation will
     *  be invoked to obtain the keys
     *
     * @param context current {@link MetadataContext} that will be used by the {@link MetadataResolver}
     * @return  Successful {@link Result} if the keys are obtained without errors
     *          Failure {@link Result} when no Dynamic keys are a available or the retrieval fails for any reason
     */
    public Result<List<MetadataKey>> getMetadataKeys(MetadataContext context)
    {
        if (!operationModel.getMetadataKeyParameter().isPresent())
        {
            return ResultFactory.success(Collections.singletonList(new NullMetadataKey()));
        }

        try
        {
            return ResultFactory.success(operationModel.getMetadataResolverFactory().getResolver().getMetadataKeys(context));
        }
        catch (Exception e)
        {
            return ResultFactory.failure(null, e.getMessage(), e);
        }
    }

    /**
     *  Resolves the {@link OperationMetadataDescriptor} for the current operation using
     *  only the Static Java types of the Operation parameters, attributes and output.
     *
     * @return  An {@link OperationMetadataDescriptor} with the Static Metadata representation
     *          of the Operation.
     */
    public Result<OperationMetadataDescriptor> getMetadata()
    {
        List<ParameterMetadataDescriptor> paramDescriptors = getStaticParameterDescriptors();
        ParameterMetadataDescriptor outputDescriptor = new ImmutableParameterMetadataDescriptor(RETURN_PARAM_NAME, operationModel.getReturnType(), false);
        ParameterMetadataDescriptor attributesDescriptor = getAttributesMetadataDescriptor();

        return ResultFactory.success(new ImmutableOperationMetadataDescriptor(operationModel.getName(), paramDescriptors, outputDescriptor, attributesDescriptor));
    }

    /**
     *  Resolves the {@link OperationMetadataDescriptor} for the current operation using
     *  Static and Dynamic resolving of the Operation parameters, attributes and output.
     *
     *  If {@link Content} or Output of the operation have a {@link MetadataResolver} associated
     *  that can be used to resolve the Dynamic MetadataTypes, then the {@link OperationMetadataDescriptor}
     *  will contain those Dynamic types instead of the Static Java type declaration.
     *
     *  When neither {@link Content} nor Output have Dynamic types, then invoking this method is the
     *  same as invoking {@link this#getMetadata()}
     *
     * @param context current {@link MetadataContext} that will be used by the {@link MetadataResolver}
     * @param key {@link MetadataKey} of the type which's structure has to be resolved,
     *            used both for input and ouput types
     *
     * @return  Successful {@link Result} if the keys are obtained without errors
     *          Failure {@link Result} when no Dynamic keys are a available or the retrieval fails for any reason
     */
    public Result<OperationMetadataDescriptor> getMetadata(MetadataContext context, MetadataKey key)
    {
        if (!(operationModel.hasDynamicContentType() || operationModel.hasDynamicOutputType()))
        {
            return getMetadata();
        }

        Result<List<ParameterMetadataDescriptor>> paramDescriptors = getDynamicParameterDescriptors(context, key);
        Result<ParameterMetadataDescriptor> outputDescriptor = getOutputMetadataDescriptor(context, key);
        ImmutableParameterMetadataDescriptor attributesDescriptor = getAttributesMetadataDescriptor();

        OperationMetadataDescriptor operationDescriptor = new ImmutableOperationMetadataDescriptor(operationModel.getName(),
                                                                                                   paramDescriptors.get(),
                                                                                                   outputDescriptor.get(),
                                                                                                   attributesDescriptor);

        return ResultFactory.mergeResults(operationDescriptor, paramDescriptors, outputDescriptor);
    }

    /**
     * Given a {@link MetadataKey} of a type and a {@link MetadataContext},
     * resolves the {@link MetadataType} of the {@link Content} parameter using
     * the {@link MetadataResolver} associated to the current operation.
     *
     * @param context MetaDataContext of the MetaData resolution
     * @param key     {@link MetadataKey} of the type which's structure has to be resolved
     * @return the {@link MetadataType} of the {@link Content} parameter
     */
    public Result<MetadataType> getContentMetadata(MetadataContext context, MetadataKey key)
    {
        Optional<ParameterModel> contentParameter = operationModel.getContentParameter();
        if (!contentParameter.isPresent()){
            return ResultFactory.failure(null, NO_CONTENT_PARAMETER_FOUND, NO_DYNAMIC_TYPE_AVAILABLE, "");
        }

        if (!operationModel.hasDynamicContentType())
        {
            return ResultFactory.success(contentParameter.get().getType());
        }

        return getDynamicMetadata(contentParameter.get().getType(), resolver -> resolver.getContentMetadata(context, key));
    }

    /**
     * Given a {@link MetadataKey} of a type and a {@link MetadataContext},
     * resolves the {@link MetadataType} of the Operation's output using
     * the {@link MetadataResolver} associated to the current operation.
     *
     * @param context MetaDataContext of the MetaData resolution
     * @param key     {@link MetadataKey} of the type which's structure has to be resolved
     * @return the {@link MetadataType} of the output
     */
    public Result<MetadataType> getOutputMetadata(final MetadataContext context, final MetadataKey key)
    {
        if (!operationModel.hasDynamicOutputType())
        {
            return ResultFactory.success(operationModel.getReturnType());
        }

        return getDynamicMetadata(operationModel.getReturnType(), resolver -> resolver.getOutputMetadata(context, key));
    }

    /**
     * @return A List containing a {@link ParameterMetadataDescriptor} for each parameter of the operation
     *         using only the Java type and ignoring if any parameter has a dynamic type.
     */
    private List<ParameterMetadataDescriptor> getStaticParameterDescriptors()
    {
        return operationModel.getParameterModels().stream()
                .map(model -> new ImmutableParameterMetadataDescriptor(model.getName(), model.getType(), false))
                .collect(Collectors.toList());
    }

    /**
     * @return A List containing a {@link ParameterMetadataDescriptor} for each parameter of the operation
     *         using Java type for static parameters and resolving the dynamic type for the {@link Content}
     *         if on is available.
     */
    private Result<List<ParameterMetadataDescriptor>> getDynamicParameterDescriptors(MetadataContext context, MetadataKey key)
    {
        if (!operationModel.hasDynamicContentType())
        {
            return ResultFactory.success(getStaticParameterDescriptors());
        }

        List<ParameterMetadataDescriptor> paramDescriptors = new ArrayList<>(operationModel.getParameterModels().size());
        paramDescriptors.addAll(operationModel.getParameterModels().stream()
                                        .filter(p -> !p.equals(operationModel.getContentParameter().get()))
                                        .map(model -> new ImmutableParameterMetadataDescriptor(model.getName(), model.getType(), false))
                                        .collect(Collectors.toList()));

        Result<MetadataType> contentResult = getContentMetadata(context, key);
        paramDescriptors.add(new ImmutableParameterMetadataDescriptor(operationModel.getContentParameter().get().getName(),
                                                                      contentResult.get(), operationModel.hasDynamicContentType()));

        if (contentResult.isSucess())
        {
            return ResultFactory.success(paramDescriptors);
        }

        return ResultFactory.failure(paramDescriptors, contentResult.getMessage(), contentResult.getFailureType(), contentResult.getStacktrace());
    }

    /**
     * @return The {@link ParameterMetadataDescriptor} that represents the Output metadata of the operation
     */
    private Result<ParameterMetadataDescriptor> getOutputMetadataDescriptor(MetadataContext context, MetadataKey key)
    {
        if (!operationModel.hasDynamicOutputType())
        {
            return ResultFactory.success(new ImmutableParameterMetadataDescriptor(RETURN_PARAM_NAME, operationModel.getReturnType(), false));
        }

        Result<MetadataType> result = getOutputMetadata(context, key);
        ParameterMetadataDescriptor descriptor = new ImmutableParameterMetadataDescriptor(RETURN_PARAM_NAME, result.get(),
                                                                                          operationModel.hasDynamicOutputType());
        if (result.isSucess())
        {
            return ResultFactory.success(descriptor);
        }

        return ResultFactory.failure(descriptor, result.getMessage(), result.getFailureType(), result.getStacktrace());
    }


    /**
     * @return The {@link ParameterMetadataDescriptor} that represents Attributes metadata
     *         for the value that this operation sets on the output {@link MuleMessage#getAttributes()} field.
     */
    private ImmutableParameterMetadataDescriptor getAttributesMetadataDescriptor()
    {
        return new ImmutableParameterMetadataDescriptor(MESSAGE_ATTRIBUTES_NAME, operationModel.getAttributesType(), false);
    }

    /**
     * Uses the {@link MetadataDelegate} to resolve dynamic metadata of the operation
     * passing the associated {@link MetadataResolver}.
     *
     * @param javaType Java type used as default if no dynamic type is available
     * @param delegate Delegate which performs the final invocation to the {@link MetadataResolver}
     * @return The MetadataType resolved by the delegate invocation.
     *         Sucess if the dynamic type has been successfully fetched, Failure otherwise.
     */
    private Result<MetadataType> getDynamicMetadata(MetadataType javaType, MetadataDelegate delegate)
    {
        try
        {
            MetadataResolverFactory resolverFactory = operationModel.getMetadataResolverFactory();
            MetadataType type = delegate.resolve(resolverFactory.getResolver());
            if (type != null && !(type instanceof NullType)){
                return ResultFactory.success(type);
            }
            return ResultFactory.failure(javaType, NO_DYNAMIC_TYPE_AVAILABLE_DEFAULTING_TO_JAVA_TYPE, NO_DYNAMIC_TYPE_AVAILABLE, "");
        }
        catch (Exception e)
        {
            return ResultFactory.failure(javaType, e.getMessage(), e);
        }
    }

    private interface MetadataDelegate
    {
        MetadataType resolve(MetadataResolver resolver) throws MetadataResolvingException;
    }
}
