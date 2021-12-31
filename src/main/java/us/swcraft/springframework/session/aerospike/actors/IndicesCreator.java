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
package us.swcraft.springframework.session.aerospike.actors;

import static us.swcraft.springframework.session.aerospike.actors.ActorsEcoSystem.INDICES_CREATOR;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.aerospike.client.query.IndexType;

import us.swcraft.springframework.session.store.aerospike.AerospikeOperations;

/**
 * Actor handles indices creation. Currently only one secondary index on session
 * expiration timestamp is created.
 */
//@Component(INDICES_CREATOR)
public class IndicesCreator {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private AerospikeOperations<String> aerospikeOperations;

    public void prepare() {
        log.trace("Prepare session store...");
        aerospikeOperations.createIndex(PersistentSessionAerospike.EXPIRED_BIN,
                PersistentSessionAerospike.EXPIRED_INDEX, IndexType.NUMERIC);
    }

}
