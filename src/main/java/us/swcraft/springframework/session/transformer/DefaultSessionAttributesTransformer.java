/*
 * Copyright 2022 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.swcraft.springframework.session.transformer;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import us.swcraft.springframework.session.model.MarshalledAttribute;
import us.swcraft.springframework.session.store.SerializationException;
import us.swcraft.springframework.session.store.SessionAttributesTransformer;
import us.swcraft.springframework.session.store.StoreSerializer;

@Component("ssa-defaultSessionAttributesTransformer")
public class DefaultSessionAttributesTransformer implements SessionAttributesTransformer {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    @Named("ssa-attrobuteSerializer")
    private StoreSerializer<Serializable> attributeSerializer;

    @Inject
    @Named("ssa-marshalledAttrobutesSerializer")
    private StoreSerializer<Map<String, MarshalledAttribute>> marshalledAttributesSerializer;

    @SuppressWarnings("rawtypes")
    private Class marshalledAttributesMapClass = new HashMap<String, MarshalledAttribute>().getClass();

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] marshall(final Map<String, Object> sessionAttributes) {
        final Map<String, MarshalledAttribute> marshalledAttributes = new HashMap<>(sessionAttributes.size());

        final long start = System.nanoTime();
        try {
            for (Map.Entry<String, Object> sessionAttribute : sessionAttributes.entrySet()) {
                final Object attrValue = sessionAttribute.getValue();
                final String attrName = sessionAttribute.getKey();
                log.trace("Process session attribute '{}' value '{}'", attrName, attrValue);

                if (MarshalledAttribute.class.isAssignableFrom(attrValue.getClass())) {
                    // save attribute "as is".
                    log.trace("Save session attribute '{}' 'as is'", attrName, attrValue);
                    marshalledAttributes.put(sessionAttribute.getKey(), (MarshalledAttribute) attrValue);
                    continue;
                }

                if (isEligebleForSerialization(attrValue)) {
                    try {
                        final byte[] binValue = attributeSerializer.serialize((Serializable) attrValue);
                        final MarshalledAttribute marshalledAttribute = new MarshalledAttribute(attrName,
                                attrValue.getClass().getName(), binValue);
                        log.trace("Save session attribute '{}' as serialized {}", attrName, marshalledAttribute);
                        marshalledAttributes.put(attrName, marshalledAttribute);
                    } catch (Exception e) {
                        log.warn("Unable to marshall class {}: {} - ignore", attrValue.getClass().getName(),
                                e.getMessage());
                    }
                    continue;
                }

                log.debug("Attribute '{}' value '{}' is not eligible for serialization - ignore.", attrName, attrValue);
            }

            // Marshall the result map and convert into byte array
            final byte[] binaryAttrs = marshalledAttributesSerializer.serialize(marshalledAttributes);
            log.trace("Session data: {} bytes", binaryAttrs.length);
            return binaryAttrs;
        } catch (Exception e) {
            final String msg = "Unable to marshall session attributes";
            log.error(msg, e);
            return new byte[0];
        } finally {
            log.trace("Session marshalled in {} ns", System.nanoTime() - start);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> unmarshal(final byte[] binarySessionData) {
        if (binarySessionData == null || binarySessionData.length < 2) {
            log.warn("Stored session data is too short");
            return Collections.emptyMap();
        }

        final long start = System.nanoTime();
        try {
            final Map<String, MarshalledAttribute> marshalledAttributes = marshalledAttributesSerializer
                    .deserialize(binarySessionData, marshalledAttributesMapClass);
            final Map<String, Object> unmarshalledAttributes = new HashMap<>(marshalledAttributes.size());

            for (Map.Entry<String, MarshalledAttribute> entry : marshalledAttributes.entrySet()) {
                final String attributeName = entry.getKey();
                final MarshalledAttribute marshalledAttribute = entry.getValue();
                log.trace("Load session attribute '{}' from serialized form {}", attributeName, marshalledAttribute);
                // check if we can de-serialize value
                try {
                    Class.forName(marshalledAttribute.getClassName());
                    final Serializable attrValue = (Serializable) attributeSerializer
                            .deserialize(marshalledAttribute.getContent(), Serializable.class);
                    unmarshalledAttributes.put(attributeName, attrValue);
                } catch (ClassNotFoundException e) {
                    // Probably created by another webapp
                    log.debug("Unknown class '{}' for attribute '{}' in stored session. Put it in session 'as-is'",
                            marshalledAttribute.getClassName(), marshalledAttribute.getAttributeName());
                    unmarshalledAttributes.put(attributeName, marshalledAttribute);
                } catch (SerializationException e) {
                    // Something else
                    log.warn(
                            "Unable to deserialize class '{}' for attribute '{}' in stored session. Attribute removed. Error: {}",
                            marshalledAttribute.getClassName(), marshalledAttribute.getAttributeName(), e.getMessage());
                    log.debug("", e);
                }
            }
            return unmarshalledAttributes;

        } catch (Exception e) {
            log.error("Unable to unmarshall session data", e);
            return Collections.emptyMap();

        } finally {
            log.trace("Session unmarshalled in {} ns", System.nanoTime() - start);
        }

    }

    /**
     * Checks if object itself of all elements in collections are serializble.
     * 
     * @param attrValue
     * @return
     */
    @SuppressWarnings("rawtypes")
    private boolean isEligebleForSerialization(final Object attrValue) {

        // list or set
        if (Iterable.class.isAssignableFrom(attrValue.getClass())) {
            Iterable iter = (Iterable) attrValue;
            for (Object object : iter) {
                if (!Serializable.class.isAssignableFrom(object.getClass())) {
                    log.trace("one element in collection is not serializble: {}", object);
                    return false;
                }
            }
        }

        // map
        if (Map.class.isAssignableFrom(attrValue.getClass())) {
            Map m = (Map) attrValue;
            for (Object key : m.keySet()) {
                if (!Serializable.class.isAssignableFrom(key.getClass())) {
                    log.trace("key in map is not serializble: {}", key);
                    return false;
                }
                Object value = m.get(key);
                if (!Serializable.class.isAssignableFrom(value.getClass())) {
                    log.trace("value in map is not serializble: {}", value);
                    return false;
                }
            }
        }

        // array
        if (attrValue.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(attrValue); i++) {
                if (!Serializable.class.isAssignableFrom(Array.get(attrValue, i).getClass())) {
                    log.trace("array element is not serializble: {}", Array.get(attrValue, i));
                    return false;
                }
            }
        }

        // everything else
        if (Serializable.class.isAssignableFrom(attrValue.getClass())) {
            return true;
        }
        log.trace("attribute is not serializble: {}", attrValue);
        return false;
    }
}
