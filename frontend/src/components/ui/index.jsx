/**
 * components/ui/index.jsx
 * Design-system primitives — Poppins font, green-800 primary, green-100 backgrounds.
 */
import { forwardRef } from 'react'
import { createPortal } from 'react-dom'
import { X, AlertTriangle } from 'lucide-react'
import { cx } from '../../utils'

// ── Button ───────────────────────────────────────────────────
export const Button = forwardRef(function Button(
  { children, variant = 'primary', size = 'md', loading = false, className, disabled, ...props },
  ref
) {
  const base = 'inline-flex items-center justify-center gap-2 font-semibold rounded-xl transition-all duration-150 active:scale-95 select-none disabled:opacity-50 disabled:pointer-events-none'
  const variants = {
    primary:   'bg-[#5CB338] hover:bg-[#4d962e] text-white shadow-sm hover:shadow-md',
    secondary: 'bg-white hover:bg-green-50 text-[#5CB338] border border-green-200 shadow-sm hover:shadow-md',
    ghost:     'text-muted hover:bg-green-50 hover:text-[#5CB338]',
    danger:    'bg-red-50 hover:bg-red-100 text-red-600 border border-red-200',
  }
  const sizes = {
    sm:   'text-xs px-3 py-1.5 gap-1.5',
    md:   'text-sm px-4 py-2.5',
    lg:   'text-sm px-5 py-3',
    icon: 'p-2',
  }
  return (
    <button ref={ref} disabled={disabled || loading}
      className={cx(base, variants[variant], sizes[size], className)} {...props}>
      {loading && <Spinner size="sm" className="text-current" />}
      {children}
    </button>
  )
})

// ── Spinner ──────────────────────────────────────────────────
export function Spinner({ size = 'md', className }) {
  const sizes = { sm: 'w-3.5 h-3.5 border-[1.5px]', md: 'w-5 h-5 border-2', lg: 'w-7 h-7 border-2' }
  return <span role="status" className={cx('rounded-full border-current border-t-transparent animate-spin inline-block', sizes[size], className)} />
}

// ── Input ────────────────────────────────────────────────────
export const Input = forwardRef(function Input(
  { id, label, error, hint, required, className, ...props }, ref
) {
  return (
    <div className="space-y-1.5">
      {label && (
        <label htmlFor={id} className="block text-xs font-semibold text-muted tracking-wide">
          {label}{required && <span className="text-red-500 ml-0.5">*</span>}
        </label>
      )}
      <input id={id} ref={ref} aria-invalid={Boolean(error)} className={cx('field', error && 'field-error', className)} {...props} />
      {error && <p className="text-xs text-red-500 mt-1 flex items-center gap-1"><AlertTriangle className="w-3 h-3" />{error}</p>}
      {hint && !error && <p className="text-xs text-muted mt-1">{hint}</p>}
    </div>
  )
})

// ── Textarea ─────────────────────────────────────────────────
export const Textarea = forwardRef(function Textarea(
  { id, label, error, hint, required, className, rows = 3, ...props }, ref
) {
  return (
    <div className="space-y-1.5">
      {label && <label htmlFor={id} className="block text-xs font-semibold text-muted tracking-wide">{label}{required && <span className="text-red-500 ml-0.5">*</span>}</label>}
      <textarea id={id} ref={ref} rows={rows} aria-invalid={Boolean(error)} className={cx('field resize-none', error && 'field-error', className)} {...props} />
      {error && <p className="text-xs text-red-500 mt-1">{error}</p>}
      {hint && !error && <p className="text-xs text-muted mt-1">{hint}</p>}
    </div>
  )
})

// ── Select ───────────────────────────────────────────────────
export const Select = forwardRef(function Select(
  { label, error, children, className, ...props }, ref
) {
  return (
    <div className="space-y-1.5">
      {label && <label className="block text-xs font-semibold text-muted tracking-wide">{label}</label>}
      <select ref={ref} className={cx('field bg-white cursor-pointer', error && 'field-error', className)} {...props}>
        {children}
      </select>
      {error && <p className="text-xs text-red-500 mt-1">{error}</p>}
    </div>
  )
})

// ── Badge ────────────────────────────────────────────────────
export function Badge({ children, variant = 'gray', className }) {
  const variants = {
    green: 'bg-green-100 text-[#5CB338] ring-1 ring-green-200',
    gray:  'bg-gray-100  text-gray-600  ring-1 ring-gray-200',
    amber: 'bg-amber-50  text-amber-700 ring-1 ring-amber-200',
    red:   'bg-red-50    text-red-600   ring-1 ring-red-200',
  }
  return (
    <span className={cx('inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold tracking-wide', variants[variant], className)}>
      {children}
    </span>
  )
}

