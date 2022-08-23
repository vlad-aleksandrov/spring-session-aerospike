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
package us.swcraft.springframework.session.store.aerospike;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.ScanCallback;
import com.aerospike.client.policy.CommitLevel;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.IndexType;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.client.task.IndexTask;

/**
 * Helper class that simplifies Aerospike data access code.
 * <br>
 * Performs automatic serialization/deserialization between the given objects
 * and the underlying binary data in the Aerospike store. It uses Kryo
 * serialization with Snappy compression for objects
 * <br>
 * Once configured, this class is thread-safe.
 * <br>
 * Note that while the template is generified, it is up to the
 * serializers/deserializers to properly convert the given Objects to and from
 * binary data.
 * <br>
 * <b>This is the central class in Aerospike support</b>.
 *
 * @author Vlad Aleksandrov
 */
public class AerospikeTemplate extends AerospikeAccessor implements AerospikeOperations<String> {

    private final static Bin[] BIN_ARRAY_TYPE = new Bin[0];
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Namespace name.
     */
    private String namespace;

    /**
     * Aerospike name for session data.
     */
    private String setname;

    /**
     * Aerospike session record TTL.
     */
    private int expiration;

    private WritePolicy deletePolicy;
    private WritePolicy writePolicy;
    private Policy readPolicy;

    public void init() {
        Assert.hasLength(namespace, "Aerospike 'namespace' name for session data is not configured");
        Assert.hasLength(setname, "Aerospike 'setname' name for session data is not configured");

        deletePolicy = new WritePolicy();
        deletePolicy.commitLevel = CommitLevel.COMMIT_MASTER;

        writePolicy = new WritePolicy();
        writePolicy.recordExistsAction = RecordExistsAction.UPDATE;
        writePolicy.commitLevel = CommitLevel.COMMIT_ALL;
        writePolicy.expiration = expiration;

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
        try {
            return getAerospikeClient().exists(readPolicy, recordKey);
        } catch (AerospikeException e) {
            log.error("check exist  fails", e);
            return false;
        }
    }

    @Override
    public void delete(final String key) {
        log.trace("delete {} key", key);
        Assert.notNull(key, "key can't be null");
        final Key recordKey = new Key(namespace, setname, key);
        try {
            getAerospikeClient().delete(deletePolicy, recordKey);
        } catch (AerospikeException e) {
            log.error("delete key fails", e);
        }
    }

    @Override
    public void deleteBin(final String key, final String binName) {
        log.trace("delete {} bin in record key {}", binName, key);
        Assert.notNull(key, "key can't be null");
        final Key recordKey = new Key(namespace, setname, key);
        Assert.notNull(binName, "bin name can't be null");
        final Bin bin = Bin.asNull(binName);
        try {
            getAerospikeClient().put(deletePolicy, recordKey, bin);
        } catch (AerospikeException e) {
            log.error("delete bin fails", e);
        }
    }

    @Override
    public void persist(final String key, final Bin bin) {
        log.trace("persist {} bin in record key {}", bin, key);
        Assert.notNull(key, "key can't be null");
        final Key recordKey = new Key(namespace, setname, key);
        Assert.notNull(bin, "bin can't be null");
        try {
            getAerospikeClient().put(writePolicy, recordKey, bin);
        } catch (AerospikeException e) {
            log.error("write fails", e);
        }
    }

    @Override
    public void persist(final String key, final Set<Bin> bins) {
        Assert.notNull(key, "key can't be null");
        final Key recordKey = new Key(namespace, setname, key);
        Assert.notNull(bins, "bins can't be null");
        Assert.notEmpty(bins, "bins should have data to store");
        try {
            getAerospikeClient().put(writePolicy, recordKey, bins.toArray(BIN_ARRAY_TYPE));
        } catch (AerospikeException e) {
            log.error("write fails", e);
        }
    }

    @Override
    public Record fetch(final String key) {
        Assert.notNull(key, "key can't be null");
        final Key recordKey = new Key(namespace, setname, key);
        try {
            final Record r = getAerospikeClient().get(readPolicy, recordKey);
            if (r != null) {
                getAerospikeClient().touch(writePolicy, recordKey);
            }
            return r;
        } catch (AerospikeException e) {
            log.error("read fails", e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createIndex(final String binName, final String indexName, final IndexType indexType) {
        final Policy policy = new Policy();
        policy.totalTimeout = 0; // Do not timeout on index create.

        try {
            final IndexTask task = getAerospikeClient().createIndex(policy, namespace, setname, indexName, binName,
                    indexType);
            task.waitTillComplete();
            log.debug("Index '{}' on {}:{} bin '{}' created", indexName, namespace, setname, binName);

        } catch (AerospikeException e) {
            if (e.getResultCode() == 200) {
                log.trace("Index {} already exists", indexName);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> fetchRange(final String idBinName, final String indexedBinName, final long begin,
            final long end) {

        log.debug("Fetch range {}:{} on {}:{} bin '{}'", begin, end, namespace, setname, indexedBinName);

        final Statement stmt = new Statement();
        stmt.setNamespace(namespace);
        stmt.setSetName(setname);
        stmt.setBinNames(indexedBinName);
        stmt.setFilter(Filter.range(indexedBinName, begin, end));

        try {
            final RecordSet rs = getAerospikeClient().query(null, stmt);
            final Set<String> result = new HashSet<>();
            try {
                while (rs.next()) {
                    Key key = rs.getKey();
                    log.trace("Found key: {}", key);
                    Record record = getAerospikeClient().get(readPolicy, key, idBinName);
                    if (record != null) {
                        result.add(record.getString(idBinName));
                    }
                }
            } finally {
                rs.close();
            }
            return result;
        } catch (AerospikeException e) {
            log.error("query failed", e);
            return Collections.emptySet();
        }
    }

    public void setNamespace(final String namespace) {
        log.debug("Session store namespace: {}", namespace);
        this.namespace = namespace;
    }

    public void setSetname(final String setname) {
        log.debug("Session store setname: {}", setname);
        this.setname = setname;
    }

    public void setExpiration(final int expiration) {
        this.expiration = expiration;
    }

    @Override
    public void deleteAll() {
        getAerospikeClient().scanAll(new ScanPolicy(), namespace, setname, new ScanCallback() {
            public void scanCallback(Key key, Record record) throws AerospikeException {
                getAerospikeClient().delete(writePolicy, key);
            }
        }, new String[] {});
    }

}
