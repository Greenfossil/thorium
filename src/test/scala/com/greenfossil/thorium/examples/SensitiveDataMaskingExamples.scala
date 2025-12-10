package com.greenfossil.thorium.examples

import com.greenfossil.thorium.*
import com.greenfossil.thorium.decorators.FirstResponderDecoratingFunction
import com.greenfossil.thorium.decorators.FirstResponderDecoratingFunction.SensitiveFieldConfig

/**
 * Examples of configuring sensitive data masking in FirstResponder
 */
object SensitiveDataMaskingExamples {

  /**
   * Example 1: Using default configuration
   * Masks all standard sensitive fields (password, secret, token, ssn, credit card, etc.)
   */
  def defaultConfiguration(config: Configuration): FirstResponderDecoratingFunction = {
    new FirstResponderDecoratingFunction(config)
  }

  /**
   * Example 2: Custom sensitive field configuration
   * Add application-specific sensitive fields
   */
  def customSensitiveFields(config: Configuration): FirstResponderDecoratingFunction = {
    val customConfig = SensitiveFieldConfig(
      sensitiveFieldNames = Set(
        // Standard fields
        "password", "passwd", "pwd",
        "secret", "token", "apikey", "api_key",
        "ssn", "social_security",
        "creditcard", "credit_card", "cvv", "cvc",
        "pin", "auth", "authorization",
        // Application-specific fields
        "user_secret", "access_token", "refresh_token",
        "private_key", "account_number", "routing_number"
      ),
      maskValue = "[REDACTED]"
    )

    new FirstResponderDecoratingFunction(
      configuration = config,
      sensitiveFieldConfig = customConfig
    )
  }

  /**
   * Example 3: Partial masking for identification
   * Show partial data for support/debugging while protecting sensitive parts
   */
  def partialMaskingConfiguration(config: Configuration): FirstResponderDecoratingFunction = {
    val partialConfig = SensitiveFieldConfig(
      sensitiveFieldNames = Set("password", "secret", "token"),
      maskValue = "***",
      partialMaskFields = Map(
        "email" -> (3, 4),        // user***@example.com
        "phone" -> (3, 2),        // 555***89
        "mobile" -> (3, 2),       // 555***89
        "account" -> (4, 4),      // 1234***5678
        "username" -> (2, 2)      // jo***hn
      )
    )

    new FirstResponderDecoratingFunction(
      configuration = config,
      sensitiveFieldConfig = partialConfig
    )
  }

  /**
   * Example 4: Strict masking for compliance
   * Mask everything sensitive with no partial exposure
   */
  def strictComplianceConfiguration(config: Configuration): FirstResponderDecoratingFunction = {
    val strictConfig = SensitiveFieldConfig(
      sensitiveFieldNames = Set(
        "password", "passwd", "pwd",
        "secret", "token", "apikey", "api_key",
        "ssn", "social_security", "sin", "nino",
        "creditcard", "credit_card", "cardnumber", "card_number", "cvv", "cvc", "cvv2",
        "pin", "auth", "authorization",
        "email", "phone", "mobile", "address",
        "dob", "birth", "birthdate",
        "passport", "license", "id_number",
        "salary", "income", "account"
      ),
      maskValue = "***REDACTED_BY_POLICY***",
      partialMaskFields = Map.empty // No partial masking in strict mode
    )

    new FirstResponderDecoratingFunction(
      configuration = config,
      sensitiveFieldConfig = strictConfig
    )
  }

  /**
   * Example 5: Development-friendly configuration
   * Minimal masking for easier debugging in development
   */
  def developmentConfiguration(config: Configuration): FirstResponderDecoratingFunction = {
    val devConfig = SensitiveFieldConfig(
      sensitiveFieldNames = Set("password", "secret", "creditcard", "cvv"),
      maskValue = "[MASKED]",
      partialMaskFields = Map(
        "email" -> (5, 5),
        "phone" -> (5, 4),
        "account" -> (4, 4)
      )
    )

    new FirstResponderDecoratingFunction(
      configuration = config,
      sensitiveFieldConfig = devConfig,
      maxPlainTextContentLength = 4096 // Larger limit for dev
    )
  }

  /**
   * Example 6: Industry-specific configuration (Healthcare/HIPAA)
   */
  def hipaaCompliantConfiguration(config: Configuration): FirstResponderDecoratingFunction = {
    val hipaaConfig = SensitiveFieldConfig(
      sensitiveFieldNames = Set(
        "password", "secret", "token",
        "ssn", "social_security",
        "medical_record", "mrn", "patient_id",
        "diagnosis", "prescription", "medication",
        "insurance", "policy_number",
        "dob", "birthdate", "date_of_birth",
        "address", "phone", "email"
      ),
      maskValue = "***PHI_REDACTED***",
      partialMaskFields = Map.empty
    )

    new FirstResponderDecoratingFunction(
      configuration = config,
      sensitiveFieldConfig = hipaaConfig
    )
  }

  /**
   * Example 7: Financial services configuration (PCI-DSS)
   */
  def pciDssCompliantConfiguration(config: Configuration): FirstResponderDecoratingFunction = {
    val pciConfig = SensitiveFieldConfig(
      sensitiveFieldNames = Set(
        "password", "pin",
        "cardnumber", "card_number", "pan", "creditcard", "credit_card",
        "cvv", "cvc", "cvv2", "cid", "security_code",
        "expiry", "expiration", "exp_date",
        "track1", "track2", "magnetic_stripe",
        "account_number", "routing_number",
        "swift", "iban", "bic"
      ),
      maskValue = "***PCI_REDACTED***",
      partialMaskFields = Map(
        "cardnumber" -> (4, 4) // Show first and last 4 digits only
      )
    )

    new FirstResponderDecoratingFunction(
      configuration = config,
      sensitiveFieldConfig = pciConfig
    )
  }

  /**
   * Test case data to verify masking works correctly
   */
  object TestData {
    val formData = "username=john&password=secret123&email=john@example.com&phone=5551234567"
    val jsonData = """{"username":"john","password":"secret123","api_key":"xyz789","email":"john@example.com"}"""
    val multipartData = "username=john;password=secret123;creditcard=4111111111111111;cvv=123"

    // Expected outputs with default config:
    // formData -> "username=john&password=***REDACTED***&email=joh***com&phone=555***67"
    // jsonData -> {"username":"john","password":"***REDACTED***","api_key":"***REDACTED***","email":"joh***com"}
    // multipartData -> "username=john;password=***REDACTED***;creditcard=***REDACTED***;cvv=***REDACTED***"
  }
}

