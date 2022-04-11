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

import static us.swcraft.springframework.session.aerospike.PersistentSessionAerospike.CREATION_TIME_BIN;
import static us.swcraft.springframework.session.aerospike.PersistentSessionAerospike.EXPIRED_BIN;
import static us.swcraft.springframework.session.aerospike.PersistentSessionAerospike.EXPIRED_INDEX;
import static us.swcraft.springframework.session.aerospike.PersistentSessionAerospike.LAST_ACCESSED_BIN;
import static us.swcraft.springframework.session.aerospike.PersistentSessionAerospike.MAX_INACTIVE_BIN;
import static us.swcraft.springframework.session.aerospike.PersistentSessionAerospike.SESSION_ATTRIBUTES_BIN;
import static us.swcraft.springframework.session.aerospike.PersistentSessionAerospike.SESSION_ID_BIN;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.session.ExpiringSession;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.aerospike.client.Bin;
import com.aerospike.client.Record;
import com.aerospike.client.query.IndexType;

import us.swcraft.springframework.session.model.SessionSnapshot;
import us.swcraft.springframework.session.model.StoreMetadata;
import us.swcraft.springframework.session.store.SessionAttributesTransformer;
import us.swcraft.springframework.session.store.aerospike.AerospikeOperations;

/**
 * <p>
 * A {@link org.springframework.session.SessionRepository} that is implemented
 * using Aerospike storage. In a web environment, this is typically used in
 * combination with {@link SessionRepositoryFilter}. This implementation
 * supports {@link SessionDestroyedEvent}.
 * </p>
 *
 *
 * @author Vlad Aleksandrov
 */
