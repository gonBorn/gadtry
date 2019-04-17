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
package com.github.harbby.gadtry.aop;

import com.github.harbby.gadtry.aop.impl.AopFactoryImpl;
import com.github.harbby.gadtry.aop.impl.CutModeImpl;
import com.github.harbby.gadtry.aop.impl.JavassistProxy;
import com.github.harbby.gadtry.aop.impl.JdkProxy;
import com.github.harbby.gadtry.aop.impl.Proxy;
import com.github.harbby.gadtry.aop.model.MethodInfo;
import com.github.harbby.gadtry.aop.model.Pointcut;
import com.github.harbby.gadtry.aop.v1.FilterBuilder;
import com.github.harbby.gadtry.aop.v1.MethodFilter;
import com.github.harbby.gadtry.collection.mutable.MutableList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.github.harbby.gadtry.base.MoreObjects.checkState;

public interface AopFactory
{
    List<Pointcut> getPointcuts();

    <T> T proxy(Class<T> interfaces, T instance);

    /**
     * Not implemented
     *
     * @param aspects aspects
     * @return AopFactory
     */
    public static AopFactory create(Aspect... aspects)
    {
        List<Pointcut> pointcuts = new ArrayList<>();
        Binder binder = new Binder()
        {
            @Deprecated
            @Override
            public PointBuilder bind(String pointName, String location)
            {
                throw new UnsupportedOperationException("this method have't support!");
            }

            @Override
            public FilterBuilder bind(String pointName)
            {
                Pointcut pointcut = new Pointcut(pointName);
                pointcuts.add(pointcut);
                return new FilterBuilder(pointcut);
            }
        };
        for (Aspect aspect : aspects) {
            aspect.register(binder);
        }

        return new AopFactoryImpl(MutableList.copy(pointcuts));
    }

    public static <T> ByInstance<T> proxy(Class<T> interfaces)
    {
        if (interfaces.isInterface()) {
            return instance -> new ProxyBuilder<>(interfaces, instance, JdkProxy::newProxyInstance);
        }
        else {
            checkState(!Modifier.isFinal(interfaces.getModifiers()), interfaces + " is final");
            return instance -> new ProxyBuilder<>(interfaces, instance, JavassistProxy::newProxyInstance);
        }
    }

    public static <T> ByClass<T> proxyInstance(T instance)
    {
        return aClass -> {
            checkState(aClass.isInstance(instance), instance + " not instanceof " + aClass);
            if (aClass.isInterface()) {
                return new ProxyBuilder<T>(aClass, instance, JdkProxy::newProxyInstance);
            }
            else {
                checkState(!Modifier.isFinal(aClass.getModifiers()), aClass + " is final");
                return new ProxyBuilder<T>(aClass, instance, JavassistProxy::newProxyInstance);
            }
        };
    }

    public interface ByClass<T>
    {
        public ProxyBuilder<T> byClass(Class<?> aclass);
    }

    public interface ByInstance<T>
    {
        public ProxyBuilder<T> byInstance(T instance);
    }

    public static class ProxyBuilder<T>
            extends CutModeImpl<T>
            implements MethodFilter<ProxyBuilder>
    {
        //-- method filter
        private Class<? extends Annotation>[] methodAnnotations;
        private Class<?>[] returnTypes;
        private Function<MethodInfo, Boolean> whereMethod;

        private ProxyBuilder(Class<?> interfaces, T instance, Proxy proxy)
        {
            super(interfaces, instance, proxy);
        }

        @Override
        @SafeVarargs
        public final ProxyBuilder<T> methodAnnotated(Class<? extends Annotation>... methodAnnotations)
        {
            this.methodAnnotations = methodAnnotations;
            return this;
        }

        @Override
        public ProxyBuilder<T> returnType(Class<?>... returnTypes)
        {
            this.returnTypes = returnTypes;
            return this;
        }

        @Override
        public ProxyBuilder<T> whereMethod(Function<MethodInfo, Boolean> whereMethod)
        {
            this.whereMethod = whereMethod;
            return this;
        }

        @Override
        protected Function<MethodInfo, Boolean> getMethodFilter()
        {
            return (methodAnnotations == null || methodAnnotations.length == 0) &&
                    (returnTypes == null || returnTypes.length == 0) ? whereMethod :
                    MethodFilter.buildFilter(methodAnnotations, returnTypes, whereMethod);
        }
    }
}
