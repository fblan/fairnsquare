# Feature: Use Case Logging via CDI Interceptor

**Date:** 2026-02-24

## What, Why and Constraints

### What
A reusable logging mechanism based on CDI interceptors:
- `@Log` — interceptor binding annotation that enables automatic logging of method invocations.
- `@LogTag("name")` — parameter annotation that includes the parameter value as a named tag in the log entry.
- `LogInterceptor` — CDI interceptor that logs method name, tagged parameters, result (or error), and elapsed time.

### Why
All use case calls in `SplitUseCases` should be logged for observability and debugging. Using an interceptor avoids polluting business logic with logging code and ensures consistent log format across all use cases.

### Constraints
- Uses JBoss Logger (`org.jboss.logging.Logger`) consistent with the existing codebase pattern.
- Logs under the target class's logger name (e.g. `SplitUseCases`), not under `LogInterceptor`.
- INFO level for successful invocations, ERROR level for exceptions.
- The interceptor re-throws all exceptions — it is purely observational.

---

## How

### Files Created

**`sharedkernel/logging/Log.java`**
CDI `@InterceptorBinding` annotation. `@Inherited` so it can be placed on a class to apply to all public methods. Can also be placed on individual methods.

**`sharedkernel/logging/LogTag.java`**
`@Target(PARAMETER)` annotation with a `value()` attribute specifying the tag name. The interceptor extracts these at invocation time via reflection.

**`sharedkernel/logging/LogInterceptor.java`**
CDI `@Interceptor` at `@Priority(APPLICATION)`. On each invocation:
1. Extracts tagged parameters into `key=value` pairs via `Method.getParameters()` reflection.
2. Starts a timer.
3. Calls `context.proceed()`.
4. On success: logs at INFO with `method=X tags result=Y duration=Zms`.
5. On exception: logs at ERROR with `method=X tags error=message duration=Zms`, then rethrows.

### Files Modified

**`split/service/SplitUseCases.java`**
- Added `@Log` on the class (all 14 public methods are now logged).
- Added `@LogTag("splitId")` on all `splitId` parameters.
- Added `@LogTag("participantId")` on `updateParticipant` and `removeParticipant`.
- Added `@LogTag("expenseId")` on `removeExpense` and `updateExpense`.
- Added `@LogTag("payerId")` on `addExpenseByNight`, `addExpenseByPerson`, `addExpenseEqual`.

### Log Output Example
```
method=addExpenseByNight splitId=abc123 payerId=xyz789 result=ExpenseByNight{...} duration=12ms
method=getSplit splitId=abc123 result=empty duration=3ms
```

`Optional` results are automatically unwrapped: present values are logged directly, absent values are logged as `empty`.

---

## Tests

All tests are automated (`@QuarkusTest`) in `LogInterceptorTest` using a dedicated `LoggedTestService` test bean:

| Test | Scenario |
|---|---|
| `shouldLogMethodNameAndResultOnSuccess` | Verifies method name and result appear in log |
| `shouldLogTaggedParameters` | Verifies `@LogTag` value appears as `name=value` |
| `shouldLogMultipleTags` | Verifies multiple tags appear in a single log entry |
| `shouldLogWithoutTagsWhenNonePresent` | Verifies logging works with no `@LogTag` annotations |
| `shouldLogErrorOnException` | Verifies error message and tag appear on exception |
| `shouldUnwrapPresentOptionalInLog` | Verifies `Optional.of(x)` logs as `result=x` |
| `shouldLogEmptyForAbsentOptional` | Verifies `Optional.empty()` logs as `result=empty` |
| `shouldLogAtInfoLevelOnSuccess` | Verifies JUL INFO level for successful calls |
| `shouldLogAtSevereLevelOnError` | Verifies JUL SEVERE level for exceptions |
| `shouldIncludeDurationInLog` | Verifies `duration=Nms` format in log output |

Full test suite: **229 tests, 0 failures**.