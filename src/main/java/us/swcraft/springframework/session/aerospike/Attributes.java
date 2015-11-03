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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class consists of {@code static} utility methods for operating
 * on attributes.
 */
class Attributes {

    /**
     * Checks if attributes are equal for saving optimization purposes - attributes are not serialized and stored
     * if there is no changes in values. For the purposes of session attributes
     * atomics are "equal" it they wrap the equal values.
     * 
     * @param o1
     * @param o2
     * @return <code>true</code> if attributes are equal.
     */
    public static boolean areEqual(final Object o1, final Object o2) {
        if (o1 instanceof AtomicInteger && o2 instanceof AtomicInteger) {
            return areEqualAtomicInteger((AtomicInteger) o1, (AtomicInteger) o2);
        }
        if (o1 instanceof AtomicLong && o2 instanceof AtomicLong) {
            return areEqualAtomicLong((AtomicLong) o1, (AtomicLong) o2);
        }
        if (o1 instanceof AtomicBoolean && o2 instanceof AtomicBoolean) {
            return areEqualAtomicBoolean((AtomicBoolean) o1, (AtomicBoolean) o2);
        }
        if (o1 instanceof Map && o2 instanceof Map) {
            return areEqualMaps((Map<?, ?>) o1, (Map<?, ?>) o2);
        } else {
            return Objects.deepEquals(o1, o2);
        }
    }

    /**
     * Checks if two maps are equal.
     * 
     * @param m1
     * @param m2
     * @return <code>true</code> if both maps have the same size and equal key-value pairs.
     */
    private static boolean areEqualMaps(final Map<?, ?> m1, final Map<?, ?> m2) {
        if (m1.size() != m2.size()) {
            return false;
        }
        for (Object key : m1.keySet()) {
            Object v1 = m1.get(key);
            Object v2 = m2.get(key);
            if (v1 instanceof AtomicInteger && v2 instanceof AtomicInteger) {
                return areEqualAtomicInteger((AtomicInteger) v1, (AtomicInteger) v2);
            }
            if (v1 instanceof AtomicLong && v2 instanceof AtomicLong) {
                return areEqualAtomicLong((AtomicLong) v1, (AtomicLong) v2);
            }
            if (v1 instanceof AtomicBoolean && v2 instanceof AtomicBoolean) {
                return areEqualAtomicBoolean((AtomicBoolean) v1, (AtomicBoolean) v2);
            }

            if (!Objects.deepEquals(v1, v2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean areEqualAtomicInteger(final AtomicInteger a1, final AtomicInteger a2) {
        return a1.get() == a2.get();
    }

    private static boolean areEqualAtomicLong(final AtomicLong a1, final AtomicLong a2) {
        return a1.get() == a2.get();
    }

    private static boolean areEqualAtomicBoolean(final AtomicBoolean a1, final AtomicBoolean a2) {
        return a1.get() == a2.get();
    }

}
