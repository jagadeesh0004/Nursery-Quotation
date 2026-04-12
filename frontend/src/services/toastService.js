import toast from 'react-hot-toast'

/**
 * Centralized toast notification service
 *
 * Usage:
 *   toastService.success('Item created')
 *   toastService.error('Something went wrong')
 *   toastService.warning('Are you sure?')
 *   toastService.info('Loading...')
 *
 * Advanced:
 *   toastService.promise(
 *     apiCall(),
 *     { loading: 'Saving...', success: 'Saved!', error: 'Failed' }
 *   )
 */

const DEFAULT_DURATION = 4000
const LONG_DURATION = 6000

export const toastService = {
  success: (message, options = {}) =>
    toast.success(message, {
      duration: options.duration || DEFAULT_DURATION,
      ...options,
    }),

  error: (message, options = {}) =>
    toast.error(message, {
      duration: options.duration || LONG_DURATION,
      ...options,
    }),

  warning: (message, options = {}) =>
    toast(message, {
      icon: '⚠️',
      duration: options.duration || LONG_DURATION,
      ...options,
    }),

  info: (message, options = {}) =>
    toast(message, {
      icon: 'ℹ️',
      duration: options.duration || DEFAULT_DURATION,
      ...options,
    }),

  loading: (message) =>
    toast.loading(message),

  dismiss: (toastId) =>
    toast.dismiss(toastId),

  // Utility: show promise-based toast
  promise: (promise, messages) =>
    toast.promise(promise, {
      loading: messages.loading || 'Processing...',
      success: messages.success || 'Success!',
      error: messages.error || 'Failed',
    }),
}

