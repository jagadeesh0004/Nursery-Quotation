import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Users, Search, Plus } from 'lucide-react'
import toast from 'react-hot-toast'
import { customerService } from '../services'
import { Button, Card, EmptyState, PageHeader, Skeleton } from '../components/ui'
import { getError } from '../utils'

export default function DashboardPage() {
  const navigate = useNavigate()
  const [customers, setCustomers] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    customerService.list(search).then(r => setCustomers(r.data.data || []))
      .catch(err => { const message = getError(err); if (message) toast.error(message) })
      .finally(() => setLoading(false))
  }, [search])

  return (
    <div className="animate-fade-up">
      <PageHeader title="Customers" subtitle="Manage customers and their quotations and invoices."
        action={<Button onClick={() => navigate('/quotations/new')}><Plus className="w-4 h-4" />New Quotation</Button>} />
      <div className="mb-5 max-w-xl relative">
        <Search className="absolute left-3.5 top-3 w-4 h-4 text-gray-400" />
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search customers..."
          className="field pl-10 w-full" />
      </div>
      <Card className="overflow-hidden">
        {loading ? <div className="p-6 space-y-4">{[1, 2, 3].map(i => <Skeleton key={i} className="h-16 w-full" />)}</div>
          : customers.length === 0 ? <EmptyState icon={Users} title="No customers yet" description="Customers appear here when you create a quotation or invoice." />
          : <div className="divide-y divide-gray-50">{customers.map(c => (
            <button key={c.id} onClick={() => navigate(`/customers/${c.id}`)}
              className="w-full text-left px-5 py-4 hover:bg-green-50/60 transition-colors">
              <p className="font-semibold text-ink">{c.name}</p>
              {c.phone && <p className="text-xs text-muted font-mono mt-1">{c.phone}</p>}
              {c.address && <p className="text-xs text-muted mt-1">{c.address}</p>}
              <p className="text-xs text-muted mt-1">{c.quotationCount} Quotations | {c.invoiceCount} Invoices</p>
            </button>
          ))}</div>}
      </Card>
    </div>
  )
}
