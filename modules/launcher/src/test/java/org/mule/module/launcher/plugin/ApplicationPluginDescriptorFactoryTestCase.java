/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.launcher.plugin.ApplicationPluginDescriptorFactory.PLUGIN_PROPERTIES;
import static org.mule.module.launcher.plugin.ApplicationPluginDescriptorFactory.PROPERTY_LOADER_EXPORTED;
import static org.mule.module.launcher.plugin.ApplicationPluginDescriptorFactory.PROPERTY_LOADER_OVERRIDE;
import static org.mule.util.FileUtils.stringToFile;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicyParser;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApplicationPluginDescriptorFactoryTestCase extends AbstractMuleTestCase
{

    public static final String PLUGIN_NAME = "testPlugin";

    @Rule
    public TemporaryFolder pluginsFolder = new TemporaryFolder();

    private final ClassLoaderLookupPolicyParser classLoaderLookupPolicyParser = mock(ClassLoaderLookupPolicyParser.class);
    private ApplicationPluginDescriptorFactory descriptorFactory = new ApplicationPluginDescriptorFactory(classLoaderLookupPolicyParser);

    @Test
    public void parsesPluginWithNoDescriptor() throws Exception
    {
        final File pluginFolder = createPluginFolder();

        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).assertPluginDescriptor(pluginDescriptor);
    }

    @Test
    public void parsesLoaderOverrides() throws Exception
    {
        final File pluginFolder = createPluginFolder();

        final String overrides = "org.foo, org.bar";
        final ClassLoaderLookupPolicy classLoaderLookupPolicy = mock(ClassLoaderLookupPolicy.class);
        when(classLoaderLookupPolicyParser.parse(overrides)).thenReturn(classLoaderLookupPolicy);

        new PluginPropertiesBuilder(pluginFolder).overriding(overrides).build();

        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).configuredWith(classLoaderLookupPolicy).assertPluginDescriptor(pluginDescriptor);
    }

    @Test
    public void parsesLoaderExport() throws Exception
    {
        final File pluginFolder = createPluginFolder();

        final Set<String> loaderExport = new HashSet<>();
        loaderExport.add("org.foo");
        loaderExport.add("org.bar");

        new PluginPropertiesBuilder(pluginFolder).exporting(loaderExport).build();

        when(classLoaderLookupPolicyParser.parse(anyString())).thenReturn(ClassLoaderLookupPolicy.NULL_LOOKUP_POLICY);

        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).exporting(loaderExport).assertPluginDescriptor(pluginDescriptor);
    }

    @Test
    public void parsesLibraries() throws Exception
    {
        final File pluginFolder = createPluginFolder();

        final File pluginLibFolder = new File(pluginFolder, "lib");
        assertThat(pluginLibFolder.mkdir(), is(true));

        final File jar1 = createDummyJarFile(pluginLibFolder, "lib1.jar");
        final File jar2 = createDummyJarFile(pluginLibFolder, "lib2.jar");
        final URL[] libraries = new URL[] {jar1.toURI().toURL(), jar2.toURI().toURL()};

        final ApplicationPluginDescriptor pluginDescriptor = descriptorFactory.create(pluginFolder);

        new PluginDescriptorChecker(pluginFolder).containing(libraries).assertPluginDescriptor(pluginDescriptor);
    }

    private File createPluginFolder()
    {
        final File pluginFolder = new File(pluginsFolder.getRoot(), PLUGIN_NAME);
        assertThat(pluginFolder.mkdir(), is(true));
        return pluginFolder;
    }

    private File createDummyJarFile(File pluginLibFolder, String child) throws IOException
    {
        final File jar1 = new File(pluginLibFolder, child);
        FileUtils.write(jar1, "foo");
        return jar1;
    }

    private static class PluginDescriptorChecker
    {

        private final File pluginFolder;
        private URL[] runtimeLibs = new URL[0];;
        private Set<String> exportedPrefixes = Collections.emptySet();
        private ClassLoaderLookupPolicy classLoaderLookupPolicy = ClassLoaderLookupPolicy.NULL_LOOKUP_POLICY;

        public PluginDescriptorChecker(File pluginFolder)
        {
            this.pluginFolder = pluginFolder;
        }

        public PluginDescriptorChecker exporting(Set<String> exports)
        {
            exportedPrefixes = exports;
            return this;
        }

        public PluginDescriptorChecker containing(URL[] libraries)
        {
            runtimeLibs = libraries;
            return this;
        }

        public PluginDescriptorChecker configuredWith(ClassLoaderLookupPolicy classLoaderLookupPolicy)
        {
            this.classLoaderLookupPolicy = classLoaderLookupPolicy;
            return this;
        }

        public void assertPluginDescriptor(ApplicationPluginDescriptor pluginDescriptor)
        {
            assertThat(pluginDescriptor.getName(), equalTo(pluginFolder.getName()));
            try
            {
                assertThat(pluginDescriptor.getRuntimeClassesDir(), equalTo(new File(pluginFolder, "classes").toURI().toURL()));
            }
            catch (MalformedURLException e)
            {
                throw new AssertionError("Can't compare classes dir", e);
            }

            assertThat(pluginDescriptor.getRuntimeLibs(), equalTo(runtimeLibs));
            assertThat(pluginDescriptor.getExportedClassPackages(), equalTo(exportedPrefixes));
            assertThat(pluginDescriptor.getRootFolder(), equalTo(pluginFolder));
            assertThat(pluginDescriptor.getClassLoaderLookupPolicy(), is(classLoaderLookupPolicy));
        }
    }

    private static class PluginPropertiesBuilder
    {

        private final File pluginFolder;
        private String overrides;
        private Set<String> exporting = new HashSet<>();

        public PluginPropertiesBuilder(File pluginFolder)
        {
            this.pluginFolder = pluginFolder;
        }

        public PluginPropertiesBuilder overriding(String overrides)
        {
            this.overrides = overrides;

            return this;
        }

        public PluginPropertiesBuilder exporting(Set<String> exporting)
        {
            this.exporting = exporting;

            return this;
        }

        public File build() throws IOException
        {
            final File pluginProperties = new File(pluginFolder, PLUGIN_PROPERTIES);
            if (pluginProperties.exists())
            {
                throw new IllegalStateException(String.format("File '%s' already exists", pluginProperties.getAbsolutePath()));
            }

            if (!StringUtils.isEmpty(overrides))
            {
                final String descriptorProperty = generateDescriptorProperty(PROPERTY_LOADER_OVERRIDE, overrides);

                stringToFile(pluginProperties.getAbsolutePath(), descriptorProperty, true);
            }

            if (!exporting.isEmpty())
            {
                final String descriptorProperty = generatePackageListProperty(this.exporting, PROPERTY_LOADER_EXPORTED);

                stringToFile(pluginProperties.getAbsolutePath(), descriptorProperty, true);
            }

            return pluginProperties;
        }

        private String generateDescriptorProperty(String propertyName, String propertyValue)
        {
            StringBuilder builder = new StringBuilder(propertyName).append("=").append(propertyValue);

            return builder.toString();
        }

        private String generatePackageListProperty(Set<String> packages, String propertyName)
        {
            StringBuilder builder = new StringBuilder(propertyName).append("=");
            boolean firstElement = true;
            for (String override : packages)
            {
                if (firstElement)
                {
                    firstElement = false;
                }
                else
                {
                    builder.append(",");
                }
                builder.append(override);
            }

            return builder.toString();
        }
    }
}