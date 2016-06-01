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

import static us.swcraft.springframework.session.aerospike.actors.ActorsEcoSystem.SERIALIZE_ATTRIBUTE_WORKER;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import us.swcraft.springframework.session.messages.AttributeSerializationRequest;
import us.swcraft.springframework.session.messages.AttributeSerializationResponse;
import us.swcraft.springframework.session.store.SerializationException;
import us.swcraft.springframework.session.store.StoreCompression;
import us.swcraft.springframework.session.store.StoreSerializer;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Actor handles session serialization/deserialization. The session attributes are serialized as standard
 * {@link java.lang.Object}.
 */
@Component(SERIALIZE_ATTRIBUTE_WORKER)
@Scope("prototype")
public class AttributeSerializerWorker extends UntypedActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this.getClass().getSimpleName());

    @SuppressWarnings({ "rawtypes" })
    private StoreSerializer<Object> converter;

    /**
     * Configures attribute serializer implementation and compression type.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void preStart() throws Exception {
        String className = context().system().settings().config()
                .getString("session.aerospike.actors.attributeserializer.class");
        log.info("Session attribute serializer: {}", className);
        StoreCompression compression = StoreCompression.valueOf(context().system().settings().config()
                .getString("session.aerospike.actors.attributeserializer.compression"));
        log.info("Session attribute compression: {}", compression);
        converter = (StoreSerializer<Object>) Class.forName(className)
                .getConstructor(StoreCompression.class).newInstance(compression);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        log.debug("handle message {}", message);
        if (message instanceof AttributeSerializationRequest) {
            // serialize attributes and send result back to caller
            AttributeSerializationRequest attribute = (AttributeSerializationRequest) message;
            try {
                final byte[] result = converter.serialize(attribute.getValue());
                getSender().tell(new AttributeSerializationResponse(attribute.getKey(), result), getSelf());
            } catch (SerializationException e) {
                log.error("Unable to serialize {} attribute: {}", attribute.getKey(), e.getCause().getMessage());
                log.debug("", e.getCause());
                getSender().tell(new AttributeSerializationResponse(attribute.getKey(), new byte[0]), getSelf());
            }
        }
        else {
            unhandled(message);
        }

    }

}
