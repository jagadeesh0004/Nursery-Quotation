/**
 * DashboardPage — lists all quotations
 * GET  /quotations           list (search + pagination)
 * DELETE /quotations/:id     soft delete
 * POST /quotations/:id/duplicate  duplicate
 */
import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Eye, Copy, Trash2, FileText } from 'lucide-react'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'
import { quotationService } from '../services'
import { Button, Badge, Skeleton, EmptyState, ConfirmDialog, PageHeader, SearchBar, Card } from '../components/ui'
import { formatCurrency, formatDate, getError, formatQuotationId } from '../utils'

export default function DashboardPage() {
  const navigate = useNavigate()
  const { profile } = useAuth()
  const [quotations,   setQuotations]   = useState([])
  const [loading,      setLoading]      = useState(true)
  const [search,       setSearch]       = useState('')
  const [dateFilter,   setDateFilter]   = useState('')
  const [deleteId,     setDeleteId]     = useState(null)
  const [dupeLoading,  setDupeLoading]  = useState(null)

  const fetchList = useCallback(async () => {
    setLoading(true)
    try {
      const res = await quotationService.list({ search, page: 0, size: 50 })
      const raw = res.data.data
      setQuotations(Array.isArray(raw) ? raw : (raw?.content ?? []))
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
    finally { setLoading(false) }
  }, [search])

  useEffect(() => { fetchList() }, [fetchList])

  async function handleDelete() {
    try {
      await quotationService.delete(deleteId)
      toast.success('Quotation deleted')
      setDeleteId(null)
      fetchList()
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
  }

  async function handleDuplicate(id) {
    setDupeLoading(id)
    try {
      const res = await quotationService.duplicate(id)
      toast.success('Duplicated as new draft')
      navigate(`/quotations/${res.data.data?.id}`)
    } catch (err) {
      const message = getError(err)
      if (message) toast.error(message)
    }
    finally { setDupeLoading(null) }
  }

  const filteredQuotations = quotations.filter(q => {
    if (!dateFilter) return true
    return q.createdAt && q.createdAt.startsWith(dateFilter)
  })

  return (
    <div className="animate-fade-up">
      <PageHeader title="Quotations" subtitle="Manage and track all your generated quotations."
        action={<Button onClick={() => navigate('/quotations/new')}><Plus className="w-4 h-4" />New Quotation</Button>}
      />


      <div className="flex flex-col sm:flex-row gap-3 mb-5 max-w-2xl">
        <SearchBar value={search} onChange={e => setSearch(e.target.value)}
          placeholder="Search by customer name…" className="flex-1" />
        <input
          type="date"
          value={dateFilter}
          onChange={e => setDateFilter(e.target.value)}
          className="w-full sm:w-[160px] px-4 py-2.5 rounded-xl border border-gray-200 bg-white text-sm text-ink placeholder:text-gray-400 font-sans transition-all duration-150 outline-none focus:border-green-600 focus:ring-2 focus:ring-green-600/15 h-[42px]"
        />
      </div>

      <Card className="overflow-hidden">
        {loading ? (
          <div className="divide-y divide-gray-50">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="flex items-center gap-4 px-5 py-4">
                <Skeleton className="w-9 h-9 rounded-xl" />
                <div className="flex-1 space-y-2"><Skeleton className="h-4 w-36" /><Skeleton className="h-3 w-24" /></div>
                <Skeleton className="h-5 w-16 rounded-full" />
                <Skeleton className="h-4 w-20" />
              </div>
            ))}
          </div>
        ) : filteredQuotations.length === 0 ? (
          <EmptyState icon={FileText} title="No quotations yet"
            description="Create your first quotation to get started."
            action={<Button onClick={() => navigate('/quotations/new')}><Plus className="w-4 h-4" />Create Quotation</Button>}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[600px] sm:min-w-max">
              <thead>
                <tr className="bg-green-50 border-b border-green-100">
                  {['ID', 'Customer', 'Date', 'Status', 'Total', ''].map((h, i) => (
                    <th key={i} className={`px-3 sm:px-5 py-3 text-[11px] font-semibold text-[#5CB338] tracking-widest uppercase
                      ${['Total', ''].includes(h) ? 'text-right' : 'text-left'}
                      ${h === 'Date' ? 'hidden sm:table-cell' : ''}
                      ${h === 'Status' ? 'hidden md:table-cell' : ''}`}>
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50 stagger">
                {filteredQuotations.map(q => (
                  <tr key={q.id}
                    className="group hover:bg-green-50/50 transition-colors cursor-pointer animate-fade-up"
                    onClick={() => navigate(`/quotations/${q.id}`)}>
                     <td className="px-3 sm:px-5 py-3 sm:py-4">
                       <span className="font-mono text-xs text-muted bg-gray-100 px-2 py-0.5 rounded-md">{q.quotationNumber || '-'}</span>
                     </td>
                    <td className="px-3 sm:px-5 py-3 sm:py-4">
                      <p className="text-sm font-semibold text-ink">{q.customerName || <span className="text-gray-400 italic text-xs font-normal">No name</span>}</p>
                      {q.customerPhone && <p className="text-xs text-muted font-mono mt-0.5">{q.customerPhone}</p>}
                    </td>
                    <td className="px-3 sm:px-5 py-3 sm:py-4 text-xs text-muted hidden sm:table-cell">{formatDate(q.createdAt)}</td>
                    <td className="px-3 sm:px-5 py-3 sm:py-4 hidden md:table-cell">
                      <Badge variant={q.status === 'FINAL' ? 'green' : 'amber'}>{q.status}</Badge>
                    </td>
                    <td className="px-3 sm:px-5 py-3 sm:py-4 text-right">
                      <span className="text-sm font-bold text-[#5CB338] font-mono">{formatCurrency(q.totalAmount)}</span>
                    </td>
                    <td className="px-3 sm:px-5 py-3 sm:py-4">
                      <div className="flex items-center justify-end gap-1 opacity-100 lg:opacity-0 lg:group-hover:opacity-100 transition-opacity"
                        onClick={e => e.stopPropagation()}>
                        <button title="View" onClick={() => navigate(`/quotations/${q.id}`)}
                          className="p-1.5 rounded-lg hover:bg-green-100 text-gray-400 hover:text-green-700 transition-colors">
                          <Eye className="w-3.5 h-3.5" />
                        </button>
                        <button title="Duplicate" onClick={() => handleDuplicate(q.id)} disabled={dupeLoading === q.id}
                          className="p-1.5 rounded-lg hover:bg-blue-50 text-gray-400 hover:text-blue-600 transition-colors disabled:opacity-40">
                          <Copy className="w-3.5 h-3.5" />
                        </button>
                        <button title="Delete" onClick={() => setDeleteId(q.id)}
                          className="p-1.5 rounded-lg hover:bg-red-100 text-red-500 hover:text-red-700 transition-colors">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <ConfirmDialog open={!!deleteId} onClose={() => setDeleteId(null)} onConfirm={handleDelete}
        title="Delete Quotation" description="This quotation will be permanently removed. This cannot be undone."
        confirmLabel="Delete" danger />
    </div>
  )
}
