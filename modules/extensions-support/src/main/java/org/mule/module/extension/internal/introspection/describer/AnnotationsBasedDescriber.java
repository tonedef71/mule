/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.describer;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getExtension;
import static org.mule.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getMemberName;
import static org.mule.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseDisplayAnnotations;
import static org.mule.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseMetadataAnnotations;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getExposedFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getField;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getInterfaceGenerics;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getOperationMethods;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterGroupFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getSourceName;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getSuperClassGenerics;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.connection.ConnectionProvider;
import org.mule.extension.api.annotation.Alias;
import org.mule.extension.api.annotation.Configuration;
import org.mule.extension.api.annotation.Configurations;
import org.mule.extension.api.annotation.Expression;
import org.mule.extension.api.annotation.Extensible;
import org.mule.extension.api.annotation.Extension;
import org.mule.extension.api.annotation.ExtensionOf;
import org.mule.extension.api.annotation.OnException;
import org.mule.extension.api.annotation.Operations;
import org.mule.extension.api.annotation.Sources;
import org.mule.extension.api.annotation.connector.Providers;
import org.mule.extension.api.annotation.metadata.MetadataScope;
import org.mule.extension.api.annotation.param.Connection;
import org.mule.extension.api.annotation.param.Optional;
import org.mule.extension.api.annotation.param.UseConfig;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.ExceptionEnricherFactory;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.ConnectionProviderDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.fluent.HasModelProperties;
import org.mule.extension.api.introspection.declaration.fluent.OperationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.SourceDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.WithParameters;
import org.mule.extension.api.introspection.declaration.spi.Describer;
import org.mule.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.extension.api.introspection.property.DisplayModelProperty;
import org.mule.extension.api.introspection.property.DisplayModelPropertyBuilder;
import org.mule.extension.api.runtime.source.Source;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.module.extension.internal.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.module.extension.internal.introspection.ParameterGroup;
import org.mule.module.extension.internal.introspection.VersionResolver;
import org.mule.module.extension.internal.model.property.ConfigTypeModelProperty;
import org.mule.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.module.extension.internal.model.property.ExtendingOperationModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.module.extension.internal.runtime.exception.DefaultExceptionEnricherFactory;
import org.mule.module.extension.internal.runtime.executor.ReflectiveOperationExecutorFactory;
import org.mule.module.extension.internal.runtime.metadata.DefaultMetadataResolverFactory;
import org.mule.module.extension.internal.runtime.source.DefaultSourceFactory;
import org.mule.module.extension.internal.util.IntrospectionUtils;
import org.mule.util.ArrayUtils;
import org.mule.util.CollectionUtils;
import org.mule.util.collection.ImmutableSetCollector;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link Describer} which generates a {@link Descriptor} by
 * scanning annotations on a type provided in the constructor
 *
 * @since 3.7.0
 */
public final class AnnotationsBasedDescriber implements Describer
{

    public static final String DEFAULT_CONNECTION_PROVIDER_NAME = "connection";
    public static final String CUSTOM_CONNECTION_PROVIDER_SUFFIX = "-" + DEFAULT_CONNECTION_PROVIDER_NAME;

    private final Class<?> extensionType;
    private final VersionResolver versionResolver;
    private final ClassTypeLoader typeLoader;

    /**
     * An ordered {@link List} used to locate a {@link FieldDescriber} that can handle
     * an specific {@link Field}
     */
    private List<FieldDescriber> fieldDescribers;

    public AnnotationsBasedDescriber(Class<?> extensionType)
    {
        this(extensionType, new ManifestBasedVersionResolver(extensionType));
    }

    public AnnotationsBasedDescriber(Class<?> extensionType, VersionResolver versionResolver)
    {
        checkArgument(extensionType != null, String.format("describer %s does not specify an extension type", getClass().getName()));
        this.extensionType = extensionType;
        this.versionResolver = versionResolver;
        typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(extensionType.getClassLoader());

        initialiseFieldDescribers();
    }