@Component("aerospikeStoreSessionRepository")
public class AerospikeStoreSessionRepository
        implements SessionRepository<AerospikeStoreSessionRepository.AerospikeSession> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private StoreMetadata storeMetadata;

    @Inject
    private AerospikeOperations<String> aerospikeOperations;

    @Inject
    private AerospikeSessionExpirationPolicy expirationPolicy;

    @Inject
    private SessionAttributesTransformer transformer;

    @Inject
    @Named("ssa-taskExecutor")
    private Executor taskExecutor;

    /**
     * Storage initialization.
     */
    @PostConstruct
    public void init() {
        log.trace("Prepare session store...");
        // create index on "expired" bin
        aerospikeOperations.createIndex(EXPIRED_BIN, EXPIRED_INDEX + "." + storeMetadata.getSetname(), IndexType.NUMERIC);
    }

    public void save(final AerospikeSession session) {
        // Check if session data is a special transient session (transient attribute is true). The transient session is not stored.
        final Object transientAttr = session.getAttribute("transient");
        if (transientAttr != null && Boolean.valueOf(transientAttr.toString())) {
            log.trace("not saved - transient session {}", session.getId());
            return;
        }

        final SessionSnapshot sessionSnapshot = createSessionSnapshot(session);
        log.debug("Prepare and save {}", sessionSnapshot);

        CompletableFuture.supplyAsync(() -> prepareAndSave(sessionSnapshot), taskExecutor).whenComplete((id, e) -> {
            if (e == null) {
                log.debug("Session {} saved", id);
            } else {
                log.error("Sesion {} save failed", id, e);
            }
        });
    }

    private String prepareAndSave(final SessionSnapshot sessionSnapshot) {
        final String sessionId = sessionSnapshot.getSessionId();
        final Set<Bin> binsToSave = new HashSet<>();

        if (!aerospikeOperations.hasKey(sessionId)) {
            log.trace("Save new session {}", sessionId);
            // newly created session - save "created", "max inactive interval"
            // and "id" itself
            binsToSave.add(new Bin(CREATION_TIME_BIN, sessionSnapshot.getCreationTime()));
            binsToSave.add(new Bin(MAX_INACTIVE_BIN, sessionSnapshot.getMaxInactiveIntervalInSec()));
            binsToSave.add(new Bin(SESSION_ID_BIN, sessionId));
        } else {
            log.trace("Update existing session {}", sessionId);
        }
        // always update last access timestamp
        binsToSave.add(new Bin(LAST_ACCESSED_BIN, sessionSnapshot.getLastAccessedTime()));
        if (sessionSnapshot.getMaxInactiveIntervalInSec() > 0) {
            // update expired only for session with expiration
            binsToSave.add(new Bin(EXPIRED_BIN, sessionSnapshot.getExpirationTimestamp()));
        }
        if (sessionSnapshot.isUpdated()) {
            log.trace("Session {} attributes: {}", sessionId, sessionSnapshot.getSessionAttrs());
            final byte[] attrs = transformer.marshall(sessionSnapshot.getSessionAttrs());
            binsToSave.add(new Bin(SESSION_ATTRIBUTES_BIN, attrs));
        }
        aerospikeOperations.persist(sessionId, binsToSave);
        return sessionId;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredSessions() {
        this.expirationPolicy.cleanExpiredSessions();
    }

    public AerospikeSession getSession(final String id) {
        final Record sessionRecord = aerospikeOperations.fetch(id);
        if (sessionRecord == null) {
            log.debug("Session {} not found", id);
            return null;
        }
        // reconstruct Aerospike session - extract metadata first
        final MapSession loaded = new MapSession();

        loaded.setId(id);
        log.debug("Session id: {}", loaded.getId());
        loaded.setCreationTime(sessionRecord.getLong(CREATION_TIME_BIN));
        log.debug("Session created: {}", loaded.getCreationTime());
        loaded.setMaxInactiveIntervalInSeconds(sessionRecord.getInt(MAX_INACTIVE_BIN));
        log.debug("Session max inactive interval: {}", loaded.getMaxInactiveIntervalInSeconds());
        loaded.setLastAccessedTime(sessionRecord.getLong(LAST_ACCESSED_BIN));
        log.debug("Session last access time: {}", loaded.getLastAccessedTime());
        if (loaded.isExpired()) {
            return null;
        } else {
            // now extract session attributes as byte array and then convert it
            // back to map
            final byte[] serializedAttributes = (byte[]) sessionRecord.getValue(SESSION_ATTRIBUTES_BIN);

            final Map<String, Object> attributes = transformer.unmarshal(serializedAttributes);
            if (serializedAttributes == null) {
                final AerospikeSession session = new AerospikeSession();
                session.setLastAccessedTime(System.currentTimeMillis());
                return session;
            }

            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                loaded.setAttribute(key, value);
            }
            // restore session
            final AerospikeSession session = new AerospikeSession(loaded);
            session.setLastAccessedTime(System.currentTimeMillis());
            return session;
        }

    }

    /**
     * Creates immutable snapshot of session metadata and attributes.
     *
     * @param aerospikeSession
     * @return immutable session snapshot
     */
    private SessionSnapshot createSessionSnapshot(final AerospikeSession aerospikeSession) {
        final SessionSnapshot.Builder builder = new SessionSnapshot.Builder(aerospikeSession.getId())
                .creationTime(aerospikeSession.getCreationTime())
                .expirationTimestamp(aerospikeSession.getExpirationTimestamp())
                .lastAccessedTime(aerospikeSession.getLastAccessedTime())
                .maxInactiveIntervalInSec(aerospikeSession.getMaxInactiveIntervalInSeconds())
                .updated(aerospikeSession.isUpdated());

        final Set<String> attributeNames = aerospikeSession.getAttributeNames();
        for (String name : attributeNames) {
            builder.addAattribute(name, aerospikeSession.getAttribute(name));
        }
        return builder.build();
    }

    public void delete(final String sessionId) {
        log.debug("Removing session '{}'", sessionId);
        this.expirationPolicy.onDelete(sessionId, true);
    }

    public AerospikeSession createSession() {
        final AerospikeSession aerospikeSession = new AerospikeSession();
        aerospikeSession.setMaxInactiveIntervalInSeconds(storeMetadata.getMaxInactiveIntervalInSeconds());
        return aerospikeSession;
    }

    /**
     * A custom implementation of {@link Session} that uses a {@link MapSession}
     * as the basis for its mapping.
     *
     * @author Vlad Aleksandrov
     */
    public final class AerospikeSession implements ExpiringSession {
        private final MapSession cached;
        private Long expirationTimestamp;
        /**
         * Dirty session attributes flag
         */
        private boolean updated = false;

        /**
         * Creates a new instance.
         */
        AerospikeSession() {
            this(new MapSession());
        }

        /**
         * Creates a new instance from the provided {@link MapSession}
         *
         * @param cached
         *            the {@MapSession} that represents the persisted session
         *            that was retrieved. Cannot be null.
         */
        AerospikeSession(final MapSession cached) {
            Assert.notNull(cached, "MapSession cannot be null");
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
            if (!Attributes.areEqual(cached.getAttribute(attributeName), attributeValue)) {
                cached.setAttribute(attributeName, attributeValue);
                updated = true;
            }
        }

        /**
         * Removes attribute and sets "updated" flag if the attribute did exist
         * in session.
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
