/**
 * Error mapper: converts backend errors into user-friendly messages
 *
 * Handles:
 * - Structured error responses (our API format)
 * - Network errors
 * - Timeout errors
 * - Unknown errors
 *
 * Returns an object:
 * {
 *   message: string,        // Main error message to show in toast
 *   errors: string[],       // Detailed field errors (if any)
 *   statusCode: number,     // HTTP status code
 *   isNetwork: boolean,     // True if network error
 * }
 */

export const errorMapper = {
  /**
   * Extract error message from API response
   *
   * @param {Error} error - Axios error or generic error
   * @returns {Object} Mapped error with message and details
   */
  mapError(error) {
    // Network/timeout errors
    if (!error.response) {
      if (error.code === 'ECONNABORTED') {
        return {
          message: 'Request timeout. Please try again.',
          errors: [],
          statusCode: null,
          isNetwork: true,
        }
      }

      if (error.message === 'Network Error' || !navigator.onLine) {
        return {
          message: 'Network error. Please check your internet connection.',
          errors: [],
          statusCode: null,
          isNetwork: true,
        }
      }

      return {
        message: error.message || 'Something went wrong. Please try again.',
        errors: [],
        statusCode: null,
        isNetwork: true,
      }
    }

    // Structured error response from backend
    const { status, data } = error.response

    // Our API error format: { success: false, message, errors?, timestamp }
    if (data?.message) {
      return {
        message: data.message,
        errors: data.errors || [],
        statusCode: status,
        isNetwork: false,
      }
    }

    // Fallback: generic error messages by status code
    const statusMessages = {
      400: 'Bad request. Please check your input.',
      401: 'Your session has expired. Please login again.',
      403: "You don't have permission to perform this action.",
      404: 'The resource you requested was not found.',
      422: 'Invalid input. Please check your data.',
      500: 'Server error. Please try again later.',
      502: 'Service temporarily unavailable. Please try again later.',
      503: 'Service temporarily unavailable. Please try again later.',
    }

    return {
      message: statusMessages[status] || 'Something went wrong. Please try again.',
      errors: [],
      statusCode: status,
      isNetwork: false,
    }
  },

  /**
   * Get a single error message (for toast)
   * Combines message and first field error if present
   */
  getToastMessage(error) {
    const mapped = this.mapError(error)
    if (mapped.errors.length > 0) {
      return mapped.errors[0] // Show first validation error
    }
    return mapped.message
  },

  /**
   * Check if error is authentication-related (401)
   */
  isAuthError(error) {
    return error.response?.status === 401
  },

  /**
   * Check if error is permission-related (403)
   */
  isForbiddenError(error) {
    return error.response?.status === 403
  },

  /**
   * Check if error is validation-related (400)
   */
  isValidationError(error) {
    return error.response?.status === 400 && error.response?.data?.errors
  },

  /**
   * Check if error is not found (404)
   */
  isNotFoundError(error) {
    return error.response?.status === 404
  },

  /**
   * Check if error is server error (500+)
   */
  isServerError(error) {
    return error.response?.status >= 500
  },
}

