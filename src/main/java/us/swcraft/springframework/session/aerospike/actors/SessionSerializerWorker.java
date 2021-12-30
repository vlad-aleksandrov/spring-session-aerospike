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

import static us.swcraft.springframework.session.aerospike.actors.ActorsEcoSystem.SERIALIZE_SESSION_WORKER;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import us.swcraft.springframework.session.messages.SessionAttributes;
import us.swcraft.springframework.session.messages.SessionAttributesBinary;
import us.swcraft.springframework.session.store.StoreCompression;
import us.swcraft.springframework.session.store.StoreSerializer;

import com.google.common.collect.ImmutableMap;

/**
 * Actor handles session serialization/deserialization. The session attributes are serialized as standard
 * {@link java.util.HashMap}.
 */
@Component(SERIALIZE_SESSION_WORKER)
@Scope("prototype")
public class SessionSerializerWorker  {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @SuppressWarnings("rawtypes")
    private StoreSerializer<HashMap> converter;
    

    /**
     * Configures serializer implementation and compression type
     */
//    @SuppressWarnings({ "unchecked", "rawtypes" })
//    @Override
//    public void preStart() throws Exception {
//        String className = context().system().settings().config()
//                .getString("session.aerospike.actors.serializer.class");
//        log.info("Session store serializer: {}", className);
//        StoreCompression compression = StoreCompression.valueOf(context().system().settings().config()
//                .getString("session.aerospike.actors.serializer.compression"));
//        log.info("Session store compression: {}", compression);
//        converter = (StoreSerializer<HashMap>) Class.forName(className)
//                .getConstructor(StoreCompression.class).newInstance(compression);
//    }
//
//    @Override
//    public void onReceive(Object message) throws Exception {
//        log.debug("handle message {}", message);
//        if (message instanceof SessionAttributes) {
//            // serialize attributes and send result back to persister
//            SessionAttributes attributes = (SessionAttributes) message;
//            final byte[] result = converter.serialize(new HashMap<String, Object>(attributes.getAttributes()));
//            getSender().tell(new SessionAttributesBinary(result), getSelf());
//        }
//        else if (message instanceof SessionAttributesBinary) {
//            SessionAttributesBinary binaryData = (SessionAttributesBinary) message;
//            @SuppressWarnings("unchecked")
//            final Map<String, Object> attributes = converter.deserialize(binaryData.getAttributes(), HashMap.class);
//            getSender().tell(ImmutableMap.copyOf(attributes), getSelf());
//        }
//        else {
//            unhandled(message);
//        }
//
//    }

}
