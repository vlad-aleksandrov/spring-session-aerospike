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

import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.Routee;
import akka.routing.Router;
import akka.routing.SmallestMailboxRoutingLogic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import v.a.org.springframework.session.support.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Actor handles fetching of expired sessions.
 */
@Component("expiredSessionsFetcher")
@Scope("prototype")
public class ExpiredSessionsFetcher extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), "ExpiredSessionsFetcher");


    @Override
    public void onReceive(Object message) throws Exception {
        log.info("handle message {}", message);
    }

}
