/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import org.mule.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class ClassLoaderLookupPolicyParser
{

    private static String[] systemPackages = {
            "java.",
            "javax.",
            "org.mule.",
            "com.mulesoft.",
    };

    public ClassLoaderLookupPolicy parse(String config)
    {
        Set<String> overridden = new HashSet<>();
        Set<String> blocked = new HashSet<>();

        if (!StringUtils.isEmpty(config))
        {
            final String[] overrides = config.split(",");

            if (overrides.length != 0)
            {
                for (String override : overrides)
                {
                    override = StringUtils.defaultString(override).trim();

                    // 'blocked' package definitions come with a '-' prefix
                    boolean isBlocked = override.startsWith("-");
                    if (isBlocked)
                    {
                        override = override.substring(1);
                    }

                    String dottedOverride;
                    if (override.endsWith("."))
                    {
                        dottedOverride = override;
                        override = override.substring(0, override.length() - 1);
                    }
                    else
                    {
                        dottedOverride = override + ".";
                    }

                    if (isSystemPackage(dottedOverride))
                    {
                        throw new IllegalArgumentException("Can't override a system package. Offending value: " + override);
                    }

                    overridden.add(override);
                    if (isBlocked)
                    {
                        blocked.add(override);
                    }
                }
            }
        }

        return new ClassLoaderLookupPolicy(overridden, blocked);
    }

    private boolean isSystemPackage(String override)
    {
        for (String systemPackage : systemPackages)
        {
            if (override.startsWith(systemPackage))
            {
                return true;
            }
        }

        return false;
    }
}
