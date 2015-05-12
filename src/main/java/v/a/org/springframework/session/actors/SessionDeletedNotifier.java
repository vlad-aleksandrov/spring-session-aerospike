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

import javax.inject.Inject;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.session.events.SessionDestroyedEvent;
import org.springframework.stereotype.Component;

import v.a.org.springframework.session.messages.SessionDeletedNotification;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Actor handles deleted sessions notification.
 */
@Component("sessionDeletedNotifier")
@Scope("prototype")
public class SessionDeletedNotifier extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), "SessionDeletedNotifier");

    @Inject
    private ApplicationEventPublisher eventPublisher;

    public SessionDeletedNotifier() {
        ActorRef mediator = DistributedPubSubExtension.get(getContext().system()).mediator();
        // subscribe to the topic named "sessionDeleted"
        mediator.tell(new DistributedPubSubMediator.Subscribe("sessionDeleted", getSelf()),
                getSelf());
    }

    public void onReceive(Object msg) {
        if (msg instanceof SessionDeletedNotification) {
            SessionDeletedNotification notification = (SessionDeletedNotification) msg;
            log.debug("Notifiy sesion expiration: {}", msg);
            publishEvent(new SessionDestroyedEvent(this, notification.getId()));
        }
        else if (msg instanceof DistributedPubSubMediator.SubscribeAck)
            log.info("subscribing");
        else
            unhandled(msg);
    }

    private void publishEvent(ApplicationEvent event) {
        try {
            this.eventPublisher.publishEvent(event);
        } catch (Throwable ex) {
            log.error("Error publishing " + event, ex);
        }
    }

}
