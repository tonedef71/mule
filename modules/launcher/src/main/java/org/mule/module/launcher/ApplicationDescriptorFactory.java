/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static java.lang.String.format;
import org.mule.config.PreferredObjectSelector;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicyParser;
import org.mule.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.descriptor.DescriptorParser;
import org.mule.module.launcher.descriptor.EmptyApplicationDescriptor;
import org.mule.module.launcher.descriptor.PropertiesDescriptorParser;
import org.mule.module.launcher.plugin.ApplicationPluginDescriptor;
import org.mule.module.launcher.plugin.ApplicationPluginDescriptorFactory;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.FileUtils;
import org.mule.util.FilenameUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.imageio.spi.ServiceRegistry;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * Creates artifact descriptor for application
 */
public class ApplicationDescriptorFactory implements ArtifactDescriptorFactory<ApplicationDescriptor>
{

    public static final String SYSTEM_PROPERTY_OVERRIDE = "-O";
    public static final String PROPERTIES_DESCRIPTOR_PARSER = "properties";

    protected Map<String, DescriptorParser> parserRegistry = new HashMap<>();
    private ApplicationPluginDescriptorFactory pluginDescriptorFactory = new ApplicationPluginDescriptorFactory(new ClassLoaderLookupPolicyParser());

    public ApplicationDescriptorFactory()
    {
        // defaults first
        parserRegistry.put(PROPERTIES_DESCRIPTOR_PARSER, new PropertiesDescriptorParser(new ClassLoaderLookupPolicyParser()));

        final Iterator<DescriptorParser> it = ServiceRegistry.lookupProviders(DescriptorParser.class);

        MultiMap overrides = new MultiValueMap();
        while (it.hasNext())
        {
            final DescriptorParser parser = it.next();
            overrides.put(parser.getSupportedFormat(), parser);
        }
        mergeParserOverrides(overrides);
    }

    public ApplicationDescriptor create(File artifactFolder) throws ArtifactDescriptorCreateException
    {

        if (!artifactFolder.exists())
        {
            throw new IllegalArgumentException(format("Application directory does not exist: '%s'", artifactFolder));
        }

        final String appName = artifactFolder.getName();

        @SuppressWarnings("unchecked")
        Collection<File> deployFiles = FileUtils.listFiles(artifactFolder, new WildcardFileFilter("mule-deploy.*"), null);
        if (deployFiles.size() > 1)
        {
            throw new ArtifactDescriptorCreateException(format("More than one mule-deploy descriptors found in application '%s'", appName));
        }

        ApplicationDescriptor desc;

        try
        {
            // none found, return defaults
            if (deployFiles.isEmpty())
            {
                desc = new EmptyApplicationDescriptor(appName);
            }
            else
            {
                // lookup the implementation by extension
                final File descriptorFile = deployFiles.iterator().next();
                final String ext = FilenameUtils.getExtension(descriptorFile.getName());
                final DescriptorParser descriptorParser = parserRegistry.get(ext);

                if (descriptorParser == null)
                {
                    throw new ArtifactDescriptorCreateException(format("Unsupported deployment descriptor format for app '%s': %s", appName, ext));
                }

                desc = descriptorParser.parse(descriptorFile, appName);

                // app name is external to the deployment descriptor
                desc.setName(appName);
            }

            // get a ref to an optional app props file (right next to the descriptor)
            final File appPropsFile = new File(artifactFolder, ApplicationDescriptor.DEFAULT_APP_PROPERTIES_RESOURCE);
            setApplicationProperties(desc, appPropsFile);

            final Set<ApplicationPluginDescriptor> plugins = parsePluginDescriptors(artifactFolder, desc);
            desc.setPlugins(plugins);

            desc.setSharedPluginLibs(findSharedPluginLibs(appName));
        }
        catch (IOException e)
        {
            throw new ArtifactDescriptorCreateException("Unable to create application descriptor", e);
        }

        return desc;
    }

    public void setPluginDescriptorFactory(ApplicationPluginDescriptorFactory pluginDescriptorFactory)
    {
        this.pluginDescriptorFactory = pluginDescriptorFactory;
    }

    private Set<ApplicationPluginDescriptor> parsePluginDescriptors(File appDir, ApplicationDescriptor appDescriptor) throws IOException
    {
        final File pluginsDir = new File(appDir, MuleFoldersUtil.PLUGINS_FOLDER);
        String[] pluginZips = pluginsDir.list(new SuffixFileFilter(".zip"));
        if (pluginZips == null || pluginZips.length == 0)
        {
            return Collections.emptySet();
        }

        Arrays.sort(pluginZips);
        Set<ApplicationPluginDescriptor> pds = new HashSet<>(pluginZips.length);

        for (String pluginZip : pluginZips)
        {
            final String pluginName = StringUtils.removeEnd(pluginZip, ".zip");
            // must unpack as there's no straightforward way for a ClassLoader to use a jar within another jar/zip
            final File tmpDir = new File(MuleContainerBootstrapUtils.getMuleTmpDir(),
                                         appDescriptor.getName() + "/plugins/" + pluginName);
            FileUtils.unzip(new File(pluginsDir, pluginZip), tmpDir);
            final ApplicationPluginDescriptor pd = pluginDescriptorFactory.create(tmpDir);

            pds.add(pd);
        }

        return pds;
    }

    private URL[] findSharedPluginLibs(String appName) throws MalformedURLException
    {
        Set<URL> urls = new HashSet<>();

        final File sharedPluginLibs = MuleFoldersUtil.getAppSharedPluginLibsFolder(appName);
        if (sharedPluginLibs.exists())
        {
            Collection<File> jars = FileUtils.listFiles(sharedPluginLibs, new String[] {"jar"}, false);

            for (File jar : jars)
            {
                urls.add(jar.toURI().toURL());
            }
        }

        return urls.toArray(new URL[0]);
    }

    public void setApplicationProperties(ApplicationDescriptor desc, File appPropsFile)
    {
        // ugh, no straightforward way to convert a HashTable to a map
        Map<String, String> m = new HashMap<>();

        if (appPropsFile.exists() && appPropsFile.canRead())
        {
            final Properties props;
            try
            {
                props = PropertiesUtils.loadProperties(appPropsFile.toURI().toURL());
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("Unable to obtain application properties file URL", e);
            }
            for (Object key : props.keySet())
            {
                m.put(key.toString(), props.getProperty(key.toString()));
            }
        }

        // Override with any system properties prepended with "-O" for ("override"))
        Properties sysProps = System.getProperties();
        for (Map.Entry<Object, Object> entry : sysProps.entrySet())
        {
            String key = entry.getKey().toString();
            if (key.startsWith(SYSTEM_PROPERTY_OVERRIDE))
            {
                m.put(key.substring(SYSTEM_PROPERTY_OVERRIDE.length()), entry.getValue().toString());
            }
        }
        desc.setAppProperties(m);
    }

    /**
     * Merge default and discovered overrides for descriptor parsers, taking weight into account
     *
     * @param overrides discovered parser overrides
     */
    protected void mergeParserOverrides(MultiMap overrides)
    {
        PreferredObjectSelector<DescriptorParser> selector = new PreferredObjectSelector<>();

        for (Map.Entry<String, DescriptorParser> entry : parserRegistry.entrySet())
        {
            @SuppressWarnings("unchecked")
            final Collection<DescriptorParser> candidates = (Collection<DescriptorParser>) overrides.get(entry.getKey());

            if (candidates != null)
            {
                parserRegistry.put(entry.getKey(), selector.select(candidates.iterator()));
            }
        }

    }
}
