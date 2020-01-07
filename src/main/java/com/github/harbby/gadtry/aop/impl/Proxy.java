/*
 * Copyright (C) 2018 The GadTry Authors
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
package com.github.harbby.gadtry.aop.impl;

import com.github.harbby.gadtry.aop.ProxyRequest;
import com.github.harbby.gadtry.base.Arrays;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;

import static com.github.harbby.gadtry.base.MoreObjects.checkState;

public interface Proxy
{
    <T> T getProxy(ClassLoader loader, InvocationHandler handler, Class<?>... interfaces);

    public static <T> T proxy(ProxyRequest<T> request)
    {
        Class<?> superclass = request.getSuperclass();

        Class<?>[] interfaces = Arrays.asArray(superclass, request.getInterfaces(), Class.class);
        if (superclass.isInterface() && request.getBasePackage() == null) {
            return JdkProxy.newProxyInstance(request.getClassLoader(), request.getHandler(), interfaces);
        }
        else {
            checkState(!Modifier.isFinal(superclass.getModifiers()), superclass + " is final");
            return JavassistProxy.newProxyInstance(request);
        }
    }
}
