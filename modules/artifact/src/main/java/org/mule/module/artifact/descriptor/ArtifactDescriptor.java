/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.descriptor;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicy;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public class ArtifactDescriptor
{

    private String name;
    private File rootFolder;
    //TODO(pablo.kraan): extract exported packages and resources into a new class
    private Set<String> exportedClassPackages = Collections.emptySet();
    private Set<String> exportedResourcePackages = Collections.emptySet();
    private ClassLoaderLookupPolicy classLoaderLookupPolicy = ClassLoaderLookupPolicy.NULL_LOOKUP_POLICY;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public File getRootFolder()
    {
        return rootFolder;
    }

    public void setRootFolder(File rootFolder)
    {
        if (rootFolder == null)
        {
            throw new IllegalArgumentException("Root folder cannot be null");
        }

        this.rootFolder = rootFolder;
    }

    public void setExportedClassPackages(Set<String> exported)
    {
        this.exportedClassPackages = Collections.unmodifiableSet(exported);
    }

    /**
     * @return an immutable set of exported class prefix names
     */
    public Set<String> getExportedClassPackages()
    {
        return exportedClassPackages;
    }

    public ClassLoaderLookupPolicy getClassLoaderLookupPolicy()
    {
        return classLoaderLookupPolicy;
    }

    public void setClassLoaderLookupPolicy(ClassLoaderLookupPolicy classLoaderLookupPolicy)
    {
        checkArgument(classLoaderLookupPolicy != null, "Classloader lookup policy must be non null");
        this.classLoaderLookupPolicy = classLoaderLookupPolicy;
    }

    public Set<String> getExportedResourcePackages()
    {
        return exportedResourcePackages;
    }

    public void setExportedResourcePackages(Set<String> exportedResourcePackages)
    {
        this.exportedResourcePackages = exportedResourcePackages;
    }
}
