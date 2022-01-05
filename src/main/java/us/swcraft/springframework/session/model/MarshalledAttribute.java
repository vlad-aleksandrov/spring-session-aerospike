/*
 * Copyright 2022 the original author or authors.
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

package us.swcraft.springframework.session.model;

import java.io.Serializable;
import java.util.Arrays;

import org.springframework.util.Assert;


/**
 * Immutable serialized session attribute.
 */
public class MarshalledAttribute implements Serializable  {

    private static final long serialVersionUID = 1L;
    
    private final String attributeName;
    
    private final String className;
    private final byte[] content;

    public MarshalledAttribute(String attributeName, String className, byte[] content) {
        Assert.notNull(attributeName, "attribute name can't be null");
        Assert.notNull(className, "class name can't be null");
        Assert.notNull(content, "Serialized content can't be null");
        this.attributeName = attributeName;
        this.className = className;
        this.content = Arrays.copyOf(content, content.length);
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getClassName() {
        return className;
    }

    public byte[] getContent() {
        return content;
    }
    
    @Override
    public String toString() {
        return new StringBuilder().append(this.getClass()).append("[").append(this.attributeName).append("-").append(this.className).append(" ").append(content.length)
                .append(" bytes]").toString();
    }

}
