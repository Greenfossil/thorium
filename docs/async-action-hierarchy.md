~~~~# AsyncAction Type Hierarchy

## Context

Thorium 0.11.2 introduces a unified async/sync type hierarchy, improving API ergonomics and type safety for async operations.

## Design Principles

- **`AsyncAction` and `timeout` are separate but complementary concepts.** `AsyncAction` = "I return Future". `noTimeout`/`timeout` = "control the request timeout". These compose orthogonally.
- **`Action` (sync) is a subtype of `AsyncAction` (async).** The system is async by default. `Action` constrains the return type to `ActionResponse`.
- **`EssentialAction.serve()` stays in `EssentialAction`.** It handles both sync/async results via pattern matching — no need to split.
- **No `.async` methods.** `AsyncAction` IS async — the class name is the marker.

## Target Hierarchy

```
EssentialAction (unchanged, keeps serve())
  └── AsyncAction (NEW)
        └── Action (sync subset)
```

## Changes

### File: `EssentialAction.scala`

#### 1. `AsyncAction` trait + companion object (NEW)

```scala
trait AsyncAction extends EssentialAction

object AsyncAction:
  def apply(fn: Request => AsyncActionResponse): AsyncAction = ...
  def multipart(fn: MultipartRequest => AsyncActionResponse): AsyncAction = ...
  def noTimeout: AsyncActionBuilder = AsyncActionBuilder(ClearTimeout)
  def timeout(duration: Duration): AsyncActionBuilder = AsyncActionBuilder(SetTimeout(duration))
```

#### 2. Builder types with shared timeout

```scala
private trait ActionTimeout:
  def timeoutConfig: TimeoutConfig
  protected def applyTimeout(request: Request): Unit

case class AsyncActionBuilder(timeoutConfig: TimeoutConfig) extends ActionTimeout:
  def apply(fn: Request => AsyncActionResponse): AsyncAction = ...
  def multipart(fn: MultipartRequest => AsyncActionResponse): AsyncAction = ...

case class ActionBuilder(timeoutConfig: TimeoutConfig) extends ActionTimeout:
  def apply(fn: Request => ActionResponse): Action = ...
  def multipart(fn: MultipartRequest => ActionResponse): Action = ...
```

#### 3. `Action` trait — change parent

```scala
// BEFORE:
trait Action extends EssentialAction

// AFTER:
trait Action extends AsyncAction
```

#### 4. `Action` companion object — constrain and simplify

```scala
object Action:
  def apply(fn: Request => ActionResponse): Action = ...
  def multipart(fn: MultipartRequest => ActionResponse): Action = ...
  def noTimeout: ActionBuilder = ActionBuilder(ClearTimeout)
  def timeout(duration: Duration): ActionBuilder = ActionBuilder(SetTimeout(duration))
```

#### 5. Internal types (made public for downstream consumers)

```scala
enum TimeoutConfig:
  case ClearTimeout
  case SetTimeout(duration: Duration)
```

### Thorium API Summary

| Method | Return type | Timeout control |
|--------|-----------|----------------|
| `AsyncAction.apply(fn)` | `AsyncAction` | default |
| `AsyncAction.multipart(fn)` | `AsyncAction` | default |
| `AsyncAction.noTimeout.apply(fn)` | `AsyncAction` | cleared |
| `AsyncAction.noTimeout.multipart(fn)` | `AsyncAction` | cleared |
| `AsyncAction.timeout(dur).apply(fn)` | `AsyncAction` | explicit |
| `AsyncAction.timeout(dur).multipart(fn)` | `AsyncAction` | explicit |
| `Action.apply(fn)` | `Action` | default |
| `Action.multipart(fn)` | `Action` | default |
| `Action.noTimeout.apply(fn)` | `Action` | cleared |
| `Action.noTimeout.multipart(fn)` | `Action` | cleared |
| `Action.timeout(dur).apply(fn)` | `Action` | explicit |
| `Action.timeout(dur).multipart(fn)` | `Action` | explicit |

## Tests

### Existing test migration

- `AsyncActionSuite.scala`: `Action.async` → `AsyncAction`, `Action.multipartAsync` → `AsyncAction.multipart`
- `ResourceManagementSuite.scala`: `Action.async` → `AsyncAction`

### New tests to add

- `AsyncAction` compilation: `apply`, `multipart`, `noTimeout.apply`, `noTimeout.multipart`, `timeout(dur).apply`, `timeout(dur).multipart`
- `Action` compilation: same with sync return types
- `AsyncAction` is a supertype of `Action`

## Execution Order

1. `EssentialAction.scala` — add `AsyncAction` + builder, modify `Action`
2. Tests: migrate existing, add new
3. Verify: compile, run all tests

---

# elementum-web Builder Refactoring

## Context

elementum-web currently uses **nested objects** (`noTimeout`/`timeout`) inside companion objects for timeout composition. This duplicates ~24 near-identical methods across 4 companion objects. Thorium uses **builder classes** instead, which is cleaner and eliminates the duplication. This plan aligns elementum-web with Thorium's builder pattern.

## Problem

Each companion object (`AsyncContextAction`, `ContextAction`, `AsyncSpaceContextAction`, `SpaceContextAction`) has `noTimeout` and `timeout` nested objects, each with `apply` and `multipart` methods. All 8 methods follow the same pattern:

