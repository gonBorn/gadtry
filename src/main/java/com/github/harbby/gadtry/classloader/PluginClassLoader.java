/*
 * Copyright (C) 2018 The Harbby Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.harbby.gadtry.classloader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class PluginClassLoader
        extends DirClassLoader
{
    private static final ClassLoader PLATFORM_CLASS_LOADER = findPlatformClassLoader();

    private final ClassLoader spiClassLoader;
    private final List<String> spiPackages;
    private final List<String> spiResources;

    public PluginClassLoader(
            List<URL> urls,
            ClassLoader spiClassLoader,
            List<String> spiPackages)
    {
        this(urls,
                spiClassLoader,
                spiPackages,
                spiPackages.stream().map(PluginClassLoader::classNameToResource).collect(Collectors.toList()));
    }

    private PluginClassLoader(
            List<URL> urls,
            ClassLoader spiClassLoader,
            List<String> spiPackages,
            List<String> spiResources)
    {
        // plugins should not have access to the system (application) class loader
        super(urls.toArray(new URL[urls.size()]), PLATFORM_CLASS_LOADER);
        this.spiClassLoader = requireNonNull(spiClassLoader, "spiClassLoader is null");
        this.spiPackages = Collections.unmodifiableList(spiPackages);  //== ImmutableList.copyOf()
        this.spiResources = Collections.unmodifiableList(spiResources);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException
    {
        // grab the magic lock
        synchronized (getClassLoadingLock(name)) {
            // Check if class is in the loaded classes cache
            Class<?> cachedClass = findLoadedClass(name);
            if (cachedClass != null) {
                return resolveClass(cachedClass, resolve);
            }

            // If this is an SPI class, only check SPI class loader
            if (isSpiClass(name)) {
                return resolveClass(spiClassLoader.loadClass(name), resolve);
            }

            // Look for class locally
            return super.loadClass(name, resolve);
        }
    }

    private Class<?> resolveClass(Class<?> clazz, boolean resolve)
    {
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    @Override
    public URL getResource(String name)
    {
        // If this is an SPI resource, only check SPI class loader
        if (isSpiResource(name)) {
            return spiClassLoader.getResource(name);
        }

        // Look for resource locally
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name)
            throws IOException
    {
        // If this is an SPI resource, use SPI resources
        if (isSpiClass(name)) {
            return spiClassLoader.getResources(name);
        }

        // Use local resources
        return super.getResources(name);
    }

    private boolean isSpiClass(String name)
    {
        // todo maybe make this more precise and only match base package
        return spiPackages.stream().anyMatch(name::startsWith);
    }

    private boolean isSpiResource(String name)
    {
        // todo maybe make this more precise and only match base package
        return spiResources.stream().anyMatch(name::startsWith);
    }

    private static String classNameToResource(String className)
    {
        return className.replace('.', '/');
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private static ClassLoader findPlatformClassLoader()
    {
        try {
            // use platform class loader on Java 9
            Method method = ClassLoader.class.getMethod("getPlatformClassLoader");
            return (ClassLoader) method.invoke(null);
        }
        catch (NoSuchMethodException ignored) {
            // use null class loader on Java 8
            return null;
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }
}
