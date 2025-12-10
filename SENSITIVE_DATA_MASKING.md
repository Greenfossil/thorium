# Sensitive Data Masking in FirstResponder

## Overview

The `FirstResponderDecoratingFunction` now includes comprehensive sensitive data masking capabilities to prevent logging of sensitive information such as passwords, credit cards, SSNs, and other confidential data.

## Features

### 1. **Configurable Field-Based Masking**

The system uses a `SensitiveFieldConfig` case class that allows customization of:

- **Complete Masking**: Fields containing sensitive keywords are completely masked
- **Partial Masking**: Fields like email and phone numbers can show partial information
- **Custom Mask Value**: Configurable mask string (default: `***REDACTED***`)

### 2. **Content-Type Aware Masking**

The masking system intelligently handles different content types:

- **application/x-www-form-urlencoded**: Masks field=value pairs
- **application/json**: Masks JSON field values
- **text/plain**: Applies both form and JSON masking patterns
- **multipart/form-data**: Masks individual parts by field name

### 3. **Pattern Matching**

Uses case-insensitive substring matching to identify sensitive fields:

```scala
// Default sensitive field patterns
"password", "passwd", "pwd"
"secret", "token", "apikey", "api_key"
"ssn", "social_security"
"creditcard", "credit_card", "cardnumber", "card_number", "cvv", "cvc"
"pin", "auth", "authorization"
```

## Usage

### Default Configuration

```scala
val firstResponder = new FirstResponderDecoratingFunction(configuration)
// Uses default SensitiveFieldConfig with standard patterns
```

### Custom Configuration

```scala
val customConfig = SensitiveFieldConfig(
  sensitiveFieldNames = Set(
    "password", "secret", "api_key", "custom_sensitive_field"
  ),
  maskValue = "[MASKED]",
  partialMaskFields = Map(
    "email" -> (2, 3),     // Shows first 2 and last 3 chars: ab***com
    "phone" -> (4, 2)      // Shows first 4 and last 2 chars: 1234***89
  )
)

val firstResponder = new FirstResponderDecoratingFunction(
  configuration = config,
  sensitiveFieldConfig = customConfig
)
```

## Industry Best Practices Implemented

### 1. **Defense in Depth**
- Multiple layers of masking (field name matching + pattern matching)
- Content-type specific handling
- Both complete and partial masking options

### 2. **Fail-Safe Defaults**
- Comprehensive default list of sensitive field patterns
- Conservative masking approach (better to mask than expose)
- Works out-of-the-box with zero configuration

### 3. **Configurability**
- Externally configurable sensitive field list
- Customizable mask values
- Partial masking for fields that need identification (e.g., email for support)

### 4. **Performance Considerations**
- Masking only occurs when trace logging is enabled
- Pattern compilation happens once
- Efficient string replacement using Scala regex

### 5. **Compliance Alignment**

The implementation helps meet requirements from:

- **PCI-DSS**: Masks credit card data (PAN, CVV)
- **GDPR**: Protects personal identifiable information
- **HIPAA**: Prevents logging of sensitive health data
- **SOC 2**: Demonstrates security controls in logging

## Examples

### Before Masking
```
Dump body - body: username=john&password=secret123&email=john@example.com
```

### After Masking
```
Dump body - body: username=john&password=***REDACTED***&email=joh***com
```

### JSON Masking
```json
// Before
{"username": "john", "password": "secret123", "api_key": "xyz789"}

// After
{"username": "john", "password": "***REDACTED***", "api_key": "***REDACTED***"}
```

### Multipart Masking
```
// Before
partBody: username=john;password=secret123;file=document.pdf

// After
partBody: username=john;password=***REDACTED***;file=document.pdf
```

## Additional Recommendations

### 1. **Log Rotation and Retention**
- Implement log rotation to prevent sensitive data accumulation
- Set appropriate retention periods based on compliance requirements
- Consider encrypting log files at rest

### 2. **Access Controls**
- Restrict access to log files to authorized personnel only
- Implement audit logging for log file access
- Use separate log streams for different sensitivity levels

### 3. **Monitoring and Alerting**
- Monitor for potential sensitive data leaks in logs
- Set up alerts for unusual logging patterns
- Regular security audits of logging practices

### 4. **Development Best Practices**
- Use trace/debug levels for detailed logging
- Never log sensitive data at INFO or higher levels
- Code review logging statements for potential sensitive data exposure
- Include logging security in security training

### 5. **Consider Structured Logging**
```scala
// Use structured logging with field-level control
logger.trace("User login", 
  "username" -> username,
  "password" -> "[MASKED]",
  "timestamp" -> timestamp
)
```

### 6. **Hash for Debugging**
When you need to correlate requests without exposing sensitive data:
```scala
s"password=hash:${value.hashCode}"  // Consistent but not reversible
```

### 7. **Allowlist Approach (Future Enhancement)**
Instead of masking sensitive fields, consider logging only explicitly allowed fields:
```scala
case class AllowedFieldConfig(
  allowedFields: Set[String] = Set("username", "timestamp", "action")
)
// Only log fields in the allowed list
```

## Testing

Ensure your masking configuration works correctly:

```scala
class SensitiveDataMaskingSpec extends AnyFlatSpec with Matchers {
  "maskSensitiveValue" should "mask password fields" in {
    val config = SensitiveFieldConfig()
    val result = maskSensitiveValue("password", "secret123", config)
    result shouldBe "***REDACTED***"
  }
  
  "maskFormData" should "mask sensitive fields in form data" in {
    val config = SensitiveFieldConfig()
    val body = "username=john&password=secret123"
    val result = maskFormData(body, config)
    result should include("password=***REDACTED***")
    result should include("username=john")
  }
}
```

## Troubleshooting

### Sensitive Data Still Appearing in Logs

1. Verify the field name matches your configuration patterns
2. Check if the content-type is supported
3. Ensure trace logging is enabled and masking is triggered
4. Add the specific field name to `sensitiveFieldNames`

### Performance Issues

1. Reduce `maxPlainTextContentLength` to limit body size
2. Use more specific field name patterns
3. Consider disabling trace logging in production
4. Profile regex pattern matching if needed

## Future Enhancements

- Integration with external secret scanning tools
- Machine learning-based sensitive data detection
- Configurable regex patterns for value-based detection
- Integration with data classification systems
- Audit trail for masking operations

