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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import us.swcraft.springframework.session.aerospike.AerospikeStoreSessionRepository.AerospikeSession;

/**
 * A strategy for expiring and deleting {@link AerospikeSession} instances.
 * 
 * @author Vlad Aleksandrov
 */

@Component
public class AerospikeSessionExpirationPolicy {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

//    private final ActorSelection removerActor;
//    private final ActorSelection expiredSessionsCaretakerActor;
//
//    public AerospikeSessionExpirationPolicy(ActorSelection removerActor, ActorSelection expiredSessionsCaretakerActor) {
//        super();
//        this.removerActor = removerActor;
//        this.expiredSessionsCaretakerActor = expiredSessionsCaretakerActor;
//    }
//
//    public void onDelete(final String sessionId) {
//        log.debug("Session '{}' is going to be deleted.", sessionId);
//        this.removerActor.tell(new DeleteSession(sessionId), null);
//    }
//
    public void cleanExpiredSessions() {
        log.debug("Expired sessions cleanup");
//        this.expiredSessionsCaretakerActor.tell(SessionControlEvent.CLEAR_EXPIRED_SESSIONS, null);
    }

}
