/**
 * QuotationDetailPage
 * GET    /quotations/:id           → load
 * GET    /quotations/:id/pdf       → blob download
 * POST   /quotations/:id/duplicate
 * DELETE /quotations/:id
 */
import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Download, Share2, Edit2, Copy, Trash2, MapPin, Phone, User, CheckCircle, Clock, FilePlus } from 'lucide-react'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'
import { quotationService, invoiceService } from '../services'
import { Button, Badge, Skeleton, ConfirmDialog, Card } from '../components/ui'
import { formatCurrency, formatNumber, formatDate, downloadBlob, getError, formatQuotationId } from '../utils'
import { getUnitWeightFromDimension, formatWeight } from '../constants/weightMapping'

export default function QuotationDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { profile } = useAuth()

  const [quotation,   setQuotation]   = useState(null)
  const [loading,     setLoading]     = useState(true)
  const [pdfLoading,  setPdfLoading]  = useState(false)
  const [dupeLoading, setDupeLoading] = useState(false)
  const [createLoading, setCreateLoading] = useState(false)
  const [showDelete,  setShowDelete]  = useState(false)

  useEffect(() => {
    quotationService.getOne(id)
      .then(r  => setQuotation(r.data.data))
      .catch((err) => {
        const message = getError(err)
        if (message) toast.error(message)
      })
      .finally(() => setLoading(false))
  }, [id])

  async function handleDownloadPdf() {
    setPdfLoading(true)
    try {
      const res = await quotationService.downloadPdf(id)
      downloadBlob(res.data, `Quotation-${q.quotationNumber || id}.pdf`)
      toast.success('PDF downloaded')
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
    finally { setPdfLoading(false) }
  }

  function handleShare() {
    if (!quotation?.publicToken) { toast.error('No share link available'); return }
    const url = `${window.location.origin}/public/quotations/${quotation.publicToken}`
    navigator.clipboard.writeText(url).then(() => toast.success('Share link copied!')).catch(() => toast.error('Could not copy link'))
  }

  async function handleDuplicate() {
    setDupeLoading(true)
    try {
      const res = await quotationService.duplicate(id)
      toast.success('Duplicated as draft')
      navigate(`/quotations/${res.data.data?.id}`)
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
    finally { setDupeLoading(false) }
  }

  async function handleCreateInvoice() {
    setCreateLoading(true)
    try {
      const res = await invoiceService.importFromQuotation(id)
      toast.success('Invoice created from quotation')
      navigate(`/invoices/${res.data.data?.id}`)
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    } finally { setCreateLoading(false) }
  }

  async function handleDelete() {
    try {
      await quotationService.delete(id)
      toast.success('Quotation deleted')
      navigate('/dashboard', { replace: true })
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
  }

  if (loading) return (
    <div className="max-w-3xl space-y-4 animate-fade-in">
      <Skeleton className="h-9 w-56" />
      <Skeleton className="h-56 w-full rounded-2xl" />
      <Skeleton className="h-48 w-full rounded-2xl" />
    </div>
  )
  if (!quotation) return <p className="text-muted">Quotation not found.</p>

  const q       = quotation
  const isFinal = q.status === 'FINAL'
  const displayNurseryName = q.nurseryName || profile?.nurseryName || 'Your Nursery'
  const formattedId = q.quotationNumber || '-'
  const showHeight = Boolean(q.heightEnabled)
  const totalQuantity = (q.items || []).reduce((sum, it) => sum + (parseInt(it.quantity) || 0), 0)
  const totalNetWeight = (q.items || []).reduce((sum, it) => {
    const qty = parseInt(it.quantity) || 0
    const unit = getUnitWeightFromDimension(it.dimension)
    return sum + unit * qty
  }, 0)
  const summaryRows = [
    { label: 'Total Qty', value: totalQuantity },
    { label: 'Net Weight', value: formatWeight(totalNetWeight) },
    ...((q.shippingCharges || 0) > 0 ? [{ label: 'Loading Charges', value: formatCurrency(q.shippingCharges) }] : []),
    ...((q.discountAmount || 0) > 0 ? [{ label: 'Discount', value: formatCurrency(q.discountAmount) }] : []),
  ]
  const amountRows = [
    { label: 'Total', value: formatCurrency(q.totalAmount), tone: 'text-ink', emphasize: false },
    { label: 'Grand Total', value: formatCurrency(q.grandTotal), tone: 'text-[#5CB338]', emphasize: true },
  ]

  return (
    <div className="animate-fade-up max-w-3xl">
      {/* Top bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div className="flex items-start sm:items-center gap-3">
          <button onClick={() => navigate('/dashboard')}
            className="w-9 h-9 rounded-xl bg-white border border-gray-200 flex items-center justify-center hover:bg-green-50 shadow-sm transition-colors flex-shrink-0">
            <ArrowLeft className="w-4 h-4 text-gray-600" />
          </button>
          <div>
            <div className="flex flex-wrap items-center gap-2.5">
               <h1 className="text-xl sm:text-2xl font-bold text-ink leading-tight">Quotation {formattedId}</h1>
              <Badge variant={isFinal ? 'green' : 'amber'}>
                {isFinal ? <><CheckCircle className="w-2.5 h-2.5" />FINAL</> : <><Clock className="w-2.5 h-2.5" />DRAFT</>}
              </Badge>
            </div>
            <p className="text-xs text-muted mt-1">Generated on {formatDate(q.createdAt)}</p>
          </div>
        </div>
        <div className="flex items-center gap-2 w-full sm:w-auto flex-wrap sm:flex-nowrap">
          {q.publicToken && <Button variant="secondary" size="sm" className="flex-1 sm:flex-none justify-center" onClick={handleShare}><Share2 className="w-3.5 h-3.5" />Share</Button>}
          <Button size="sm" className="flex-1 sm:flex-none justify-center" onClick={handleDownloadPdf} loading={pdfLoading}><Download className="w-3.5 h-3.5" />Download PDF</Button>
        </div>
      </div>

      {/* Quotation card */}
      <Card className="overflow-hidden mb-4">
        {/* Header: nursery + customer */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 p-4 sm:p-6 bg-green-50 border-b border-green-100">
          <div>
            <h2 className="text-xl font-bold text-green-900 mb-1">{displayNurseryName}</h2>
            {q.nurseryOwnerName && <p className="text-xs text-muted font-semibold mb-2">Proprietor: {q.nurseryOwnerName}</p>}
            {q.nurseryAddress && <div className="flex items-start gap-1.5 text-sm text-muted mb-1"><MapPin className="w-3.5 h-3.5 mt-0.5 flex-shrink-0" />{q.nurseryAddress}</div>}
            {q.nurseryPhone  && <div className="flex items-center gap-1.5 text-sm text-muted"><Phone className="w-3.5 h-3.5 flex-shrink-0" />{q.nurseryPhone}</div>}
          </div>
          <div>
            <p className="text-[11px] font-bold text-muted uppercase tracking-widest mb-2 flex items-center gap-1.5"><User className="w-3 h-3" />Quotation To</p>
            {q.customerName ? (
              <>
                <p className="font-semibold text-ink">{q.customerName}</p>
                {q.customerPhone   && <p className="text-sm text-muted font-mono mt-0.5">{q.customerPhone}</p>}
                {q.customerAddress && <p className="text-sm text-muted mt-0.5 leading-relaxed">{q.customerAddress}</p>}
              </>
            ) : <p className="text-sm text-muted italic">No customer details</p>}
          </div>
        </div>

        {q.notes && (
          <div className="px-4 sm:px-6 py-4 border-b border-gray-100 bg-white">
            <p className="text-[11px] font-bold text-muted uppercase tracking-widest mb-2">Notes</p>
            <p className="text-sm text-muted leading-relaxed whitespace-pre-line">{q.notes}</p>
          </div>
        )}

        {/* Table */}
        <div className="overflow-x-auto border-x border-gray-200">
          <table className={`w-full border-collapse ${showHeight ? 'min-w-[720px]' : 'min-w-[600px]'} sm:min-w-max`}>
            <thead>
              <tr className="bg-[#5CB338]">
                <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-left">ID</th>
                <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-left">Plant Name</th>
                 <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-left whitespace-nowrap">Bag / Pot Size</th>
                {showHeight && <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-left whitespace-nowrap">Height</th>}
                <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-right">Qty</th>
                <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-right">Rate</th>
                <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-right">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {(q.items || []).map((item, i) => (
                <tr key={i} className={i % 2 === 1 ? 'bg-green-50/40' : ''}>
                  <td className="border border-gray-200 px-3 sm:px-5 py-3.5 text-sm text-muted font-mono">{i + 1}</td>
                  <td className="border border-gray-200 px-3 sm:px-5 py-3.5 text-sm font-semibold text-ink break-words min-w-[100px]">{item.plantName}</td>
                  <td className="border border-gray-200 px-3 sm:px-5 py-3.5 text-sm text-muted whitespace-nowrap">{item.dimension || '-'}</td>
                  {showHeight && <td className="border border-gray-200 px-3 sm:px-5 py-3.5 text-sm text-muted whitespace-nowrap">{item.height || '-'}</td>}
                  <td className="border border-gray-200 px-3 sm:px-5 py-3.5 text-sm text-right text-muted font-mono">{item.quantity}</td>
                  <td className="border border-gray-200 px-3 sm:px-5 py-3.5 text-sm text-right text-muted font-mono">{formatCurrency(item.price)}</td>
                  <td className="border border-gray-200 px-3 sm:px-5 py-3.5 text-sm text-right font-bold text-[#5CB338] font-mono">{formatNumber(item.price * item.quantity)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Grand total */}
        <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_minmax(280px,360px)] gap-4 sm:gap-6 px-4 sm:px-6 py-4 sm:py-5 border-t border-gray-100 bg-gray-50/50 items-start">
          <div className="rounded-2xl border border-gray-200 bg-white/80 px-4 py-4">
            <div className="grid grid-cols-[minmax(120px,170px)_1fr] gap-x-4 gap-y-3 items-baseline">
              {summaryRows.map(row => (
                <div key={row.label} className="contents">
                  <span className="text-[11px] sm:text-xs font-semibold text-muted uppercase tracking-[0.22em]">
                    {row.label}
                  </span>
                  <span className="text-base sm:text-lg font-bold text-ink font-mono leading-none">
                    {row.value}
                  </span>
                </div>
              ))}
            </div>
            {q.loadingChargesEnabled && (
              <div className="flex items-center gap-3 mt-1">
                <span className="text-sm font-semibold text-muted uppercase tracking-widest">Loading Charges</span>
                <span className="text-xl font-bold text-ink font-mono">
                  {formatCurrency(Math.round(q.loadingChargesAmount || 0))}
                </span>
              </div>
            )}
          </div>
          <div className="rounded-2xl border border-green-100 bg-white px-4 sm:px-5 py-4">
            <div className="space-y-3">
              {amountRows.map((row, index) => (
                <div
                  key={row.label}
                  className={`flex items-baseline justify-between gap-4 ${index > 0 ? 'border-t border-gray-100 pt-3' : ''}`}
                >
                  <span className={`uppercase tracking-[0.2em] ${row.emphasize ? 'text-xs sm:text-sm font-semibold text-muted' : 'text-[11px] sm:text-xs text-muted'}`}>
                    {row.label}
                  </span>
                  <span className={`font-mono leading-none text-right ${row.emphasize ? `text-2xl sm:text-3xl font-bold ${row.tone}` : `text-base sm:text-lg font-semibold ${row.tone}`}`}>
                    {row.value}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </Card>

      {/* Actions */}
      <div className="flex flex-wrap items-center gap-2.5">
        <Button variant="secondary" className="flex-1 sm:flex-none justify-center" onClick={() => navigate(`/quotations/${id}/edit`)}><Edit2 className="w-3.5 h-3.5" />Edit</Button>
        <Button variant="secondary" className="flex-1 sm:flex-none justify-center" onClick={handleDuplicate} loading={dupeLoading}><Copy className="w-3.5 h-3.5" />Duplicate</Button>
        <Button variant="secondary" className="flex-1 sm:flex-none justify-center" onClick={handleCreateInvoice} loading={createLoading}><FilePlus className="w-3.5 h-3.5" />Create Invoice</Button>
        <Button variant="ghost" className="w-full sm:w-auto justify-center text-red-500 hover:bg-red-100 hover:text-red-700 sm:ml-auto" onClick={() => setShowDelete(true)}>
          <Trash2 className="w-4 h-4" />Delete
        </Button>
      </div>

      <ConfirmDialog open={showDelete} onClose={() => setShowDelete(false)} onConfirm={handleDelete}
        title="Delete Quotation" description={`Quotation ${formattedId} will be permanently deleted.`}
        confirmLabel="Delete" danger />
    </div>
  )
}
