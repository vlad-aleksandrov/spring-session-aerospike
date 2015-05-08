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
package v.a.org.springframework.session.actors;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import v.a.org.springframework.session.messages.SessionControlMessage;
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
 * A sample supervisor which should handle exceptions and general feedback
 * for the actual actors.
 * <p/>
 * A router is configured at startup time, managing a pool of task actors.
 */
@Component
@Scope("prototype")
public class Supervisor extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), "Supervisor");

    @Inject
    private SpringExtension springExtension;

    private Router router;

    @Override
    public void preStart() throws Exception {

        log.info("Starting up");

        List<Routee> routees = new ArrayList<Routee>();
        
        // initialize actors
        // single fetcher        
        ActorRef fetcher = getContext().actorOf(springExtension.props("expiredSessionsFetcher"));
        getContext().watch(fetcher);
        routees.add(new ActorRefRoutee(fetcher));
        
        // multiple removers
        /*
        for (int i = 0; i < 100; i++) {
            ActorRef actor = getContext().actorOf(springExtension.props("sessionRemover"));
            getContext().watch(actor);
            routees.add(new ActorRefRoutee(actor));
        }
        */
        router = new Router(new SmallestMailboxRoutingLogic(), routees);
        super.preStart();
    }

    @Override
    public void onReceive(Object message) throws Exception {

        if (message instanceof SessionControlMessage) {
            router.route(message, getSender());
        } else if (message instanceof Terminated) {
            // Readd task actors if one failed
            router = router.removeRoutee(((Terminated) message).actor());
            ActorRef actor = getContext().actorOf(springExtension.props
                    ("taskActor"));
            getContext().watch(actor);
            router = router.addRoutee(new ActorRefRoutee(actor));
        } else {
            log.error("Unable to handle message {}", message);
        }
    }

    @Override
    public void postStop() throws Exception {
        log.info("Shutting down");
        super.postStop();
    }
}
