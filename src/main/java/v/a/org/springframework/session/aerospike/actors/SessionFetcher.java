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

import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.SESSION_FETCHER;
import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.SESSION_SERIALIZER;
import static v.a.org.springframework.session.aerospike.actors.PersistentSessionAerospike.CREATION_TIME_BIN;
import static v.a.org.springframework.session.aerospike.actors.PersistentSessionAerospike.LAST_ACCESSED_BIN;
import static v.a.org.springframework.session.aerospike.actors.PersistentSessionAerospike.MAX_INACTIVE_BIN;
import static v.a.org.springframework.session.aerospike.actors.PersistentSessionAerospike.SESSION_ATTRIBUTES_BIN;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.session.MapSession;
import org.springframework.stereotype.Component;

import scala.concurrent.duration.Duration;
import v.a.org.springframework.session.messages.FetchSession;
import v.a.org.springframework.session.messages.SessionAttributesBinary;
import v.a.org.springframework.session.messages.SessionControlEvent;
import v.a.org.springframework.store.aerospike.AerospikeOperations;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.aerospike.client.Record;

/**
 * Actor handles sessions fetching. This is "per-request" actor because we have to store state here (session id and
 * metadata) while waiting for de-serialization results. When session attributes de-serialization is complete, the
 * {@link MapSession} instance will be built and sent to caller.
 */
@Component(SESSION_FETCHER)
@Scope("prototype")
public class SessionFetcher extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass().getSimpleName());

    @Inject
    private AerospikeOperations<String> sessionAerospikeOperations;

    // State data
    private String sessionId;
    private MapSession loaded;
    private ActorRef requester;

    private SessionFetcher() {
        // give this actor 30 sec to complete its task
        getContext().setReceiveTimeout(Duration.create("3000 seconds"));
    }

    @Override
    public void onReceive(Object message) throws Exception {
        log.debug("handle message {}", message);
        if (message instanceof FetchSession) {
            log.debug("Fetch session {}", message);
            this.sessionId = ((FetchSession)message).getId();
            this.requester = getSender();
            final Record sessionRecord = fetch();
            if (sessionRecord == null) {
                notifyNotFound();
            } else {
                // reconstruct Aerospike session - extract metadata first
                loaded = new MapSession();
                loaded.setId(this.sessionId);
                loaded.setCreationTime(sessionRecord.getLong(CREATION_TIME_BIN));
                loaded.setMaxInactiveIntervalInSeconds(sessionRecord.getInt(MAX_INACTIVE_BIN));
                loaded.setLastAccessedTime(sessionRecord.getLong(LAST_ACCESSED_BIN));
                if (loaded.isExpired()) {
                    notifyNotFound();
                } else {
                    // now extract session attributes as byte array and send it to converter for deserialization, expecting
                    // hashmap of attributes back for future processing
                    final byte[] serializedAttributes = (byte[]) sessionRecord.getValue(SESSION_ATTRIBUTES_BIN);
                    getContext().actorSelection("../" + SESSION_SERIALIZER).tell(
                            new SessionAttributesBinary(serializedAttributes), self());
                }
            }
        }
        // message from serializer: stored attributes as Map
        else if (message instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> storedAttributes = (Map<String, Object>) message;
            for (Map.Entry<String, Object> entry : storedAttributes.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                loaded.setAttribute(key, value);
            }
            requester.tell(loaded, getSelf());
        }
        else if (message instanceof ReceiveTimeout) {
            log.error("Unable to fetch session {} in time. Aborting!", message);
            getContext().stop(getSelf());
        }
        else {
            log.error("Unable to handle message {}", message);
            unhandled(message);
        }

    }

    @Override
    public void postStop() throws Exception {
        log.info("Shutting down");
        super.postStop();
    }


    /**
     * Fetches session record from Aerospike.
     * @return
     */
    private Record fetch() {
        final Record sessionRecord = sessionAerospikeOperations.fetch(sessionId);
        if (sessionRecord == null) {
            log.debug("Session {} not found in store", sessionId);
            return null;
        } else {
            return sessionRecord;
        }
    }
    
    private void notifyNotFound() {
        requester.tell(SessionControlEvent.NOT_FOUND, getSelf());
        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }

}
