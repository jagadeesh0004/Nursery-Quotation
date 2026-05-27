/**
 * PlantsPage — Plants Master
 *
 * IMPROVED PLANT CREATION FLOW:
 * User fills plant name + all initial dimensions in ONE modal,
 * then we call:
 *   1. POST /plants            → creates the plant
 *   2. POST /plants/:id/dimensions (x N) → adds each dimension
 * Both steps happen sequentially behind a single "Create Plant" button.
 *
 * Other APIs:
 *   GET    /plants                         → list
 *   POST   /plants/:id/dimensions          → add size to existing plant
 *   DELETE /plants/:id                     → soft-delete plant
 *   DELETE /plants/:id/dimensions/:dimId   → soft-delete dimension
 */
import { useState, useEffect, useCallback } from 'react'
import { Plus, Trash2, ChevronDown, ChevronUp, Leaf, Package2, AlertTriangle } from 'lucide-react'
import toast from 'react-hot-toast'
import { plantService } from '../services'
import { Button, Input, Modal, EmptyState, Skeleton, ConfirmDialog, PageHeader, SearchBar, Card, InfoBox } from '../components/ui'
import { formatCurrency, getError, cx } from '../utils'

// ── Blank dimension row template ─────────────────────────────
const newDimRow = () => ({ dimension: '', price: '' })
const DIMENSION_FORMAT_HINT = 'Use NxN, Pot, or N" Pot (e.g. 8x10, Pot, 12" Pot)'
const DIMENSION_PATTERN = /^(?:\d+\s*[xX]\s*\d+|pot|\d+"\s*pot)$/i

export default function PlantsPage() {
  const [plants,   setPlants]   = useState([])
  const [loading,  setLoading]  = useState(true)
  const [search,   setSearch]   = useState('')
  const [expanded, setExpanded] = useState({})

  // ── Create-plant modal (name + dimensions together) ──────
  const [createOpen,  setCreateOpen]  = useState(false)
  const [newName,     setNewName]     = useState('')
  const [dimRows,     setDimRows]     = useState([newDimRow()])
  const [creating,    setCreating]    = useState(false)
  const [createErrs,  setCreateErrs]  = useState({})   // { name, dims: [{dim,price}] }

  // ── Add-size-to-existing modal ───────────────────────────
  const [addSizeFor,  setAddSizeFor]  = useState(null)
  const [addDim,      setAddDim]      = useState('')
  const [addPrice,    setAddPrice]    = useState('')
  const [addingSz,    setAddingSz]    = useState(false)
  const [addSzErrs,   setAddSzErrs]   = useState({})

  // ── Delete confirm ───────────────────────────────────────
  const [deleteTarget, setDeleteTarget] = useState(null)

  // ── Load ─────────────────────────────────────────────────
  const fetchPlants = useCallback(async () => {
    setLoading(true)
    try {
      const res = await plantService.list()
      setPlants(res.data.data || [])
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { fetchPlants() }, [fetchPlants])

  const filtered = plants.filter(p => p.name?.toLowerCase().includes(search.toLowerCase()))

  function toggle(id) { setExpanded(p => ({ ...p, [id]: !p[id] })) }

  // ── Dimension row helpers ────────────────────────────────
  function updateDimRow(i, field, val) {
    setDimRows(prev => prev.map((r, idx) => idx === i ? { ...r, [field]: val } : r))
  }
  function addDimRow()    { setDimRows(p => [...p, newDimRow()]) }
  function removeDimRow(i){ setDimRows(p => p.filter((_, idx) => idx !== i)) }
  function isValidDimensionFormat(value) { return DIMENSION_PATTERN.test(value.trim()) }

  // ── Validate create form ─────────────────────────────────
  function validateCreate() {
    const errs = { dims: dimRows.map(() => ({})) }
    let ok = true
    if (!newName.trim()) { errs.name = 'Plant name is required'; ok = false }

    // At least one dimension must be complete if any row is partially filled
    const filledRows = dimRows.filter(r => r.dimension.trim() || r.price)

    // Validate mapping properly using original indices
    dimRows.forEach((r, i) => {
      const hasDim = r.dimension.trim().length > 0
      const hasPrice = r.price !== ''

      if (hasDim || hasPrice) {
        if (!hasDim) {
          errs.dims[i].dimension = 'Required'; ok = false
        } else if (!isValidDimensionFormat(r.dimension)) {
          errs.dims[i].dimension = DIMENSION_FORMAT_HINT; ok = false
        }

        if (!hasPrice || isNaN(parseFloat(r.price)) || parseFloat(r.price) < 0) {
          errs.dims[i].price = 'Enter a valid price (0 or more)'; ok = false
        }
      }
    })
    setCreateErrs(errs)
    return ok
  }

  // ── Create plant → then add dimensions sequentially ──────
  async function handleCreatePlant() {
    if (!validateCreate()) return
    setCreating(true)
    try {
      // Step 1: create plant
      const plantRes = await plantService.create(newName.trim())
      const plantId  = plantRes.data.data?.id

      // Step 2: add each non-empty dimension (Promise.all for speed)
      const filledRows = dimRows.filter(r => r.dimension.trim() && r.price !== '')
      if (filledRows.length > 0 && plantId) {
        await Promise.all(
          filledRows.map(r => plantService.addDimension(plantId, r.dimension.trim(), parseFloat(r.price) || 0))
        )
      }

      toast.success(`"${newName.trim()}" created with ${filledRows.length} size(s)`)
      resetCreateModal()
      fetchPlants()
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
    finally { setCreating(false) }
  }

  function resetCreateModal() {
    setCreateOpen(false)
    setNewName('')
    setDimRows([newDimRow()])
    setCreateErrs({})
  }

  // ── Add size to existing plant ───────────────────────────
  function validateAddSize() {
    const errs = {}
    if (!addDim.trim()) {
      errs.dimension = 'Dimension label is required'
    } else if (!isValidDimensionFormat(addDim)) {
      errs.dimension = DIMENSION_FORMAT_HINT
    }

    if (!addPrice || isNaN(parseFloat(addPrice)) || parseFloat(addPrice) < 0)
      errs.price = 'Enter a valid price (0 or more)'
    setAddSzErrs(errs)
    return Object.keys(errs).length === 0
  }

  async function handleAddSize() {
    if (!validateAddSize()) return
    setAddingSz(true)
    try {
      await plantService.addDimension(addSizeFor, addDim.trim(), parseFloat(addPrice) || 0)
      toast.success('Size added')
      setAddSizeFor(null); setAddDim(''); setAddPrice(''); setAddSzErrs({})
      fetchPlants()
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
    finally { setAddingSz(false) }
  }

  // ── Delete ───────────────────────────────────────────────
  async function handleDelete() {
    try {
      if (deleteTarget.type === 'plant') {
        await plantService.deletePlant(deleteTarget.plantId)
        toast.success('Plant removed')
      } else {
        await plantService.deleteDimension(deleteTarget.plantId, deleteTarget.dimId)
        toast.success('Size removed')
      }
      setDeleteTarget(null)
      fetchPlants()
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
  }

  return (
    <div className="animate-fade-up">
      <PageHeader title="Plants Master" subtitle="Manage your plant catalog, bag sizes, and default prices."
        action={
          <Button onClick={() => { resetCreateModal(); setCreateOpen(true) }}>
            <Plus className="w-4 h-4 sm:mr-1" /> <span className="hidden sm:inline">Add Plant</span>
          </Button>
        }
      />

      <SearchBar value={search} onChange={e => setSearch(e.target.value)}
        placeholder="Search plants…" className="w-full sm:max-w-sm mb-5" />

      <Card className="overflow-hidden">
        {loading ? (
          <div className="divide-y divide-gray-50">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="flex items-center gap-4 px-5 py-4">
                <Skeleton className="w-9 h-9 rounded-xl" />
                <Skeleton className="h-4 w-48" />
                <Skeleton className="h-5 w-16 rounded-full ml-auto" />
              </div>
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState icon={Leaf} title="No plants found"
            description="Add your first plant with its sizes to start building your catalog."
            action={<Button onClick={() => { resetCreateModal(); setCreateOpen(true) }}><Plus className="w-4 h-4" />Add Plant</Button>}
          />
        ) : (
          <div className="divide-y divide-gray-100 stagger">
            {filtered.map(plant => (
              <div key={plant.id} className="animate-fade-up">
                {/* Accordion header */}
                <button onClick={() => toggle(plant.id)}
                  className="w-full flex items-center gap-3 px-5 py-4 hover:bg-green-50/60 transition-colors text-left">
                  <div className="w-9 h-9 rounded-xl bg-green-100 flex items-center justify-center flex-shrink-0">
                    <Leaf className="w-4 h-4 text-green-700" strokeWidth={1.5} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <span className="font-semibold text-sm text-ink">{plant.name}</span>
                    <span className={cx('ml-2.5 text-[11px] font-semibold px-2 py-0.5 rounded-full',
                      plant.dimensions?.length ? 'bg-green-100 text-[#5CB338]' : 'bg-gray-100 text-gray-400')}>
                      {plant.dimensions?.length || 0} {plant.dimensions?.length === 1 ? 'size' : 'sizes'}
                    </span>
                  </div>
                  {expanded[plant.id] ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
                </button>

                {/* Expanded content */}
                {expanded[plant.id] && (
                  <div className="px-5 pb-5 pt-2 animate-fade-in bg-green-50/30">
                    {plant.dimensions?.length > 0 ? (
                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5 mb-4">
                        {plant.dimensions.map(dim => (
                          <div key={dim.id}
                            className="group flex items-center justify-between px-4 py-3 bg-white rounded-xl border border-gray-100 hover:border-green-200 shadow-sm transition-all">
                            <div className="flex items-center gap-2.5">
                              <div className="w-7 h-7 rounded-lg bg-green-50 flex items-center justify-center">
                                <Package2 className="w-3.5 h-3.5 text-green-600" strokeWidth={1.5} />
                              </div>
                              <div>
                                <p className="text-sm font-semibold text-ink">{dim.label || dim.dimension}</p>
                                <p className="text-xs font-bold text-green-700 font-mono mt-0.5">{formatCurrency(dim.price)}</p>
                              </div>
                            </div>
                            <button onClick={() => setDeleteTarget({ type: 'dim', plantId: plant.id, dimId: dim.id })}
                              className="opacity-0 group-hover:opacity-100 p-2 rounded-lg hover:bg-red-100 text-red-500 hover:text-red-700 transition-all">
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-xs text-muted italic mb-4">No sizes yet — add one below.</p>
                    )}

                    <div className="flex flex-wrap items-center gap-2 mt-1 pt-3 border-t border-green-100/50">
                      <Button variant="secondary" size="sm"
                        onClick={() => { setAddSizeFor(plant.id); setAddDim(''); setAddPrice(''); setAddSzErrs({}) }}>
                        <Plus className="w-3.5 h-3.5" /> Add Size
                      </Button>
                      <Button variant="ghost" size="sm"
                        className="text-red-500 hover:bg-red-100 hover:text-red-700 font-medium"
                        onClick={() => setDeleteTarget({ type: 'plant', plantId: plant.id })}>
                        <Trash2 className="w-4 h-4 mr-1.5" /> Delete Plant
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* ── Create Plant Modal (name + dimensions together) ── */}
      <Modal open={createOpen} onClose={resetCreateModal} title="Add New Plant" width="max-w-lg"
        footer={
          <div className="flex justify-end gap-2.5">
            <Button variant="secondary" onClick={resetCreateModal}>Cancel</Button>
            <Button onClick={handleCreatePlant} loading={creating} disabled={!newName.trim()}>
              Create Plant
            </Button>
          </div>
        }
      >
        <div className="space-y-5">
          {/* Plant name */}
          <Input label="Plant Name" placeholder="e.g. Areca Palm" required
            value={newName} onChange={e => setNewName(e.target.value)}
            error={createErrs.name}
            onKeyDown={e => e.key === 'Enter' && !e.shiftKey && handleCreatePlant()}
            autoFocus />

          {/* Dimensions */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="block text-xs font-semibold text-muted tracking-wide">
                Sizes &amp; Prices
                <span className="ml-1.5 text-[10px] font-normal text-gray-400">(optional — can add later)</span>
              </label>
              <button onClick={addDimRow}
                className="text-xs text-green-700 hover:text-[#4d962e] font-semibold flex items-center gap-1 hover:underline">
                <Plus className="w-3 h-3" /> Add row
              </button>
            </div>

            <div className="space-y-2">
              {dimRows.map((row, i) => (
                <div key={i} className="flex items-start gap-2 sm:gap-3">
                  {/* Dimension label */}
                  <div className="flex-1">
                    <input
                      type="text"
                      placeholder="e.g. Small (5×7)"
                      value={row.dimension}
                      onChange={e => updateDimRow(i, 'dimension', e.target.value)}
                      className={cx('field text-sm', createErrs.dims?.[i]?.dimension && 'field-error')}
                    />
                    {createErrs.dims?.[i]?.dimension && (
                      <p className="text-xs text-red-500 mt-1 flex items-center gap-1">
                        <AlertTriangle className="w-3 h-3" />{createErrs.dims[i].dimension}
                      </p>
                    )}
                  </div>
                  {/* Price — required number field with validation */}
                  <div className="w-[100px] sm:w-32 shrink-0">
                    <div className="relative">
                      <span className="absolute left-2.5 sm:left-3 top-1/2 -translate-y-1/2 text-[11px] sm:text-xs text-muted font-semibold">Rs</span>
                      <input
                        type="number"
                        placeholder="0"
                        min="0"
                        step="0.01"
                        value={row.price}
                        onChange={e => updateDimRow(i, 'price', e.target.value)}
                        className={cx('field text-sm pl-7 sm:pl-9 font-mono', createErrs.dims?.[i]?.price && 'field-error')}
                      />
                    </div>
                    {createErrs.dims?.[i]?.price && (
                      <p className="text-xs text-red-500 mt-1">{createErrs.dims[i].price}</p>
                    )}
                  </div>
                  {/* Remove row */}
                  {dimRows.length > 1 && (
                      <button onClick={() => removeDimRow(i)}
                        className="mt-1 p-2 rounded-lg hover:bg-red-100 text-red-500 hover:text-red-700 transition-colors flex-shrink-0">
                        <Trash2 className="w-4 h-4" />
                      </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          <InfoBox variant="green">
            💡 Prices set here auto-fill when you create quotations. You can always edit them later.
          </InfoBox>
        </div>
      </Modal>

      {/* ── Add Size to Existing Plant Modal ──────────────── */}
      <Modal open={!!addSizeFor} onClose={() => setAddSizeFor(null)} title="Add Size"
        footer={
          <div className="flex justify-end gap-2.5">
            <Button variant="secondary" onClick={() => setAddSizeFor(null)}>Cancel</Button>
            <Button onClick={handleAddSize} loading={addingSz} disabled={!addDim.trim() || !addPrice}>
              Add Size
            </Button>
          </div>
        }
      >
        <div className="space-y-4">
          <Input label="Bag Size / Dimension" placeholder="e.g. Medium (8×10)" required
            value={addDim} onChange={e => setAddDim(e.target.value)}
            error={addSzErrs.dimension} autoFocus />
          <div>
            <label className="block text-xs font-semibold text-muted tracking-wide mb-1.5">
              Default Price (Rs) <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-xs text-muted font-semibold">Rs</span>
              <input type="number" placeholder="0" min="0" step="0.01"
                value={addPrice} onChange={e => setAddPrice(e.target.value)}
                className={cx('field pl-9 font-mono', addSzErrs.price && 'field-error')} />
            </div>
            {addSzErrs.price && <p className="text-xs text-red-500 mt-1 flex items-center gap-1"><AlertTriangle className="w-3 h-3" />{addSzErrs.price}</p>}
            <p className="text-xs text-muted mt-1.5">This auto-fills when creating quotations. Enter 0 if free.</p>
          </div>
        </div>
      </Modal>

      {/* ── Delete Confirm ───────────────────────────────── */}
      <ConfirmDialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)} onConfirm={handleDelete}
        title={deleteTarget?.type === 'plant' ? 'Delete Plant' : 'Remove Size'}
        description={deleteTarget?.type === 'plant'
          ? 'This will delete the plant and all its sizes permanently.'
          : 'This size will be removed permanently.'}
        confirmLabel="Delete" danger />
    </div>
  )
}
