import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, CheckCircle, Clock, Download, Edit2, Eye, Plus } from 'lucide-react'
import toast from 'react-hot-toast'
import { customerService, invoiceService } from '../services'
import { Badge, Button, Card, Spinner } from '../components/ui'
import { formatCurrency, formatDate, getError, downloadBlob } from '../utils'

export default function CustomerDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [customer, setCustomer] = useState(null)

  useEffect(() => {
    customerService.getOne(id).then(r => {
      const c = r.data.data
      setCustomer(c)
    }).catch(err => { const message = getError(err); if (message) toast.error(message) })
  }, [id])

  if (!customer) return <Spinner size="lg" className="text-green-600 mx-auto mt-20" />

  const newDocument = type => navigate(`/${type}/new?customerId=${id}`)
  return <div className="animate-fade-up space-y-6">
    <button onClick={() => navigate('/dashboard')} className="text-sm text-muted flex items-center gap-2"><ArrowLeft className="w-4 h-4" />Customers</button>
    <h1 className="text-2xl font-bold text-ink">{customer.name}</h1>
    <Card className="p-5">
      <div className="text-sm text-muted space-y-2"><p><b>Name:</b> {customer.name}</p><p><b>Phone:</b> {customer.phone || '—'}</p><p><b>Address:</b> {customer.address || '—'}</p></div>
    </Card>
    <DocumentSection title="Quotations" items={customer.quotations} onNew={() => newDocument('quotations')} onOpen={x => navigate(`/quotations/${x.id}`)} />
    <DocumentSection title="Invoices" items={customer.invoices} onNew={() => newDocument('invoices')} onOpen={x => navigate(`/invoices/${x.id}`)}
      onPdf={async x => { try { const r = await invoiceService.downloadPdf(x.id); downloadBlob(r.data, `invoice-${x.id}.pdf`) } catch (err) { const message = getError(err); if (message) toast.error(message) } }} />
  </div>
}

function DocumentSection({ title, items, onNew, onOpen, onPdf }) {
  return <Card className="overflow-hidden">
    <div className="px-5 py-4 flex justify-between border-b border-gray-100"><h2 className="font-semibold text-ink">{title}</h2><Button size="sm" onClick={onNew}><Plus className="w-3.5 h-3.5" />New {title.slice(0, -1)}</Button></div>
    {items.length === 0 ? <p className="p-5 text-sm text-muted">No {title.toLowerCase()} yet.</p> : <div className="overflow-x-auto">
      <table className="w-full min-w-[600px]">
        <thead><tr className="bg-green-50 border-b border-green-100">{['ID', 'Customer', 'Date', 'Status', 'Total', ''].map((h, i) => <th key={i} className={`px-3 sm:px-5 py-3 text-[11px] font-semibold text-[#5CB338] tracking-widest uppercase ${['Total', ''].includes(h) ? 'text-right' : 'text-left'} ${h === 'Date' ? 'hidden sm:table-cell' : ''} ${h === 'Status' ? 'hidden md:table-cell' : ''}`}>{h}</th>)}</tr></thead>
        <tbody className="divide-y divide-gray-50">{items.map(x => <tr key={x.id} className="group hover:bg-green-50/50 transition-colors cursor-pointer" onClick={() => onOpen(x)}>
          <td className="px-3 sm:px-5 py-3 sm:py-4"><span className="font-mono text-xs text-muted bg-gray-100 px-2 py-0.5 rounded-md">{x.number}</span></td>
          <td className="px-3 sm:px-5 py-3 sm:py-4"><p className="text-sm font-semibold text-ink">{x.customerName || 'No name'}</p></td>
          <td className="px-3 sm:px-5 py-3 sm:py-4 text-xs text-muted hidden sm:table-cell">{formatDate(x.createdAt)}</td>
          <td className="px-3 sm:px-5 py-3 sm:py-4 hidden md:table-cell"><Badge variant={x.status === 'FINAL' ? 'green' : 'amber'}>{x.status === 'FINAL' ? <><CheckCircle className="w-2.5 h-2.5" />FINAL</> : <><Clock className="w-2.5 h-2.5" />DRAFT</>}</Badge></td>
          <td className="px-3 sm:px-5 py-3 sm:py-4 text-right"><span className="text-sm font-bold text-[#5CB338] font-mono">{formatCurrency(x.total)}</span></td>
          <td className="px-3 sm:px-5 py-3 sm:py-4"><div className="flex items-center justify-end gap-1 opacity-100 lg:opacity-0 lg:group-hover:opacity-100" onClick={e => e.stopPropagation()}>
            <button title="View" onClick={() => onOpen(x)} className="p-1.5 rounded-lg hover:bg-green-100 text-gray-400 hover:text-green-700"><Eye className="w-3.5 h-3.5" /></button>
            {onPdf && <button title="Download PDF" onClick={() => onPdf(x)} className="p-1.5 rounded-lg hover:bg-blue-50 text-gray-400 hover:text-blue-600"><Download className="w-3.5 h-3.5" /></button>}
            {x.status !== 'FINAL' && <button title="Edit" onClick={() => navigate(`/${title.toLowerCase()}/${x.id}/edit`)} className="p-1.5 rounded-lg hover:bg-amber-50 text-gray-400 hover:text-amber-700"><Edit2 className="w-3.5 h-3.5" /></button>}
          </div></td>
        </tr>)}</tbody>
      </table>
    </div>}
  </Card>
}
