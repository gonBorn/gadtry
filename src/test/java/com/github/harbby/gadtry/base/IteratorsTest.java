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
package com.github.harbby.gadtry.base;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IteratorsTest
{
    @Test
    public void isEmptyTest()
    {
        List list = new ArrayList();
        Assert.assertTrue(Iterators.isEmpty(list));
    }

    @Test
    public void iteratorIsEmptyTest()
    {
        Iterator<?> iterator = Iterators.empty();
        Assert.assertTrue(Iterators.isEmpty(iterator));
    }
}
