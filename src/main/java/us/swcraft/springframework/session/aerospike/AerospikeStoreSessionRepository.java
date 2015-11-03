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

import static us.swcraft.springframework.session.aerospike.actors.ActorsEcoSystem.*;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.session.ExpiringSession;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.SessionMessageListener;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.util.Assert;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import us.swcraft.springframework.session.support.SpringExtension;
import us.swcraft.springframework.session.messages.FetchSession;
import us.swcraft.springframework.session.messages.SessionControlEvent;
import us.swcraft.springframework.session.messages.SessionSnapshot;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.pattern.Patterns;
import akka.util.Timeout;

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

    private final ActorSystem actorSystem;
    private final SpringExtension springExtension;

    private final AerospikeSessionExpirationPolicy expirationPolicy;

    /**
     * If non-null, this value is used to override the default value for
     * {@link AerospikeSession#setMaxInactiveIntervalInSeconds(int)}.
     */
    private Integer defaultMaxInactiveInterval;

    /**
     * Creates a new instance.
     *
     */
    public AerospikeStoreSessionRepository(ActorSystem actorSystem, SpringExtension springExtension) {
        Assert.notNull(actorSystem, "actorSystem cannot be null");
        Assert.notNull(springExtension, "springExtension cannot be null");
        this.actorSystem = actorSystem;
        this.springExtension = springExtension;
        this.expirationPolicy = new AerospikeSessionExpirationPolicy(actorSystem.actorSelection("/user/"
                + SEESION_REMOVER), actorSystem.actorSelection("/user/" + EXPIRED_SESSIONS_CARETAKER));

        // create secondary index on "expired" bin
        ActorRef indicesCreatorRef = actorSystem.actorOf(springExtension.props(INDICES_CREATOR), INDICES_CREATOR);
        indicesCreatorRef.tell(SessionControlEvent.CREATE_INDICES, null);
        // since we need this actor only once, stop it right away
        indicesCreatorRef.tell(PoisonPill.getInstance(), null);
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
        // Create "per-request" persister actor and hand over a session snapshot to be saved
        ActorRef persisterRef = actorSystem.actorOf(springExtension.props(SESSION_PERSISTER), SESSION_PERSISTER + "_"
                + UUID.randomUUID());
        persisterRef.tell(createSessionSnapshot(session), null);
    }

    @Scheduled(cron = "0 * * * * *")
    public void cleanupExpiredSessions() {
        this.expirationPolicy.cleanExpiredSessions();
    }

    public AerospikeSession getSession(String id) {
        // Create "per-request" fetcher actor and hand over a session id to be fetched
        ActorRef fetcherRef = actorSystem.actorOf(springExtension.props(SESSION_FETCHER),
                SESSION_FETCHER + "_" + UUID.randomUUID());
                
        Timeout timeout = new Timeout(Duration.create(actorSystem.settings().config().getInt("session.aerospike.fetch-timeout"), "seconds"));
        Future<Object> future = Patterns.ask(fetcherRef, new FetchSession(id), timeout);

        // this blocks current running thread
        long start = System.nanoTime();
        try {
            Object result = Await.result(future, timeout.duration());
            log.debug("{}", result);
            if (result == SessionControlEvent.NOT_FOUND) {
                log.warn("Session {} not found", id);
                return null;
            } else if (result instanceof MapSession) {
                final AerospikeSession session = new AerospikeSession((MapSession) result);
                session.setLastAccessedTime(System.currentTimeMillis());
                return session;
            } else {
                log.error("Unknown response: {}", result);
                return null;
            }
        } catch (Exception e) {
            log.error("Session {} fetch problem: {}", id, e.getMessage());
            log.debug("", e);
            return null;
        } finally {
            log.trace("Session load: {} ns", System.nanoTime() - start);
        }

    }

    /**
     * Creates immutable snapshot of message metadata and attributes.
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

        Set<String> attributeNames = aerospikeSession.getAttributeNames();
        for (String name : attributeNames) {
            builder.addAattribute(name, aerospikeSession.getAttribute(name));
        }
        return builder.build();
    }

    public void delete(final String sessionId) {
        log.debug("Removing session '{}'", sessionId);
        this.expirationPolicy.onDelete(sessionId);
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
            if (!Attributes.areEqual(cached.getAttribute(attributeName), attributeValue)) {
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
