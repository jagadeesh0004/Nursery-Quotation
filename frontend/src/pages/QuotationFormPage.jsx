/**
 * QuotationFormPage — create + edit quotations
 * GET  /plants           → plant dropdown data
 * GET  /quotations/:id   → load existing (edit mode)
 * POST /quotations       → create
 * PUT  /quotations/:id   → update
 */
import { useState, useEffect } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ArrowLeft, Plus, Trash2, Calculator, Save, CheckCheck, AlertTriangle } from 'lucide-react'
import toast from 'react-hot-toast'
import { quotationService, plantService, customerService } from '../services'
import { Button, Input, Textarea, Spinner, SectionCard, Select } from '../components/ui'
import { formatCurrency, formatNumber, getError, cx } from '../utils'

const newItem = () => ({ plantName: '', dimension: '', height: '', quantity: '', price: '' })

export default function QuotationFormPage() {
  const { id }   = useParams()
  const isEdit   = Boolean(id)
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [plants,          setPlants]          = useState([])
  const [customerName,    setCustomerName]    = useState('')
  const [customerPhone,   setCustomerPhone]   = useState('')
  const [customerAddress, setCustomerAddress] = useState('')
  const [notes,           setNotes]           = useState('')
  const [heightEnabled,   setHeightEnabled]   = useState(false)
  const [shippingCharges, setShippingCharges] = useState('')
  const [discountType,    setDiscountType]    = useState('AMOUNT')
  const [discountValue,   setDiscountValue]   = useState('')
  const [items,           setItems]           = useState([newItem()])
  const [pageLoading,     setPageLoading]     = useState(isEdit)
  const [saving,          setSaving]          = useState(false)
  const [errors,          setErrors]          = useState({})
  const [activeSearch,    setActiveSearch]    = useState(null)
  const [activeDimSearch, setActiveDimSearch] = useState(null)
  const [quickAddLoading, setQuickAddLoading] = useState({})

  useEffect(() => {
    const customerId = searchParams.get('customerId')
    if (!isEdit && customerId) customerService.getOne(customerId).then(r => {
      const c = r.data.data
      setCustomerName(c.name || '')
      setCustomerPhone(c.phone || '')
      setCustomerAddress(c.address || '')
    }).catch(err => { const message = getError(err); if (message) toast.error(message) })
  }, [isEdit, searchParams])

  useEffect(() => {
    plantService.list()
      .then(r => setPlants(r.data.data || []))
      .catch((err) => {
        const message = getError(err)
        if (message) toast.error(message)
      })
  }, [])

  useEffect(() => {
    if (!isEdit) return
    setPageLoading(true)
    quotationService.getOne(id).then(r => {
      const q = r.data.data
      setCustomerName(q.customerName || '')
      setCustomerPhone(q.customerPhone || '')
      setCustomerAddress(q.customerAddress || '')
      setNotes(q.notes || '')
      setHeightEnabled(Boolean(q.heightEnabled))
      setShippingCharges(q.shippingCharges ?? '')
      setDiscountType(q.discountType || 'AMOUNT')
      setDiscountValue(q.discountType === 'PERCENTAGE'
        ? (q.discountPercentage ?? '')
        : (q.discountAmount ?? ''))
      setItems(q.items?.length ? q.items.map(i => ({
        plantName: i.plantName || '', dimension: i.dimension || '', height: i.height || '',
        quantity: i.quantity ?? 0, price: i.price ?? '',
      })) : [newItem()])
    }).catch((err) => {
      const message = getError(err)
      if (message) toast.error(message)
    })
    .finally(() => setPageLoading(false))
  }, [id, isEdit])

  function getDimensions(plantName) {
    return plants.find(p => p.name === plantName)?.dimensions || []
  }

  function updateItem(index, field, value) {
    if (field === 'quantity') value = value === '' ? '' : Number(value)
    setItems(prev => {
      const next = [...prev]
      next[index] = { ...next[index], [field]: value }
      if (field === 'plantName') { next[index].dimension = ''; next[index].price = '' }
      if (field === 'dimension') {
        const match = getDimensions(next[index].plantName).find(d => (d.dimension || d.label) === value)
        if (match?.price != null) next[index].price = match.price
      }
      return next
    })
  }

  function addItem()       { setItems(p => [...p, newItem()]) }
  function removeItem(idx) { setItems(p => p.filter((_, i) => i !== idx)) }

  const grandTotal = items.reduce(
    (sum, it) => sum + (parseFloat(it.price) || 0) * (parseInt(it.quantity) || 0), 0
  )
  const shippingValue = parseFloat(shippingCharges) || 0
  const rawDiscountValue = parseFloat(discountValue) || 0
  const discountAmount = discountType === 'PERCENTAGE'
    ? (grandTotal * rawDiscountValue) / 100
    : rawDiscountValue
  const finalGrandTotal = grandTotal + shippingValue - discountAmount

  const allUniqueDimensions = Array.from(new Set(plants.flatMap(p => p.dimensions?.map(d => d.dimension || d.label) || []))).filter(Boolean).sort()

  const desktopCols = heightEnabled
    ? '3fr 1.75fr 1.5fr 1.75fr 1.75fr 1.75fr 0.75fr'
    : '3.25fr 2.25fr 1.75fr 2fr 2fr 1fr'

     //const desktopCols = '3.25fr 2.25fr 1.75fr 2fr 2fr 1fr'
  function validate(shouldFinalize) {
    const errs = {}
    if (shouldFinalize && !customerName.trim()) errs.customerName = 'Customer name is required'
    if (shouldFinalize && !customerAddress.trim()) errs.customerAddress = 'Address is required to finalize'
    if (discountType === 'PERCENTAGE' && rawDiscountValue > 100) errs.discountValue = 'Discount percentage cannot exceed 100'
    if (discountAmount > grandTotal) errs.discountValue = 'Discount cannot exceed the plant total'
    if (shippingValue < 0) errs.shippingCharges = 'Loading charges cannot be negative'
    const validItems = items.filter(it => it.plantName)
    if (validItems.length === 0) errs.items = 'Add at least one plant item to save the quotation'
    items.forEach((it, i) => {
      if (it.plantName && (it.price === '' || it.price === null || it.price === undefined)) {
        if (!errs.itemRows) errs.itemRows = {}
        errs.itemRows[i] = 'Price is required'
      } else if (heightEnabled && it.plantName && (!it.height || !it.height.trim())) {
        if (!errs.itemRows) errs.itemRows = {}
        errs.itemRows[i] = 'Height is required when enabled'
      }
    })
    setErrors(errs)
    return errs
  }

  async function handleSave(shouldFinalize = false) {
    const currentErrs = validate(shouldFinalize)
    if (Object.keys(currentErrs).length > 0) {
      if (currentErrs.customerName) {
        toast.error('Please add the customer name before finalizing')
      } else if (currentErrs.items) {
        toast.error(currentErrs.items)
      } else {
        toast.error('Please fix the highlighted errors before saving')
      }
      return
    }
    setSaving(true)
    try {
      const payload = {
        customerName:    customerName.trim()    || undefined,
        customerPhone:   customerPhone.trim()   || undefined,
        customerAddress: customerAddress.trim() || undefined,
        notes:           notes.trim()           || undefined,
        heightEnabled,
        shippingCharges: shippingValue,
        discount: discountValue === ''
          ? undefined
          : {
              type: discountType,
              value: rawDiscountValue,
            },
        items: items.filter(it => it.plantName).map(it => ({
          plantName: it.plantName,
          dimension: it.dimension,
          height: heightEnabled ? (it.height || '').trim() : undefined,
          quantity:  parseInt(it.quantity) || 0,
          price:     parseFloat(it.price)  || 0,
        })),
        shouldFinalize,
      }
      let res
      if (isEdit) res = await quotationService.update(id, payload)
      else        res = await quotationService.create(payload)
      toast.success(shouldFinalize ? 'Quotation finalized!' : 'Draft saved!')
      navigate(`/quotations/${res.data.data?.id || id}`)
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
    finally { setSaving(false) }
  }

  async function handleQuickAddPlant(idx) {
    const it = items[idx]
    if (!it.plantName || !it.dimension || !it.price) {
      toast.error('Please enter Plant Name, Size, and Price to add to master')
      return
    }
    setQuickAddLoading(prev => ({ ...prev, [`plant-${idx}`]: true }))
    try {
      const created = await plantService.create(it.plantName.trim())
      const plantId = created.data.data?.id
      if (plantId) {
        await plantService.addDimension(plantId, it.dimension.trim(), parseFloat(it.price) || 0)
      }
      const r = await plantService.list()
      setPlants(r.data.data || [])
      toast.success('Plant added to master list')
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    } finally {
      setQuickAddLoading(prev => ({ ...prev, [`plant-${idx}`]: false }))
    }
  }

  async function handleQuickAddDimension(idx) {
    const it = items[idx]
    if (!it.plantName || !it.dimension || !it.price) {
      toast.error('Please enter Size and Price to add to master')
      return
    }
    setQuickAddLoading(prev => ({ ...prev, [`dimension-${idx}`]: true }))
    try {
      const plant = plants.find(p => p.name.toLowerCase() === it.plantName.toLowerCase())
      if (plant?.id) {
        await plantService.addDimension(plant.id, it.dimension.trim(), parseFloat(it.price) || 0)
        const r = await plantService.list()
        setPlants(r.data.data || [])
        toast.success('Size added to master list')
      }
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    } finally {
      setQuickAddLoading(prev => ({ ...prev, [`dimension-${idx}`]: false }))
    }
  }

  function toggleHeightEnabled() {
    setHeightEnabled(prev => {
      const next = !prev
      if (!next) {
        setItems(current => current.map(item => ({ ...item, height: '' })))
      }
      return next
    })
  }

  if (pageLoading) return (
    <div className="flex items-center justify-center h-64 gap-3 text-muted">
      <Spinner size="md" /> Loading quotation...
    </div>
  )

 

  return (
    <div className="animate-fade-up max-w-3xl">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate(-1)}
          className="w-9 h-9 rounded-xl bg-white border border-gray-200 flex items-center justify-center hover:bg-green-50 transition-colors shadow-sm">
          <ArrowLeft className="w-4 h-4 text-gray-600" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-ink">{isEdit ? 'Edit Quotation' : 'New Quotation'}</h1>
          <p className="text-sm text-muted mt-0.5">{isEdit ? 'Update draft details below.' : 'Draft a new quotation for a customer.'}</p>
        </div>
      </div>

      {/* Customer Details */}
      <SectionCard title="Customer Details" icon="👤" className="mb-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
          <Input label="Customer Name" placeholder="e.g. Ravi Traders"
            value={customerName} onChange={e => setCustomerName(e.target.value)}
            error={errors.customerName} />
          <Input label="Phone Number" type="tel" placeholder="9876543210"
            value={customerPhone} onChange={e => setCustomerPhone(e.target.value)} />
        </div>
        <Textarea label="Address" placeholder="Shipping / delivery address"
          value={customerAddress} onChange={e => setCustomerAddress(e.target.value)} rows={2}
          error={errors.customerAddress} />
        <Textarea label="Notes" placeholder="Add any special instructions or remarks"
          value={notes} onChange={e => setNotes(e.target.value)} rows={3} />
      </SectionCard>

      {/* Items */}
      <SectionCard title="Items" icon="🌿" className="mb-4">
        <div className="mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 rounded-xl border border-green-100 bg-green-50/60 px-4 py-3">
          <div>
            <p className="text-sm font-semibold text-ink">Height option</p>
            <p className="text-xs text-muted">Turn this on only for quotations that need height values on every plant row.</p>
          </div>
          <button
            type="button"
            onClick={toggleHeightEnabled}
            className={cx(
              'inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold transition-colors border',
              heightEnabled
                ? 'bg-green-600 text-white border-green-600'
                : 'bg-white text-muted border-gray-200 hover:border-green-300 hover:text-[#5CB338]'
            )}
          >
            <span className={cx('w-2.5 h-2.5 rounded-full', heightEnabled ? 'bg-white' : 'bg-gray-300')} />
            {heightEnabled ? 'Height enabled' : 'Height disabled'}
          </button>
        </div>

        {errors.items && (
          <div className="mb-3 flex items-center gap-2 text-xs text-red-600 bg-red-50 border border-red-200 rounded-xl px-3 py-2">
            <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" /> {errors.items}
          </div>
        )}

        {/* ── Column headers (desktop only) ── */}
        <div
          className="hidden sm:grid gap-3 mb-2 px-1"
          style={{ gridTemplateColumns: desktopCols }}
        >
          {[
            ['Plant Name', ''],
            ['Bag Size', ''],
            ...(heightEnabled ? [['Height', '']] : []),
            ['Qty', 'text-center'],
            ['Rate', ''],
            ['Total', 'text-right'],
            ['', ''],
          ].map(([h, cls]) => (
            <span key={h} className={cx('text-[11px] font-semibold text-muted uppercase tracking-wide', cls)}>{h}</span>
          ))}
        </div>

        <div className="space-y-4 sm:space-y-3">
          {items.map((item, idx) => {
            const existingPlant    = plants.find(p => p.name.toLowerCase() === (item.plantName || '').toLowerCase())
            const isCustomPlant    = item.plantName && !existingPlant
            const isNewDimension   = existingPlant && item.dimension && !getDimensions(item.plantName).some(d => (d.dimension || d.label)?.toLowerCase() === item.dimension.toLowerCase())
            const isPlantQuickAdding = Boolean(quickAddLoading[`plant-${idx}`])
            const isDimensionQuickAdding = Boolean(quickAddLoading[`dimension-${idx}`])

            const searchLower      = (item.plantName || '').toLowerCase()
            const filteredPlants   = activeSearch === idx ? plants.filter(p => p.name.toLowerCase().includes(searchLower)) : []

            const dimSearchLower   = (item.dimension || '').toLowerCase()
            const knownDimsForPlant = getDimensions(item.plantName).map(d => d.dimension || d.label)
            let filteredDims = []
            if (activeDimSearch === idx) {
              filteredDims = dimSearchLower
                ? allUniqueDimensions.filter(d => d.toLowerCase().includes(dimSearchLower))
                : knownDimsForPlant.length > 0 ? knownDimsForPlant : allUniqueDimensions.slice(0, 10)
            }

            return (
              <div key={idx} className="transition-colors">

                {/* ── Desktop row (fr grid) ── */}
                <div
                  className={cx(
                    'hidden sm:grid gap-3 items-start',
                    errors.itemRows?.[idx] ? 'bg-red-50/40 rounded-xl px-2 py-1' : ''
                  )}
                  style={{ gridTemplateColumns: desktopCols }}
                >
                  {/* Plant */}
                  <div className="flex flex-col relative">
                    <input
                      autoComplete="off"
                      value={item.plantName}
                      onChange={e => { updateItem(idx, 'plantName', e.target.value); setActiveSearch(idx) }}
                      onFocus={() => setActiveSearch(idx)}
                      onBlur={() => setTimeout(() => setActiveSearch(null), 200)}
                      placeholder="Search or type plant"
                      className="field bg-white w-full text-sm"
                    />
                    {activeSearch === idx && filteredPlants.length > 0 && (
                      <div className="absolute top-full left-0 right-0 mt-1 max-h-48 overflow-y-auto bg-white border border-gray-200 rounded-xl shadow-lg z-50 py-1">
                        {filteredPlants.map(p => (
                          <div key={p.id} className="px-3 py-2 text-sm hover:bg-green-50 cursor-pointer text-gray-700"
                            onClick={() => { updateItem(idx, 'plantName', p.name); setActiveSearch(null) }}>
                            {p.name}
                          </div>
                        ))}
                      </div>
                    )}
                    {isCustomPlant && activeSearch !== idx && (
                      <div className="mt-2 flex flex-col gap-1.5 items-start animate-fade-in">
                        <p className="text-[10px] text-amber-600 font-medium bg-amber-50 px-1.5 py-0.5 rounded border border-amber-100 inline-block w-fit">
                          New Plant detected. You can add it to master.
                        </p>
                        <button onClick={() => handleQuickAddPlant(idx)} disabled={isPlantQuickAdding}
                          className="text-[11px] bg-green-50 text-green-700 hover:bg-green-100 px-2.5 py-1.5 rounded-lg border border-green-200 transition-colors flex items-center gap-1 font-medium shadow-sm w-fit disabled:opacity-60 disabled:cursor-not-allowed">
                          {isPlantQuickAdding ? <Spinner size="sm" className="text-green-700" /> : <Plus className="w-3 h-3" />} Add "{item.plantName}" to Master
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Size */}
                  <div className="flex flex-col relative">
                    <input type="text" autoComplete="off"
                      value={item.dimension}
                      onChange={e => { updateItem(idx, 'dimension', e.target.value); setActiveDimSearch(idx) }}
                      onFocus={() => setActiveDimSearch(idx)}
                      onBlur={() => setTimeout(() => setActiveDimSearch(null), 200)}
                      placeholder='5x5, 10 kgs, Pot'
                      className="field bg-white w-full text-sm"
                    />
                    {activeDimSearch === idx && filteredDims.length > 0 && (
                      <div className="absolute top-full left-0 right-0 mt-1 max-h-48 overflow-y-auto bg-white border border-gray-200 rounded-xl shadow-lg z-50 py-1">
                        {filteredDims.map(d => (
                          <div key={d} className="px-3 py-2 text-sm hover:bg-green-50 cursor-pointer text-gray-700"
                            onClick={() => { updateItem(idx, 'dimension', d); setActiveDimSearch(null) }}>
                            {d}
                          </div>
                        ))}
                      </div>
                    )}
                    {isNewDimension && activeDimSearch !== idx && (
                      <div className="mt-2 flex flex-col gap-1.5 items-start animate-fade-in">
                        <p className="text-[10px] text-blue-600 font-medium bg-blue-50 px-1.5 py-0.5 rounded border border-blue-100 inline-block w-fit">
                          New Size detected.
                        </p>
                        <button onClick={() => handleQuickAddDimension(idx)} disabled={isDimensionQuickAdding}
                          className="text-[11px] bg-blue-50 text-blue-700 hover:bg-blue-100 px-2.5 py-1.5 rounded-lg border border-blue-200 transition-colors flex items-center gap-1 font-medium shadow-sm w-fit text-left leading-tight disabled:opacity-60 disabled:cursor-not-allowed">
                          {isDimensionQuickAdding ? <Spinner size="sm" className="text-blue-700" /> : <Plus className="w-3 h-3 flex-shrink-0" />} Add "{item.dimension}" size
                        </button>
                      </div>
                    )}
                  </div>

                  {heightEnabled && (
                    <div className="flex flex-col relative">
                      <input
                        type="text"
                        autoComplete="off"
                        value={item.height || ''}
                        onChange={e => updateItem(idx, 'height', e.target.value)}
                        placeholder="6 inch, 6 ft, 6'"
                        className="field bg-white w-full text-sm"
                      />
                    </div>
                  )}

                  {/* Qty */}
                  <div className="flex flex-col">
                    <input type="number" min="0" placeholder="0"
                      value={item.quantity}
                      onChange={e => updateItem(idx, 'quantity', e.target.value)}
                      className="field w-full text-sm text-center font-mono bg-white"
                    />
                  </div>

                  {/* Rate */}
                  <div className="flex flex-col">
                    <div className="relative">
                      <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-[11px] text-muted font-semibold pointer-events-none">Rs</span>
                      <input type="number" min="0" step="0.01" placeholder="0"
                        value={item.price}
                        onChange={e => updateItem(idx, 'price', e.target.value)}
                        className={cx('field w-full text-sm font-mono pl-7 bg-white',
                          errors.itemRows?.[idx] && 'field-error border-red-400')}
                      />
                    </div>
                    {errors.itemRows?.[idx] && (
                      <p className="text-[10px] text-red-500 mt-1 truncate">{errors.itemRows[idx]}</p>
                    )}
                  </div>

                  {/* Row total */}
                  <div className="flex items-center justify-end h-full">
                    <span className="text-sm font-bold text-[#5CB338] font-mono truncate mt-2">
                      {formatNumber((parseFloat(item.price) || 0) * (parseInt(item.quantity) || 0))}
                    </span>
                  </div>

                  {/* Delete */}
                  <div className="flex items-center justify-center h-full">
                    {items.length > 1 ? (
                      <button onClick={() => removeItem(idx)}
                        className="w-10 h-10 rounded-lg hover:bg-red-100 text-red-500 hover:text-red-700 flex items-center justify-center transition-colors">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    ) : <div className="w-10 h-10" />}
                  </div>
                </div>

                {/* ── Mobile row (original 12-col grid) ── */}
                <div className={cx(
                  'sm:hidden grid grid-cols-12 gap-x-2 gap-y-3 items-start p-3.5 rounded-xl',
                  errors.itemRows?.[idx] ? 'bg-red-50 border border-red-200' : 'bg-gray-50 border border-gray-100'
                )}>
                  {/* Plant */}
                  <div className="col-span-12 flex flex-col relative">
                    <label className="block text-[10px] font-semibold text-muted uppercase mb-1.5">Plant Name</label>
                    <input autoComplete="off" value={item.plantName}
                      onChange={e => { updateItem(idx, 'plantName', e.target.value); setActiveSearch(idx) }}
                      onFocus={() => setActiveSearch(idx)}
                      onBlur={() => setTimeout(() => setActiveSearch(null), 200)}
                      placeholder="Search or type plant"
                      className="field bg-white w-full text-sm"
                    />
                    {activeSearch === idx && filteredPlants.length > 0 && (
                      <div className="absolute top-full left-0 right-0 mt-1 max-h-48 overflow-y-auto bg-white border border-gray-200 rounded-xl shadow-lg z-50 py-1">
                        {filteredPlants.map(p => (
                          <div key={p.id} className="px-3 py-2 text-sm hover:bg-green-50 cursor-pointer text-gray-700"
                            onClick={() => { updateItem(idx, 'plantName', p.name); setActiveSearch(null) }}>
                            {p.name}
                          </div>
                        ))}
                      </div>
                    )}
                    {isCustomPlant && activeSearch !== idx && (
                      <div className="mt-2 flex flex-col gap-1.5 items-start animate-fade-in">
                        <p className="text-[10px] text-amber-600 font-medium bg-amber-50 px-1.5 py-0.5 rounded border border-amber-100 inline-block w-fit">
                          New Plant detected. You can add it to master.
                        </p>
                        <button onClick={() => handleQuickAddPlant(idx)} disabled={isPlantQuickAdding}
                          className="text-[11px] bg-green-50 text-green-700 hover:bg-green-100 px-2.5 py-1.5 rounded-lg border border-green-200 transition-colors flex items-center gap-1 font-medium shadow-sm w-fit disabled:opacity-60 disabled:cursor-not-allowed">
                          {isPlantQuickAdding ? <Spinner size="sm" className="text-green-700" /> : <Plus className="w-3 h-3" />} Add "{item.plantName}" to Master
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Size */}
                  <div className="col-span-7 flex flex-col relative">
                    <label className="block text-[10px] font-semibold text-muted uppercase mb-1.5">Bag Size / Dimension</label>
                    <input type="text" autoComplete="off" value={item.dimension}
                      onChange={e => { updateItem(idx, 'dimension', e.target.value); setActiveDimSearch(idx) }}
                      onFocus={() => setActiveDimSearch(idx)}
                      onBlur={() => setTimeout(() => setActiveDimSearch(null), 200)}
                      placeholder='e.g. 5x5, 10 kgs, 12" Pot'
                      className="field bg-white w-full text-sm"
                    />
                    {activeDimSearch === idx && filteredDims.length > 0 && (
                      <div className="absolute top-full left-0 right-0 mt-1 max-h-48 overflow-y-auto bg-white border border-gray-200 rounded-xl shadow-lg z-50 py-1">
                        {filteredDims.map(d => (
                          <div key={d} className="px-3 py-2 text-sm hover:bg-green-50 cursor-pointer text-gray-700"
                            onClick={() => { updateItem(idx, 'dimension', d); setActiveDimSearch(null) }}>
                            {d}
                          </div>
                        ))}
                      </div>
                    )}
                    {isNewDimension && activeDimSearch !== idx && (
                      <div className="mt-2 flex flex-col gap-1.5 items-start animate-fade-in">
                        <p className="text-[10px] text-blue-600 font-medium bg-blue-50 px-1.5 py-0.5 rounded border border-blue-100 inline-block w-fit">
                          New Size detected.
                        </p>
                        <button onClick={() => handleQuickAddDimension(idx)} disabled={isDimensionQuickAdding}
                          className="text-[11px] bg-blue-50 text-blue-700 hover:bg-blue-100 px-2.5 py-1.5 rounded-lg border border-blue-200 transition-colors flex items-center gap-1 font-medium shadow-sm w-fit text-left leading-tight disabled:opacity-60 disabled:cursor-not-allowed">
                          {isDimensionQuickAdding ? <Spinner size="sm" className="text-blue-700" /> : <Plus className="w-3 h-3 flex-shrink-0" />} Add "{item.dimension}" size
                        </button>
                      </div>
                    )}
                  </div>

                  {heightEnabled && (
                    <div className="col-span-12 flex flex-col">
                      <label className="block text-[10px] font-semibold text-muted uppercase mb-1.5">Height</label>
                      <input
                        type="text"
                        autoComplete="off"
                        value={item.height || ''}
                        onChange={e => updateItem(idx, 'height', e.target.value)}
                        placeholder="6 inch, 6 ft, 6'"
                        className="field bg-white w-full text-sm"
                      />
                    </div>
                  )}

                  {/* Qty */}
                  <div className="col-span-5 flex flex-col">
                    <label className="block text-[10px] font-semibold text-muted uppercase mb-1.5 text-center">Qty</label>
                    <input type="number" min="0" placeholder="0"
                      value={item.quantity}
                      onChange={e => updateItem(idx, 'quantity', e.target.value)}
                      className="field w-full text-sm text-center font-mono bg-white"
                    />
                  </div>

                  {/* Rate */}
                  <div className="col-span-6 flex flex-col">
                    <label className="block text-[10px] font-semibold text-muted uppercase mb-1.5">Rate</label>
                    <div className="relative">
                      <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-[11px] text-muted font-semibold pointer-events-none">Rs</span>
                      <input type="number" min="0" step="0.01" placeholder="0"
                        value={item.price}
                        onChange={e => updateItem(idx, 'price', e.target.value)}
                        className={cx('field w-full text-sm font-mono pl-7 bg-white',
                          errors.itemRows?.[idx] && 'field-error border-red-400')}
                      />
                    </div>
                    {errors.itemRows?.[idx] && <p className="text-[10px] text-red-500 mt-1 truncate">{errors.itemRows[idx]}</p>}
                  </div>

                  {/* Total */}
                  <div className="col-span-4 flex flex-col">
                    <label className="block text-[10px] font-semibold text-muted uppercase mb-1.5">Total</label>
                    <span className="text-sm font-bold text-[#5CB338] font-mono truncate mt-2">
                      {formatNumber((parseFloat(item.price) || 0) * (parseInt(item.quantity) || 0))}
                    </span>
                  </div>

                  {/* Delete */}
                  <div className="col-span-2 flex items-end justify-end pb-1">
                    {items.length > 1 ? (
                      <button onClick={() => removeItem(idx)}
                        className="w-10 h-10 rounded-xl hover:bg-red-100 text-red-500 bg-white border border-red-100 hover:text-red-700 flex items-center justify-center transition-colors shadow-sm">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    ) : <div className="w-10 h-10" />}
                  </div>
                </div>

              </div>
            )
          })}
        </div>

        <button onClick={addItem}
          className="mt-3 w-full flex items-center justify-center gap-2 py-3 rounded-xl border-2 border-dashed border-gray-200 hover:border-green-400 hover:bg-green-50/50 text-sm text-muted hover:text-[#5CB338] font-medium transition-all">
          <Plus className="w-4 h-4" /> Add Item
        </button>
      </SectionCard>

      <SectionCard title="Amount Summary" icon="💰" className="mb-4">
        <div className="grid grid-cols-1 gap-4 mb-4">
          <Input
            label="Loading Charges"
            type="number"
            min="0"
            step="0.01"
            value={shippingCharges}
            onChange={e => setShippingCharges(e.target.value)}
            error={errors.shippingCharges}
            placeholder="0.00"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-[1fr,1.4fr] gap-4 items-start">
          <Select label="Discount Type" value={discountType} onChange={e => setDiscountType(e.target.value)}>
            <option value="AMOUNT">Amount</option>
            <option value="PERCENTAGE">Percentage</option>
          </Select>
          <Input
            label={discountType === 'PERCENTAGE' ? 'Discount Percentage' : 'Discount Amount'}
            type="number"
            min="0"
            step="0.01"
            value={discountValue}
            onChange={e => setDiscountValue(e.target.value)}
            error={errors.discountValue}
            hint={discountType === 'PERCENTAGE'
              ? `Calculated discount amount: ${formatCurrency(discountAmount)}`
              : undefined}
          />
        </div>

        <div className="mt-4 rounded-2xl border border-green-100 bg-green-50/60 px-4 py-4">
          <div className="flex items-center justify-between text-sm text-muted mb-2">
            <span>Discount Amount</span>
            <span className="font-mono font-semibold text-ink">{formatCurrency(discountAmount)}</span>
          </div>
          <div className="flex items-center justify-between text-base font-semibold text-ink">
            <span>Grand Total</span>
            <span className={cx('font-mono text-xl', finalGrandTotal < 0 ? 'text-red-600' : 'text-[#5CB338]')}>
              {formatCurrency(finalGrandTotal)}
            </span>
          </div>
        </div>
      </SectionCard>

      {/* Footer */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-4 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-xl bg-green-100 flex items-center justify-center">
            <Calculator className="w-5 h-5 text-[#5CB338]" strokeWidth={1.5} />
          </div>
          <div>
            <p className="text-[11px] font-semibold text-muted uppercase tracking-widest">Grand Total</p>
            <p className="text-2xl font-bold text-[#5CB338] font-mono leading-none mt-0.5">{formatCurrency(finalGrandTotal)}</p>
          </div>
        </div>
        <div className="flex gap-2.5 w-full sm:w-auto">
          <Button variant="secondary" onClick={() => handleSave(false)} loading={saving} className="flex-1 sm:flex-none">
            <Save className="w-4 h-4" /> Save Draft
          </Button>
          <Button onClick={() => handleSave(true)} loading={saving} className="flex-1 sm:flex-none">
            <CheckCheck className="w-4 h-4" /> Finalize
          </Button>
        </div>
      </div>
    </div>
  )
}
