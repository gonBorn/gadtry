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
package com.github.harbby.gadtry.collection.tuple;

import java.util.Objects;

public class Tuple7<F1, F2, F3, F4, F5, F6, F7>
        implements Tuple
{
    private final F1 f1;
    private final F2 f2;
    private final F3 f3;
    private final F4 f4;
    private final F5 f5;
    private final F6 f6;
    private final F7 f7;

    public Tuple7(F1 f1, F2 f2, F3 f3, F4 f4, F5 f5, F6 f6, F7 f7)
    {
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.f4 = f4;
        this.f5 = f5;
        this.f6 = f6;
        this.f7 = f7;
    }

    public static <F1, F2, F3, F4, F5, F6, F7> Tuple7<F1, F2, F3, F4, F5, F6, F7> of(F1 f1, F2 f2, F3 f3, F4 f4, F5 f5, F6 f6, F7 f7)
    {
        return new Tuple7<>(f1, f2, f3, f4, f5, f6, f7);
    }

    public F1 f1()
    {
        return f1;
    }

    public F2 f2()
    {
        return f2;
    }

    public F3 f3()
    {
        return f3;
    }

    public F4 f4()
    {
        return f4;
    }

    public F5 f5()
    {
        return f5;
    }

    public F6 f6()
    {
        return f6;
    }

    public F7 f7()
    {
        return f7;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(f1, f2, f3, f4, f5, f6, f7);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        @SuppressWarnings("rawtypes")
        Tuple7 other = (Tuple7) obj;
        return Objects.equals(this.f1, other.f1) &&
                Objects.equals(this.f2, other.f2) &&
                Objects.equals(this.f3, other.f3) &&
                Objects.equals(this.f4, other.f4) &&
                Objects.equals(this.f5, other.f5) &&
                Objects.equals(this.f6, other.f6) &&
                Objects.equals(this.f7, other.f7);
    }

    @Override
    public String toString()
    {
        return String.format("(%s, %s, %s, %s, %s, %s, %s)", f1, f2, f3, f4, f5, f6, f7);
    }

    @Override
    public int getArity()
    {
        return 7;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getField(int pos)
    {
        switch (pos) {
            case 1:
                return (T) f1;
            case 2:
                return (T) f2;
            case 3:
                return (T) f3;
            case 4:
                return (T) f4;
            case 5:
                return (T) f5;
            case 6:
                return (T) f6;
            case 7:
                return (T) f7;
            default:
                throw new IndexOutOfBoundsException(String.valueOf(pos));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Tuple7<F1, F2, F3, F4, F5, F6, F7> copy()
    {
        return new Tuple7<>(f1, f2, f3, f4, f5, f6, f7);
    }
}
