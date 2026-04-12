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
  get: () => api.get('/profile'),

   update: (data) =>
     api.put('/profile', {
       ownerName: data.ownerName,
       nurseryName: data.nurseryName,
       address: data.address,
       phone: data.phone,
       email: data.email,
     }),

    uploadLogo: async (file, profile) => {
      // Compress image
      const compressed = await compressImage(file)

      // Upload to Supabase Storage
      const fileName = `${profile.userId}_${Date.now()}.webp`
      const { data, error } = await supabase.storage
        .from('logos')
        .upload(fileName, compressed, { upsert: true })

      if (error) throw error

      const publicUrl = `${import.meta.env.VITE_SUPABASE_URL}/storage/v1/object/public/logos/${fileName}`

      // Save URL to backend
      return api.put('/profile', {
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

  getPdf: (id) =>
    api.get(`/quotations/${id}/pdf`, {
      responseType: 'blob',
      timeout: 30000,
    }),

  downloadPdf: (id) =>
    api.get(`/quotations/${id}/pdf`, {
      responseType: 'blob',
      timeout: 30000,
    }),

  trackView: (token) => api.post(`/public/quotations/${token}/track-view`),
}

/**
 * publicService — Handle /public/quotations endpoints (no auth required)
 */
export const publicService = {
  getQuotation: (token) => api.get(`/public/quotations/${token}`),

  getPdf: (token) =>
    api.get(`/public/quotations/${token}/pdf`, {
      responseType: 'blob',
      timeout: 30000,
    }),

  downloadPdf: (token) =>
    api.get(`/public/quotations/${token}/pdf`, {
      responseType: 'blob',
      timeout: 30000,
    }),
}
