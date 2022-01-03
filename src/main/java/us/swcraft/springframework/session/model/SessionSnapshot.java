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
package us.swcraft.springframework.session.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable session snapshot to save.
 */
public class SessionSnapshot {

    private final String sessionId;
    private long expirationTimestamp;
    private boolean updated;
    private long creationTime;
    private long lastAccessedTime;
    private int maxInactiveIntervalInSec;
    private Map<String, Object> sessionAttrs;

    public static class Builder {

        private final String sessionId;
        private long expirationTimestamp;
        private boolean updated;
        private long creationTime;
        private long lastAccessedTime;
        private int maxInactiveIntervalInSec;
        private Map<String, Object> sessionAttrs = new HashMap<>();

        public Builder(final String sessionId) {
            this.sessionId = sessionId;
        }

        public Builder expirationTimestamp(final long expirationTimestamp) {
            this.expirationTimestamp = expirationTimestamp;
            return this;
        }

        public Builder updated(final boolean updated) {
            this.updated = updated;
            return this;
        }

        public Builder creationTime(final long creationTime) {
            this.creationTime = creationTime;
            return this;
        }

        public Builder lastAccessedTime(final long lastAccessedTime) {
            this.lastAccessedTime = lastAccessedTime;
            return this;
        }

        public Builder maxInactiveIntervalInSec(final int maxInactiveIntervalInSec) {
            this.maxInactiveIntervalInSec = maxInactiveIntervalInSec;
            return this;
        }

        /**
         * Note: an attribute is added only if both name and value are not null.
         * 
         * @param name
         * @param value
         * @return
         */
        public Builder addAattribute(final String name, final Object value) {
            if (name != null && value != null) {
                this.sessionAttrs.put(name, value);
            }
            return this;
        }

        public SessionSnapshot build() {
            return new SessionSnapshot(this);
        }

    }

    private SessionSnapshot(final Builder builder) {
        sessionId = builder.sessionId;
        expirationTimestamp = builder.expirationTimestamp;
        updated = builder.updated;
        creationTime = builder.creationTime;
        lastAccessedTime = builder.lastAccessedTime;
        maxInactiveIntervalInSec = builder.maxInactiveIntervalInSec;
        sessionAttrs = new HashMap<>(builder.sessionAttrs);
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    public boolean isUpdated() {
        return updated;
    }

    public Long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public Map<String, Object> getSessionAttrs() {
        return sessionAttrs;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public Integer getMaxInactiveIntervalInSec() {
        return maxInactiveIntervalInSec;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(this.getClass()).append("[").append(this.getSessionId()).append("]")
                .toString();
    }

}
