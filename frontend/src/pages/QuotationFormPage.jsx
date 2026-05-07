/**
 * QuotationFormPage — create + edit quotations
 * GET  /plants           → plant dropdown data
 * GET  /quotations/:id   → load existing (edit mode)
 * POST /quotations       → create
 * PUT  /quotations/:id   → update
 */
import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Plus, Trash2, Calculator, Save, CheckCheck, AlertTriangle } from 'lucide-react'
import toast from 'react-hot-toast'
import { quotationService, plantService } from '../services'
import { Button, Input, Textarea, Spinner, SectionCard } from '../components/ui'
import { formatCurrency, getError, cx } from '../utils'

const newItem = () => ({ plantName: '', dimension: '', quantity: 1, price: '' })

export default function QuotationFormPage() {
  const { id }   = useParams()
  const isEdit   = Boolean(id)
  const navigate = useNavigate()

  const [plants,          setPlants]          = useState([])
  const [customerName,    setCustomerName]    = useState('')
  const [customerPhone,   setCustomerPhone]   = useState('')
  const [customerAddress, setCustomerAddress] = useState('')
  const [items,           setItems]           = useState([newItem()])
  const [pageLoading,     setPageLoading]     = useState(isEdit)
  const [saving,          setSaving]          = useState(false)
  const [errors,          setErrors]          = useState({})
  const [activeSearch,    setActiveSearch]    = useState(null)
  const [activeDimSearch, setActiveDimSearch] = useState(null)

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
      setItems(q.items?.length ? q.items.map(i => ({
        plantName: i.plantName || '', dimension: i.dimension || '',
        quantity: i.quantity || 1, price: i.price ?? '',
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

  const allUniqueDimensions = Array.from(new Set(plants.flatMap(p => p.dimensions?.map(d => d.dimension || d.label) || []))).filter(Boolean).sort()

  function validate(shouldFinalize) {
    const errs = {}

    if (!customerName.trim()) {
      errs.customerName = 'Customer name is required'
    }

    if (shouldFinalize && !customerPhone.trim()) {
      errs.customerPhone = 'Phone number is required to finalize'
    }

    if (shouldFinalize && !customerAddress.trim()) {
      errs.customerAddress = 'Address is required to finalize'
    }

    const validItems = items.filter(it => it.plantName)
    if (validItems.length === 0) errs.items = 'Add at least one plant item to save the quotation'

    // Validate price fields — must not be empty on any filled row
    items.forEach((it, i) => {
      if (it.plantName && (it.price === '' || it.price === null || it.price === undefined)) {
        if (!errs.itemRows) errs.itemRows = {}
        errs.itemRows[i] = 'Price is required'
      }
    })

    setErrors(errs)
    return errs
  }

  async function handleSave(shouldFinalize = false) {
    const currentErrs = validate(shouldFinalize)
    if (Object.keys(currentErrs).length > 0) {
      if (currentErrs.customerName) {
        toast.error('Please add the customer details (Name) to save')
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
        items: items.filter(it => it.plantName).map(it => ({
          plantName: it.plantName,
          dimension: it.dimension,
          quantity:  parseInt(it.quantity) || 1,
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
    }
  }

  async function handleQuickAddDimension(idx) {
    const it = items[idx]
    if (!it.plantName || !it.dimension || !it.price) {
      toast.error('Please enter Size and Price to add to master')
      return
    }
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
    }
  }

  if (pageLoading) return (
    <div className="flex items-center justify-center h-64 gap-3 text-muted">
      <Spinner size="md" /> Loading quotation…
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
      </SectionCard>

      {/* Items */}
      <SectionCard title="Items" icon="🌿" className="mb-4">
        {errors.items && (
          <div className="mb-3 flex items-center gap-2 text-xs text-red-600 bg-red-50 border border-red-200 rounded-xl px-3 py-2">
            <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" /> {errors.items}
          </div>
        )}

        {/* Column headers */}
        <div className="hidden sm:grid grid-cols-12 gap-3 mb-2 px-1">
          {[['Plant Name','col-span-4'],['Bag Size','col-span-3'],['Qty','col-span-1'],['Price / Pc','col-span-2'],['Total','col-span-1 text-right'],['','col-span-1']].map(([h, cls]) => (
            <span key={h} className={cx('text-[11px] font-semibold text-muted uppercase tracking-wide', cls)}>{h}</span>
          ))}
        </div>

        <div className="space-y-4 sm:space-y-3">
          {items.map((item, idx) => {
            const existingPlant = plants.find(p => p.name.toLowerCase() === (item.plantName || '').toLowerCase());
            const isCustomPlant = item.plantName && !existingPlant;
            const isNewDimension = existingPlant && item.dimension && !getDimensions(item.plantName).some(d => (d.dimension || d.label)?.toLowerCase() === item.dimension.toLowerCase());

            // Filter plants for custom dropdown
            const searchLower = (item.plantName || '').toLowerCase()
            const filteredPlants = activeSearch === idx
              ? plants.filter(p => p.name.toLowerCase().includes(searchLower))
              : []

            // Filter dimensions for custom dropdown
            const dimSearchLower = (item.dimension || '').toLowerCase()
            const knownDimsForPlant = getDimensions(item.plantName).map(d => d.dimension || d.label)

            let filteredDims = []
            if (activeDimSearch === idx) {
              if (dimSearchLower) {
                filteredDims = allUniqueDimensions.filter(d => d.toLowerCase().includes(dimSearchLower))
              } else {
                filteredDims = knownDimsForPlant.length > 0 ? knownDimsForPlant : allUniqueDimensions.slice(0, 10)
              }
            }

            return (
              <div key={idx} className="flex flex-col gap-2 transition-colors">
                <div className={cx(
                  'grid grid-cols-12 gap-x-2 gap-y-3 sm:gap-3 items-start p-3.5 sm:p-0 rounded-xl sm:rounded-none',
                  errors.itemRows?.[idx] ? 'bg-red-50 border border-red-200 sm:border-transparent sm:bg-transparent' : 'bg-gray-50 sm:bg-transparent border border-gray-100 sm:border-transparent'
                )}>
                  {/* Plant */}
                  <div className="col-span-12 sm:col-span-4 flex flex-col relative">
                    <label className="sm:hidden block text-[10px] font-semibold text-muted uppercase mb-1.5 whitespace-nowrap">Plant Name</label>
                    <input
                      autoComplete="off"
                      value={item.plantName}
                      onChange={e => {
                        updateItem(idx, 'plantName', e.target.value)
                        setActiveSearch(idx)
                      }}
                      onFocus={() => setActiveSearch(idx)}
                      onBlur={() => setTimeout(() => setActiveSearch(null), 200)}
                      placeholder="Search or type plant"
                      className="field bg-white w-full text-sm"
                    />

                    {/* Custom Dropdown */}
                    {activeSearch === idx && filteredPlants.length > 0 && (
                      <div className="absolute top-full left-0 right-0 mt-1 max-h-48 overflow-y-auto bg-white border border-gray-200 rounded-xl shadow-lg z-50 py-1">
                        {filteredPlants.map(p => (
                          <div
                            key={p.id}
                            className="px-3 py-2 text-sm hover:bg-green-50 cursor-pointer text-gray-700"
                            onClick={() => {
                              updateItem(idx, 'plantName', p.name)
                              setActiveSearch(null)
                            }}
                          >
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
                        <button
                          onClick={() => handleQuickAddPlant(idx)}
                          className="text-[11px] bg-green-50 text-green-700 hover:bg-green-100 px-2.5 py-1.5 rounded-lg border border-green-200 transition-colors flex items-center gap-1 font-medium shadow-sm w-fit"
                        >
                          <Plus className="w-3 h-3" /> Add "{item.plantName}" to Master
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Dimension */}
                  <div className="col-span-6 sm:col-span-3 flex flex-col relative">
                    <label className="sm:hidden block text-[10px] font-semibold text-muted uppercase mb-1.5 whitespace-nowrap">Bag Size</label>
                    <input type="text"
                      autoComplete="off"
                      value={item.dimension}
                      onChange={e => {
                        updateItem(idx, 'dimension', e.target.value)
                        setActiveDimSearch(idx)
                      }}
                      onFocus={() => setActiveDimSearch(idx)}
                      onBlur={() => setTimeout(() => setActiveDimSearch(null), 200)}
                      placeholder="Size (e.g. 5x5)"
                      className="field bg-white w-full text-sm" />

                    {/* Custom Dropdown for Dimensions */}
                    {activeDimSearch === idx && filteredDims.length > 0 && (
                      <div className="absolute top-full left-0 right-0 mt-1 max-h-48 overflow-y-auto bg-white border border-gray-200 rounded-xl shadow-lg z-50 py-1">
                        {filteredDims.map(d => (
                          <div
                            key={d}
                            className="px-3 py-2 text-sm hover:bg-green-50 cursor-pointer text-gray-700"
                            onClick={() => {
                              updateItem(idx, 'dimension', d)
                              setActiveDimSearch(null)
                            }}
                          >
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
                        <button
                          onClick={() => handleQuickAddDimension(idx)}
                          className="text-[11px] bg-blue-50 text-blue-700 hover:bg-blue-100 px-2.5 py-1.5 rounded-lg border border-blue-200 transition-colors flex items-center gap-1 font-medium shadow-sm w-fit text-left leading-tight"
                        >
                          <Plus className="w-3 h-3 flex-shrink-0" /> Add "{item.dimension}" size
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Qty */}
                  <div className="col-span-4 sm:col-span-1 flex flex-col">
                    <label className="sm:hidden block text-[10px] font-semibold text-muted uppercase text-center mb-1.5 whitespace-nowrap">Qty</label>
                    <input type="number" min="1" placeholder="1"
                      value={item.quantity} onChange={e => updateItem(idx, 'quantity', e.target.value)}
                      className="field w-full text-sm text-center font-mono bg-white" />
                  </div>

                  {/* Price — required number with visible validation */}
                  <div className="col-span-6 sm:col-span-2 flex flex-col">
                    <label className="sm:hidden block text-[10px] font-semibold text-muted uppercase mb-1.5 whitespace-nowrap">Price/Pc</label>
                    <div className="relative">
                      <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-[11px] text-muted font-semibold pointer-events-none">Rs</span>
                      <input type="number" min="0" step="0.01" placeholder="0"
                        value={item.price}
                        onChange={e => updateItem(idx, 'price', e.target.value)}
                        className={cx('field w-full text-sm font-mono pl-7 bg-white',
                          errors.itemRows?.[idx] && 'field-error border-red-400')} />
                    </div>
                    {errors.itemRows?.[idx] && <p className="text-[10px] text-red-500 mt-1 max-w-full truncate">{errors.itemRows[idx]}</p>}
                  </div>

                  {/* Row total */}
                  <div className="col-span-5 sm:col-span-1 flex flex-col sm:items-end justify-start h-full">
                    <label className="sm:hidden block text-[10px] font-semibold text-muted uppercase mb-1.5 whitespace-nowrap">Total</label>
                    <span className="text-sm font-bold text-[#5CB338] font-mono truncate mt-2">
                      {formatCurrency((parseFloat(item.price) || 0) * (parseInt(item.quantity) || 0))}
                    </span>
                  </div>

                  {/* Delete row */}
                  <div className="col-span-3 sm:col-span-1 flex flex-col items-end sm:items-center justify-start h-full">
                    <label className="sm:hidden block text-[10px] text-transparent uppercase mb-1.5 whitespace-nowrap select-none">Del</label>
                    {items.length > 1 ? (
                      <button onClick={() => removeItem(idx)}
                        className="w-10 h-10 sm:w-10 sm:h-10 rounded-xl sm:rounded-lg hover:bg-red-100 text-red-500 bg-white sm:bg-transparent border border-red-100 sm:border-transparent hover:text-red-700 flex items-center justify-center transition-colors shadow-sm sm:shadow-none">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    ) : (
                      <div className="w-10 h-10 sm:w-10 sm:h-10"></div>
                    )}
                  </div>
                </div>
               </div>
            )})}
        </div>

        <button onClick={addItem}
          className="mt-3 w-full flex items-center justify-center gap-2 py-3 rounded-xl border-2 border-dashed border-gray-200 hover:border-green-400 hover:bg-green-50/50 text-sm text-muted hover:text-[#5CB338] font-medium transition-all">
          <Plus className="w-4 h-4" /> Add Item
        </button>
      </SectionCard>

      {/* Footer */}
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-4 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-xl bg-green-100 flex items-center justify-center">
            <Calculator className="w-5 h-5 text-[#5CB338]" strokeWidth={1.5} />
          </div>
          <div>
            <p className="text-[11px] font-semibold text-muted uppercase tracking-widest">Grand Total</p>
            <p className="text-2xl font-bold text-[#5CB338] font-mono leading-none mt-0.5">{formatCurrency(grandTotal)}</p>
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
