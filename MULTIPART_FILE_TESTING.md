# MultipartFile2Suite - Comprehensive Test Suite for New Multipart File APIs

## Overview

The `MultipartFile2Suite` is a comprehensive test suite designed to thoroughly test the new multipart file upload APIs in Thorium:

- **`findFile(predicate: (fieldName:String, fileName:String, contentType:MediaType, content:InputStream) => Boolean): Try[MultipartFile]`**
- **`findFiles(validatorFn: (fieldName:String, fileName:String, contentType:MediaType, content:InputStream) => Boolean): Try[List[MultipartFile]]`**

## Key Features Tested

### 1. **Format Support**
- ✅ PDF files (.pdf) - Validates content and magic numbers
- ✅ JPEG files (.jpg) - Full JPEG validation
- ✅ PNG files (.png) - Full PNG validation
- ❌ Rejects other formats (text, executables, archives)

### 2. **Security & Robustness**

#### A. **File Format Validation**
- Magic number validation (actual file content inspection, not just extension checking)
- Content-Type header verification
- Prevention of format spoofing (e.g., .txt file pretending to be .pdf)

#### B. **DOS Attack Prevention**
- File size limits (5MB max in test suite)
- Streaming validation without buffering entire file in memory
- Early rejection of oversized files

#### C. **Malicious File Detection**
- **Executable files**: Detects EXE, DLL, COM, BAT, CMD, SCR, VBS, PS1
- **Archive files**: Detects ZIP, RAR, 7Z, JAR (prevents archive bombs)
- **Dangerous extensions**: Blocks executable-related extensions
- **Magic number verification**: Inspects actual file content, not just headers

### 3. **API Design & Ease of Use**

#### Simple Validators
```scala
// Format validation only
request.findFile((fieldName, fileName, contentType, content) => {
  contentType.is(MediaType.PDF) || 
  contentType.is(MediaType.PNG) ||
  contentType.is(MediaType.JPEG)
})

// Size validation
request.findFile((fieldName, fileName, contentType, content) => {
  // Check file size by reading stream
  val buffer = new Array[Byte](8192)
  var totalSize = 0
  var bytesRead = 0
  while ({bytesRead = content.read(buffer); bytesRead != -1}) {
    totalSize += bytesRead
    if (totalSize > maxSize) return false
  }
  totalSize <= maxSize
})

// Magic number validation
request.findFile((fieldName, fileName, contentType, content) => {
  val magicBytes = new Array[Byte](4)
  content.read(magicBytes)
  // Check magic bytes for PDF, JPEG, PNG, block EXE/ZIP
  isPdfMagic(magicBytes) || isJpegMagic(magicBytes) || isPngMagic(magicBytes)
})
```

#### Multiple File Processing
```scala
// All files must pass validation (all-or-nothing)
request.findFiles(validator).map { files =>
  // Process all files - this means ALL passed validation
}
```

### 4. **Error Handling**
- Uses `Try[T]` for functional error handling
- Proper exception propagation
- Clear error messages for debugging
- `recover` method for graceful fallbacks

## Test Cases

### Format Validation Tests
1. **✅ findFile: Accept valid PDF** - Validates PDF format and magic numbers
2. **✅ findFile: Accept valid JPG** - Validates JPEG format
3. **✅ findFile: Accept valid PNG** - Validates PNG format
4. **❌ findFile: Reject text file** - Rejects wrong MIME type
5. **❌ findFile: Reject executable EXE** - Detects EXE magic number (0x4D5A)

### Size & DOS Prevention Tests
6. **❌ findFile: Reject oversized file** - DOS attack prevention (>5MB limit)
7. **✅ findFile: Accept file within size limit** - Accepts valid sized files

### Extension & Filename Tests
8. **❌ findFile: Reject file with dangerous extension** - Blocks .exe, .bat, .cmd, etc.
9. **✅ findFile: Accept file with any field name** - Works with different form field names

### Multiple File Handling
10. **✅ findFiles: Accept multiple valid files** - Returns list of valid files
11. **❌ findFiles: Reject all if one fails (all-or-nothing)** - Atomic validation
12. **✅ findFiles: Accept all when all pass** - All files successfully validated

### Archive/Compression Prevention
13. **❌ findFile: Reject ZIP files** - Detects archive bombs

### Error Handling & Edge Cases
14. **✅ API Error Handling** - Proper Try[T] error propagation
15. **✅ Content stream inspection** - Validator can examine file content

## Magic Number Reference

The test suite validates files using their magic numbers (file signatures):

| Format | Magic Bytes | Hex Notation |
|--------|------------|--------------|
| PDF | `%PDF` | `25 50 44 46` |
| JPEG | `ÿØÿ` | `FF D8 FF` |
| PNG | `‰PNG` | `89 50 4E 47` |
| EXE/DLL | `MZ` | `4D 5A` |
| ZIP/JAR | `PK` | `50 4B` |

