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
package v.a.org.springframework.session.aerospike.actors;

import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.EXPIRED_SESSIONS_CARETAKER;
import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.SEESION_REMOVER;
import static v.a.org.springframework.session.aerospike.actors.PersistentSessionAerospike.EXPIRED_BIN;
import static v.a.org.springframework.session.aerospike.actors.PersistentSessionAerospike.SESSION_ID_BIN;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import v.a.org.springframework.session.messages.ClearExpiredSessions;
import v.a.org.springframework.session.messages.DeleteSession;
import v.a.org.springframework.store.aerospike.AerospikeOperations;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Actor handles expired sessions .
 */
@Component(EXPIRED_SESSIONS_CARETAKER)
@Scope("prototype")
public class ExpiredSessionsCaretaker extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass().getSimpleName());

    @Inject
    private AerospikeOperations<String> aerospikeOperations;

    @Override
    public void onReceive(Object message) throws Exception {
        log.debug("handle message {}", message);
        if (message instanceof ClearExpiredSessions) {
            Set<String> expiredSession = aerospikeOperations.fetchRange(SESSION_ID_BIN, EXPIRED_BIN, 0L,
                    System.currentTimeMillis());
            for (String sessionId : expiredSession) {
                getContext().actorSelection("../" + SEESION_REMOVER).tell(new DeleteSession(sessionId), self());
            }

        } else {
            log.error("Unable to handle message {}", message);
            unhandled(message);
        }
    }

}
