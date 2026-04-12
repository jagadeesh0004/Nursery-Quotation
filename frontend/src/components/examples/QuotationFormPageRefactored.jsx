/**
 * PRODUCTION EXAMPLE: QuotationFormPage - Refactored with Complete Error Handling
 *
 * This shows how to upgrade from basic error handling to production-level:
 *
 * BEFORE: toastService.error(getError(err))
 * AFTER:  Structured error mapping, field-level validation, proper loading states
 *
 * Key improvements:
 * ✅ Better error messages with field details
 * ✅ Field-level error display
 * ✅ Loading toast with dismiss pattern
 * ✅ Separate validation layers
 * ✅ Better error categorization
 * ✅ Network error handling
 */

import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Plus, Trash2, Calculator, Save, CheckCheck, AlertTriangle } from 'lucide-react'
import { toastService, errorMapper, quotationService, plantService } from '../services'
import { Button, Input, Textarea, Spinner, SectionCard } from '../components/ui'
import { formatCurrency, cx } from '../utils'

const newItem = () => ({ plantName: '', dimension: '', quantity: 1, price: '' })

export default function QuotationFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()

  // ─── STATE ────────────────────────────────────────────────────────────────
  const [plants, setPlants] = useState([])
  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [customerAddress, setCustomerAddress] = useState('')
  const [items, setItems] = useState([newItem()])
  const [pageLoading, setPageLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)

  // ─── ERROR STATE (structured) ─────────────────────────────────────────────
  const [fieldErrors, setFieldErrors] = useState({})        // Local validation
  const [backendErrors, setBackendErrors] = useState({})    // From API
  const [generalError, setGeneralError] = useState(null)    // Overall error

  const [activeSearch, setActiveSearch] = useState(null)
  const [activeDimSearch, setActiveDimSearch] = useState(null)

  // ─── INITIAL DATA LOAD ────────────────────────────────────────────────────

  // Load plants list
  useEffect(() => {
    const loadPlants = async () => {
      try {
        const res = await plantService.list()
        setPlants(res.data.data || [])
      } catch (error) {
        const message = errorMapper.getToastMessage(error)
        toastService.error(message)
        console.error('Failed to load plants:', error)
      }
    }

    loadPlants()
  }, [])

  // Load existing quotation (edit mode)
  useEffect(() => {
    if (!isEdit) return

    const loadQuotation = async () => {
      setPageLoading(true)
      try {
        const res = await quotationService.getOne(id)
        const q = res.data.data

        setCustomerName(q.customerName || '')
        setCustomerPhone(q.customerPhone || '')
        setCustomerAddress(q.customerAddress || '')
        setItems(
          q.items?.length
            ? q.items.map((i) => ({
                plantName: i.plantName || '',
                dimension: i.dimension || '',
                quantity: i.quantity || 1,
                price: i.price ?? '',
              }))
            : [newItem()]
        )
      } catch (error) {
        const mapped = errorMapper.mapError(error)

        // Handle 404
        if (errorMapper.isNotFoundError(error)) {
          toastService.error('Quotation not found')
          navigate('/dashboard')
          return
        }

        // Handle 403
        if (errorMapper.isForbiddenError(error)) {
          toastService.error('You cannot access this quotation')
          navigate('/dashboard')
          return
        }

        // Handle other errors
        toastService.error(mapped.message)
        console.error('Failed to load quotation:', error)
      } finally {
        setPageLoading(false)
      }
    }

    loadQuotation()
  }, [id, isEdit, navigate])

  // ─── HELPER FUNCTIONS ─────────────────────────────────────────────────────

  function getDimensions(plantName) {
    return plants.find((p) => p.name === plantName)?.dimensions || []
  }

  function updateItem(index, field, value) {
    setItems((prev) => {
      const next = [...prev]
      next[index] = { ...next[index], [field]: value }
      if (field === 'plantName') {
        next[index].dimension = ''
        next[index].price = ''
      }
      if (field === 'dimension') {
        const match = getDimensions(next[index].plantName).find(
          (d) => (d.dimension || d.label) === value
        )
        if (match?.price != null) next[index].price = match.price
      }
      return next
    })

    // Clear field error when user fixes it
    if (fieldErrors[`item_${index}_${field}`]) {
      setFieldErrors((prev) => {
        const next = { ...prev }
        delete next[`item_${index}_${field}`]
        return next
      })
    }
  }

  function addItem() {
    setItems((p) => [...p, newItem()])
  }

  function removeItem(idx) {
    setItems((p) => p.filter((_, i) => i !== idx))
  }

  const grandTotal = items.reduce(
    (sum, it) => sum + (parseFloat(it.price) || 0) * (parseInt(it.quantity) || 0),
    0
  )

  // ─── VALIDATION ───────────────────────────────────────────────────────────

  /**
   * Local validation (fast feedback)
   * Returns errors object or empty if valid
   */
  function validateLocally(shouldFinalize) {
    const errs = {}

    // Customer name (always required)
    if (!customerName.trim()) {
      errs.customerName = 'Customer name is required'
    }

    // Phone (required to finalize)
    if (shouldFinalize && !customerPhone.trim()) {
      errs.customerPhone = 'Phone number is required to finalize'
    } else if (customerPhone.trim() && !/^\d+$/.test(customerPhone)) {
      errs.customerPhone = 'Phone must contain only numbers'
    }

    // Address (required to finalize)
    if (shouldFinalize && !customerAddress.trim()) {
      errs.customerAddress = 'Address is required to finalize'
    }

    // Items validation
    const validItems = items.filter((it) => it.plantName)
    if (validItems.length === 0) {
      errs.items = 'Add at least one plant item to save'
    }

    // Price validation per item
    items.forEach((it, i) => {
      if (!it.plantName) return // Skip empty rows

      if (!it.price || parseFloat(it.price) <= 0) {
        errs[`item_${i}_price`] = 'Price must be greater than 0'
      }

      if (it.quantity < 1) {
        errs[`item_${i}_quantity`] = 'Quantity must be at least 1'
      }
    })

    return errs
  }

  /**
   * Clear errors when user fixes them
   */
  const clearFieldError = (field) => {
    setFieldErrors((prev) => {
      const next = { ...prev }
      delete next[field]
      return next
    })
  }

  // ─── API SUBMISSION ───────────────────────────────────────────────────────

  /**
   * Save quotation with comprehensive error handling
   */
  async function handleSave(shouldFinalize = false) {
    // Clear previous errors
    setFieldErrors({})
    setBackendErrors({})
    setGeneralError(null)

    // Local validation
    const validationErrors = validateLocally(shouldFinalize)
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors)

      // Show helpful toast
      const firstError = Object.values(validationErrors)[0]
      toastService.error(firstError)
      return
    }

    setSaving(true)
    const toastId = toastService.loading(
      shouldFinalize ? 'Finalizing quotation...' : 'Saving draft...'
    )

    try {
      const payload = {
        customerName: customerName.trim() || undefined,
        customerPhone: customerPhone.trim() || undefined,
        customerAddress: customerAddress.trim() || undefined,
        items: items
          .filter((it) => it.plantName)
          .map((it) => ({
            plantName: it.plantName,
            dimension: it.dimension,
            quantity: parseInt(it.quantity) || 1,
            price: parseFloat(it.price) || 0,
          })),
        shouldFinalize,
      }

      // Make API call
      let res
      if (isEdit) {
        res = await quotationService.update(id, payload)
      } else {
        res = await quotationService.create(payload)
      }

      // Success!
      toastService.dismiss(toastId)
      toastService.success(
        shouldFinalize
          ? '✅ Quotation finalized and sent!'
          : '✅ Draft saved successfully'
      )

      // Navigate to detail view
      navigate(`/quotations/${res.data.data?.id || id}`)
    } catch (error) {
      // Dismiss loading toast
      toastService.dismiss(toastId)

      // Map error
      const mapped = errorMapper.mapError(error)

      // ─── HANDLE DIFFERENT ERROR TYPES ────────────────────────────────────

      if (errorMapper.isValidationError(error)) {
        // Backend validation error
        // Example: ["price: Price must be greater than 0", "quantity: Quantity must be positive"]

        const fieldErrors = {}
        const backendErrors = {}

        mapped.errors.forEach((errMsg) => {
          // Try to extract field name
          const [field, ...msgParts] = errMsg.split(':')
          const cleanField = field.trim().toLowerCase()
          const message = msgParts.join(':').trim()

          // Map backend field names to form field names
          if (cleanField.includes('name')) {
            fieldErrors.customerName = message || errMsg
          } else if (cleanField.includes('phone')) {
            fieldErrors.customerPhone = message || errMsg
          } else if (cleanField.includes('address')) {
            fieldErrors.customerAddress = message || errMsg
          } else if (cleanField.includes('price')) {
            backendErrors.price = message || errMsg
          } else {
            backendErrors[cleanField] = message || errMsg
          }
        })

        setFieldErrors(fieldErrors)
        setBackendErrors(backendErrors)
        toastService.error(mapped.errors[0] || 'Validation failed')
      } else if (errorMapper.isAuthError(error)) {
        // 401 Unauthorized - already handled by API interceptor
        toastService.error('Your session expired. Please login again.')
      } else if (errorMapper.isForbiddenError(error)) {
        // 403 Forbidden
        toastService.error('You cannot perform this action')
      } else if (errorMapper.isNotFoundError(error)) {
        // 404 Not Found
        toastService.error('Quotation not found')
        navigate('/dashboard')
      } else if (errorMapper.isServerError(error)) {
        // 500+ Server error
        toastService.error('Server error. Please try again later.')
        setGeneralError('Server is experiencing issues. Please try again in a few moments.')
      } else if (mapped.isNetwork) {
        // Network error
        toastService.error('Network error. Please check your connection.')
        setGeneralError('Check your internet connection and try again.')
      } else {
        // Unknown error
        toastService.error(mapped.message)
      }

      console.error('Save error:', {
        status: mapped.statusCode,
        message: mapped.message,
        errors: mapped.errors,
        originalError: error,
      })
    } finally {
      setSaving(false)
    }
  }

  // ─── QUICK ADD PLANT ──────────────────────────────────────────────────────

  async function handleQuickAddPlant(idx) {
    const it = items[idx]

    if (!it.plantName || !it.dimension || !it.price) {
      toastService.error('Please enter Plant Name, Size, and Price')
      return
    }

    try {
      const created = await plantService.create(it.plantName.trim())
      const plantId = created.data.data?.id

      if (plantId) {
        await plantService.addDimension(plantId, it.dimension.trim(), parseFloat(it.price) || 0)
      }

      const res = await plantService.list()
      setPlants(res.data.data || [])
      toastService.success('Plant added to master list')
    } catch (error) {
      const mapped = errorMapper.mapError(error)

      if (errorMapper.isValidationError(error)) {
        toastService.error('Please check the plant details')
      } else {
        toastService.error(mapped.message)
      }

      console.error('Quick add plant error:', error)
    }
  }

  // ─── QUICK ADD DIMENSION ──────────────────────────────────────────────────

  async function handleQuickAddDimension(idx) {
    const it = items[idx]

    if (!it.plantName || !it.dimension || !it.price) {
      toastService.error('Please enter Size and Price')
      return
    }

    try {
      const plant = plants.find((p) => p.name.toLowerCase() === it.plantName.toLowerCase())

      if (plant?.id) {
        await plantService.addDimension(plant.id, it.dimension.trim(), parseFloat(it.price) || 0)
        const res = await plantService.list()
        setPlants(res.data.data || [])
        toastService.success('Size added to master list')
      }
    } catch (error) {
      const mapped = errorMapper.mapError(error)
      toastService.error(mapped.message)
      console.error('Quick add dimension error:', error)
    }
  }

  // ─── RENDER ───────────────────────────────────────────────────────────────

  if (pageLoading) {
    return (
      <div className="flex items-center justify-center h-64 gap-3 text-muted">
        <Spinner size="md" />
        Loading quotation…
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate(-1)}
          className="p-2 hover:bg-gray-100 rounded-lg transition"
        >
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-2xl font-bold">
          {isEdit ? 'Edit Quotation' : 'Create Quotation'}
        </h1>
      </div>

      {/* General Error Alert */}
      {generalError && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700 flex items-start gap-3">
          <AlertTriangle size={20} className="mt-0.5 flex-shrink-0" />
          <div>
            <p className="font-medium">Error</p>
            <p className="text-sm">{generalError}</p>
          </div>
        </div>
      )}

      {/* Customer Details */}
      <SectionCard title="Customer Details" className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Name *</label>
          <Input
            value={customerName}
            onChange={(e) => {
              setCustomerName(e.target.value)
              clearFieldError('customerName')
            }}
            className={cx(fieldErrors.customerName && 'border-red-500')}
            placeholder="Customer name"
          />
          {fieldErrors.customerName && (
            <p className="text-red-500 text-sm mt-1">{fieldErrors.customerName}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">Phone</label>
          <Input
            value={customerPhone}
            onChange={(e) => {
              setCustomerPhone(e.target.value)
              clearFieldError('customerPhone')
            }}
            className={cx(fieldErrors.customerPhone && 'border-red-500')}
            placeholder="Phone number"
          />
          {fieldErrors.customerPhone && (
            <p className="text-red-500 text-sm mt-1">{fieldErrors.customerPhone}</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">Address</label>
          <Textarea
            value={customerAddress}
            onChange={(e) => {
              setCustomerAddress(e.target.value)
              clearFieldError('customerAddress')
            }}
            placeholder="Delivery address"
          />
          {fieldErrors.customerAddress && (
            <p className="text-red-500 text-sm mt-1">{fieldErrors.customerAddress}</p>
          )}
        </div>
      </SectionCard>

      {/* Items Error */}
      {fieldErrors.items && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-red-700 text-sm">
          {fieldErrors.items}
        </div>
      )}

      {/* Items */}
      <SectionCard title="Items" className="space-y-3">
        {items.map((item, idx) => (
          <div
            key={idx}
            className={cx(
              'p-4 border rounded-lg',
              fieldErrors[`item_${idx}_price`] ? 'border-red-200 bg-red-50' : 'border-gray-200'
            )}
          >
            {/* Item fields */}
            <div className="grid gap-3">
              {/* Plant, dimension, quantity, price inputs */}
              {/* ... (existing item rendering code) ... */}
            </div>

            {fieldErrors[`item_${idx}_price`] && (
              <p className="text-red-500 text-sm mt-2">{fieldErrors[`item_${idx}_price`]}</p>
            )}

            {/* Quick add buttons */}
            <div className="flex gap-2 mt-3">
              <button
                type="button"
                onClick={() => handleQuickAddPlant(idx)}
                className="text-sm px-3 py-1 bg-blue-100 text-blue-700 rounded hover:bg-blue-200"
              >
                + Add to Plants
              </button>
              <button
                type="button"
                onClick={() => handleQuickAddDimension(idx)}
                className="text-sm px-3 py-1 bg-green-100 text-green-700 rounded hover:bg-green-200"
              >
                + Add Size
              </button>
            </div>
          </div>
        ))}

        <button
          onClick={addItem}
          className="w-full py-2 px-4 border border-dashed border-gray-300 rounded-lg hover:border-gray-400 flex items-center justify-center gap-2"
        >
          <Plus size={18} /> Add Item
        </button>
      </SectionCard>

      {/* Total & Actions */}
      <SectionCard className="space-y-4">
        <div className="flex justify-between items-center text-lg font-bold">
          <span>Total:</span>
          <span>{formatCurrency(grandTotal)}</span>
        </div>

        <div className="flex gap-3">
          <Button
            onClick={() => handleSave(false)}
            disabled={saving}
            className="flex-1 gap-2"
          >
            <Save size={18} />
            {saving ? 'Saving...' : 'Save as Draft'}
          </Button>

          <Button
            onClick={() => handleSave(true)}
            disabled={saving}
            variant="success"
            className="flex-1 gap-2"
          >
            <CheckCheck size={18} />
            {saving ? 'Finalizing...' : 'Finalize'}
          </Button>
        </div>
      </SectionCard>
    </div>
  )
}

