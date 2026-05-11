# Status and Error Codes

## 1. HTTP Status Mapping
| HTTP Status | Meaning | Typical Use |
|---|---|---|
| `200` | OK | Read or update success |
| `201` | Created | New resource created |
| `204` | No Content | Delete success with empty body |
| `400` | Bad Request | Invalid query/body format |
| `401` | Unauthorized | Not logged in or invalid session |
| `403` | Forbidden | Role mismatch or privilege violation |
| `404` | Not Found | Resource does not exist |
| `409` | Conflict | Duplicate or state conflict |
| `422` | Unprocessable Entity | Validation semantic errors |
| `500` | Internal Server Error | Unexpected backend failures |

## 2. Business Error Families

### AUTH_*
| Code | Meaning |
|---|---|
| `AUTH_INVALID_CREDENTIALS` | Email/password/role mismatch |
| `AUTH_EMAIL_EXISTS` | Registration email already exists |
| `AUTH_NOT_LOGIN` | Session missing or expired |
| `AUTH_NOT_FOUND` | Session user cannot be found |

### VALIDATION_*
| Code | Meaning |
|---|---|
| `VALIDATION_REQUIRED_FIELD` | Missing required field |
| `VALIDATION_INVALID_FORMAT` | Invalid field format |
| `VALIDATION_INVALID_ENUM` | Unknown enum value |
| `VALIDATION_DATE_RANGE` | Invalid date range |

### CV_*
| Code | Meaning |
|---|---|
| `CV_INVALID_PATH` | CV file name is blank, nested, or contains path traversal |
| `CV_NOT_FOUND` | CV path is not attached to any user record |
| `CV_FILE_NOT_FOUND` | CV path exists but the physical file is missing |
| `CV_PERMISSION_DENIED` | Current user cannot view this CV |

### JOB_*
| Code | Meaning |
|---|---|
| `JOB_NOT_FOUND` | Job ID does not exist |
| `JOB_DEADLINE_INVALID` | Deadline is in invalid range |
| `JOB_PERMISSION_DENIED` | MO cannot modify this job |

### APPLICATION_*
| Code | Meaning |
|---|---|
| `APPLICATION_DUPLICATE` | Same TA applied same job already |
| `APPLICATION_NOT_FOUND` | Application ID not found |
| `APPLICATION_STATUS_INVALID` | Unsupported status transition |

### WORKLOAD_*
| Code | Meaning |
|---|---|
| `WORKLOAD_DATA_MISSING` | Required workload source data missing |
| `WORKLOAD_CALCULATION_FAILED` | Workload calculation failure |

### SYSTEM_*
| Code | Meaning |
|---|---|
| `SYSTEM_NOT_IMPLEMENTED` | Endpoint reserved but not implemented |
| `SYSTEM_IO_FAILURE` | File storage failure |
| `SYSTEM_UNKNOWN` | Unclassified internal error |

### AI_*
| Code | Meaning |
|---|---|
| `AI_PROVIDER_NOT_CONFIGURED` | Reserved for future provider call mode when a model key is required |
| `AI_PROVIDER_FAILED` | Provider call failed or was interrupted |
| `AI_PROVIDER_EMPTY_RESPONSE` | Provider responded successfully but no usable text was returned |

## 3. Error Response Example
```json
{
  "success": false,
  "data": null,
  "meta": {
    "path": "/api/v1/ta/applications"
  },
  "error": {
    "code": "APPLICATION_DUPLICATE",
    "message": "You already applied for this job.",
    "details": []
  }
}
```
