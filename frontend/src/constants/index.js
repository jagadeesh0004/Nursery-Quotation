// ─── API ────────────────────────────────────────────────────────────────────
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090/api'

// ─── App Routes ─────────────────────────────────────────────────────────────
export const ROUTES = {
  LOGIN:            '/login',
  DASHBOARD:        '/dashboard',
  PLANTS:           '/plants',
  QUOTATION_NEW:    '/quotations/new',
  QUOTATION_EDIT:   '/quotations/:id/edit',
  QUOTATION_DETAIL: '/quotations/:id',
  INVOICES:         '/invoices',
  INVOICE_NEW:      '/invoices/new',
  INVOICE_EDIT:     '/invoices/:id/edit',
  INVOICE_DETAIL:   '/invoices/:id',
  SETTINGS:         '/settings',
  PUBLIC_QUOTATION: '/public/quotations/:token',
}

// ─── Quotation statuses ──────────────────────────────────────────────────────
export const STATUS = {
  DRAFT: 'DRAFT',
  FINAL: 'FINAL',
}
