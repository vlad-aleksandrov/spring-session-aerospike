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
package us.swcraft.springframework.session.messages;

import java.util.Arrays;

/**
 * Immutable session attributes.
 *
 */
public class SessionAttributesBinary {

    private final byte[] attributes;

    /**
     * Creates copy of serialized session data.
     * 
     * @param attributes
     */
    public SessionAttributesBinary(final byte[] attributes) {
        this.attributes = Arrays.copyOf(attributes, attributes.length);
    }

    public byte[] getAttributes() {
        return attributes;
    }

}
