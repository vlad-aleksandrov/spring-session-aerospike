/*
 * Copyright 2015 the original author or authors.
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

package v.a.org.springframework.session.aerospike;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import v.a.org.springframework.session.aerospike.AerospikeStoreSessionRepository.AerospikeSession;
import v.a.org.springframework.store.aerospike.AerospikeOperations;

/**
 * A strategy for expiring {@link AerospikeSession} instances.
 * 
 * @author Vlad Aleksandrov
 */
final class AerospikeSessionExpirationPolicy {

    private static final Logger log = LoggerFactory.getLogger(AerospikeSessionExpirationPolicy.class);

    private final AerospikeOperations<String> sessionAerospikeOperations;

    public AerospikeSessionExpirationPolicy(AerospikeOperations<String> sessionAerospikeOperations) {
        super();
        this.sessionAerospikeOperations = sessionAerospikeOperations;
    }

    public void onDelete(String sessionId) {
        // TODO
    }

    public void cleanExpiredSessions() {
        // TODO
    }

}
