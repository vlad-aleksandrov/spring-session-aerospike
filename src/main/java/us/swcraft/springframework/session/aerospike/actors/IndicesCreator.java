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

import static us.swcraft.springframework.session.aerospike.actors.ActorsEcoSystem.INDICES_CREATOR;
import static us.swcraft.springframework.session.messages.SessionControlEvent.CREATE_INDICES;

import javax.inject.Inject;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import us.swcraft.springframework.store.aerospike.AerospikeOperations;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.aerospike.client.query.IndexType;

/**
 * Actor handles indices creation. Currently only one secondary index on session expiration timestamp is created.
 */
@Component(INDICES_CREATOR)
@Scope("prototype")
public class IndicesCreator extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass().getSimpleName());

    @Inject
    private AerospikeOperations<String> aerospikeOperations;

    @Override
    public void onReceive(Object message) throws Exception {
        log.debug("handle message {}", message);
        if (message == CREATE_INDICES) {
            aerospikeOperations.createIndex(PersistentSessionAerospike.EXPIRED_BIN,
                    PersistentSessionAerospike.EXPIRED_INDEX, IndexType.NUMERIC);
        } else {
            unhandled(message);
        }

    }

}
