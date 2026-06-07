/**
 * PublicQuotationPage — no-auth shareable view
 * GET /public/quotations/:token      → quotation data
 * GET /public/quotations/:token/pdf  → PDF blob download
 */
import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { Download, MapPin, Phone, Sprout, AlertCircle } from 'lucide-react'
import toast from 'react-hot-toast'
import { publicService } from '../services'
import { Button, Skeleton } from '../components/ui'
import { formatCurrency, formatNumber, formatDate, downloadBlob, getError, formatQuotationId } from '../utils'
import { getUnitWeightFromDimension, formatWeight } from '../constants/weightMapping'

export default function PublicQuotationPage() {
  const { token } = useParams()

  const [quotation,  setQuotation]  = useState(null)
  const [loading,    setLoading]    = useState(true)
  const [error,      setError]      = useState(null)
  const [pdfLoading, setPdfLoading] = useState(false)

  useEffect(() => {
    publicService.getQuotation(token)
      .then(r  => setQuotation(r.data.data))
      .catch(err => {
        const message = getError(err)
        if (message) setError(message)
      })
      .finally(() => setLoading(false))
  }, [token])

  async function handleDownload() {
    setPdfLoading(true)
    try {
      const res = await publicService.downloadPdf(token)
      downloadBlob(res.data, `quotation-${quotation?.id || token}.pdf`)
      toast.success('PDF downloaded')
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
    finally { setPdfLoading(false) }
  }

  if (loading) return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-6">
      <div className="w-full max-w-2xl space-y-4">
        <Skeleton className="h-8 w-40 mx-auto" />
        <Skeleton className="h-64 w-full rounded-2xl" />
        <Skeleton className="h-40 w-full rounded-2xl" />
      </div>
    </div>
  )

  if (error) return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-6">
      <div className="text-center">
        <div className="w-16 h-16 rounded-2xl bg-red-50 border border-red-100 flex items-center justify-center mx-auto mb-5">
          <AlertCircle className="w-7 h-7 text-red-400" strokeWidth={1.5} />
        </div>
        <h2 className="text-xl font-bold text-ink mb-2">Quotation Unavailable</h2>
        <p className="text-sm text-muted max-w-xs mx-auto">{error}</p>
      </div>
    </div>
  )

  const q = quotation
  const displayNurseryName = q.nurseryName || 'Nursery'
  const formattedId = formatQuotationId(displayNurseryName, q.id)
  const showHeight = Boolean(q.heightEnabled)
  const totalQuantity = (q.items || []).reduce((sum, it) => sum + (parseInt(it.quantity) || 0), 0)
  const totalNetWeight = (q.items || []).reduce((sum, it) => {
    const qty = parseInt(it.quantity) || 0
    const unit = getUnitWeightFromDimension(it.dimension)
    return sum + unit * qty
  }, 0)

  return (
    <div className="min-h-screen bg-surface py-8 px-4">
      {/* Brand bar */}
      <div className="flex items-center justify-center gap-2.5 mb-8">
        <div className="w-9 h-9 rounded-xl bg-[#5CB338] flex items-center justify-center">
          <Sprout className="w-4 h-4 text-white" />
        </div>
        <span className="text-lg font-bold text-ink">QuotoNursery</span>
      </div>

      <div className="max-w-2xl mx-auto">
        {/* Title + download */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-ink">Quotation {formattedId}</h1>
            <p className="text-sm text-muted mt-0.5">Generated on {formatDate(q.createdAt)}</p>
          </div>
          <Button onClick={handleDownload} loading={pdfLoading} className="w-full sm:w-auto justify-center">
            <Download className="w-4 h-4" /> Download PDF
          </Button>
        </div>

        {/* Quotation card */}
        <div className="bg-white rounded-2xl border border-gray-100 shadow-md overflow-hidden">
          {/* Header */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 p-4 sm:p-6 bg-green-50 border-b border-green-100">
            <div>
              <h2 className="text-xl font-bold text-green-900 mb-1">{displayNurseryName}</h2>
              {q.nurseryOwnerName && <p className="text-xs text-muted font-semibold mb-2">Proprietor: {q.nurseryOwnerName}</p>}
              {q.nurseryAddress && (
                <div className="flex items-start gap-1.5 text-sm text-muted mb-1">
                  <MapPin className="w-3.5 h-3.5 mt-0.5 flex-shrink-0" strokeWidth={1.5} /> {q.nurseryAddress}
                </div>
              )}
              {q.nurseryPhone && (
                <div className="flex items-center gap-1.5 text-sm text-muted">
                  <Phone className="w-3.5 h-3.5 flex-shrink-0" strokeWidth={1.5} /> {q.nurseryPhone}
                </div>
              )}
            </div>
            <div>
              <p className="text-[11px] font-bold text-muted uppercase tracking-widest mb-2">Quotation To:</p>
              {q.customerName ? (
                <>
                  <p className="font-semibold text-ink">{q.customerName}</p>
                  {q.customerPhone   && <p className="text-sm text-muted font-mono mt-0.5">{q.customerPhone}</p>}
                  {q.customerAddress && <p className="text-sm text-muted mt-1 leading-relaxed">{q.customerAddress}</p>}
                  <p className="text-xs text-muted font-mono mt-2">
                    Date: {formatDate(q.createdAt)} &nbsp;&nbsp; {formattedId}
                  </p>
                </>
              ) : <p className="text-sm text-muted italic">No customer info</p>}
            </div>
          </div>

          {q.notes && (
            <div className="px-4 sm:px-6 py-4 border-b border-gray-100 bg-white">
              <p className="text-[11px] font-bold text-muted uppercase tracking-widest mb-2">Notes</p>
              <p className="text-sm text-muted leading-relaxed whitespace-pre-line">{q.notes}</p>
            </div>
          )}

          {/* Items table */}
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

          {/* Grand total row */}
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
              </div>
            <div className="hidden sm:block w-px h-8 bg-gray-200"></div>
            <div className="flex items-center gap-3">
              <span className="text-sm font-semibold text-muted uppercase tracking-widest">Grand Total</span>
              <span className="text-2xl sm:text-3xl font-bold text-[#5CB338] font-mono truncate">{formatCurrency(q.totalAmount)}</span>
            </div>
          </div>

          <div className="px-4 sm:px-6 py-4 border-t border-gray-100 text-center">
            <p className="text-xs text-muted italic">Thank you for your business. We look forward to working with you.</p>
          </div>
        </div>

        <p className="text-center text-xs text-muted mt-6">
          Powered by <span className="text-green-700 font-semibold">QuotoNursery</span>
        </p>
      </div>
    </div>
  )
}
