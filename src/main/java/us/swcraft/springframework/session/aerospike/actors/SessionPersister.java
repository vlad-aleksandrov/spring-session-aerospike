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
package us.swcraft.springframework.session.aerospike.actors;

import static us.swcraft.springframework.session.aerospike.actors.ActorsEcoSystem.SESSION_PERSISTER;
import static us.swcraft.springframework.session.aerospike.actors.ActorsEcoSystem.SESSION_SERIALIZER;
import static us.swcraft.springframework.session.aerospike.actors.PersistentSessionAerospike.CREATION_TIME_BIN;
import static us.swcraft.springframework.session.aerospike.actors.PersistentSessionAerospike.EXPIRED_BIN;
import static us.swcraft.springframework.session.aerospike.actors.PersistentSessionAerospike.LAST_ACCESSED_BIN;
import static us.swcraft.springframework.session.aerospike.actors.PersistentSessionAerospike.MAX_INACTIVE_BIN;
import static us.swcraft.springframework.session.aerospike.actors.PersistentSessionAerospike.SESSION_ATTRIBUTES_BIN;
import static us.swcraft.springframework.session.aerospike.actors.PersistentSessionAerospike.SESSION_ID_BIN;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import us.swcraft.springframework.session.messages.SessionAttributes;
import us.swcraft.springframework.session.messages.SessionAttributesBinary;
import us.swcraft.springframework.session.messages.SessionSnapshot;
import us.swcraft.springframework.session.store.aerospike.AerospikeOperations;


import com.aerospike.client.Bin;

/**
 * Actor handles sessions persistence. This is "per-request" actor because we have to store state here (session id and
 * metadata) while waiting for serialization results. When session attributes serialization is complete, the immutable
 * session will be created and saved.
 */
@Component(SESSION_PERSISTER)
@Scope("prototype")
public class SessionPersister {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Internal events.
     */
    enum InternalEvents {
        /**
         * It is time to save session data and die.
         */
        SAVE
    }

    @Inject
    private AerospikeOperations<String> sessionAerospikeOperations;

    // State data
    private String sessionId;
    private final Set<Bin> binsToSave = new HashSet<>();

    private SessionPersister() {
        // give this actor 5 sec to complete its task
//        getContext().setReceiveTimeout(Duration.create("5 seconds"));
    }

//    @Override
//    public void onReceive(Object message) throws Exception {
//        log.debug("handle message {}", message);
//        if (message instanceof SessionSnapshot) {
//            log.debug("Prepare session metadata to save {}", message);
//            prepareMetadata((SessionSnapshot) message);
//        }
//        else if (message instanceof SessionAttributesBinary) {
//            SessionAttributesBinary serializedAttributes = (SessionAttributesBinary) message;
//            log.debug("Got serialized session - {} bytes", serializedAttributes);
//            binsToSave.add(new Bin(SESSION_ATTRIBUTES_BIN, serializedAttributes.getAttributes()));
//            getSelf().tell(InternalEvents.SAVE, getSelf());
//        }
//        else if (message == InternalEvents.SAVE) {
//            save();
//            // mission complete - take pill and die now
//            getSelf().tell(PoisonPill.getInstance(), getSelf());
//        }
//        else if (message instanceof ReceiveTimeout) {
//            log.error("Unable to store session {} in time. Aborting!", message);
//            getContext().stop(getSelf());
//        }
//        else {
//            log.error("Unable to handle message {}", message);
//            unhandled(message);
//        }
//
//    }

//    @Override
//    public void postStop() throws Exception {
//        log.debug("Shutting down {}", this);
//        super.postStop();
//    }
//
//    private void prepareMetadata(final SessionSnapshot sessionSnapshot) {
//        this.sessionId = sessionSnapshot.getSessionId();
//
//        if (!sessionAerospikeOperations.hasKey(sessionId)) {
//            log.debug("Save new session {}", sessionId);
//            // newly created session - save "created", "max inactive interval" and "id" itself
//            binsToSave.add(new Bin(CREATION_TIME_BIN, sessionSnapshot.getCreationTime()));
//            binsToSave.add(new Bin(MAX_INACTIVE_BIN, sessionSnapshot.getMaxInactiveIntervalInSec()));
//            binsToSave.add(new Bin(SESSION_ID_BIN, this.sessionId));
//        } else {
//            log.debug("Update existing session {}", sessionId);
//        }
//        // always updated last access timestamp
//        binsToSave.add(new Bin(LAST_ACCESSED_BIN, sessionSnapshot.getLastAccessedTime()));
//        if (sessionSnapshot.getMaxInactiveIntervalInSec() > 0) {
//            // update expired only for session with expiration
//            binsToSave.add(new Bin(EXPIRED_BIN, sessionSnapshot.getExpirationTimestamp()));
//        }
//        if (sessionSnapshot.isUpdated()) {
//            // find serializer and send session data for serialization, wait for SessionAttributesBinary
//            getContext().actorSelection("../" + SESSION_SERIALIZER).tell(
//                    new SessionAttributes(sessionSnapshot.getSessionAttrs()), self());
//        } else {
//            // send "save session" message to self right away
//            getSelf().tell(InternalEvents.SAVE, getSender());
//        }
//    }
//
//    private void save() {
//        sessionAerospikeOperations.persist(sessionId, binsToSave);
//    }

}
