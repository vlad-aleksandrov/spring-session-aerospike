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
package us.swcraft.springframework.store.aerospike;

import java.util.Set;

import com.aerospike.client.Bin;
import com.aerospike.client.Record;
import com.aerospike.client.query.IndexType;
import com.aerospike.client.query.RecordSet;

/**
 * Interface that specified a basic set of Aerospike operations, implemented by {@link AerospikeTemplate}. Not often used but a
 * useful option for extensibility and testability (as it can be easily mocked or stubbed).
 * 
 * @author Vlad Aleksandrov
 */
public interface AerospikeOperations<K> {

    boolean hasKey(K key);

    void delete(K key);
    
    void deleteBin(K key, String binName);
    
    /**
     * Removes all records from set.
     */
    void deleteAll();
    
    /**
     * Persists a single bin in record. 
     * @param key
     * @param data
     */
    void persist(K key, Bin value);
    
    /**
     * Persists multiple bins in the record. 
     * @param key
     * @param data
     */
    void persist(K key, Set<Bin> data);

    Record fetch(K key);
    
    /**
     * Creates secondary index.
     * @param binName
     * @param indexName
     * @param indexType
     */
    void createIndex(String binName, String indexName, IndexType indexType);
    
    /**
     * Fetches keys of records matching range query for indexed bin.
     * @param binName
     * @param begin
     * @param end
     * @return
     */
    Set<K> fetchRange(String idBinName, String indexedBinName, long begin, long end);

}
