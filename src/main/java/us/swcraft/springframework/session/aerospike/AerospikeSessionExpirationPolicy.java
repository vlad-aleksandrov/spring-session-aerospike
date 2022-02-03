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

package us.swcraft.springframework.session.aerospike;

import static us.swcraft.springframework.session.aerospike.PersistentSessionAerospike.EXPIRED_BIN;
import static us.swcraft.springframework.session.aerospike.PersistentSessionAerospike.SESSION_ID_BIN;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.stereotype.Component;

import us.swcraft.springframework.session.aerospike.AerospikeStoreSessionRepository.AerospikeSession;
import us.swcraft.springframework.session.store.aerospike.AerospikeOperations;

/**
 * A strategy for expiring and deleting {@link AerospikeSession} instances.
 *
 * @author Vlad Aleksandrov
 */

@Component
public class AerospikeSessionExpirationPolicy {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private AerospikeOperations<String> aerospikeOperations;

    @Inject
    private ApplicationEventPublisher eventPublisher;

    public void cleanExpiredSessions() {
        log.debug("Expired sessions cleanup");
        final Set<String> expiredSession = aerospikeOperations.fetchRange(SESSION_ID_BIN, EXPIRED_BIN, 0L,
                System.currentTimeMillis());
        for (String sessionId : expiredSession) {
            onDelete(sessionId);
        }
    }

    public void onDelete(final String sessionId) {
        if (sessionId != null) {
            aerospikeOperations.delete(sessionId);
            log.trace("Session {} deleted", sessionId);
            publishEvent(new SessionDestroyedEvent(this, sessionId));
        }
    }

    private void publishEvent(final ApplicationEvent event) {
        try {
            this.eventPublisher.publishEvent(event);
        } catch (Throwable ex) {
            log.error("Error publishing " + event, ex);
        }
    }

}
