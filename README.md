# Kattis - Redis-Compatible Server in Kotlin

A Redis-compatible server implementation built from scratch in Kotlin, designed as a learning project for building
distributed systems. The goal is to implement core Redis features and eventually build a distributed engine similar to
systems like Kafka.

The server implements the RESP (Redis Serialization Protocol) and supports the following commands:

**Basic Commands:**

- `PING` - Test server connectivity
- `ECHO` - Echo back a message

**Key-Value Operations:**

- `SET key value` - Set a key to a string value
- `GET key` - Get the value of a key
- `DEL key [key ...]` - Delete one or more keys
- `EXISTS key [key ...]` - Check if keys exist

**Expiration:**

- `EXPIRE key seconds` - Set a timeout on a key
- `TTL key` - Get the time to live for a key
- `PERSIST key` - Remove the expiration from a key

**Database:**

- `FLUSHDB` - Remove all keys from the current database

**Performance:** Achieves ~49k requests/second on redis-benchmark for SET/GET operations.

## Architecture

### Technology Stack

- **Language:** Kotlin (JVM 21)
- **Concurrency:** Kotlin Coroutines for async I/O
- **Networking:** Ktor Network for raw TCP sockets
- **Functional Programming:** Arrow-kt for error handling
- **Testing:** Kotest

## Getting Started

### Running the Server

```bash
# Start the server on port 6379
./gradlew run
```

The server will start listening on `localhost:6379`.

### Testing with redis-cli

```bash
# Connect to the server
redis-cli -p 6379

# Try some commands
127.0.0.1:6379> PING
PONG
127.0.0.1:6379> SET mykey "Hello"
OK
127.0.0.1:6379> GET mykey
"Hello"
127.0.0.1:6379> EXPIRE mykey 10
(integer) 1
127.0.0.1:6379> TTL mykey
(integer) 10
```

### Running Benchmarks

Test performance using the standard Redis benchmark tool:

```bash
# Basic SET/GET performance
redis-benchmark -t set,get -q
```

do not use ping, it does not support inline commands

### Running Tests

```bash
# Run all tests
./gradlew test
```

## Roadmap

### Phase 1: Basics

- [x] Create TCP server listening on port 6379
- [x] Implement RESP protocol parser and serializer
- [x] Basic commands: `PING`, `ECHO`
- [x] Key-value operations: `SET`, `GET`, `DEL`, `EXISTS`
- [x] Pass redis-benchmark for SET/GET
- [x] TTL support: `EXPIRE`, `TTL`, `PERSIST`
- [x] Proactive background expiration
- [ ] Support type specific commands like:
    - [ ] `INCR`, `DECR` etc.
    - [ ] `LPUSH`, `LLEN`, `LPOP` etc.
    - maybe others, like set and hash operations.
- [ ] Add optional params to commands
- [x] Try optimizing for performance - what can we learn?
  - ByteStrings are not that big of a deal
- [ ] Implement disaster recovery from file

### Phase 2: Advanced

- [ ] Implement Pub/Sub `PUBLISH`, `SUBSCRIBE`, `UNSUBSCRIBE`, `PUBSUB CHANNELS`
- [ ] Implement Redis Streams (`XADD`, `XREAD`) for Kafka-like messaging

### Phase 3: Distributed Layer (Future)

- [ ] Implement Raft consensus algorithm for leader election across multiple nodes
- [ ] **Goal:** Zero-downtime operation during leader failures