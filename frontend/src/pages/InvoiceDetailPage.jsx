import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Download, Edit2, Trash2, MapPin, Phone, User, CheckCircle, Clock } from 'lucide-react'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'
import { invoiceService } from '..//services'
import { Button, Badge, Skeleton, ConfirmDialog, Card } from '../components/ui'
import { formatCurrency, formatDate, downloadBlob, getError } from '../utils'
import { getUnitWeightFromDimension, formatWeight } from '../constants/weightMapping'

export default function InvoiceDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { profile } = useAuth()

  const [invoice,   setInvoice]   = useState(null)
  const [loading,     setLoading]     = useState(true)
  const [pdfLoading,  setPdfLoading]  = useState(false)
  const [showDelete,  setShowDelete]  = useState(false)

  async function loadInvoice() {
    const r = await invoiceService.getOne(id)
    setInvoice(r.data.data)
  }

  useEffect(() => {
    invoiceService.getOne(id)
      .then(r  => setInvoice(r.data.data))
      .catch((err) => {
        const message = getError(err)
        if (message) toast.error(message)
      })
      .finally(() => setLoading(false))
  }, [id])

  async function handleDownloadPdf() {
    setPdfLoading(true)
    try {
      const res = await invoiceService.downloadPdf(id)
      downloadBlob(res.data, `invoice-${id}.pdf`)
      toast.success('PDF downloaded')
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
    finally { setPdfLoading(false) }
  }

  async function handleDelete() {
    try {
      await invoiceService.delete(id)
      toast.success('Invoice deleted')
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
  if (!invoice) return <p className="text-muted">Invoice not found.</p>

  const q       = invoice
  const isFinal = q.status === 'FINAL'
  const displayNurseryName = q.nurseryName || profile?.nurseryName || 'Your Nursery'
  const formattedId = q.invoiceNumber || '-'
  const showHeight = Boolean(q.heightEnabled)
  const totalQuantity = (q.items || []).reduce((sum, it) => sum + (parseInt(it.quantity) || 0), 0)
  const totalNetWeight = (q.items || []).reduce((sum, it) => {
    const qty = parseInt(it.quantity) || 0
    const unit = getUnitWeightFromDimension(it.dimension)
    return sum + unit * qty
  }, 0)

  return (
    <div className="animate-fade-up max-w-3xl">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div className="flex items-start sm:items-center gap-3">
          <button onClick={() => navigate('/dashboard')}
            className="w-9 h-9 rounded-xl bg-white border border-gray-200 flex items-center justify-center hover:bg-green-50 shadow-sm transition-colors flex-shrink-0">
            <ArrowLeft className="w-4 h-4 text-gray-600" />
          </button>
          <div>
            <div className="flex flex-wrap items-center gap-2.5">
               <h1 className="text-xl sm:text-2xl font-bold text-ink leading-tight">Invoice {formattedId}</h1>
              <Badge variant={isFinal ? 'green' : 'amber'}>
                {isFinal ? <><CheckCircle className="w-2.5 h-2.5" />FINAL</> : <><Clock className="w-2.5 h-2.5" />DRAFT</>}
              </Badge>
            </div>
            <p className="text-xs text-muted mt-1">Generated on {formatDate(q.createdAt)}</p>
          </div>
        </div>
        <div className="flex items-center gap-2 w-full sm:w-auto flex-wrap sm:flex-nowrap">
          <Button size="sm" className="flex-1 sm:flex-none justify-center" onClick={handleDownloadPdf} loading={pdfLoading}><Download className="w-3.5 h-3.5" />Download PDF</Button>
        </div>
      </div>

      <Card className="overflow-hidden mb-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 p-4 sm:p-6 bg-green-50 border-b border-green-100">
          <div>
            <h2 className="text-xl font-bold text-green-900 mb-1">{displayNurseryName}</h2>
            {q.nurseryOwnerName && <p className="text-xs text-muted font-semibold mb-2">Proprietor: {q.nurseryOwnerName}</p>}
            {q.nurseryAddress && <div className="flex items-start gap-1.5 text-sm text-muted mb-1"><MapPin className="w-3.5 h-3.5 mt-0.5 flex-shrink-0" />{q.nurseryAddress}</div>}
            {q.nurseryPhone  && <div className="flex items-center gap-1.5 text-sm text-muted"><Phone className="w-3.5 h-3.5 flex-shrink-0" />{q.nurseryPhone}</div>}
          </div>
          <div>
            <p className="text-[11px] font-bold text-muted uppercase tracking-widest mb-2 flex items-center gap-1.5"><User className="w-3 h-3" />Invoice To</p>
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

        <div className="overflow-x-auto border-x border-gray-200">
          <table className={`w-full border-collapse ${showHeight ? 'min-w-[720px]' : 'min-w-[600px]'} sm:min-w-max`}>
            <thead>
              <tr className="bg-[#5CB338]">
                <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-left">ID</th>
                <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-left">Plant Name</th>
                <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-left whitespace-nowrap">Bag / Pot Size</th>
                {showHeight && <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-left whitespace-nowrap">Height</th>}
                <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-right">Qty</th>
                <th className="border border-[#4a912e] px-3 sm:px-5 py-3 text-[11px] font-semibold text-white/90 uppercase tracking-widest text-right">Price/Pc</th>
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
                  <td className="border border-gray-200 px-3 sm:px-5 py-3.5 text-sm text-right font-bold text-[#5CB338] font-mono">{formatCurrency(item.price * item.quantity)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="flex flex-col sm:flex-row justify-end items-end sm:items-center gap-3 sm:gap-8 px-4 sm:px-6 py-4 sm:py-5 border-t border-gray-100 bg-gray-50/50">
          <div className="flex flex-col">
            <div className="flex items-center gap-3">
              <span className="text-sm font-semibold text-muted uppercase tracking-widest">Total Qty</span>
              <span className="text-xl font-bold text-ink font-mono">{totalQuantity}</span>
            </div>
            <div className="flex items-center gap-3 mt-1">
              <span className="text-sm font-semibold text-muted uppercase tracking-widest">Net Weight</span>
              <span className="text-xl font-bold text-ink font-mono">{formatWeight(totalNetWeight)}</span>
            </div>
            {(q.shippingCharges || 0) > 0 && (
              <div className="flex items-center gap-3 mt-1">
                <span className="text-sm font-semibold text-muted uppercase tracking-widest">Shipping</span>
                <span className="text-lg font-bold text-ink font-mono">{formatCurrency(q.shippingCharges)}</span>
              </div>
            )}
            {(q.discountAmount || 0) > 0 && (
              <div className="flex items-center gap-3 mt-1">
                <span className="text-sm font-semibold text-muted uppercase tracking-widest">Discount</span>
                <span className="text-lg font-bold text-ink font-mono">{formatCurrency(q.discountAmount)}</span>
              </div>
            )}
          </div>
          <div className="hidden sm:block w-px h-8 bg-gray-200"></div>
          <div className="w-full sm:max-w-md space-y-2">
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted">Total</span>
              <span className="font-mono text-ink">{formatCurrency(q.totalAmount)}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="font-semibold text-muted uppercase tracking-widest">Grand Total</span>
              <span className="text-2xl font-bold text-[#5CB338] font-mono truncate">{formatCurrency(q.grandTotal)}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted">Total Advance</span>
              <span className="font-mono text-ink">{formatCurrency(q.totalAdvance || 0)}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="font-semibold text-muted uppercase tracking-widest">Remaining</span>
              <span className="text-xl font-bold text-ink font-mono">{formatCurrency(q.remainingAmount || 0)}</span>
            </div>
          </div>
        </div>
      </Card>

      <div className="flex flex-wrap items-center gap-2.5">
        <Button variant="secondary" className="flex-1 sm:flex-none justify-center" onClick={() => navigate(`/invoices/${id}/edit`)}><Edit2 className="w-3.5 h-3.5" />Edit</Button>
        <Button variant="ghost" className="w-full sm:w-auto justify-center text-red-500 hover:bg-red-100 hover:text-red-700 sm:ml-auto" onClick={() => setShowDelete(true)}>
          <Trash2 className="w-4 h-4" />Delete
        </Button>
      </div>

      <ConfirmDialog open={showDelete} onClose={() => setShowDelete(false)} onConfirm={handleDelete}
        title="Delete Invoice" description={`Invoice ${formattedId} will be permanently deleted.`}
        confirmLabel="Delete" danger />
    </div>
  )
}
