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
package v.a.org.springframework.session.messages;

import java.util.Arrays;

import org.springframework.util.Assert;

/**
 * Immutable serialized session attribute.
 */
public class SerializedAttribute {

    private final String className;
    private byte[] content;

    public SerializedAttribute(String className, byte[] content) {
        Assert.notNull(content, "Serialized content can't be null");
        this.className = className;
        this.content = Arrays.copyOf(content, content.length);
    }

    @Override
    public String toString() {
        return new StringBuilder().append(this.getClass()).append("[").append(this.className).append(content.length)
                .append("]").toString();
    }

}
