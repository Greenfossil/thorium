# CSRF Pre-Auth Verification Configuration Guide

## Purpose
Provide safe configuration profiles for enabling pre-auth verification bypass in `CSRFGuardModule` while keeping CSRF enforcement intact elsewhere.

This guide assumes Thorium is only adding a **narrow CSRF policy exception** for an already-existing verification endpoint. It is not intended as a template for general-purpose endpoint exemptions.

## Assumptions
- Token verification mechanism already exists in your application.
- CSRF bypass is only needed so requests can reach that verification endpoint.
- `preAuthVerificationBypass` defaults to disabled when omitted.
- Exact route and exact method matching are preferred for version 1.
- Required headers and content types are supplemental request-shape constraints, not primary trust anchors.

## Recommended Config Keys
```hocon
app.http.csrf.preAuthVerificationBypass {
  enabled = false
  allowPaths = []
  allowMethods = ["POST"]
  requiredContentTypes = []
  requiredHeaders = []
}
```

Notes:
- Keep the config surface small for the first release.
- Prefer exact verification paths only.
- Avoid introducing broad pattern support unless operational evidence later justifies it.
- If `requiredContentTypes` is configured, Thorium accepts exact matches and parameterized equivalents such as `application/json; charset=UTF-8`.

## Empty-list semantics

The three main matching lists do **not** behave the same way when left empty:

- `allowPaths = []` means **no request path can match the bypass**. Even with `enabled = true`, the bypass becomes operationally inert and requests fall back to the normal CSRF flow with a path mismatch.
- `requiredHeaders = []` means **no extra header-presence constraint is applied**. A request may still receive the bypass if the configured path, method, and content-type checks pass.
- `requiredContentTypes = []` means **no content-type constraint is applied**. A request may still receive the bypass if the configured path, method, and required-header checks pass.

In practice, `allowPaths` is the primary route gate. If it is empty, the bypass cannot be granted even if the other lists are empty.

## How `requiredContentTypes` works

When `requiredContentTypes` is configured, Thorium checks the incoming request content type against the configured values.

- Thorium accepts exact media-type matches.
- Thorium also accepts parameterized forms such as `application/json; charset=UTF-8` when the configured required value is `application/json`.
- If the request has no `Content-Type` header and `requiredContentTypes` is non-empty, the bypass is **not** granted and the request falls back to the normal CSRF flow.
- If `requiredContentTypes = []`, Thorium applies no content-type restriction for bypass matching.

Like `requiredHeaders`, this is a supplemental request-shape constraint, not a primary trust signal.

## How `requiredHeaders` works

When `requiredHeaders` is configured, Thorium only checks whether each named header is present on the incoming request before allowing the CSRF bypass.

- Thorium does **not** treat these headers as proof of identity or trust.
- Thorium does **not** validate the header values for business meaning.
- Thorium uses them only as supplemental request-shape constraints.
- If any configured header is missing, the bypass is not granted and the request falls back to the normal CSRF flow.

That means the real security decision for the verification request must still live in the verification endpoint and its surrounding controls.

## What the illustrative headers mean

The staging and production profiles use two illustrative headers:

### `X-Verify-Channel`

Use this header to label which verification channel or entry path the request belongs to, for example:

- `mobile-app`
- `email-link`
- `password-reset`
- `device-binding`

Purpose:

- makes bypass-eligible requests look more like the intended verification traffic shape,
- gives downstream services a simple way to log or branch on the declared verification channel,
- helps operators distinguish one verification flow from another during rollout and incident review.

Thorium itself does not interpret the value. If you require stronger semantics such as an allowlist of channel values, that validation should happen in your application, gateway, or verification service.

### `X-Correlation-Id`

Use this header to carry a request correlation or trace identifier that follows the same request across systems.

Purpose:

- links edge, gateway, Thorium, and verifier logs together for one request path,
- makes it easier to investigate bypass decisions and verification failures,
- improves auditability during staged rollout and production monitoring.

Important clarification:

- A correlation ID is usually **not** meant to be stable across many separate requests from the same client.
- It is meant to be stable **within one request journey** so logs from different layers can be tied back to the same transaction.
- The value may be generated by a client, but operationally it is often better if a gateway, ingress layer, or application boundary issues it, normalizes it, or overwrites untrusted values.
- Issuing such an ID does **not** require Thorium to maintain a persistent store; it is normally a stateless per-request generation step.

Thorium does not validate the format, origin, or uniqueness of the correlation ID. Requiring the header simply ensures that bypass-eligible requests include an observability handle that your other systems can use.

## Recommended scheme for `X-Correlation-Id`

To avoid collision, confusion, and over-trusting client-supplied values, prefer this model:

1. A trusted ingress, gateway, or application boundary issues the correlation ID if it is missing.
2. If a client sends `X-Correlation-Id`, that upstream layer may preserve it as a secondary reference, but should be free to normalize or replace it.
3. Logs should primarily trust the upstream-issued or upstream-normalized value.
4. The correlation ID should be treated as a **request trace key**, not as client identity.
5. Thorium itself does not need to store, reserve, or look up correlation IDs for this model to work.

Practical guidance:

