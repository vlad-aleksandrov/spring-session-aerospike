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

import java.util.AbstractMap;

import org.springframework.util.Assert;

/**
 * Immutable session attribute serialization request.
 */
public class AttributeSerializationRequest extends AbstractMap.SimpleImmutableEntry<String, Object> {

    private static final long serialVersionUID = 1L;

    public AttributeSerializationRequest(final String key, final Object value) {
        super(key, value);
        Assert.notNull(key, "Key can't be null");
        Assert.notNull(key, "Value can't be null");
    }

    @Override
    public String toString() {
        return new StringBuilder().append(this.getClass()).append("[").append(this.getKey()).append(this.getValue())
                .append("]").toString();
    }

}
