import { useState, useCallback } from 'react'
import { errorMapper } from '../services/errorMapper'
import { toastService } from '../services/toastService'

/**
 * Custom hook for API calls with built-in error handling and loading states
 *
 * Usage:
 *
 *   const { execute, loading, error, data } = useApiCall(quotationService.create)
 *
 *   const handleSubmit = async () => {
 *     const result = await execute({ name: 'Quote 1' })
 *     if (result) { // Success
 *       toastService.success('Created!')
 *       navigate('/quotations')
 *     }
 *   }
 *
 * Or with automatic toast:
 *
 *   const result = await execute(data, {
 *     successMessage: 'Quotation created',
 *     errorMessage: 'Failed to create'
 *   })
 *
 * @param {Function} apiFunction - API call function (must return Promise)
 * @param {Object} options - Configuration
 * @param {boolean} options.showToast - Auto-show error toast (default: true)
 * @returns {Object} { execute, loading, error, data, reset }
 */
export function useApiCall(apiFunction, options = {}) {
  const { showToast = true } = options

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [data, setData] = useState(null)

  const execute = useCallback(
    async (payload, callOptions = {}) => {
      try {
        setLoading(true)
        setError(null)

        // Call the API
        const response = await apiFunction(payload)

        // Success
        setData(response.data)

        // Show success toast if configured
        if (callOptions.successMessage) {
          toastService.success(callOptions.successMessage)
        }

        return response.data
      } catch (err) {
        // Map error
        const mappedError = errorMapper.mapError(err)
        setError(mappedError)

        // Show error toast if configured
        if (showToast || callOptions.showToast) {
          toastService.error(
            callOptions.errorMessage || errorMapper.getToastMessage(err)
          )
        }

        return null
      } finally {
        setLoading(false)
      }
    },
    [apiFunction, showToast]
  )

  const reset = useCallback(() => {
    setLoading(false)
    setError(null)
    setData(null)
  }, [])

  return {
    execute,
    loading,
    error,
    data,
    reset,
    isError: !!error,
  }
}

