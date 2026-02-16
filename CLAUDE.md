# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kattis is a Redis-compatible server implementation in Kotlin, built as a learning project for distributed systems.
Currently implements RESP protocol and basic Redis commands.

## Development Commands

```bash
# Build the project
./gradlew build

# Run the server (listens on port 6379)
./gradlew run

# Run all tests
./gradlew test

# Run tests with verbose output
./gradlew test --info

# Run specific test class
./gradlew test --tests "KattisCommandResolvingTest"

# Connect with redis-cli for manual testing
redis-cli -p 6379

# Benchmark performance
redis-benchmark -t set,get -q
```

**Important:** Do not use `redis-benchmark -t ping` - the server does not support inline commands.

## Architecture

### Request Flow

```
TCP Client → TcpServerUtils → RespProtocol.deserialize → RespArray
  → KattisCommand.resolve → KattisCommandFactory → Command instance
  → KattisCommandDispatcher → CommandHandler → KeyValueStore
  → Response (RespValue) → RespProtocol.serialize → TCP Client
```

### RESP Protocol Layer (`resp/`)

- **RespValue**: Sealed class hierarchy representing all RESP types (SimpleString, Integer, BulkString, Array, etc.)
- **RespProtocol**: Deserializes incoming byte streams into RespValue objects and serializes responses back
- **Key constraint:** Inline commands are NOT supported - only RESP protocol format

### Command Pattern (`command/`)

Each Redis command follows a three-part pattern:

1. **Command data class**: `data class SetCommand(val key: String, val value: RespBulkString) : KattisCommand`
2. **Factory object**: Parses `RespArray` into Command, validates arguments, returns
   `Either<RespSimpleError, KattisCommand>`
3. **Handler class**: Executes command logic, interacts with storage, returns `Either<RespSimpleError, RespValue<*>>`

Registration requires:

- Add enum entry to `KattisCommandType`
- Register factory in `KattisCommandFactory.registry`
- Register handler in `KattisCommandDispatcher.handlers`

### Storage Layer (`store/`)

**Port-based architecture** using interface segregation:

- `KeyValueGetPort`, `KeyValueSetPort`, `KeyValueDeletePort`, `KeyExistsPort`, `KeyValueClearPort`
- `KeyValueSetExpirationPort`, `KeyValueGetTtlPort`
- `KeyValueStore` interface combines all ports
- `BasicKeyValueStore` implementation uses `ConcurrentHashMap<String, StoreValue>`

**StoreValue wrapper pattern:**

```kotlin
data class StoreValue(
    val value: RespValue<*>,
    val expires: Instant? = null
)
```

**TTL/Expiration implementation:**

- **Lazy expiration**: Keys checked on access via `computeIfPresent` with expiration check
- **No double-map pattern**: Uses single map with wrapper to avoid synchronization issues
- `expire(key, at: Instant?)` - nullable Instant allows PERSIST (removes expiration)
- `ttl(key)` returns `Pair<Instant?, Boolean>` where Boolean indicates key existence

### Error Handling

Uses Arrow-kt's `Either<RespSimpleError, T>` throughout:

- Left: Error response sent to client
- Right: Success value
- Chain operations with `flatMap`, extract with `merge()`
- Throwing for non-recoverable issues are acceptable

### Concurrency Model

- Kotlin Coroutines for async I/O
- Each client connection runs in separate coroutine
- `supervisorScope` prevents one connection failure from crashing server
- `ConcurrentHashMap` with atomic operations (`compute`, `computeIfPresent`) for thread-safe storage

## Key Constraints & Patterns

1. **No inline commands**: Only RESP protocol format is supported
2. **Atomic store operations**: Use `compute`/`computeIfPresent` instead of separate get/set
3. **Arrow Either**: All command handlers return `Either<RespSimpleError, RespValue<*>>`
4. **Port interfaces**: Storage operations through interface segregation, not direct store access
5. **Expiration on access**: No background cleanup currently - keys expire lazily when accessed

## Testing

- **Kotest** framework with FunSpec style
- Test structure: `KattisCommandResolvingTest` (parsing), `KattisCommandDispatcherTest` (execution)
- Use `shouldBe` matchers, `Either.isRight()`, `Either.getOrNull()`, `Either.leftOrNull()`
- Mock store with anonymous object implementing `KeyValueStore` interface

## Current goals

We are missing proactive expiration, list and integer operations and disaster recovery. Refer to README.md to update
this section.
Do not manually update the README.md file unless specifically asked - instead let me know if you think some part of it
has become outdated.

## Next Phase Goals

Phase 2 will focus on Pub/Sub and Redis Streams. Phase 3 will add Raft consensus for distributed operations.