    private void initialiseFieldDescribers()
    {
        fieldDescribers = ImmutableList.of(new TlsContextFieldDescriber(), new DefaultFieldDescriber(typeLoader));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Descriptor describe(DescribingContext context)
    {
        Extension extension = getExtension(extensionType);
        DeclarationDescriptor declaration = context.getDeclarationDescriptor()
                .named(extension.name())
                .onVersion(getVersion(extension))
                .fromVendor(extension.vendor())
                .describedAs(extension.description())
                .withExceptionEnricherFactory(getExceptionEnricherFactory(extensionType))
                .withModelProperty(new ImplementingTypeModelProperty(extensionType));

        declareConfigurations(declaration, extensionType);
        declareOperations(declaration, extensionType);
        declareConnectionProviders(declaration, extensionType);
        declareMessageSources(declaration, extensionType);

        return declaration;
    }

    private String getVersion(Extension extension)
    {
        return versionResolver.resolveVersion(extension);
    }

    private void declareConfigurations(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Class<?>[] configurationClasses = getConfigurationClasses(extensionType);
        if (ArrayUtils.isEmpty(configurationClasses))
        {
            declareConfiguration(declaration, extensionType);
        }
        else
        {
            for (Class<?> configurationClass : configurationClasses)
            {
                declareConfiguration(declaration, configurationClass);
            }
        }
    }

    private Class<?>[] getConfigurationClasses(Class<?> extensionType)
    {
        Configurations configs = extensionType.getAnnotation(Configurations.class);
        return configs == null ? ArrayUtils.EMPTY_CLASS_ARRAY : configs.value();
    }

    private void declareMessageSources(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Sources sources = extensionType.getAnnotation(Sources.class);
        if (sources != null)
        {
            for (Class<? extends Source> declaringClass : sources.value())
            {
                declareMessageSource(declaration, declaringClass);
            }
        }
    }

    private void declareConfiguration(DeclarationDescriptor declaration, Class<?> configurationType)
    {
        checkConfigurationIsNotAnOperation(configurationType);
        ConfigurationDescriptor configuration;

        Configuration configurationAnnotation = configurationType.getAnnotation(Configuration.class);
        if (configurationAnnotation != null)
        {
            configuration = declaration.withConfig(configurationAnnotation.name()).describedAs(configurationAnnotation.description());
        }
        else
        {
            configuration = declaration.withConfig(Extension.DEFAULT_CONFIG_NAME).describedAs(Extension.DEFAULT_CONFIG_DESCRIPTION);
        }

        configuration.createdWith(new TypeAwareConfigurationFactory(configurationType))
                .withModelProperty(new ImplementingTypeModelProperty(configurationType));

        declareAnnotatedParameters(configurationType, configuration, configuration.with());
    }

    private void checkConfigurationIsNotAnOperation(Class<?> configurationType)
    {
        Class<?>[] operationClasses = getOperationClasses(extensionType);
        for (Class<?> operationClass : operationClasses)
        {
            if (configurationType.isAssignableFrom(operationClass) || operationClass.isAssignableFrom(configurationType))
            {
                throw new IllegalConfigurationModelDefinitionException(String.format("Configuration class '%s' cannot be the same class (nor a derivative) of any operation class '%s",
                                                                                     configurationType.getName(), operationClass.getName()));
            }
        }
    }

    private void checkOperationIsNotAnExtension(Class<?> operationType)
    {
        if (operationType.isAssignableFrom(extensionType) || extensionType.isAssignableFrom(operationType))
        {
            throw new IllegalOperationModelDefinitionException(String.format("Operation class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
                                                                             operationType.getName(), extensionType.getName()));
        }
    }

    private void declareMessageSource(DeclarationDescriptor declaration, Class<? extends Source> sourceType)
    {
        //TODO: MULE-9220: Add a Syntax validator which checks that a Source class doesn't try to declare operations, configs, etc
        SourceDescriptor source = declaration.withMessageSource(getSourceName(sourceType));

        List<Class<?>> sourceGenerics = getSuperClassGenerics(sourceType, Source.class);

        if (sourceGenerics.size() != 2)
        {
            //TODO: MULE-9220: Add a syntax validator for this
            throw new IllegalModelDefinitionException(String.format("Message source class '%s' was expected to have 2 generic types " +
                                                                    "(one for the Payload type and another for the Attributes type) but %d were found",
                                                                    sourceType.getName(), sourceGenerics.size()));
        }

        source.sourceCreatedBy(new DefaultSourceFactory(sourceType))
                .whichReturns(typeLoader.load(sourceGenerics.get(0)))
                .withAttributesOfType(typeLoader.load(sourceGenerics.get(1)))
                .withExceptionEnricherFactory(getExceptionEnricherFactory(sourceType))
                .withModelProperty(new ImplementingTypeModelProperty(sourceType));

        declareAnnotatedParameters(sourceType, source, source.with());
    }

    private void declareAnnotatedParameters(Class<?> annotatedType, Descriptor descriptor, WithParameters with)
    {
        declareSingleParameters(getParameterFields(annotatedType), with);
        List<ParameterGroup> groups = declareConfigurationParametersGroups(annotatedType, with, null);
        if (!CollectionUtils.isEmpty(groups) && descriptor instanceof HasModelProperties)
        {
            ((HasModelProperties) descriptor).withModelProperty(new ParameterGroupModelProperty(groups));
        }
    }

    private java.util.Optional<ExceptionEnricherFactory> getExceptionEnricherFactory(AnnotatedElement element)
    {
        OnException onExceptionAnnotation = element.getAnnotation(OnException.class);
        if (onExceptionAnnotation != null)
        {
            return java.util.Optional.of(new DefaultExceptionEnricherFactory(onExceptionAnnotation.value()));
        }
        return java.util.Optional.empty();
    }

    private List<ParameterGroup> declareConfigurationParametersGroups(Class<?> annotatedType, WithParameters with, ParameterGroup parent)
    {
        List<ParameterGroup> groups = new LinkedList<>();
        for (Field field : getParameterGroupFields(annotatedType))
        {
            //TODO: MULE-9220
            if (field.isAnnotationPresent(Optional.class))
            {
                throw new IllegalParameterModelDefinitionException(String.format("@%s can not be applied along with @%s. Affected field [%s] in [%s].", Optional.class.getSimpleName(), org.mule.extension.api.annotation.ParameterGroup.class.getSimpleName(), field.getName(), annotatedType));
            }

            Set<ParameterDescriptor> parameters = declareSingleParameters(getExposedFields(field.getType()), with);

            if (!parameters.isEmpty())
            {
                ParameterGroup group = new ParameterGroup(field.getType(), field);
                groups.add(group);

                for (ParameterDescriptor descriptor : parameters)
                {
                    ParameterDeclaration parameter = inheritGroupParentDisplayProperties(parent, field, group, descriptor);

                    group.addParameter(parameter.getName(), getField(field.getType(),
                                                                     getMemberName(parameter, parameter.getName()),
                                                                     getType(parameter.getType())));
                }

                List<ParameterGroup> childGroups = declareConfigurationParametersGroups(field.getType(), with, group);
                if (!CollectionUtils.isEmpty(childGroups))
                {
                    group.addModelProperty(new ParameterGroupModelProperty(childGroups));
                }
            }
        }

        return groups;
    }

    private ParameterDeclaration inheritGroupParentDisplayProperties(ParameterGroup parent, Field field, ParameterGroup group, ParameterDescriptor descriptor)
    {
        ParameterDeclaration parameter = descriptor.getDeclaration();
        DisplayModelProperty parameterDisplayProperty = descriptor.getDeclaration().getModelProperty(DisplayModelProperty.class).orElse(null);

        DisplayModelPropertyBuilder builder = parameterDisplayProperty == null
                                              ? DisplayModelPropertyBuilder.create()
                                              : DisplayModelPropertyBuilder.create(parameterDisplayProperty);

        // Inherit parent placement model properties
        DisplayModelProperty groupDisplay = null;
        DisplayModelProperty parentDisplay = parent != null ? parent.getModelProperty(DisplayModelProperty.class).orElse(null) : null;
        if (parentDisplay != null)
        {
            builder.groupName(parentDisplay.getGroupName())
                    .tabName(parentDisplay.getTabName())
                    .order(parentDisplay.getOrder());

            groupDisplay = builder.build();
        }
        else
        {
            groupDisplay = parseDisplayAnnotations(field, field.getName(), builder);
        }

        if (groupDisplay != null)
        {
            descriptor.withModelProperty(groupDisplay);
            group.addModelProperty(groupDisplay);
        }
        return parameter;
    }

    private Set<ParameterDescriptor> declareSingleParameters(Collection<Field> parameterFields, WithParameters with)
    {
        return parameterFields.stream()
                .map(field -> getFieldDescriber(field).describe(field, with))
                .collect(new ImmutableSetCollector<>());
    }

    private FieldDescriber getFieldDescriber(Field field)
    {
        java.util.Optional<FieldDescriber> describer = fieldDescribers.stream()
                .filter(fieldDescriber -> fieldDescriber.accepts(field))
                .findFirst();

        if (describer.isPresent())
        {
            return describer.get();
        }

        throw new IllegalModelDefinitionException(String.format(
                "Could not find a %s capable of parsing the field '%s' on class '%s'",
                FieldDescriber.class.getSimpleName(), field.getName(), field.getDeclaringClass().getName()));
    }

    private void declareOperations(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Class<?>[] operations = getOperationClasses(extensionType);
        for (Class<?> actingClass : operations)
        {
            declareOperation(declaration, actingClass);
        }
    }

    private Class<?>[] getOperationClasses(Class<?> extensionType)
    {
        Operations operations = extensionType.getAnnotation(Operations.class);
        return operations == null ? ArrayUtils.EMPTY_CLASS_ARRAY : operations.value();
    }

    private <T> void declareOperation(DeclarationDescriptor declaration, Class<T> actingClass)
    {
        checkOperationIsNotAnExtension(actingClass);

        for (Method method : getOperationMethods(actingClass))
        {
            OperationDescriptor operation = declaration.withOperation(method.getName())
                    .withModelProperty(new ImplementingMethodModelProperty(method))
                    .executorsCreatedBy(new ReflectiveOperationExecutorFactory<>(actingClass, method))
                    .whichReturns(IntrospectionUtils.getMethodReturnType(method, typeLoader))
                    .withAttributesOfType(IntrospectionUtils.getMethodReturnAttributesType(method, typeLoader))
                    .withExceptionEnricherFactory(getExceptionEnricherFactory(method))
                    .withMetadataResolverFactory(getMetadataResolverFactory(extensionType, method));

            declareOperationParameters(method, operation);
            calculateExtendedTypes(actingClass, method, operation);
        }
    }

    private java.util.Optional<MetadataResolverFactory> getMetadataResolverFactory(Class<?> extensionType, Method method)
    {
        MetadataScope metadataScopeAnnotation = method.getAnnotation(MetadataScope.class);
        metadataScopeAnnotation = metadataScopeAnnotation == null ? extensionType.getAnnotation(MetadataScope.class) : metadataScopeAnnotation;

        if (metadataScopeAnnotation != null)
        {
            return java.util.Optional.of(new DefaultMetadataResolverFactory(metadataScopeAnnotation.value()));
        }

        return java.util.Optional.empty();
    }

    private void declareConnectionProviders(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Providers providers = extensionType.getAnnotation(Providers.class);
        if (providers != null)
        {
            for (Class<?> providerClass : providers.value())
            {
                declareConnectionProvider(declaration, providerClass);
            }
        }
    }

    private <T> void declareConnectionProvider(DeclarationDescriptor declaration, Class<T> providerClass)
    {
        String name = DEFAULT_CONNECTION_PROVIDER_NAME;
        String description = EMPTY;

        Alias aliasAnnotation = providerClass.getAnnotation(Alias.class);
        if (aliasAnnotation != null)
        {
            name = aliasAnnotation.value() + CUSTOM_CONNECTION_PROVIDER_SUFFIX;
            description = aliasAnnotation.description();
        }

        List<Class<?>> providerGenerics = getInterfaceGenerics(providerClass, ConnectionProvider.class);

        if (providerGenerics.size() != 2)
        {
            //TODO: MULE-9220: Add a syntax validator for this
            throw new IllegalConnectionProviderModelDefinitionException(String.format("Connection provider class '%s' was expected to have 2 generic types " +
                                                                                      "(one for the config type and another for the connection type) but %d were found",
                                                                                      providerClass.getName(), providerGenerics.size()));
        }

        ConnectionProviderDescriptor providerDescriptor = declaration.withConnectionProvider(name)
                .describedAs(description)
                .createdWith(new DefaultConnectionProviderFactory<>(providerClass))
                .forConfigsOfType(providerGenerics.get(0))
                .whichGivesConnectionsOfType(providerGenerics.get(1))
                .withModelProperty(new ImplementingTypeModelProperty(providerClass));

        declareAnnotatedParameters(providerClass, providerDescriptor, providerDescriptor.with());
    }

    private void calculateExtendedTypes(Class<?> actingClass, Method method, OperationDescriptor operation)
    {
        ExtensionOf extensionOf = method.getAnnotation(ExtensionOf.class);
        if (extensionOf == null)
        {
            extensionOf = actingClass.getAnnotation(ExtensionOf.class);
        }

        if (extensionOf != null)
        {
            operation.withModelProperty(new ExtendingOperationModelProperty(extensionOf.value()));
        }
        else if (isExtensible())
        {
            operation.withModelProperty(new ExtendingOperationModelProperty(extensionType));
        }
    }

    private boolean isExtensible()
    {
        return extensionType.getAnnotation(Extensible.class) != null;
    }

    private void declareOperationParameters(Method method, OperationDescriptor operation)
    {
        List<ParsedParameter> descriptors = MuleExtensionAnnotationParser.parseParameters(method, typeLoader);

        //TODO: MULE-9220
        checkAnnotationIsNotUsedMoreThanOnce(method, operation, UseConfig.class);
        checkAnnotationIsNotUsedMoreThanOnce(method, operation, Connection.class);

        for (ParsedParameter parsedParameter : descriptors)
        {
            if (parsedParameter.isAdvertised())
            {
                ParameterDescriptor parameter = parsedParameter.isRequired()
                                                ? operation.with().requiredParameter(parsedParameter.getName())
                                                : operation.with().optionalParameter(parsedParameter.getName()).defaultingTo(parsedParameter.getDefaultValue());

                parameter.withExpressionSupport(IntrospectionUtils.getExpressionSupport(parsedParameter.getAnnotation(Expression.class)));
                parameter.describedAs(EMPTY).ofType(parsedParameter.getType());
                addTypeRestrictions(parameter, parsedParameter);
                DisplayModelProperty displayModelProperty = parseDisplayAnnotations(parsedParameter, parsedParameter.getName());
                if (displayModelProperty != null)
                {
                    parameter.withModelProperty(displayModelProperty);
                }
                parseMetadataAnnotations(parsedParameter, parameter);
            }

            Connection connectionAnnotation = parsedParameter.getAnnotation(Connection.class);
            if (connectionAnnotation != null)
            {
                operation.withModelProperty(new ConnectionTypeModelProperty(getType(parsedParameter.getType(), typeLoader.getClassLoader())));
            }

            UseConfig useConfig = parsedParameter.getAnnotation(UseConfig.class);
            if (useConfig != null)
            {
                operation.withModelProperty(new ConfigTypeModelProperty(getType(parsedParameter.getType())));
            }
        }
    }

    private void checkAnnotationIsNotUsedMoreThanOnce(Method method, OperationDescriptor operation, Class annotationClass)
    {
        Stream<java.lang.reflect.Parameter> parametersStream = Arrays
                .stream(method.getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(annotationClass));

        List<java.lang.reflect.Parameter> parameterList = parametersStream.collect(Collectors.toList());

        if (parameterList.size() > 1)
        {
            throw new IllegalModelDefinitionException(String.format("Method [%s] defined in Class [%s] of extension [%s] uses the annotation @%s more than once", method.getName(), method.getDeclaringClass(), operation.getRootDeclaration().getDeclaration().getName(), annotationClass.getSimpleName()));
        }
    }

    private void addTypeRestrictions(ParameterDescriptor parameter, ParsedParameter descriptor)
    {
        Class<?> restriction = descriptor.getTypeRestriction();
        if (restriction != null)
        {
            parameter.withModelProperty(new TypeRestrictionModelProperty<>(restriction));
        }
    }
}
