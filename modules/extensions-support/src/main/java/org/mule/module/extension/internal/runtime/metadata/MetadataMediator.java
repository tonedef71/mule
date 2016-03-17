/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.metadata;

import static java.util.Optional.empty;
import static org.mule.api.metadata.FailureType.NO_DYNAMIC_KEY_AVAILABLE;
import static org.mule.api.metadata.FailureType.NO_DYNAMIC_TYPE_AVAILABLE;
import org.mule.api.metadata.FailureType;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.Result;
import org.mule.api.metadata.descriptor.ImmutableOperationMetadataDescriptor;
import org.mule.api.metadata.descriptor.ImmutableParameterMetadataDescriptor;
import org.mule.api.metadata.descriptor.OperationMetadataDescriptor;
import org.mule.api.metadata.descriptor.ParameterMetadataDescriptor;
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
import java.util.stream.Stream;

public class MetadataMediator
{

    public static final String RETURN_PARAM_NAME = "output";
    private final OperationModel operationModel;

    public MetadataMediator(OperationModel operationModel)
    {
        this.operationModel = operationModel;
    }

    /**
     *
     * @param context
     * @return
     */
    public Result<List<MetadataKey>> getMetadataKeys(MetadataContext context)
    {
        try
        {
            if (!operationModel.getMetadataKeyParameter().isPresent())
            {
                return ResultFactory.success(Collections.singletonList(new NullMetadataKey()));
            }

            Optional<MetadataResolverFactory> resolverFactory = operationModel.getMetadataResolverFactory();
            if (!resolverFactory.isPresent())
            {
                return ResultFactory.failure(empty(), "No Dynamic Keys available", NO_DYNAMIC_KEY_AVAILABLE, empty());
            }

            return ResultFactory.success(resolverFactory.get().getResolver().getMetadataKeys(context));
        }
        catch (Exception e)
        {
            return ResultFactory.failure(empty(), e.getMessage(), e);
        }
    }

    /**
     *
     * @return
     */
    public Result<OperationMetadataDescriptor> getMetadata()
    {
        List<ParameterMetadataDescriptor> paramDescriptors = new ArrayList<>(operationModel.getParameterModels().size());
        paramDescriptors.addAll(operationModel.getParameterModels().stream()
                                        .map(model -> new ImmutableParameterMetadataDescriptor(model.getName(), model.getType(), false))
                                        .collect(Collectors.toList()));

        ParameterMetadataDescriptor outputDescriptor = new ImmutableParameterMetadataDescriptor(RETURN_PARAM_NAME, operationModel.getReturnType(), false);

        return ResultFactory.success(new ImmutableOperationMetadataDescriptor(operationModel.getName(), paramDescriptors, outputDescriptor));
    }

    /**
     *
     * @param context
     * @param key
     * @return
     */
    public Result<OperationMetadataDescriptor> getMetadata(MetadataContext context, MetadataKey key)
    {
        if (!(operationModel.hasDynamicContentType() || operationModel.hasDynamicOutputType()))
        {
            return getMetadata();
        }

        Result<List<ParameterMetadataDescriptor>> paramDescriptors = getParameterDescriptors(context, key);
        Result<ParameterMetadataDescriptor> outputDescriptor = getOutputMetadataDescriptor(context, key);

        OperationMetadataDescriptor operationDescriptor = new ImmutableOperationMetadataDescriptor(operationModel.getName(), paramDescriptors.get(),
                                                                                                   outputDescriptor.get());
        if (paramDescriptors.isSucess() && outputDescriptor.isSucess())
        {
            return ResultFactory.success(operationDescriptor);
        }

        String msg = paramDescriptors.isSucess() ? "" : paramDescriptors.getMessage();
        msg += outputDescriptor.isSucess() ? "" : "\n\n" + outputDescriptor.getMessage();

        return ResultFactory.failure(Optional.of(operationDescriptor), msg, FailureType.UNKNOWN, empty());
    }

