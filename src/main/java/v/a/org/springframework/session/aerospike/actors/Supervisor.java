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
import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.SESSION_DELETED_NOTIFIER;
import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.SESSION_PERSISTER;
import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.SESSION_FETCHER;
import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.SESSION_SERIALIZER;

import java.util.UUID;

import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.session.MapSession;
import org.springframework.stereotype.Component;

import v.a.org.springframework.session.messages.DeleteSession;
import v.a.org.springframework.session.messages.FetchSession;
import v.a.org.springframework.session.messages.SessionControlEvent;
import v.a.org.springframework.session.messages.SessionSnapshot;
import v.a.org.springframework.session.support.SpringExtension;
import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Supervisor handles exceptions and general feedback for the actual actors.
 * <p/>
 */
public class Supervisor extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass().getSimpleName());

    @Inject
    private SpringExtension springExtension;

    private ActorRef expiredSessionsCaretakerRef;
    private ActorRef removerRef;
    private ActorRef notifierRef;
    private ActorRef serializerRef;

    @Override
    public void preStart() throws Exception {

        log.info("Starting up");

        // initialize actors

        // single session remover actor
        removerRef = getContext().actorOf(springExtension.props(SEESION_REMOVER), SEESION_REMOVER);
        getContext().watch(removerRef);

        // expired sessions caretaker
        expiredSessionsCaretakerRef = getContext().actorOf(springExtension.props(EXPIRED_SESSIONS_CARETAKER),
                EXPIRED_SESSIONS_CARETAKER);
        getContext().watch(expiredSessionsCaretakerRef);

        // single notifier actor
        notifierRef = getContext().actorOf(springExtension.props(SESSION_DELETED_NOTIFIER), SESSION_DELETED_NOTIFIER);
        getContext().watch(notifierRef);

        // single session serializer.
        serializerRef = getContext().actorOf(springExtension.props(SESSION_SERIALIZER), SESSION_SERIALIZER);
        getContext().watch(serializerRef);

        super.preStart();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message == SessionControlEvent.CLEAR_EXPIRED_SESSIONS) {
            expiredSessionsCaretakerRef.tell(message, getSender());
        } else if (message instanceof DeleteSession) {
            removerRef.tell(message, getSender());
        } else if (message instanceof SessionSnapshot) {
            // Create "per-request" persister actor and hand over a session snapshot to be saved
            ActorRef persisterRef = getContext().actorOf(springExtension.props(SESSION_PERSISTER), SESSION_PERSISTER + "_" + UUID.randomUUID());
            getContext().watch(persisterRef);
            persisterRef.tell(message, getSelf());
        } else if (message instanceof FetchSession) {
            // Create "per-request" fetcher actor and hand over a session id to be fetched
            ActorRef persisterRef = getContext().actorOf(springExtension.props(SESSION_FETCHER), SESSION_FETCHER + "_" + UUID.randomUUID());
            getContext().watch(persisterRef);
            persisterRef.tell(message, getSelf());
        } else if (message instanceof MapSession) {
            // Session fetch result is being sent to caller
            getSender().tell(message, getSelf());   
            
        } else if (message instanceof Terminated) {
            log.debug("{} terminated", ((Terminated)message).actor());
        } else {
            unhandled(message);
        }
    }

}
