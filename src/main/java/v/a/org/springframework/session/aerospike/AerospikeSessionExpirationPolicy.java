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

package v.a.org.springframework.session.aerospike;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import v.a.org.springframework.session.aerospike.AerospikeStoreSessionRepository.AerospikeSession;
import v.a.org.springframework.session.messages.ClearExpiredSessions;
import v.a.org.springframework.session.messages.DeleteSession;
import akka.actor.ActorRef;

/**
 * A strategy for expiring {@link AerospikeSession} instances.
 * 
 * @author Vlad Aleksandrov
 */
final class AerospikeSessionExpirationPolicy {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ActorRef supervisorRef;

    public AerospikeSessionExpirationPolicy(ActorRef supervisorRef) {
        super();
        this.supervisorRef = supervisorRef;
    }

    public void onDelete(final String sessionId) {
        log.debug("Session '{}' deleted.", sessionId);
        supervisorRef.tell(new DeleteSession(sessionId), null);
    }

    public void cleanExpiredSessions() {
        log.debug("Expired sessions cleanup");
        supervisorRef.tell(new ClearExpiredSessions(), null);
    }

}
