/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import org.mule.module.artifact.descriptor.ArtifactDescriptor;

/**
 * Filters classes and resources using a {@link ArtifactDescriptor} describing
 * exported/blocked names.
 * <p>
 * An exact blocked/exported name match has precedence over a prefix match
 * on a blocked/exported prefix. This enables to export classes or
 * subpackages from a blocked package.
 * </p>
 */
public class ArtifactClassLoaderFilter implements ClassLoaderFilter
{

    public static final char PACKAGE_SEPARATOR = '.';
    public static final String EMPTY_PACKAGE = "";
    private final ArtifactDescriptor descriptor;

    public ArtifactClassLoaderFilter(ArtifactDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    @Override
    public boolean exportsClass(String className)
    {
        final String packageName = getPackageName(className);

        return descriptor.getExportedClassPackages().contains(packageName);
    }

    @Override
    public boolean exportsResource(String name)
    {
        final String resourcePackage = getResourceFolder(name);

        return descriptor.getExportedResourcePackages().contains(resourcePackage);
    }

    private String getResourceFolder(String resourceName)
    {
        if (resourceName == null)
        {
            return EMPTY_PACKAGE;
        }
        else
        {
            //TODO(pablo.kraan): check how to export resources from the root of an isolated component
            String pkgName = (resourceName.startsWith("/")) ? resourceName.substring(1) : resourceName;
            pkgName = (pkgName.lastIndexOf('/') < 0) ? "" : pkgName.substring(0, pkgName.lastIndexOf('/'));
            return pkgName;
        }
    }

    private String getPackageName(String className)
    {
        if (className == null)
        {
            return EMPTY_PACKAGE;
        }
        else
        {
            return (className.lastIndexOf(PACKAGE_SEPARATOR) < 0) ? EMPTY_PACKAGE : className.substring(0, className.lastIndexOf('.'));
        }
    }
}