## Validator Function Signature

```scala
(fieldName: String, fileName: String, contentType: MediaType, content: InputStream) => Boolean
```

### Parameters:
- **fieldName**: The form field name (e.g., "document", "avatar", "photo")
- **fileName**: The uploaded filename (e.g., "resume.pdf")
- **contentType**: The MIME type reported by the client
- **content**: An InputStream of the file content for inspection

### Return Value:
- **true**: File passes validation, will be saved to disk
- **false**: File fails validation, will be rejected

## Best Practices Demonstrated

### ✅ DO:
1. **Always validate magic numbers**, not just file extensions
2. **Check file size** before processing
3. **Use Try[T]** for error handling instead of throwing exceptions
4. **Validate MIME types** against a whitelist
5. **Block dangerous extensions** (exe, bat, cmd, ps1, vbs, sh, etc.)
6. **Log rejected files** for security auditing
7. **Use atomic validation** with `findFiles` when multiple files must all be valid
8. **Read the InputStream carefully** - it can only be read once

### ❌ DON'T:
1. Don't trust file extensions alone
2. Don't trust client-supplied MIME types alone
3. Don't load entire files into memory
4. Don't skip validation for "internal" uploads
5. Don't create predictable file paths (not shown in this suite but important)
6. Don't allow `.zip` or `.jar` files without explicit need

## Running the Tests

```bash
# Run the entire test suite
sbt test

# Run only this test suite
sbt "testOnly com.greenfossil.thorium.MultipartFile2Suite"

# Run specific test
sbt "testOnly com.greenfossil.thorium.MultipartFile2Suite -- -t *PDF*"

# Run with verbose output
sbt "testOnly com.greenfossil.thorium.MultipartFile2Suite -- -v"
```

## Test Infrastructure

The suite creates:
- **Test Server**: Runs on random port with validation endpoints
- **Test Files**: Creates temporary PDF, JPG, PNG, TXT, and EXE test files
- **Cleanup**: Automatically cleans up test files after tests complete

## Integration with MultipartFormData

The test suite uses the following methods from `MultipartFormData`:

```scala
def findFile(predicate: (fieldName:String, fileName:String, contentType:MediaType, content:InputStream) => Boolean): Try[MultipartFile]

def findFiles(validatorFn: (fieldName:String, fileName:String, contentType:MediaType, content:InputStream) => Boolean): Try[List[MultipartFile]]
```

These methods internally use:
- `saveFileTo(fieldName, part, validatorFn)` - Private method that validates and saves files
- `AggregatedMultipart` - From Armeria library for multipart handling
- `MultipartFile` - Armeria's multipart file representation

## Advanced Validation Scenarios

The test suite demonstrates several validation strategies:

### 1. Format-Only Validation
```scala
(fieldName, fileName, contentType, _) => {
  contentType.is(MediaType.PDF)
}
```

### 2. Size-Aware Validation
```scala
(fieldName, fileName, contentType, content) => {
  validateFileSize(content, maxSize = 5 * 1024 * 1024)
}
```

### 3. Magic Number Validation
```scala
(fieldName, fileName, contentType, content) => {
  val magicBytes = new Array[Byte](4)
  content.read(magicBytes)
  isPdfMagic(magicBytes) || isJpegMagic(magicBytes) || isPngMagic(magicBytes)
}
```

### 4. Filename-Based Validation
```scala
(fieldName, fileName, contentType, _) => {
  val blockedExtensions = Set("exe", "bat", "cmd", "vbs", "ps1", "sh")
  val ext = fileName.split('.').lastOption.getOrElse("").toLowerCase
  !blockedExtensions.contains(ext)
}
```

### 5. Combined Validation (Recommended)
```scala
(fieldName, fileName, contentType, content) => {
  isAllowedMimeType(contentType) &&
  hasValidMagicNumber(content) &&
  hasValidSize(content) &&
  hasAllowedExtension(fileName)
}
```

## Security Considerations

1. **InputStream is Single-Read**: The content InputStream can only be read once. Design validators accordingly.
2. **Async Validation**: For resource-intensive validation, consider async processing post-save.
3. **Logging**: Log all rejected files for security auditing.
4. **Rate Limiting**: Consider rate limiting file uploads per user/IP.
5. **Virus Scanning**: For production, integrate with virus scanning services (e.g., ClamAV).
6. **File Storage**: Store uploaded files outside web root to prevent direct execution.

## Conclusion

The `MultipartFile2Suite` provides:
- ✅ Comprehensive validation examples
- ✅ Security best practices
- ✅ Error handling patterns
- ✅ Real-world test scenarios
- ✅ Easy-to-understand API usage

The new APIs (`findFile` and `findFiles`) are easy to use while providing powerful validation capabilities for protecting against file upload attacks.

