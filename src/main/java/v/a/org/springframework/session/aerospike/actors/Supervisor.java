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

import static v.a.org.springframework.session.aerospike.actors.Actors.*;

import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import v.a.org.springframework.session.messages.ClearExpiredSessions;
import v.a.org.springframework.session.messages.DeleteSession;
import v.a.org.springframework.session.support.SpringExtension;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Supervisor handles exceptions and general feedback for the actual actors.
 * <p/>
 */
@Component(SUPERVISOR)
@Scope("prototype")
public class Supervisor extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass().getSimpleName());

    @Inject
    private SpringExtension springExtension;
    
    private ActorRef fetcherRef;
    private ActorRef removerRef;
    private ActorRef notifierRef;


    @Override
    public void preStart() throws Exception {

        log.info("Starting up");

        // initialize actors
        
        // single session remover actor
        removerRef = getContext().actorOf(springExtension.props(SEESION_REMOVER), SEESION_REMOVER);
        getContext().watch(removerRef);
        
        // expired sessions caretaker
        fetcherRef = getContext().actorOf(springExtension.props(EXPIRED_SESSIONS_CARETAKER), EXPIRED_SESSIONS_CARETAKER);
        getContext().watch(fetcherRef);        
        
        // single notifier actor
        notifierRef = getContext().actorOf(springExtension.props(SESSION_DELETED_NOTIFIER), SESSION_DELETED_NOTIFIER);
        getContext().watch(notifierRef);

        super.preStart();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ClearExpiredSessions) {
            fetcherRef.tell(message, getSender());
        } else if (message instanceof DeleteSession) {
            removerRef.tell(message, getSender());
        } else {
            log.error("Unable to handle message {}", message);
            unhandled(message);
        }
    }

}