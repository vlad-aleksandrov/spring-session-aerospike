/*
 * Copyright 2015 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package us.swcraft.springframework.session.aerospike;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import us.swcraft.springframework.session.aerospike.Attributes;

public class AttributesTest {
    
    @Test
    public void atomicLong() {
        assertThat(Attributes.areEqual(new AtomicLong(4), new AtomicLong(4)), is(true));
        assertThat(Attributes.areEqual(new AtomicLong(4), new AtomicLong(5)), is(false));
    }
    
    @Test
    public void atomicInteger() {
        assertThat(Attributes.areEqual(new AtomicInteger(4), new AtomicInteger(4)), is(true));
        assertThat(Attributes.areEqual(new AtomicInteger(4), new AtomicInteger(5)), is(false));
    }
    
    @Test
    public void atomicIntegerInMap() {
        Map<String, AtomicInteger> m1 = new HashMap<>();
        AtomicInteger v1 = new AtomicInteger(0);
        m1.put("K", v1);
        
        Map<String, AtomicInteger> m2 = new HashMap<>();
        AtomicInteger v2 = new AtomicInteger(0);
        m2.put("K", v2);
        
        assertThat(Attributes.areEqual(m1, m2), is(true));
        v2.incrementAndGet();
        assertThat(Attributes.areEqual(m1, m2), is(false));

    }

}
