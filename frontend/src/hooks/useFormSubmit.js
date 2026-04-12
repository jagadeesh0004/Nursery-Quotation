import { useCallback } from 'react'
import { toastService } from '../services/toastService'
import { errorMapper } from '../services/errorMapper'

/**
 * Hook for handling form submission with built-in error handling
 * Integrates with react-hook-form for validation
 *
 * Usage with react-hook-form:
 *
 *   const { handleSubmit: hookFormSubmit } = useForm()
 *   const { onSubmit, isSubmitting, fieldErrors } = useFormSubmit(quotationService.create)
 *
 *   return (
 *     <form onSubmit={hookFormSubmit(onSubmit)}>
 *       <input {...register('name', { required: 'Name is required' })} />
 *       {fieldErrors.name && <span>{fieldErrors.name}</span>}
 *       <button disabled={isSubmitting}>Save</button>
 *     </form>
 *   )
 *
 * Or fully manual:
 *
 *   const { submit, isSubmitting, error } = useFormSubmit(apiCall)
 *
 *   const handleClick = async () => {
 *     const result = await submit({ name: 'test' }, {
 *       successMessage: 'Saved!',
 *       onSuccess: () => navigate('/list')
 *     })
 *   }
 *
 * @param {Function} apiFunction - API call to execute
 * @returns {Object} submission helpers
 */
export function useFormSubmit(apiFunction) {
  const submit = useCallback(
    async (formData, options = {}) => {
      try {
        // Call API
        const response = await apiFunction(formData)

        // Show success toast
        if (options.successMessage) {
          toastService.success(options.successMessage)
        } else {
          toastService.success('Operation successful')
        }

        // Call success callback if provided
        if (options.onSuccess) {
          options.onSuccess(response)
        }

        return response
      } catch (error) {
        // Parse errors
        const mapped = errorMapper.mapError(error)

        // Show error toast
        if (options.errorMessage) {
          toastService.error(options.errorMessage)
        } else {
          toastService.error(mapped.message)
        }

        // Show detailed field errors if provided
        if (mapped.errors && mapped.errors.length > 0) {
          console.error('Validation errors:', mapped.errors)
        }

        // Call error callback if provided
        if (options.onError) {
          options.onError(mapped)
        }

        throw error
      }
    },
    [apiFunction]
  )

  return {
    submit,
  }
}

