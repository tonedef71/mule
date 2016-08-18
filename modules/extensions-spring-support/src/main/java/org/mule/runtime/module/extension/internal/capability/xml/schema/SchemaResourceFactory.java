/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;


import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.property.ImportedTypesModelProperty;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.xml.dsl.api.property.XmlModelProperty;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslResolvingContext;

import java.util.Optional;

/**
 * Implementation of {@link AbstractXmlResourceFactory} which generates the extension's XSD schema
 *
 * @since 4.0
 */
public class SchemaResourceFactory extends AbstractXmlResourceFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  protected GeneratedResource generateXmlResource(ExtensionModel extensionModel, XmlModelProperty xmlModelProperty) {

    DslResolvingContext dslContext = extensionModel.getModelProperty(ImportedTypesModelProperty.class).isPresent()
        ? new ClasspathBasedDslContext(Thread.currentThread().getContextClassLoader())
        : name -> Optional.empty();

    String schema = new SchemaGenerator().generate(extensionModel, xmlModelProperty, dslContext);
    return new GeneratedResource(xmlModelProperty.getXsdFileName(), schema.getBytes());
  }
}