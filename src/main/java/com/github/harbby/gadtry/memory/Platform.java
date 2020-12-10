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
package com.github.harbby.gadtry.memory;

import com.github.harbby.gadtry.base.Lazys;
import com.github.harbby.gadtry.base.Maths;
import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import static com.github.harbby.gadtry.base.MoreObjects.checkArgument;
import static java.util.Objects.requireNonNull;

public final class Platform
{
    private Platform() {}

    private static final Unsafe unsafe;

    public static Unsafe getUnsafe()
    {
        return unsafe;
    }

    public static long allocateMemory(long bytes)
    {
        return unsafe.allocateMemory(bytes);  //默认按16个字节对齐
    }

    /**
     * copy  {@link java.nio.ByteBuffer#allocateDirect(int)}
     * must alignByte = Math.sqrt(alignByte)
     */
    public static long[] allocateAlignMemory(long len, long alignByte)
    {
        checkArgument(Maths.isPowerOfTwo(alignByte), "Number %s power of two", alignByte);

        long size = Math.max(1L, len + alignByte);
        long base = 0;
        try {
            base = unsafe.allocateMemory(size);
        }
        catch (OutOfMemoryError x) {
            throw x;
        }
        //unsafe.setMemory(base, size, (byte) 0);

        long dataOffset;
        if (base % alignByte != 0) {
            // Round up to page boundary
            dataOffset = base + alignByte - (base & (alignByte - 1));
        }
        else {
            dataOffset = base;
        }
        long[] res = new long[2];
        res[0] = base;
        res[1] = dataOffset;
        return res;
    }

    public static void freeMemory(long address)
    {
        unsafe.freeMemory(address);
    }

    public static void registerForClean(AutoCloseable closeable)
    {
        Method createMethod = cleanerCreateMethod.get();
        try {
            createMethod.invoke(null, closeable, (Runnable) () -> {
                try {
                    closeable.close();
                }
                catch (Exception e) {
                    throwException(e);
                }
            });
        }
        catch (InvocationTargetException | IllegalAccessException e) {
            throwException(e);
        }
    }

    public static int[] getInts(long address, int count)
    {
        int[] values = new int[count];
        unsafe.copyMemory(null, address, values, Unsafe.ARRAY_INT_BASE_OFFSET, count << 2);
        return values;
    }

    public static void getInts(long address, int[] values, int count)
    {
        unsafe.copyMemory(null, address, values, Unsafe.ARRAY_INT_BASE_OFFSET, count << 2);
    }

    public static void putInts(long address, int[] values)
    {
        unsafe.copyMemory(values, Unsafe.ARRAY_INT_BASE_OFFSET, null, address, values.length << 2);
    }

    public static void putInts(long address, int[] values, int count)
    {
        unsafe.copyMemory(values, Unsafe.ARRAY_INT_BASE_OFFSET, null, address, count << 2);
    }

    private static final Supplier<Method> cleanerCreateMethod = Lazys.goLazy(() -> {
        try {
            Method createMethod;
            try {
                createMethod = Class.forName("sun.misc.Cleaner").getDeclaredMethod("create", Object.class, Runnable.class);
            }
            catch (ClassNotFoundException e) {
                //run vm: --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED
                createMethod = Class.forName("jdk.internal.ref.Cleaner").getDeclaredMethod("create", Object.class, Runnable.class);
            }
            createMethod.setAccessible(true);
            return createMethod;
        }
        catch (ClassNotFoundException | NoSuchMethodException e) {
            throwException(e);
        }
        throw new IllegalStateException("unchecked");
    });

    @SuppressWarnings("unchecked")
    public static <T> Class<T> defineClass(byte[] classBytes, ClassLoader classLoader)
    {
        try {
            Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            defineClass.setAccessible(true);
            return (Class<T>) defineClass.invoke(classLoader, null, classBytes, 0, classBytes.length);
        }
        catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e1) {
            throwException(e1);
        }
        throw new IllegalStateException("unchecked");
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> defineAnonymousClass(Class<?> hostClass, byte[] classBytes, Object[] cpPatches)
    {
        return (Class<T>) unsafe.defineAnonymousClass(hostClass, classBytes, cpPatches);
    }