    /**
     *
     * @param context
     * @param key
     * @return
     */
    public Result<MetadataType> getContentMetadata(MetadataContext context, MetadataKey key)
    {
        Optional<ParameterModel> contentParameter = operationModel.getContentParameter();
        if (!contentParameter.isPresent()){
            return ResultFactory.failure(Optional.<MetadataType>empty(), "No @Content parameter found", NO_DYNAMIC_TYPE_AVAILABLE, empty());
        }

        if (operationModel.hasDynamicContentType())
        {
            return getDynamicMetadata(contentParameter.get().getType(), resolver -> resolver.getContentMetadata(context, key));
        }

        return ResultFactory.success(contentParameter.get().getType());
    }

    /**
     *
     * @param context
     * @param key
     * @return
     */
    public Result<MetadataType> getOutputMetadata(final MetadataContext context, final MetadataKey key)
    {
        if (operationModel.hasDynamicOutputType())
        {
            return getDynamicMetadata(operationModel.getReturnType(), resolver -> resolver.getOutputMetadata(context, key));
        }

        return ResultFactory.success(operationModel.getReturnType());
    }

    private Result<List<ParameterMetadataDescriptor>> getParameterDescriptors(MetadataContext context, MetadataKey key)
    {
        List<ParameterMetadataDescriptor> paramDescriptors = new ArrayList<>(operationModel.getParameterModels().size());

        paramDescriptors.addAll(getStaticTypedParameters()
                                        .map(model -> new ImmutableParameterMetadataDescriptor(model.getName(), model.getType(), false))
                                        .collect(Collectors.toList()));

        if (operationModel.getContentParameter().isPresent())
        {
            Result<MetadataType> contentResult = getContentMetadata(context, key);
            paramDescriptors.add(new ImmutableParameterMetadataDescriptor(operationModel.getContentParameter().get().getName(),
                                                                          contentResult.get(), operationModel.hasDynamicContentType()));

            if (!contentResult.isSucess())
            {
                return ResultFactory.failure(Optional.of(paramDescriptors), contentResult.getMessage(),contentResult.getFailureType(),
                                             contentResult.getStacktrace());
            }
        }


        return ResultFactory.success(paramDescriptors);
    }

    private Stream<ParameterModel> getStaticTypedParameters()
    {
        if (!operationModel.getContentParameter().isPresent())
        {
            return operationModel.getParameterModels().stream();
        }

        return operationModel.getParameterModels().stream()
                .filter(p -> !p.equals(operationModel.getContentParameter().get()));
    }

    private Result<ParameterMetadataDescriptor> getOutputMetadataDescriptor(MetadataContext context, MetadataKey key)
    {
        if (!operationModel.hasDynamicOutputType())
        {
            return ResultFactory.success(new ImmutableParameterMetadataDescriptor(RETURN_PARAM_NAME, operationModel.getReturnType(), false));
        }

        Result<MetadataType> outputResult = getOutputMetadata(context, key);
        ParameterMetadataDescriptor descriptor = new ImmutableParameterMetadataDescriptor(RETURN_PARAM_NAME, outputResult.get(),
                                                                                          operationModel.hasDynamicOutputType());
        if (outputResult.isSucess())
        {
            return ResultFactory.success(descriptor);
        }

        return ResultFactory.failure(Optional.of(descriptor), outputResult.getMessage(), outputResult.getFailureType(),
                                     outputResult.getStacktrace());
    }


    private Result<MetadataType> getDynamicMetadata(MetadataType javaType, MetadataDelegate delegate)
    {
        try
        {
            Optional<MetadataResolverFactory> resolverFactory = operationModel.getMetadataResolverFactory();
            if (resolverFactory.isPresent())
            {
                MetadataType type = delegate.resolve(resolverFactory.get().getResolver());
                if (type != null && !(type instanceof NullType)){
                    return ResultFactory.success(type);
                }

            }
            return ResultFactory.failure(Optional.of(javaType), "No Dynamic Type available, defaulting to Java type",
                                         NO_DYNAMIC_TYPE_AVAILABLE, empty());
        }
        catch (Exception e)
        {
            return ResultFactory.failure(Optional.of(javaType), e.getMessage(), e);
        }
    }

    private interface MetadataDelegate
    {
        MetadataType resolve(MetadataResolver resolver) throws MetadataResolvingException;
    }
}
