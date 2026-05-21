## Thorium framework

![](https://img.shields.io/github/actions/workflow/status/Greenfossil/thorium/run-tests.yml?branch=master)
![](https://img.shields.io/github/license/Greenfossil/thorium)
![](https://img.shields.io/github/v/tag/Greenfossil/thorium)
![Maven Central](https://img.shields.io/maven-central/v/com.greenfossil/thorium_3)
[![javadoc](https://javadoc.io/badge2/com.greenfossil/thorium_3/javadoc.svg)](https://javadoc.io/doc/com.greenfossil/thorium_3) 

Thorium Framework is a modern microservices framework built on top of Armeria, Scala 3 and Java 17.

## CSRF Pre-Auth Verification Bypass

Thorium now supports a narrowly scoped CSRF bypass for an already-implemented pre-auth verification endpoint.

The feature is:

- disabled by default,
- fail-closed,
- exact-path scoped,
- exact-method scoped,
- intended only for pre-auth verification requests,
- designed so all other CSRF behavior remains unchanged.

The configuration entry point is:

```hocon
app.http.csrf.preAuthVerificationBypass {
  enabled = false
  allowPaths = []
  allowMethods = ["POST"]
  requiredContentTypes = []
  requiredHeaders = []
}
```

If `requiredHeaders` is configured, Thorium treats those headers as supplemental presence checks before allowing the CSRF bypass. For example:

- `X-Verify-Channel` can identify which verification flow or caller path the request belongs to.
- `X-Correlation-Id` can carry a trace identifier for log correlation across systems.

These headers improve request shaping and observability, but they are not authentication or authorization signals by themselves.

For detailed design, implementation, and configuration guidance, see:

- [`docs/CSRF_PreAuth_Verification_Design.md`](docs/CSRF_PreAuth_Verification_Design.md)
- [`docs/CSRF_PreAuth_Verification_Implementation_Plan.md`](docs/CSRF_PreAuth_Verification_Implementation_Plan.md)
- [`docs/CSRF_PreAuth_Verification_Configuration_Guide.md`](docs/CSRF_PreAuth_Verification_Configuration_Guide.md)

For more information, visit [our official website](https://thoriumframework.dev).