- generate high-entropy IDs using a standard format such as UUIDv4 or an equivalent trace ID format,
- keep the ID unique per request journey,
- treat generation as a stateless concern unless some external observability platform independently chooses to index or store it,
- do not rely on separate remote clients to coordinate uniqueness correctly on their own,
- if you want caller identity, use a different trusted control such as application credentials, signed requests, mTLS, or verifier-side authorization.

This means that yes, some scheme is needed operationally, but that scheme should normally live at the trusted edge or application boundary rather than inside Thorium's CSRF bypass matcher.

## Recommended ownership model for these headers

- Prefer letting your ingress, gateway, or application boundary set or normalize them consistently.
- If a remote client initially supplies `X-Correlation-Id`, treat it as useful for tracing only unless some trusted upstream layer adopts or rewrites it.
- Document expected values and formats outside Thorium if your application depends on them.
- Keep header naming stable so dashboards, log queries, and verifier logic do not drift.
- Treat missing headers as a signal that the request does not match the intended pre-auth verification shape.
- Do **not** treat a missing header, by itself, as proof that the caller is malicious or illegitimate. It may also indicate a misconfigured caller, gateway, or rollout issue.

## Profile 1: Development (Flexible but explicit)
Use this profile for local testing and integration checks.

```hocon
app.http.csrf.preAuthVerificationBypass {
  enabled = true
  allowPaths = ["/auth/verify-token"]
  allowMethods = ["POST"]
  requiredContentTypes = ["application/json"]
  requiredHeaders = []
}
```

Notes:
- Still route-scoped and method-scoped.
- Avoid wildcard paths even in development.
- Use this only for validation of the pre-auth verification flow, not as a shortcut for unrelated endpoints.
- Good for confirming the feature can be enabled without affecting unrelated CSRF-protected routes.

## Profile 2: Staging (Tighter validation)
Use this profile before production rollout.

```hocon
app.http.csrf.preAuthVerificationBypass {
  enabled = true
  allowPaths = ["/auth/verify-token"]
  allowMethods = ["POST"]
  requiredContentTypes = ["application/json"]
  requiredHeaders = ["X-Verify-Channel", "X-Correlation-Id"]
}
```

Notes:
- Adds explicit request-shape controls for safer pre-production validation.
- Helpful for confirming observability and policy matching behavior.
- Good point to validate that wrong path, wrong method, and missing secondary constraints all fail closed.
- Good point to validate log output for `event=csrf_preauth_verification_bypass` decisions.
- `X-Verify-Channel` is useful here when you want staging traffic to declare which verification flow is being exercised.
- `X-Correlation-Id` is useful here when you want each bypass-eligible request to remain traceable across gateway and verifier logs.

## Profile 3: Production (Minimal, strict, auditable)
Use exact-route matching and least-privilege constraints.

```hocon
app.http.csrf.preAuthVerificationBypass {
  enabled = true
  allowPaths = ["/auth/verify-token"]
  allowMethods = ["POST"]
  requiredContentTypes = ["application/json"]
  requiredHeaders = ["X-Verify-Channel", "X-Correlation-Id"]
}
```

Operational expectations:
- Bypass logs are monitored continuously.
- Any route change requires explicit config update and review.
- Non-matching requests continue through standard CSRF checks.
- Do not add extra paths until the initial rollout has stable telemetry.
- Cross-origin form-data and multipart flows should still be validated in isolation and regression-oriented tests during rollout.
- The systems that send verification traffic are expected to populate `X-Verify-Channel` and `X-Correlation-Id` consistently.
- The verification endpoint or gateway, not Thorium, should enforce any value-level policy for those headers.

## Emergency Disable Switch
If suspicious activity is detected, disable bypass immediately:

```hocon
app.http.csrf.preAuthVerificationBypass.enabled = false
```

This should restore baseline CSRF behavior for the verification endpoint.

## Unsafe Patterns to Avoid
- Broad path patterns like `/auth/*` or `/api/**` for bypass.
- Allowing multiple methods when only `POST` is needed.
- Depending on `Origin` alone for bypass trust decisions.
- Treating required headers as proof of legitimacy instead of as request-shape constraints.
- Assuming that the presence of `X-Verify-Channel` or `X-Correlation-Id` alone authenticates the caller.
- Assuming that the absence of `X-Verify-Channel` or `X-Correlation-Id` proves malicious intent.
- Relying on Thorium to validate the meaning, format, or allowed values of those headers.
- Enabling bypass without monitoring bypass decision logs.
- Expanding the feature into a generic exemption mechanism without a separate design review.

## Quick Validation Checklist
- [ ] Bypass disabled blocks verification endpoint via CSRF.
- [ ] Bypass enabled allows only configured path + method.
- [ ] Missing configured headers/content types fail closed.
- [ ] `X-Verify-Channel` and `X-Correlation-Id` are populated consistently by the expected caller path.
- [ ] Wrong path/method still requires CSRF token pair.
- [ ] Existing form/multipart CSRF behavior remains unchanged.
- [ ] Bypass decisions are visible in logs.

## Implementation Validation Notes

- Active tests now cover multipart pass/fail behavior and form-data isolation behavior alongside the bypass suite.
- The form-data isolation suite uses a cross-origin request when validating CSRF enforcement, because same-origin requests are already treated differently by the existing CSRF logic.


