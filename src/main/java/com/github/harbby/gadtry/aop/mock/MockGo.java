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
package com.github.harbby.gadtry.aop.mock;

import com.github.harbby.gadtry.aop.ProxyRequest;
import com.github.harbby.gadtry.aop.aopgo.AroundHandler;
import com.github.harbby.gadtry.aop.impl.Proxy;
import com.github.harbby.gadtry.base.JavaTypes;
import com.github.harbby.gadtry.base.Platform;
import com.github.harbby.gadtry.collection.tuple.Tuple1;
import com.github.harbby.gadtry.collection.tuple.Tuple2;

import java.lang.reflect.Method;

import static com.github.harbby.gadtry.aop.impl.Proxy.getInvocationHandler;
import static com.github.harbby.gadtry.base.Strings.lowerFirst;
import static com.github.harbby.gadtry.base.Throwables.throwsThrowable;

/**
 * MockGo
 */
public class MockGo
{
    /**
     * ThreadLocal容器性能较差,降低代理函数调用性能
     * 且这个容器只会在when().then()场景有用．因此不再考虑并发when().then()
     */
    static final Tuple1<Tuple2<Object, Method>> LAST_MOCK_BY_WHEN_METHOD = new Tuple1<>(null);

    private MockGo() {}

    public static <T> T spy(T target)
    {
        return spy((Class<T>) target.getClass(), target);
    }

    public static <T> T spy(Class<T> superclass)
    {
        try {
            T target = Platform.allocateInstance2(superclass);
            return spy(superclass, target);
        }
        catch (Exception e) {
            throw throwsThrowable(e);
        }
    }

    public static <T> T spy(Class<T> superclass, T target)
    {
        AopInvocationHandler aopInvocationHandler = new AopInvocationHandler(target);
        ProxyRequest<T> request = ProxyRequest.builder(superclass)
                .setInvocationHandler(aopInvocationHandler)
                .setTarget(target)
                .setClassLoader(superclass.getClassLoader())
                .disableSuperMethod()  //和when().do...() 冲突,因此此处关闭 see: MockGoTest.disableSuperMethodMockSpyByWhen()
                .build();
        T proxy = Proxy.proxy(request);
        aopInvocationHandler.setProxyClass(proxy.getClass());
        return proxy;
    }

    public static <T> T mock(Class<T> superclass)
    {
        AopInvocationHandler aopInvocationHandler = new AopInvocationHandler();
        ProxyRequest<T> request = ProxyRequest.builder(superclass)
                .setClassLoader(superclass.getClassLoader())
                .setInvocationHandler(aopInvocationHandler)
                .disableSuperMethod()
                .build();
        T proxy = Proxy.proxy(request);
        aopInvocationHandler.setProxyClass(proxy.getClass());
        when(proxy.toString()).thenReturn(lowerFirst(superclass.getSimpleName()));
        return proxy;
    }

    public static void initMocks(Object testObject)
    {
        MockGoAnnotations.initMocks(testObject);
    }

    public static DoBuilder doReturn(Object value)
    {
        return new DoBuilder(f -> value);
    }

    public static DoBuilder doNothing()
    {
        return doAround(f -> {
            if (f.getMethod().getReturnType() != void.class) {
                throw new MockGoException("Only void methods can doNothing()!\n" +
                        "Example of correct use of doNothing():\n" +
                        "    doNothing().\n" +
                        "    .when(mock).someVoidMethod();");
            }
            return null;
        });
    }

    public static DoBuilder doAnswer(AroundHandler function)
    {
        return doAround(function);
    }

    public static DoBuilder doAround(AroundHandler function)
    {
        return new DoBuilder(function);
    }

    public static class DoBuilder
    {
        private final AroundHandler function;

        public DoBuilder(AroundHandler function)
        {
            this.function = function;
        }

        public <T> T when(T instance)
        {
            AopInvocationHandler aopInvocationHandler = getInvocationHandler(instance);
            aopInvocationHandler.setHandler((proxy, method, args) -> {
                aopInvocationHandler.initHandler();
                aopInvocationHandler.register(method, function);
                return JavaTypes.getClassInitValue(method.getReturnType());
            });
            return instance;
        }
    }

    public static <T> WhenThenBuilder<T> when(T methodCallValue)
    {
        return new WhenThenBuilder<>();
    }

    public static DoBuilder doThrow(Throwable e)
    {
        return new DoBuilder(f -> { throw e; });
    }

    public static class WhenThenBuilder<T>
    {
        private final Tuple2<Object, Method> lastWhenMethod;

        public WhenThenBuilder()
        {
            this.lastWhenMethod = LAST_MOCK_BY_WHEN_METHOD.get();
            LAST_MOCK_BY_WHEN_METHOD.set(null);
            if (lastWhenMethod == null) {
                throw new MockGoException("when(...) does not select any Method\n" +
                        "Example of: \n" +
                        " when(someMethod(any...)).then...()");
            }
        }

        public void thenReturn(T value)
        {
            bind(p -> value);
        }

        public void thenAround(AroundHandler function)
        {
            bind(function);
        }

        public void thenThrow(Throwable e)
        {
            bind(f -> { throw e; });
        }

        private void bind(AroundHandler pointcut)
        {
            AopInvocationHandler handler = getInvocationHandler(lastWhenMethod.f1());
            handler.register(lastWhenMethod.f2(), pointcut);
        }
    }
}
