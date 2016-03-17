/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

@SmallTest
public class FineGrainedControlClassLoaderTestCase extends AbstractMuleTestCase
{

    @Test
    public void parentFirst() throws Exception
    {
        URLClassLoader parent = new URLClassLoader(new URL[] { hello() }, Thread.currentThread().getContextClassLoader());

        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[] { bye() }, parent);
        assertEquals("Hello", callHi(ext));
    }

    @Test
    public void childResolvesOverriden() throws Exception
    {
        URLClassLoader parent = new URLClassLoader(new URL[] { hello() }, Thread.currentThread().getContextClassLoader());

        final ClassLoaderLookupPolicy lookupPolicy = new ClassLoaderLookupPolicy(singleton("mypackage"), emptySet());
        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[] { bye() }, parent, lookupPolicy);

        assertEquals("Bye", callHi(ext));
    }

    @Test
    public void parentResolvesMissingOverride() throws Exception
    {
        URLClassLoader parent = new URLClassLoader(new URL[] { hello() }, Thread.currentThread().getContextClassLoader());

                final ClassLoaderLookupPolicy lookupPolicy = new ClassLoaderLookupPolicy(singleton("mypackage"), emptySet());
        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[0], parent, lookupPolicy);
        assertEquals("Hello", callHi(ext));
    }

    @Test(expected = ClassNotFoundException.class)
    public void blockedParentOverride() throws Exception
    {
        URLClassLoader parent = new URLClassLoader(new URL[] { hello() }, Thread.currentThread().getContextClassLoader());

        final ClassLoaderLookupPolicy lookupPolicy = new ClassLoaderLookupPolicy(emptySet(), singleton("mypackage"));
        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[0], parent, lookupPolicy);
        callHi(ext);
    }

    @Test
    public void blockedOverrideIsLoadedInChild() throws Exception
    {
        URLClassLoader parent = new URLClassLoader(new URL[] { hello() }, Thread.currentThread().getContextClassLoader());

        final ClassLoaderLookupPolicy lookupPolicy = new ClassLoaderLookupPolicy(emptySet(), singleton("mypackage"));
        FineGrainedControlClassLoader ext = new FineGrainedControlClassLoader(new URL[] { bye() }, parent, lookupPolicy);
        assertEquals("Bye", callHi(ext));
    }

    private URL hello()
    {
        return ClassUtils.getResource("classloader-test-hello.jar", this.getClass());
    }

    private URL bye()
    {
        return ClassUtils.getResource("classloader-test-bye.jar", this.getClass());
    }

    private String callHi(ClassLoader loader) throws Exception
    {
        Class cls = loader.loadClass("mypackage.MyClass");
        Method method = cls.getMethod("hi");
        return (String) method.invoke(cls.newInstance());
    }
}
