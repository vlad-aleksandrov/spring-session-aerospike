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

import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.*;
import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import v.a.org.springframework.session.messages.DeleteSession;
import v.a.org.springframework.session.messages.SessionDeletedNotification;
import v.a.org.springframework.store.aerospike.AerospikeOperations;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Actor handles an actual single session removal and broadcasting "session deleted" notification.
 */
@Component(DELETE_SESSION_WORKER)
@Scope("prototype")
public class DeleteSessionWorker extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass().getSimpleName());
    
    private final ActorRef mediator = DistributedPubSubExtension.get(getContext().system()).mediator();
    
    @Inject
    private AerospikeOperations<String> aerospikeOperations;
    
    @Override
    public void onReceive(Object message) throws Exception {
        log.debug("handle message {}", message);
        if (message instanceof DeleteSession) {
            DeleteSession deleteSessionMsg = (DeleteSession) message;
            String sessionId = deleteSessionMsg.getId();
            aerospikeOperations.delete(sessionId);
            mediator.tell(new DistributedPubSubMediator.Publish("sessionDeleted", new SessionDeletedNotification(sessionId)), 
                    getSelf());
        } else {
            unhandled(message);
        }
    }

}