```
timeout logic → toContextRequestResponse/toSpaceContextRequestResponse → callback → multipart parse (if multipart)
```

The only differences are:
- Return type: `AsyncContextAction` vs `ContextAction`
- Callback type: `Async` vs sync
- Multipart parse: `asMultipartFormDataAsync` vs `asMultipartFormData`
- Space variants add `spaceId`/`grantAllSpaces` parameters

## Design

### Shared timeout trait (`ApplyTimeout`)

```scala
trait ApplyTimeout:
  protected def applyTimeout(request: Request, timeoutConfig: TimeoutConfig): Unit =
    timeoutConfig match
      case ClearTimeout         => request.requestContext.clearRequestTimeout()
      case SetTimeout(duration) => request.requestContext.setRequestTimeout(duration)
```

Same pattern as Thorium's `ActionTimeout`. The builders extend this trait.

### Builder types

**ContextAction builders** (4 methods each → 2 methods):

```scala
case class AsyncContextActionBuilder(timeoutConfig: TimeoutConfig, permissions: Seq[(String, String)]) extends ApplyTimeout:
  def apply(fn: ContextRequestWithAppliedPermissionAsync): AsyncContextAction = ...
  def multipart(fn: MultipartContextRequestWithAppliedPermissionAsync): AsyncContextAction = ...

case class ContextActionBuilder(timeoutConfig: TimeoutConfig, permissions: Seq[(String, String)]) extends ApplyTimeout:
  def apply(fn: ContextRequestWithAppliedPermission): ContextAction = ...
  def multipart(fn: MultipartContextRequestWithAppliedPermission): ContextAction = ...
```

**SpaceContextAction builders** (8 methods each → 2 methods):

```scala
case class AsyncSpaceContextActionBuilder(timeoutConfig: TimeoutConfig, spaceId: Long, grantAllSpaces: Boolean, permissions: Seq[(String, String)]) extends ApplyTimeout:
  def apply(fn: SpaceScopedRequestWithAppliedPermissionAsync): AsyncSpaceContextAction = ...
  def multipart(fn: SpaceScopedMultipartRequestWithAppliedPermissionAsync): AsyncSpaceContextAction = ...

case class SpaceContextActionBuilder(timeoutConfig: TimeoutConfig, spaceId: Long, grantAllSpaces: Boolean, permissions: Seq[(String, String)]) extends ApplyTimeout:
  def apply(fn: SpaceScopedRequestWithAppliedPermission): SpaceContextAction = ...
  def multipart(fn: SpaceScopedMultipartRequestWithAppliedPermission): SpaceContextAction = ...
```

### Companion object changes

Replace nested objects with builder constructors:

```scala
object AsyncContextAction extends ContextActionSupport:
  def apply(permissions: (String, String)*)(fn: ...): AsyncContextAction = ...
  def multipart(permissions: (String, String)*)(fn: ...): AsyncContextAction = ...
  def noTimeout(permissions: (String, String)*): AsyncContextActionBuilder =
    AsyncContextActionBuilder(ClearTimeout, permissions)
  def timeout(duration: Duration, permissions: (String, String)*): AsyncContextActionBuilder =
    AsyncContextActionBuilder(SetTimeout(duration), permissions)
```

Same pattern for `ContextAction`, `AsyncSpaceContextAction`, `SpaceContextAction`.

### API surface (unchanged)

```scala
// Before (nested objects):
AsyncContextAction.noTimeout(perms*) { implicit request => user => perms => ... }
AsyncContextAction.timeout.multipart(dur, perms*) { implicit request => user => perms => ... }

// After (builders) — identical user-facing API:
AsyncContextAction.noTimeout(perms*) { implicit request => user => perms => ... }
AsyncContextAction.timeout.multipart(dur, perms*) { implicit request => user => perms => ... }
```

### Duplication reduction

| What | Before | After |
|------|--------|-------|
| Nested object methods | 8 methods × 4 types = 32 | 0 |
| Builder methods | 0 | 2 methods × 4 types = 8 |
| Support method calls | inline in each nested object | centralized in builder |
| Net method count | ~32 | ~8 |

## Files to modify

1. **`ContextActionSupport.scala`** — add `ApplyTimeout` trait (shared with Thorium's `ActionTimeout` pattern)
2. **`ContextAction.scala`** — replace 8 nested objects with 4 builder classes + builder constructors in companions

## Test changes

| Test file | Change |
|-----------|--------|
| `ContextActionAsyncCompileSuite.scala` | Existing tests unchanged — API surface identical |
| `ContextActionAsyncE2ESuite.scala` | Existing tests unchanged |
| `ResourceManagementIntegrationSuite.scala` | Existing tests unchanged |

## Execution Order

1. Add `ApplyTimeout` trait to `ContextActionSupport.scala`
2. Add 4 builder case classes to `ContextAction.scala`
3. Replace nested objects in companion objects with builder constructors
4. Verify: compile elementum-web
5. Run all tests

## Verification

- `sbt clean compile` — elementum-web compiles
- `sbt test` — all 165+ tests pass (API surface unchanged, no test changes needed)
