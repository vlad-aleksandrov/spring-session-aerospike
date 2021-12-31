/*
 * Copyright 2021 the original author or authors.
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
package us.swcraft.springframework.session.configuration;

import org.springframework.session.Session;

import us.swcraft.springframework.session.store.StoreCompression;
import us.swcraft.springframework.session.store.StoreSerializationType;

/**
 * Aerospike store metadata.
 * 
 * @author Vlad Aleksandrov
 */
public class StoreMetadata {

    /**
     * Namespace name.
     */
    private String namespace;

    /**
     * Aerospike name for session data.
     */
    private String setname;

    /**
     * Store serialization type.
     */
    private StoreSerializationType serializationType;

    /**
     * Store compression type.
     */
    private StoreCompression compression;

    /**
     * Sets the maximum inactive interval in seconds between requests before
     * newly created sessions will be invalidated. A negative time indicates
     * that the session will never timeout. The default is 1800 (30 minutes).
     *
     * @param defaultMaxInactiveInterval
     *            the number of seconds that the {@link Session} should be kept
     *            alive between client requests.
     */
    private Integer maxInactiveIntervalInSeconds;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getSetname() {
        return setname;
    }

    public void setSetname(String setname) {
        this.setname = setname;
    }

    public Integer getMaxInactiveIntervalInSeconds() {
        return maxInactiveIntervalInSeconds;
    }

    public void setMaxInactiveIntervalInSeconds(Integer maxInactiveIntervalInSeconds) {
        this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
    }

    public StoreSerializationType getSerializationType() {
        return serializationType;
    }

    public void setSerializationType(StoreSerializationType serializationType) {
        this.serializationType = serializationType;
    }

    public StoreCompression getCompression() {
        return compression;
    }

    public void setCompression(StoreCompression compression) {
        this.compression = compression;
    }

}
