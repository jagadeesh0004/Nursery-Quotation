import { clsx } from 'clsx'
import { errorMapper } from '../services'

// Currency formatter
export const formatCurrency = (amount) =>
  `Rs ${Number(amount || 0).toLocaleString('en-IN')}`

// Date formatter
export const formatDate = (date) => {
  if (!date) return '—'
  return new Date(date).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

// Initials from name
export const getInitials = (name = '') =>
  name.trim().split(/\s+/).map(n => n[0]).join('').toUpperCase().slice(0, 2) || 'N'

// Download a Blob as a file
export const downloadBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob)
  const a   = document.createElement('a')
  a.href    = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export const isAuthError = (err) =>
  err?.response?.status === 401 || err?.response?.status === 403

// ─── DEPRECATED: Use errorMapper instead ──────────────────────────────────────
// Extract readable error message from Axios error
export const getError = (err) => {
  if (isAuthError(err)) return null
  return errorMapper.getToastMessage(err)
}

export const formatQuotationId = (nurseryName = '', quotationId) => {
  if (!nurseryName || typeof nurseryName !== 'string') return `#${quotationId}`;
  const words = nurseryName.trim().split(/\s+/);
  let prefix = '';
  if (words.length === 1) {
    prefix = words[0].substring(0, 6).toUpperCase();
  } else {
    prefix = words.map(w => w[0]).join('').substring(0, 6).toUpperCase();
  }
  const paddedId = String(quotationId).padStart(4, '0');
  return `${prefix}${paddedId}`;
}

// Class name utility (re-exported as cx)
export { clsx as cx }
