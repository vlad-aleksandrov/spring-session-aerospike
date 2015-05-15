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

import static v.a.org.springframework.session.aerospike.actors.ActorsEcoSystem.SERIALIZE_SESSION_WORKER;

import java.util.HashMap;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import v.a.org.springframework.session.messages.SessionAttributes;
import v.a.org.springframework.session.messages.SessionAttributesBinary;
import v.a.org.springframework.store.StoreCompression;
import v.a.org.springframework.store.StoreSerializer;
import v.a.org.springframework.store.kryo.KryoStoreSerializer;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Actor handles session serialization/deserialization. The session attributes are serialized as standard
 * {@link java.util.HashMap}.
 */
@Component(SERIALIZE_SESSION_WORKER)
@Scope("prototype")
public class SessionSerializerWorker extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass().getSimpleName());

    private StoreSerializer<HashMap<String, Object>> converter = new KryoStoreSerializer<>(StoreCompression.SNAPPY);

    @Override
    public void onReceive(Object message) throws Exception {
        log.debug("handle message {}", message);
        if (message instanceof SessionAttributes) {
            // serialize attributes and send result back to persister
            SessionAttributes attributes = (SessionAttributes) message;
            final byte[] result = converter.serialize(new HashMap<String, Object>(attributes.getAttributes()));
            getSender().tell(new SessionAttributesBinary(result), getSelf());
        } else {
            unhandled(message);
        }

    }

}