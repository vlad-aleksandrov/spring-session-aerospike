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

import java.io.Serializable;


/**
 * Deleted session notification event.
 */
public class SessionDeletedNotification implements Serializable {

    private static final long serialVersionUID = 4L;
    
    private final String id;
    
    public SessionDeletedNotification() {
        id = null;
    }

    public SessionDeletedNotification(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(this.getClass()).append("[").append(this.getId()).append("]").toString();
    }

}
