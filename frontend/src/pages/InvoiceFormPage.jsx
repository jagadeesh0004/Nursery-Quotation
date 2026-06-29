import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Plus, Trash2, Calculator, Save, CheckCheck, AlertTriangle } from 'lucide-react'
import toast from 'react-hot-toast'
import { invoiceService, plantService } from '../services'
import { Button, Input, Textarea, Spinner, SectionCard } from '../components/ui'
import { formatCurrency, getError, cx } from '../utils'

const newItem = () => ({ plantName: '', dimension: '', quantity: 1, price: '' })

export default function InvoiceFormPage() {
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
    invoiceService.getOne(id).then(r => {
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

  function validate(shouldFinalize) {
    const errs = {}

    if (shouldFinalize && !customerName.trim()) {
      errs.customerName = 'Customer name is required'
    }

    if (shouldFinalize && !customerPhone.trim()) {
      errs.customerPhone = 'Phone number is required to finalize'
    }

    if (shouldFinalize && !customerAddress.trim()) {
      errs.customerAddress = 'Address is required to finalize'
    }

    const validItems = items.filter(it => it.plantName)
    if (validItems.length === 0) errs.items = 'Add at least one plant item to save the invoice'

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
        items: items.filter(it => it.plantName).map(it => ({
          plantName: it.plantName,
          dimension: it.dimension,
          quantity:  parseInt(it.quantity) || 1,
          price:     parseFloat(it.price)  || 0,
        })),
        shouldFinalize,
      }
      let res
      if (isEdit) res = await invoiceService.update(id, payload)
      else        res = await invoiceService.create(payload)
      toast.success(shouldFinalize ? 'Invoice finalized!' : 'Draft saved!')
      navigate(`/invoices/${res.data.data?.id || id}`)
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
    finally { setSaving(false) }
  }

  if (pageLoading) return (
    <div className="flex items-center justify-center h-64 gap-3 text-muted">
      <Spinner size="md" /> Loading invoice...
    </div>
  )

  return (
    <div className="animate-fade-up max-w-3xl">
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate(-1)}
          className="w-9 h-9 rounded-xl bg-white border border-gray-200 flex items-center justify-center hover:bg-green-50 transition-colors shadow-sm">
          <ArrowLeft className="w-4 h-4 text-gray-600" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-ink">{isEdit ? 'Edit Invoice' : 'New Invoice'}</h1>
          <p className="text-sm text-muted mt-0.5">{isEdit ? 'Update draft details below.' : 'Draft a new invoice for a customer.'}</p>
        </div>
      </div>

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

      <SectionCard title="Items" icon="🌿" className="mb-4">
        {errors.items && (
          <div className="mb-3 flex items-center gap-2 text-xs text-red-600 bg-red-50 border border-red-200 rounded-xl px-3 py-2">
            <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" /> {errors.items}
          </div>
        )}

        <div className="hidden sm:grid grid-cols-12 gap-3 mb-2 px-1">
          {[
            ['Plant Name','col-span-4'],
            ['Bag / Pot Size','col-span-3'],
            ['Qty','col-span-1'],
            ['Price / Pc','col-span-2'],
            ['Total','col-span-1 text-right'],
            ['','col-span-1']
          ].map(([h, cls]) => (
            <span key={h} className={cx('text-[11px] font-semibold text-muted uppercase tracking-wide', cls)}>{h}</span>
          ))}
        </div>

        <div className="space-y-4 sm:space-y-3">
          {items.map((item, idx) => {
            const existingPlant = plants.find(p => p.name.toLowerCase() === (item.plantName || '').toLowerCase());
            const isCustomPlant = item.plantName && !existingPlant;
            const isNewDimension = existingPlant && item.dimension && !getDimensions(item.plantName).some(d => (d.dimension || d.label)?.toLowerCase() === item.dimension.toLowerCase());

            const searchLower = (item.plantName || '').toLowerCase()

            return (
              <div key={idx} className="grid grid-cols-12 gap-3 items-center">
                <div className="col-span-4">
                  <Input label="Plant Name" placeholder="e.g. Money Plant" value={item.plantName}
                    onChange={e => updateItem(idx, 'plantName', e.target.value)} />
                </div>
                <div className="col-span-3">
                  <Input label="Size" placeholder="e.g. 12 inch pot" value={item.dimension}
                    onChange={e => updateItem(idx, 'dimension', e.target.value)} />
                </div>
                <div className="col-span-1">
                  <Input label="Qty" type="number" value={item.quantity}
                    onChange={e => updateItem(idx, 'quantity', e.target.value)} />
                </div>
                <div className="col-span-2">
                  <Input label="Price" type="number" value={item.price}
                    onChange={e => updateItem(idx, 'price', e.target.value)} />
                </div>
                <div className="col-span-1 text-right font-mono">{formatCurrency((parseFloat(item.price)||0) * (parseInt(item.quantity)||0))}</div>
                <div className="col-span-1">
                  <button onClick={() => removeItem(idx)} className="text-red-500 hover:text-red-700">Remove</button>
                </div>
              </div>
            )
          })}
        </div>

        <div className="flex items-center justify-between mt-4">
          <div className="flex items-center gap-2">
            <Button onClick={addItem} variant="ghost"><Plus className="w-4 h-4" /> Add item</Button>
          </div>
          <div className="text-right">
            <div className="text-sm text-muted">Grand Total</div>
            <div className="text-2xl font-bold text-[#5CB338]">{formatCurrency(grandTotal)}</div>
          </div>
        </div>
      </SectionCard>

      <div className="flex items-center gap-2.5">
        <Button variant="secondary" onClick={() => handleSave(false)} loading={saving}>Save Draft</Button>
        <Button onClick={() => handleSave(true)} loading={saving} className="ml-auto">Finalize Invoice</Button>
      </div>
    </div>
  )
}
