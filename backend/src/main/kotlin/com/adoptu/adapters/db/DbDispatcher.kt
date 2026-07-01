package com.adoptu.adapters.db

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Shared, bounded dispatcher for repository DB calls.
 *
 * `Dispatchers.IO` on its own is elastic/unbounded (up to 64 threads by default),
 * which assumes blocked-on-I/O threads don't consume real CPU while waiting. Under
 * a cgroup-capped container (e.g. ECS Fargate `--cpus=0.5`), that assumption breaks:
 * there's no extra parallelism to exploit, so spinning up dozens of OS threads just
 * adds scheduling/context-switch contention on top of an already-saturated half-core,
 * ballooning wall-clock latency per request even though CPU% reads the same (~50%,
 * the cgroup cap).
 *
 * `limitedParallelism(4)` caps concurrent DB-bound coroutines to 4 threads (drawn from
 * the shared IO dispatcher's thread pool) — enough to keep a few requests' I/O-wait
 * phases overlapping without triggering the same thread-thrashing regression. Sized
 * for a 0.5 vCPU task (JVM sees ~1 logical core after cgroup rounding); revisit if the
 * task's CPU allocation changes.
 *
 * Defined once and reused across all repository implementations — do not call
 * `Dispatchers.IO.limitedParallelism(n)` per call site, since each call would mint a
 * separate limited view rather than sharing one bounded worker pool.
 */
val dbDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(4)
