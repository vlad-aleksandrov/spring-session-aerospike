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


import static v.a.org.springframework.session.aerospike.AerospikeStoreSessionRepository.EXPIRED_BIN;
import static v.a.org.springframework.session.aerospike.AerospikeStoreSessionRepository.SESSION_ID_BIN;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import v.a.org.springframework.session.messages.ClearExpiredSessions;
import v.a.org.springframework.session.messages.DeleteSession;
import v.a.org.springframework.session.support.SpringExtension;
import v.a.org.springframework.store.aerospike.AerospikeOperations;
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
 * Actor handles fetching of expired sessions.
 */
@Component("expiredSessionsFetcher")
@Scope("prototype")
public class ExpiredSessionsFetcher extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), "ExpiredSessionsFetcher");

    @Inject
    private SpringExtension springExtension;
    
    @Inject
    private AerospikeOperations<String> aerospikeOperations;

    private Router router;

    @Override
    public void preStart() throws Exception {

        log.info("Starting up");

        List<Routee> routees = new ArrayList<>();

        // initialize actors
        // multiple removers
        for (int i = 0; i < 20; i++) {
            ActorRef actor = getContext().actorOf(springExtension.props("sessionRemover"));
            getContext().watch(actor);
            routees.add(new ActorRefRoutee(actor));
        }

        router = new Router(new SmallestMailboxRoutingLogic(), routees);
        super.preStart();
    }


    
    @Override
    public void onReceive(Object message) throws Exception {
        log.info("handle message {}", message);
        if (message instanceof ClearExpiredSessions) {
            Set<String> expiredSession = aerospikeOperations.fetchRange(SESSION_ID_BIN, EXPIRED_BIN, 0L, System.currentTimeMillis());
            for (String sessionId : expiredSession) {
                router.route(new DeleteSession(sessionId), getSender());
            }
            
            
        } else if (message instanceof Terminated) {
            // Readd task actors if one failed
            router = router.removeRoutee(((Terminated) message).actor());
            ActorRef actor = getContext().actorOf(springExtension.props
                    ("sessionRemover"));
            getContext().watch(actor);
            router = router.addRoutee(new ActorRefRoutee(actor));
        } else {
            log.error("Unable to handle message {}", message);
        }
    }


}
