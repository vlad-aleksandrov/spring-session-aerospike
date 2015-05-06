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

package v.a.org.springframework.session.aerospike;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.session.ExpiringSession;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.util.Assert;

import v.a.org.springframework.store.StoreCompression;
import v.a.org.springframework.store.StoreSerializer;
import v.a.org.springframework.store.aerospike.AerospikeOperations;
import v.a.org.springframework.store.kryo.KryoStoreSerializer;

import com.aerospike.client.Bin;
import com.aerospike.client.Record;

/**
 * <p>
 * A {@link org.springframework.session.SessionRepository} that is implemented using Aerospike storage. In a web
 * environment, this is typically used in combination with {@link SessionRepositoryFilter}. This implementation supports
 * {@link SessionDestroyedEvent} through {@link SessionMessageListener}.
 * </p>
 *
 * <h2>Creating a new instance</h2>
 *
 * A typical example of how to create a new instance can be seen below:
 *
 * <pre>
 * JedisConnectionFactory factory = new JedisConnectionFactory();
 * 
 * RedisOperationsSessionRepository redisSessionRepository = new RedisOperationsSessionRepository(
 *         factory);
 * </pre>
 *
 * <p>
 * For additional information on how to create a RedisTemplate, refer to the <a href =
 * "http://docs.spring.io/spring-data/data-redis/docs/current/reference/html/" >Spring Data Redis Reference</a>.
 * </p>
 *
 * <h2>Storage Details</h2>
 *
 * <p>
 * Each session is stored in Redis as a <a href="http://redis.io/topics/data-types#hashes">Hash</a>. Each session is set
 * and updated using the <a href="http://redis.io/commands/hmset">HMSET command</a>. An example of how each session is
 * stored can be seen below.
 * </p>
 *
 * <pre>
 * HMSET spring:session:sessions:&lt;session-id&gt; creationTime 1404360000000 maxInactiveInterval 1800 lastAccessedTime 1404360000000 sessionAttr:&lt;attrName&gt; someAttrValue sessionAttr2:&lt;attrName&gt; someAttrValue2
 * </pre>
 *
 * <p>
 * An expiration is associated to each session using the <a href="http://redis.io/commands/expire">EXPIRE command</a>
 * based upon the
 * {@link org.springframework.session.data.redis.RedisOperationsSessionRepository.RedisSession#getMaxInactiveIntervalInSeconds()}
 * . For example:
 * </p>
 *
 * <pre>
 * EXPIRE spring:session:sessions:&lt;session-id&gt; 1800
 * </pre>
 *
 * <p>
 * The {@link AerospikeSession} keeps track of the properties that have changed and only updates those. This means if an
 * attribute is written once and read many times we only need to write that attribute once. For example, assume the
 * session attribute "sessionAttr2" from earlier was updated. The following would be executed upon saving:
 * </p>
 *
 * <pre>
 *     HMSET spring:session:sessions:&lt;session-id&gt; sessionAttr2:&lt;attrName&gt; newValue
 *     EXPIRE spring:session:sessions:&lt;session-id&gt; 1800
 * </pre>
 *
 * <p>
 * Spring Session relies on the expired and delete <a href="http://redis.io/topics/notifications">keyspace
 * notifications</a> from Redis to fire a &lt;&lt;SessionDestroyedEvent&gt;&gt;. It is the `SessionDestroyedEvent` that
 * ensures resources associated with the Session are cleaned up. For example, when using Spring Session's WebSocket
 * support the Redis expired or delete event is what triggers any WebSocket connections associated with the session to
 * be closed.
 * </p>
 *
 * <p>
 * One problem with this approach is that Redis makes no guarantee of when the expired event will be fired if they key
 * has not been accessed. Specifically the background task that Redis uses to clean up expired keys is a low priority
 * task and may not trigger the key expiration. For additional details see <a
 * href="http://redis.io/topics/notifications">Timing of expired events</a> section in the Redis documentation.
 * </p>
 *
 * <p>
 * To circumvent the fact that expired events are not guaranteed to happen we can ensure that each key is accessed when
 * it is expected to expire. This means that if the TTL is expired on the key, Redis will remove the key and fire the
 * expired event when we try to access they key.
 * </p>
 *
 * <p>
 * For this reason, each session expiration is also tracked to the nearest minute. This allows a background task to
 * access the potentially expired sessions to ensure that Redis expired events are fired in a more deterministic
 * fashion. For example:
 * </p>
 *
 * <pre>
 *     SADD spring:session:expirations:&lt;expire-rounded-up-to-nearest-minute&gt; &lt;session-id&gt;
 *     EXPIRE spring:session:expirations:&lt;expire-rounded-up-to-nearest-minute&gt; 1800
 * </pre>
 *
 * <p>
 * The background task will then use these mappings to explicitly request each key. By accessing they key, rather than
 * deleting it, we ensure that Redis deletes the key for us only if the TTL is expired.
 * </p>
 * <p>
 * <b>NOTE</b>: We do not explicitly delete the keys since in some instances there may be a race condition that
 * incorrectly identifies a key as expired when it is not. Short of using distributed locks (which would kill our
 * performance) there is no way to ensure the consistency of the expiration mapping. By simply accessing the key, we
 * ensure that the key is only removed if the TTL on that key is expired.
 * </p>
 *
 * @author Vlad Aleksandrov
 */
