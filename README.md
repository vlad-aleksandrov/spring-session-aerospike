# spring-session-aerospike
Store session data in Aerospike.

# Overview

An session manager implementation that stores sessions in Aerospike for easy distribution of requests across a cluster of web servers. 

Sessions are stored into Aerospike immediately upon creation for use by other servers. Sessions are loaded as requested directly from Aerospike (but subsequent requests for the session during the same request context will return a ThreadLocal cache rather than hitting Aerospike multiple times.) In order to prevent collisions (and lost writes) as much as possible, session data is only updated in Aerospike if the session has been modified.

Data stored in the session must be Serializable.

# Architecture

?: provides the session creation, saving, and loading functionality.
?: ensures that sessions are saved after a request is finished processing.

# Usage

## Connection Pool Configuration
## Session Change Tracking

# Acknowledgements

The architecture of this project was inspired by and based on Redis support in "spring-session".

