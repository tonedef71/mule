/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import static java.util.Collections.emptySet;

import java.util.Collections;
import java.util.Set;

public class ClassLoaderLookupPolicy
{

    public static final ClassLoaderLookupPolicy NULL_LOOKUP_POLICY = new ClassLoaderLookupPolicy(emptySet(), emptySet());

    private final Set<String> overridden;
    private final Set<String> blocked;

    public ClassLoaderLookupPolicy(Set<String> overridden, Set<String> blocked)
    {
        this.overridden = Collections.unmodifiableSet(overridden);
        this.blocked = Collections.unmodifiableSet(blocked);
    }

    public boolean isOverridden(String name)
    {
        // find a match
        boolean overrideMatch = false;
        for (String override : overridden)
        {
            if (name.equals(override) || name.startsWith(override + "."))
            {
                overrideMatch = true;
                break;
            }
        }
        return overrideMatch;
    }

    public boolean isBlocked(String name)
    {
        boolean blockedMatch = false;
        for (String b : blocked)
        {
            if (name.equals(b) || name.startsWith(b + "."))
            {
                blockedMatch = true;
                break;
            }
        }
        return blockedMatch;
    }
}