public class AerospikeStoreSessionRepository implements
        SessionRepository<AerospikeStoreSessionRepository.AerospikeSession> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The bin name representing {@link org.springframework.session.ExpiringSession#getId()}
     */
    static final String SESSION_ID_BIN = "sessionId";

    /**
     * The bin name representing {@link org.springframework.session.ExpiringSession#getCreationTime()}
     */
    static final String CREATION_TIME_BIN = "created";

    /**
     * The bin name representing {@link org.springframework.session.ExpiringSession#getMaxInactiveIntervalInSeconds()}
     */
    static final String MAX_INACTIVE_BIN = "maxInactive";

    /**
     * The bin name representing {@link org.springframework.session.ExpiringSession#getLastAccessedTime()}
     */
    static final String LAST_ACCESSED_BIN = "lastAccessed";

    /**
     * The Aerospike bin name for session expiration timestamp. Indexed?
     */
    static final String EXPIRED_BIN = "expired";

    /**
     * The Aerospike bin name for session attributes map.
     */
    static final String SESSION_ATTRIBUTES_BIN = "sessionAttr";

    private final AerospikeOperations<String> sessionAerospikeOperations;

    private final AerospikeSessionExpirationPolicy expirationPolicy;

    @SuppressWarnings("rawtypes")
    private final StoreSerializer<HashMap> sessionAttriburesSerializer;

    /**
     * If non-null, this value is used to override the default value for
     * {@link AerospikeSession#setMaxInactiveIntervalInSeconds(int)}.
     */
    private Integer defaultMaxInactiveInterval;

    /**
     * Creates a new instance.
     *
     * @param sessionAerospikeOperations
     *            The {@link AerospikeOperations} to use for managing the sessions. Cannot be null.
     */
    @SuppressWarnings("rawtypes")
    public AerospikeStoreSessionRepository(AerospikeOperations<String> sessionAerospikeOperations) {
        Assert.notNull(sessionAerospikeOperations, "sessionAerospikeOperations cannot be null");
        this.sessionAerospikeOperations = sessionAerospikeOperations;
        this.expirationPolicy = new AerospikeSessionExpirationPolicy(sessionAerospikeOperations);
        this.sessionAttriburesSerializer = new KryoStoreSerializer<HashMap>(StoreCompression.SNAPPY);
    }

    /**
     * Sets the maximum inactive interval in seconds between requests before newly created sessions will be
     * invalidated. A negative time indicates that the session will never timeout. The default is 1800 (30 minutes).
     *
     * @param defaultMaxInactiveInterval
     *            the number of seconds that the {@link Session} should be kept alive between
     *            client requests.
     */
    public void setDefaultMaxInactiveInterval(int defaultMaxInactiveInterval) {
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }

    public void save(final AerospikeSession session) {
        final String sessionId = session.getId();
        final Set<Bin> binsToSave = new HashSet<>();

        if (!sessionAerospikeOperations.hasKey(sessionId)) {
            log.trace("Save new session {}", sessionId);
            // newly created session - save "created", "max inactive interval" and "id" itself
            binsToSave.add(new Bin(CREATION_TIME_BIN, session.getCreationTime()));
            binsToSave.add(new Bin(MAX_INACTIVE_BIN, session.getMaxInactiveIntervalInSeconds()));
            binsToSave.add(new Bin(SESSION_ID_BIN, sessionId));
        } else {
            log.trace("Update existing session {}", sessionId);
        }
        // always updated last access timestamp
        binsToSave.add(new Bin(LAST_ACCESSED_BIN, session.getLastAccessedTime()));
        if (session.getMaxInactiveIntervalInSeconds() > 0) {
            // update expired only for session with expiration
            binsToSave.add(new Bin(EXPIRED_BIN, session.getExpirationTimestamp()));
        }
        if (session.isUpdated()) {
            log.trace("Serialize and save updated attributes for session {}", sessionId);
            final HashMap<String, Object> attributesToSave = new HashMap<>();
            final Set<String> allNames = session.getAttributeNames();
            for (String name : allNames) {
                attributesToSave.put(name, session.getAttribute(name));
            }
            binsToSave.add(new Bin(SESSION_ATTRIBUTES_BIN, sessionAttriburesSerializer.serialize(attributesToSave)));
        }
        sessionAerospikeOperations.persist(sessionId, binsToSave);
    }

    @Scheduled(cron = "0 * * * * *")
    public void cleanupExpiredSessions() {
        this.expirationPolicy.cleanExpiredSessions();
    }

    public AerospikeSession getSession(String id) {
        return getSession(id, false);
    }

    /**
     *
     * @param id
     *            the session id
     * @param allowExpired
     *            if true, will also include expired sessions that have not been
     *            deleted. If false, will ensure expired sessions are not
     *            returned.
     * @return
     */
    private AerospikeSession getSession(String id, boolean allowExpired) {
        final Record sessionRecord = sessionAerospikeOperations.fetch(id);
        if (sessionRecord == null) {
            log.debug("Session {} not found in store", id);
            return null;
        }

        // reconstruct Aerospike session - extract metadata first
        final MapSession loaded = new MapSession();
        loaded.setId(id);
        loaded.setCreationTime(sessionRecord.getLong(CREATION_TIME_BIN));
        loaded.setMaxInactiveIntervalInSeconds(sessionRecord.getInt(MAX_INACTIVE_BIN));
        loaded.setLastAccessedTime(sessionRecord.getLong(LAST_ACCESSED_BIN));
        if (!allowExpired && loaded.isExpired()) {
            return null;
        }
        // now extract session attributes
        final byte[] serializedAttributes = (byte[]) sessionRecord.getValue(SESSION_ATTRIBUTES_BIN);
        @SuppressWarnings("unchecked")
        final Map<String, Object> storedAttributes = sessionAttriburesSerializer.deserialize(serializedAttributes,
                HashMap.class);
        for (Map.Entry<String, Object> entry : storedAttributes.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            loaded.setAttribute(key, value);
        }
        final AerospikeSession result = new AerospikeSession(loaded);
        result.setLastAccessedTime(System.currentTimeMillis());
        return result;
    }

    public void delete(final String sessionId) {
        if (this.sessionAerospikeOperations.hasKey(sessionId)) {
            log.debug("Removing session '{}'", sessionId);
            this.sessionAerospikeOperations.delete(sessionId);
            this.expirationPolicy.onDelete(sessionId);
        } else {
            log.warn("Session '{}' does not exist in storage", sessionId);
        }
    }

    public AerospikeSession createSession() {
        final AerospikeSession aerospikeSession = new AerospikeSession();
        if (defaultMaxInactiveInterval != null) {
            aerospikeSession.setMaxInactiveIntervalInSeconds(defaultMaxInactiveInterval);
        }
        return aerospikeSession;
    }

    /**
     * A custom implementation of {@link Session} that uses a {@link MapSession} as the basis for its mapping.
     *
     * @author Vlad Aleksandrov
     */
    final class AerospikeSession implements ExpiringSession {
        private final MapSession cached;
        private Long expirationTimestamp;
        /**
         * Dirty session attributes flag
         */
        private boolean updated = false;

        /**
         * Creates a new instance ensuring to mark all of the new attributes to be persisted in the next save operation.
         */
        AerospikeSession() {
            this(new MapSession());
        }

        /**
         * Creates a new instance from the provided {@link MapSession}
         *
         * @param cached
         *            the {@MapSession} that represents the persisted session that was retrieved. Cannot be
         *            null.
         */
        AerospikeSession(final MapSession cached) {
            Assert.notNull("MapSession cannot be null");
            this.cached = cached;
            updateExpirationTimestamp(cached.getLastAccessedTime(), cached.getMaxInactiveIntervalInSeconds());
        }

        public void setLastAccessedTime(long lastAccessedTime) {
            cached.setLastAccessedTime(lastAccessedTime);
            updateExpirationTimestamp(lastAccessedTime, cached.getMaxInactiveIntervalInSeconds());
        }

        public boolean isExpired() {
            return cached.isExpired();
        }

        public long getCreationTime() {
            return cached.getCreationTime();
        }

        public String getId() {
            return cached.getId();
        }

        public long getLastAccessedTime() {
            return cached.getLastAccessedTime();
        }

        public void setMaxInactiveIntervalInSeconds(int interval) {
            cached.setMaxInactiveIntervalInSeconds(interval);
            updateExpirationTimestamp(cached.getLastAccessedTime(), interval);
        }

        public int getMaxInactiveIntervalInSeconds() {
            return cached.getMaxInactiveIntervalInSeconds();
        }

        @SuppressWarnings("unchecked")
        public Object getAttribute(String attributeName) {
            return cached.getAttribute(attributeName);
        }

        public Set<String> getAttributeNames() {
            return cached.getAttributeNames();
        }

        public void setAttribute(String attributeName, Object attributeValue) {
            if (!Objects.equals(cached.getAttribute(attributeName), attributeValue)) {
                cached.setAttribute(attributeName, attributeValue);
                updated = true;
            }
        }

        /**
         * Removes attribute and sets "updated" flag if the attribute did exist in session.
         */
        public void removeAttribute(String attributeName) {
            if (cached.getAttribute(attributeName) != null) {
                cached.removeAttribute(attributeName);
                updated = true;
            }
        }

        public Long getExpirationTimestamp() {
            return expirationTimestamp;
        }

        /**
         * Sets expiration timestamp only if session is "expirable".
         * 
         * @param lastAccessedTime
         * @param maxInactiveIntervalInSeconds
         */
        private void updateExpirationTimestamp(long lastAccessedTime, int maxInactiveIntervalInSeconds) {
            if (maxInactiveIntervalInSeconds > 0) {
                expirationTimestamp = lastAccessedTime + TimeUnit.SECONDS.toMillis(maxInactiveIntervalInSeconds);
            }
        }

        public boolean isUpdated() {
            return updated;
        }
    }

}