    public static long reallocateMemory(long address, long oldSize, long newSize)
    {
        long newMemory = unsafe.allocateMemory(newSize);
        copyMemory(null, address, null, newMemory, oldSize);
        unsafe.freeMemory(address);
        return newMemory;
    }

    /**
     * Uses internal JDK APIs to allocate a DirectByteBuffer while ignoring the JVM's
     * MaxDirectMemorySize limit (the default limit is too low and we do not want to require users
     * to increase it).
     *
     * @param size allocate mem size
     * @return ByteBuffer
     */
    public static ByteBuffer allocateDirectBuffer(int size)
    {
        try {
            Class<?> cls = Class.forName("java.nio.DirectByteBuffer");
            Constructor<?> constructor = cls.getDeclaredConstructor(Long.TYPE, Integer.TYPE);
            constructor.setAccessible(true);
            Field cleanerField = cls.getDeclaredField("cleaner");
            cleanerField.setAccessible(true);
            long memory = unsafe.allocateMemory(size);
            ByteBuffer buffer = (ByteBuffer) constructor.newInstance(memory, size);
            Method createMethod = cleanerCreateMethod.get();
            Object cleaner = createMethod.invoke(null, buffer, (Runnable) () -> unsafe.freeMemory(memory));
            //Cleaner cleaner = Cleaner.create(buffer, () -> _UNSAFE.freeMemory(memory));
            cleanerField.set(buffer, cleaner);
            return buffer;
        }
        catch (Exception e) {
            throwException(e);
        }
        throw new IllegalStateException("unreachable");
    }

    @SuppressWarnings("unchecked")
    public static <T> T allocateInstance(Class<T> tClass)
            throws InstantiationException
    {
        return (T) unsafe.allocateInstance(tClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> T allocateInstance2(Class<T> tClass)
            throws InstantiationException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Constructor<T> superCons = (Constructor<T>) Object.class.getConstructor();
        ReflectionFactory reflFactory = ReflectionFactory.getReflectionFactory();
        Constructor<T> c = (Constructor<T>) reflFactory.newConstructorForSerialization(tClass, superCons);
        return c.newInstance();
    }

    public static void copyMemory(Object src, long srcOffset, Object dst, long dstOffset, long length)
    {
        // Check if dstOffset is before or after srcOffset to determine if we should copy
        // forward or backwards. This is necessary in case src and dst overlap.
        if (dstOffset < srcOffset) {
            while (length > 0) {
                long size = Math.min(length, UNSAFE_COPY_THRESHOLD);
                unsafe.copyMemory(src, srcOffset, dst, dstOffset, size);
                length -= size;
                srcOffset += size;
                dstOffset += size;
            }
        }
        else {
            srcOffset += length;
            dstOffset += length;
            while (length > 0) {
                long size = Math.min(length, UNSAFE_COPY_THRESHOLD);
                srcOffset -= size;
                dstOffset -= size;
                unsafe.copyMemory(src, srcOffset, dst, dstOffset, size);
                length -= size;
            }
        }
    }

    /**
     * Raises an exception bypassing compiler checks for checked exceptions.
     *
     * @param t Throwable
     */
    public static void throwException(Throwable t)
    {
        unsafe.throwException(t);
    }

    /**
     * Limits the number of bytes to copy per {@link Unsafe#copyMemory(long, long, long)} to
     * allow safepoint polling during a large copy.
     */
    private static final long UNSAFE_COPY_THRESHOLD = 1024L * 1024L;

    static {
        sun.misc.Unsafe obj = null;
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            obj = (sun.misc.Unsafe) unsafeField.get(null);
        }
        catch (Throwable cause) {
            throwException(cause);
        }
        unsafe = requireNonNull(obj);
    }
}