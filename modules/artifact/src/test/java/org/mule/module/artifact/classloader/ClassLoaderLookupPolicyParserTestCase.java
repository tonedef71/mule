/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

public class ClassLoaderLookupPolicyParserTestCase extends AbstractMuleTestCase
{

    public static final String DEFAULT_PACKAGE = "org.bar";
    public static final String CUSTOM_PACKAGE = "org.foo.bar";
    public static final Set<String> systemPackages = Collections.singleton("java.");

    private ClassLoaderLookupPolicyParser parser = new ClassLoaderLookupPolicyParser();

    @Test(expected = IllegalArgumentException.class)
    public void forbidsOverrideOfSystemPackage()
    {
        parser.parse("java.lang");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidsOverrideOfDottedSystemPackage()
    {
        parser.parse("java.lang.");
    }

    @Test
    public void overridesPackage()
    {
        final ClassLoaderLookupPolicy classLoaderLookupPolicy = parser.parse(CUSTOM_PACKAGE);

        assertThat(classLoaderLookupPolicy.isOverridden(CUSTOM_PACKAGE), is(true));
        assertThat(classLoaderLookupPolicy.isOverridden(DEFAULT_PACKAGE), is(false));
    }

    @Test
    public void overridesSystemPackagePrefix()
    {
        final String customPackage = "jav";
        final ClassLoaderLookupPolicy classLoaderLookupPolicy =  parser.parse(customPackage);

        assertThat(classLoaderLookupPolicy.isOverridden(customPackage), is(true));
        assertThat(classLoaderLookupPolicy.isOverridden(DEFAULT_PACKAGE), is(false));
    }

    @Test
    public void overridesDottedPackage()
    {
        final ClassLoaderLookupPolicy classLoaderLookupPolicy = parser.parse(CUSTOM_PACKAGE + ".");

        assertThat(classLoaderLookupPolicy.isOverridden(CUSTOM_PACKAGE), is(true));
        assertThat(classLoaderLookupPolicy.isOverridden(DEFAULT_PACKAGE), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidsBlockingOfSystemPackage()
    {
        parser.parse("-java.lang");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidsBlockingOfDottedSystemPackage()
    {
        parser.parse("-java.lang.");
    }

    @Test
    public void blocksDottedPackage()
    {
        final ClassLoaderLookupPolicy classLoaderLookupPolicy = parser.parse("-" + CUSTOM_PACKAGE + ".");

        assertThat(classLoaderLookupPolicy.isBlocked(CUSTOM_PACKAGE), is(true));
        assertThat(classLoaderLookupPolicy.isOverridden(DEFAULT_PACKAGE), is(false));
    }

    @Test
    public void blocksPackage()
    {
        final ClassLoaderLookupPolicy classLoaderLookupPolicy = parser.parse("-" + CUSTOM_PACKAGE);

        assertThat(classLoaderLookupPolicy.isBlocked(CUSTOM_PACKAGE), is(true));
        assertThat(classLoaderLookupPolicy.isOverridden(DEFAULT_PACKAGE), is(false));
    }

    @Test
    public void blocksSystemPackagePrefix()
    {
        final String customPackage = "jav";
        final ClassLoaderLookupPolicy classLoaderLookupPolicy = parser.parse("-" + customPackage);

        assertThat(classLoaderLookupPolicy.isBlocked(customPackage), is(true));
        assertThat(classLoaderLookupPolicy.isOverridden(DEFAULT_PACKAGE), is(false));
    }
}