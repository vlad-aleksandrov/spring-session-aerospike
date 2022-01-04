# spring-session-aerospike
Store session data in Aerospike.

# Overview

An session manager implementation that stores sessions in Aerospike for easy distribution of requests across a cluster of web servers. 

Sessions are stored into Aerospike immediately upon creation for use by other servers. Sessions are loaded as requested directly from Aerospike (but subsequent requests for the session during the same request context will return a ThreadLocal cache rather than hitting Aerospike multiple times.) In order to prevent collisions (and lost writes) as much as possible, session data is only updated in Aerospike if the session has been modified.

Data stored in the session must be Serializable.

# Installation

To use the latest release in your application, use this dependency entry in your `pom.xml` (WIP)

````
		<dependency>
			<groupId>us.swcraft.springframework.session.aerospike</groupId>
			<artifactId>spring-session-aerospike</artifactId>
			<version>1.0.0-alpha-8</version>
		</dependency>
````

Add Aerospike clien dependecy:

````
		<dependency>
			<groupId>com.aerospike</groupId>
			<artifactId>aerospike-client</artifactId>
			<version>5.1.11</version>
		</dependency>
````

# Architecture

- provides the session creation, saving, and loading functionality.
- ensures that sessions are saved after a request is finished processing.

# Usage

## Enabe Aerospike HTTP session storage

Add `@EnableAerospikeHttpSession` annotation to the configuration java class file and define `aerospikeClient` client bean.


## Aerospike client configuration

````java
@Configuration
@EnableAerospikeHttpSession
public class SessionStoreConfiguration {
    
    @Inject
    private Environment env;

    @Bean(destroyMethod = "close")
    public IAerospikeClient aerospikeClient() throws Exception {
        final ClientPolicy defaultClientPolicy = new ClientPolicy();
        final IAerospikeClient client = new AerospikeClient(defaultClientPolicy,
                new Host(
                        env.getProperty("aerospike.session.store.host", "localhost"),
                        Integer.valueOf(env.getProperty("aerospike.session.store.port", "3000")))
                        );
        return client;
    }

}
````

In this simple example a single aerospike node is defimed in application properties `erospike.session.store.host` and `aerospike.session.store.port`

`application.properties` example:

````
aerospike.session.store.host = localhost
aerospike.session.store.port = 3000
````



# Acknowledgements

The architecture of this project was inspired by and based on Redis support in "spring-session".

