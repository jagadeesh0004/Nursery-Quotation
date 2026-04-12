/**
 * EXAMPLE: Complete form with production-level error handling
 *
 * This demonstrates:
 * 1. Structured error responses from backend
 * 2. Automatic toast notifications
 * 3. Field-level validation errors
 * 4. Loading states
 * 5. User-friendly error messages
 *
 * For real implementation, use this as a template
 */

import { useState } from 'react'
import { toastService, errorMapper } from '../services'
import { quotationService } from '../services'

export default function QuotationFormExample() {
  const [formData, setFormData] = useState({
    customerName: '',
    customerPhone: '',
    customerAddress: '',
    items: [{ plantName: '', quantity: 1, price: '' }],
  })

  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState({}) // Field-level errors

  // ─── HANDLE INPUT CHANGE ──────────────────────────────────────────────────
  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }))
    // Clear field error when user starts editing
    if (errors[name]) {
      setErrors((prev) => {
        const newErrors = { ...prev }
        delete newErrors[name]
        return newErrors
      })
    }
  }

  // ─── LOCAL VALIDATION ─────────────────────────────────────────────────────
  const validateForm = () => {
    const newErrors = {}

    if (!formData.customerName.trim()) {
      newErrors.customerName = 'Customer name is required'
    }

    if (!formData.customerPhone.trim()) {
      newErrors.customerPhone = 'Phone is required'
    } else if (!/^\d+$/.test(formData.customerPhone)) {
      newErrors.customerPhone = 'Phone must contain only numbers'
    }

    if (formData.items.some((item) => !item.plantName)) {
      newErrors.items = 'All items must have a plant selected'
    }

    if (formData.items.some((item) => item.quantity <= 0)) {
      newErrors.items = 'Quantity must be greater than 0'
    }

    if (formData.items.some((item) => !item.price || item.price <= 0)) {
      newErrors.items = 'Price must be greater than 0'
    }

    return newErrors
  }

  // ─── HANDLE FORM SUBMIT ───────────────────────────────────────────────────
  const handleSubmit = async (e) => {
    e.preventDefault()

    // Clear previous errors
    setErrors({})

    // Local validation
    const validationErrors = validateForm()
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors)
      toastService.error('Please fix the errors below')
      return
    }

    setLoading(true)
    const toastId = toastService.loading('Creating quotation...')

    try {
      // Call API
      const response = await quotationService.create(formData)

      // Update toast
      toastService.dismiss(toastId)
      toastService.success('Quotation created successfully!')

      // Reset form
      setFormData({
        customerName: '',
        customerPhone: '',
        customerAddress: '',
        items: [{ plantName: '', quantity: 1, price: '' }],
      })

      // Navigate or update list
      console.log('Created:', response.data)
    } catch (error) {
      // Dismiss loading toast
      toastService.dismiss(toastId)

      // Map error
      const mapped = errorMapper.mapError(error)

      // Handle validation errors from backend
      if (errorMapper.isValidationError(error)) {
        // Show each field error
        const fieldErrors = {}
        mapped.errors.forEach((err) => {
          const [field] = err.split(':')
          fieldErrors[field] = err
        })
        setErrors(fieldErrors)

        // Show first error in toast
        toastService.error(mapped.errors[0] || 'Validation failed')
      } else if (errorMapper.isAuthError(error)) {
        // This is already handled by API interceptor (redirects to login)
        toastService.error('Your session expired. Please login again.')
      } else if (errorMapper.isServerError(error)) {
        toastService.error('Server error. Please try again later.')
      } else {
        toastService.error(mapped.message)
      }

      console.error('Error details:', mapped)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-1">Customer Name</label>
        <input
          type="text"
          name="customerName"
          value={formData.customerName}
          onChange={handleChange}
          className={`w-full px-3 py-2 border rounded ${
            errors.customerName ? 'border-red-500' : 'border-gray-300'
          }`}
          placeholder="Enter customer name"
        />
        {errors.customerName && (
          <p className="text-red-500 text-sm mt-1">{errors.customerName}</p>
        )}
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Phone</label>
        <input
          type="text"
          name="customerPhone"
          value={formData.customerPhone}
          onChange={handleChange}
          className={`w-full px-3 py-2 border rounded ${
            errors.customerPhone ? 'border-red-500' : 'border-gray-300'
          }`}
          placeholder="Enter phone number"
        />
        {errors.customerPhone && (
          <p className="text-red-500 text-sm mt-1">{errors.customerPhone}</p>
        )}
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Address</label>
        <textarea
          name="customerAddress"
          value={formData.customerAddress}
          onChange={handleChange}
          className="w-full px-3 py-2 border border-gray-300 rounded"
          placeholder="Enter address"
        />
      </div>

      {errors.items && (
        <div className="bg-red-50 border border-red-200 p-3 rounded text-red-700 text-sm">
          {errors.items}
        </div>
      )}

      <button
        type="submit"
        disabled={loading}
        className="px-4 py-2 bg-blue-600 text-white rounded disabled:opacity-50"
      >
        {loading ? 'Creating...' : 'Create Quotation'}
      </button>
    </form>
  )
}

