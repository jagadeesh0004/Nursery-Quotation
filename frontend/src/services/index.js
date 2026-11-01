import api from './api'
import { supabase } from '../lib/supabase'
import { compressImage } from '../utils/imageCompressor'
import { toastService } from './toastService'
import { errorMapper } from './errorMapper'

// ─── EXPORTS ──────────────────────────────────────────────────────────────────
export { toastService, errorMapper }

/**
 * profileService — Handle /profile & /profile/upload-logo endpoints
 */
export const profileService = {
  get: () => api.get('/profile', {
    timeout: 15000,  // ✅ Increased from 5s to 15s for Render cold starts
    // Matches other API timeouts for consistency
  }),

    update: (data) =>
      api.put('/profile', {
        // ✅ NOTE: userId is NOT sent - backend extracts it from auth token
        // Backend expects: nurseryName, ownerName, address, phone, email, logoPath
        nurseryName: data.nurseryName,
        ownerName: data.ownerName,
        address: data.address,
        phone: data.phone,
        email: data.email,
        // logoPath is optional, only include if provided
        ...(data.logoPath && { logoPath: data.logoPath }),
      }, { timeout: 60000 }),  // ✅ 60s timeout for first save (backend initializes plants)

    uploadLogo: async (file, profile, userId) => {
      // Compress image
      const compressed = await compressImage(file)

      // Upload to Supabase Storage
      // ✅ Use passed userId instead of profile.userId (which may be undefined)
      const fileName = `${userId}_${Date.now()}.webp`
      const { data, error } = await supabase.storage
        .from('logos')
        .upload(fileName, compressed, { upsert: true })

      if (error) throw error

      const publicUrl = `${import.meta.env.VITE_SUPABASE_URL}/storage/v1/object/public/logos/${fileName}`

       // Save URL to backend
       return api.put('/profile', {
         // ✅ NOTE: userId is NOT sent - backend extracts it from auth token
         logoPath: publicUrl,
         ownerName: profile.ownerName,
         nurseryName: profile.nurseryName,
         address: profile.address,
         phone: profile.phone,
         email: profile.email,
       })
    },
}

/**
 * plantService — Handle /plants & /plants/:id endpoints
 */
export const plantService = {
  list: () => api.get('/plants'),

  create: (name) => api.post('/plants', { name }),

  addDimension: (plantId, dimension, price) =>
    api.post(`/plants/${plantId}/dimensions`, { dimension, price }),

  deletePlant: (plantId) => api.delete(`/plants/${plantId}`),

  deleteDimension: (plantId, dimensionId) =>
    api.delete(`/plants/${plantId}/dimensions/${dimensionId}`),
}

/**
 * quotationService — Handle /quotations endpoints
 */
export const quotationService = {
  list: (filters) => api.get('/quotations', { params: filters }),

  getOne: (id) => api.get(`/quotations/${id}`),

  create: (data) => api.post('/quotations', data),

  update: (id, data) => api.put(`/quotations/${id}`, data),

  duplicate: (id) => api.post(`/quotations/${id}/duplicate`),

  delete: (id) => api.delete(`/quotations/${id}`),

  downloadPdf: (id) =>
    api.get(`/quotations/${id}/pdf`, {
      responseType: 'blob',
      timeout: 30000,
    }),
}

export const customerService = {
  list: (search) => api.get('/customers', { params: search ? { search } : {} }),
  getOne: (id) => api.get(`/customers/${id}`),
  update: (id, data) => api.put(`/customers/${id}`, data),
}

/**
 * invoiceService — Handle /invoices endpoints
 */
export const invoiceService = {
  list: (filters) => api.get('/invoices', { params: filters }),

  getOne: (id) => api.get(`/invoices/${id}`),

  create: (data) => api.post('/invoices', data),

  update: (id, data) => api.put(`/invoices/${id}`, data),

  delete: (id) => api.delete(`/invoices/${id}`),

  importFromQuotation: (quotationId) => api.post(`/invoices/from-quotation/${quotationId}`),

  downloadPdf: (id) => api.get(`/invoices/${id}/pdf`, { responseType: 'blob', timeout: 30000 }),

  getAdvances: (invoiceId) => api.get(`/api/invoices/${invoiceId}/advance`),

  addAdvance: (invoiceId, data) => api.post(`/api/invoices/${invoiceId}/advance`, data),

  updateAdvance: (advanceId, data) => api.put(`/api/advance/${advanceId}`, data),

  deleteAdvance: (advanceId) => api.delete(`/api/advance/${advanceId}`),
}

/**
 * publicService — Handle /public/quotations endpoints (no auth required)
 */
export const publicService = {
  getQuotation: (token) => api.get(`/public/quotations/${token}`),

  downloadPdf: (token) =>
    api.get(`/public/quotations/${token}/pdf`, {
      responseType: 'blob',
      timeout: 30000,
    }),
}
