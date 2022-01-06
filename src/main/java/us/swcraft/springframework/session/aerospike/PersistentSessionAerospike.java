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
package us.swcraft.springframework.session.aerospike;

/**
 * Aerospike session store constants.
 *
 */
abstract class PersistentSessionAerospike {
    
    /**
     * The bin name representing {@link org.springframework.session.ExpiringSession#getId()}
     */
    static final String SESSION_ID_BIN = "sessionId";
    
    /**
     * The bin name representing {@link org.springframework.session.ExpiringSession#getCreationTime()}
     */
    static final String CREATION_TIME_BIN = "created";
    
    /**
     * The bin name representing {@link org.springframework.session.ExpiringSession#getMaxInactiveIntervalInSeconds()}
     */
    static final String MAX_INACTIVE_BIN = "maxInactive";
    
    /**
     * The bin name representing {@link org.springframework.session.ExpiringSession#getLastAccessedTime()}
     */
    static final String LAST_ACCESSED_BIN = "lastAccessed";

    /**
     * The Aerospike bin name for session expiration timestamp
     */
    static final String EXPIRED_BIN = "expired";
    
    /**
     * The Aerospike index name prefix for session expiration timestamp
     */
    static final String EXPIRED_INDEX = "ei";

    /**
     * The Aerospike bin name for session attributes map.
     */
    static final String SESSION_ATTRIBUTES_BIN = "attributes";

}
