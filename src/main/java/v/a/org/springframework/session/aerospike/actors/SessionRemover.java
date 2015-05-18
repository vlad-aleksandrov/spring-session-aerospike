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

import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.DELETE_SESSION_WORKER;
import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.SEESION_REMOVER;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import v.a.org.springframework.session.messages.DeleteSession;
import v.a.org.springframework.session.support.SpringExtension;
import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.Routee;
import akka.routing.Router;
import akka.routing.SmallestMailboxRoutingLogic;

/**
 * Actor handles sessions removal. The removal request is just delegated to the pool of workers who does actual removal.
 */
@Component(SEESION_REMOVER)
@Scope("prototype")
public class SessionRemover extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass().getSimpleName());

    @Inject
    private SpringExtension springExtension;

    private Router router;

    @Override
    public void preStart() throws Exception {
        log.debug("Starting up {}", this);
        List<Routee> routees = new ArrayList<>();
        
        
        int poolSize = context().system().settings().config().getInt("session.aerospike.actors.remover.workers");
        // initialize multiple janitor workers
        for (int i = 0; i < poolSize; i++) {
            ActorRef actor = getContext().actorOf(springExtension.props(DELETE_SESSION_WORKER));
            getContext().watch(actor);
            routees.add(new ActorRefRoutee(actor));
        }

        router = new Router(new SmallestMailboxRoutingLogic(), routees);
        super.preStart();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        log.info("handle message {}", message);
        if (message instanceof DeleteSession) {
            router.route(message, getSender());
        } else if (message instanceof Terminated) {
            // Readd workers if one failed
            router = router.removeRoutee(((Terminated) message).actor());
            ActorRef actor = getContext().actorOf(springExtension.props(DELETE_SESSION_WORKER), DELETE_SESSION_WORKER);
            getContext().watch(actor);
            router = router.addRoutee(new ActorRefRoutee(actor));
        } else {
            log.error("Unable to handle message {}", message);
        }
    }

}
