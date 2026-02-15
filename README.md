# Distributed Redis (Kotlin Edition)

## What is this?
This is a project to practice building distributed systems. It is a custom implementation of a Redis server, built from *almost* scratch.

The goal is not to copy every Redis feature. The goal is to build the **distributed engine** (the hard part) that powers systems like Kafka,
and have acceptable results on the standard Redis benchmark tool. The main focus is on Redis Streams (the `XADD` command), which is a log-like data structure that can be used for messaging, similar to Kafka topics.
The original idea was to reimplement Kafka, but yeah, Redis Streams is close enough and has a nice Redis protocol to work with for validation and benchmark.

## The Main Idea
We are building a "Distributed Log" that looks like Redis on the outside.

1.  **The Interface:** It speaks the Redis protocol. This means you can use the standard `redis-cli` and `redis-benchmark` tools to test it.
2.  **The Engine:** It uses **Raft Consensus** internally. This means multiple nodes (servers) talk to each other to agree on data. If one node dies, the others take over without losing data.
3.  **The Storage:** It writes data to a file on disk (Append-Only Log), just like Kafka does.

## Technology Stack
* **Language:** Kotlin (JVM)
* **Concurrency:** Kotlin Coroutines (for handling many connections at once).
* **Networking:** Ktor Network (for raw TCP sockets).
* **Functional Logic:** Arrow-kt (to handle errors and complex state safely).
* **Testing:** Kotest.

## Project Roadmap

### Phase 1: The Network Layer
* Create a TCP server that listens on port `6379`.
* Parse the Redis text protocol (RESP).
* Support basic commands: `PING`, `SET`, `GET`.
* **Goal:** Pass the `redis-benchmark` test with a single node.

### CURRENTLY HERE:

We have a working TCP server that can handle `PING`, `SET`, `GET`, `DEL`, `EXISTS`, commands, and it passes the
`redis-benchmark` test for these commands at 49k rps.

### Phase 2: The Storage Layer
* Implement a Write-Ahead Log (WAL).
* When a `SET` command comes in, write it to a file on disk immediately.
* Implement `XADD` (Streams) to act like a Kafka topic.

### Phase 3: The Distributed Layer (The "Lock")
* Run 3 separate instances of the server.
* Implement **Leader Election**: The nodes vote for a leader.
* Implement **Replication**: The leader sends data to followers. The command is only "done" when the majority of nodes have it.
* **Goal:** Kill the leader process while the benchmark is running, and see the system recover automatically.

## How to Run

### Prerequisites
* Java 21+
* Redis (only for the `redis-benchmark` tool)

### Running the Server
```bash
# Start the server on port 6379
./gradlew run
```

### Running the Benchmark

Open a terminal and run the standard Redis benchmark tool against our local server.

```bash
# Test 1: Simple Set/Get performance
redis-benchmark -t set,get -q

# Test 2: Distributed Log performance (The Kafka-style test)
# Simulates 50 clients pushing data at once
redis-benchmark -t xadd -c 50 -q
```