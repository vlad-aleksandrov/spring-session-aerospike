/*
 * Copyright 2015 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package v.a.org.springframework.store.aerospike;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.CommitLevel;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;

/**
 * Helper class that simplifies Aerospike data access code.
 * <p/>
 * Performs automatic serialization/deserialization between the given objects and the underlying binary data in the
 * Aerospike store. It uses Kryo serialization with Snappy compression for objects
 * <p/>
 * Once configured, this class is thread-safe.
 * <p/>
 * Note that while the template is generified, it is up to the serializers/deserializers to properly convert the given
 * Objects to and from binary data.
 * <p/>
 * <b>This is the central class in Aerospike support</b>.
 * 
 * @author Vlad Aleksandrov
 */
public class AerospikeTemplate extends AerospikeAccessor implements AerospikeOperations<String> {

    private final static Bin[] BIN_ARRAY_TYPE = new Bin[0];
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String namespace;
    private String setname;

    private WritePolicy deletePolicy;
    private WritePolicy writePolicy;
    private Policy readPolicy;

    public void init() {
        deletePolicy = new WritePolicy();
        deletePolicy.commitLevel = CommitLevel.COMMIT_MASTER;

        writePolicy = new WritePolicy();
        writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        writePolicy.commitLevel = CommitLevel.COMMIT_ALL;

        readPolicy = new Policy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasKey(final String key) {
        log.trace("has {} key?", key);
        Assert.notNull(key, "key can't be null");
        final Key recordKey = new Key(namespace, setname, key);
        return getAerospikeClient().exists(readPolicy, recordKey);
    }

    @Override
    public void delete(final String key) {
        log.trace("delete {} key", key);
        Assert.notNull(key, "key can't be null");
        final Key recordKey = new Key(namespace, setname, key);
        getAsyncAerospikeClient().delete(deletePolicy, recordKey);
    }

    @Override
    public void deleteBin(final String key, final String binName) {
        log.trace("delete {} bin in record key {}", binName, key);
        Assert.notNull(key, "key can't be null");
        final Key recordKey = new Key(namespace, setname, key);
        Assert.notNull(binName, "bin name can't be null");
        final Bin bin = Bin.asNull(binName);
        getAsyncAerospikeClient().put(deletePolicy, recordKey, bin);
    }

    @Override
    public void persist(final String key, final Bin bin) {
        log.trace("persist {} bin in record key {}", bin, key);
        Assert.notNull(key, "key can't be null");
        final Key recordKey = new Key(namespace, setname, key);
        Assert.notNull(bin, "bin can't be null");
        getAerospikeClient().put(writePolicy, recordKey, bin);
    }

    @Override
    public void persist(final String key, final Set<Bin> bins) {
        Assert.notNull(key, "key can't be null");
        final Key recordKey = new Key(namespace, setname, key);
        Assert.notNull(bins, "bins can't be null");
        Assert.notEmpty(bins, "bins should have data to store");
        getAerospikeClient().put(writePolicy, recordKey, bins.toArray(BIN_ARRAY_TYPE));
    }

    @Override
    public Record fetch(final String key) {
        Assert.notNull(key, "key can't be null");
        final Key recordKey = new Key(namespace, setname, key);
        return getAerospikeClient().get(readPolicy, recordKey);
    }

}