// ── Skeleton ─────────────────────────────────────────────────
export function Skeleton({ className }) {
  return <div className={cx('skeleton', className)} />
}

// ── Card ─────────────────────────────────────────────────────
export function Card({ children, className, ...props }) {
  return (
    <div className={cx('bg-white rounded-2xl border border-gray-100 shadow-sm', className)} {...props}>
      {children}
    </div>
  )
}

// ── Section card with coloured left border ────────────────────
export function SectionCard({ title, icon, children, className }) {
  return (
    <div className={cx('bg-white rounded-2xl border border-gray-100 shadow-sm overflow-visible', className)}>
      {title && (
        <div className="flex items-center gap-2.5 px-5 py-3.5 bg-green-50 border-b border-green-100">
          {icon && <span className="text-base">{icon}</span>}
          <h3 className="text-sm font-semibold text-[#5CB338]">{title}</h3>
        </div>
      )}
      <div className="p-5">{children}</div>
    </div>
  )
}

// ── EmptyState ───────────────────────────────────────────────
export function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center py-20 px-6 text-center">
      {Icon && (
        <div className="w-16 h-16 rounded-2xl bg-green-50 border border-green-100 flex items-center justify-center mb-5">
          <Icon className="w-7 h-7 text-green-600" strokeWidth={1.5} />
        </div>
      )}
      <p className="font-semibold text-ink mb-2 text-base">{title}</p>
      {description && <p className="text-sm text-muted max-w-xs leading-relaxed mb-6">{description}</p>}
      {action}
    </div>
  )
}

// ── Modal ────────────────────────────────────────────────────
export function Modal({ open, onClose, title, children, footer, width = 'max-w-md' }) {
  if (!open) return null
  const modalContent = (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-black/30 backdrop-blur-[2px] animate-fade-in" onClick={onClose} />
      <div className={cx('relative bg-white rounded-2xl shadow-lg w-full animate-scale-in', width)}>
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <h2 className="text-base font-semibold text-ink">{title}</h2>
          <button onClick={onClose} className="w-7 h-7 rounded-lg bg-gray-100 hover:bg-gray-200 flex items-center justify-center transition-colors">
            <X className="w-3.5 h-3.5 text-gray-500" />
          </button>
        </div>
        <div className="px-5 py-5">{children}</div>
        {footer && <div className="px-5 pb-5">{footer}</div>}
      </div>
    </div>
  )
  return createPortal(modalContent, document.body)
}

// ── ConfirmDialog ────────────────────────────────────────────
export function ConfirmDialog({ open, onClose, onConfirm, title, description, confirmLabel = 'Confirm', danger = false }) {
  return (
    <Modal open={open} onClose={onClose} title={title}
      footer={
        <div className="flex justify-end gap-2.5">
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button
            className={danger ? '!bg-red-600 hover:!bg-red-700 !text-white !border-0' : ''}
            onClick={onConfirm}
          >
            {danger && <AlertTriangle className="w-3.5 h-3.5" />}
            {confirmLabel}
          </Button>
        </div>
      }
    >
      <p className="text-sm text-muted leading-relaxed">{description}</p>
    </Modal>
  )
}

// ── PageHeader ───────────────────────────────────────────────
export function PageHeader({ title, subtitle, action }) {
  return (
    <div className="flex items-start justify-between mb-6">
      <div>
        <h1 className="text-2xl font-bold text-ink">{title}</h1>
        {subtitle && <p className="text-sm text-muted mt-1">{subtitle}</p>}
      </div>
      {action && <div className="flex-shrink-0 ml-4">{action}</div>}
    </div>
  )
}

// ── SearchBar ────────────────────────────────────────────────
export function SearchBar({ value, onChange, placeholder = 'Search...', className }) {
  return (
    <div className={cx('relative', className)}>
      <svg className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
        <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
      </svg>
      <input type="search" value={value} onChange={onChange} placeholder={placeholder}
        className="field pl-10 pr-4 w-full" />
    </div>
  )
}

// ── InfoBox — coloured info/tip banner ───────────────────────
export function InfoBox({ children, variant = 'green' }) {
  const styles = {
    green: 'bg-green-50 border-green-200 text-[#5CB338]',
    amber: 'bg-amber-50 border-amber-200 text-amber-800',
    red:   'bg-red-50   border-red-200   text-red-700',
  }
  return (
    <div className={cx('text-xs leading-relaxed px-4 py-3 rounded-xl border font-medium', styles[variant])}>
      {children}
    </div>
  )
}
