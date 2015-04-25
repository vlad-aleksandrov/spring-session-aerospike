/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package v.a.org.springframework.store.aerospike;

import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * Interface that specified a basic set of Aerospike operations, implemented by {@link AerospikeTemplate}. Not often used but a
 * useful option for extensibility and testability (as it can be easily mocked or stubbed).
 * 
 * @author Vlad Aleksandrov
 */
public interface AerospikeOperations<K, V> {

    boolean hasKey(K key);

    void delete(K ... key);


    Boolean expire(K key, long timeout, TimeUnit unit);

    Boolean expireAt(K key, Date date);

    void persist(K key, V value);


}
