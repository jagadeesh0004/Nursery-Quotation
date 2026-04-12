import { useState, useEffect, useRef } from 'react'
import { Camera, LogOut } from 'lucide-react'
import toast from 'react-hot-toast'
import { useNavigate } from 'react-router-dom'
import { profileService } from '../services'
import { useAuth } from '../context/AuthContext'
import { Button, Input, Textarea, PageHeader, SectionCard } from '../components/ui'
import { getError } from '../utils'

export default function SettingsPage() {
  const { profile, user, fetchProfile, logout } = useAuth()
  const navigate = useNavigate()

  const [ownerName, setOwnerName]     = useState('')
  const [nurseryName, setNurseryName] = useState('')
  const [address, setAddress]         = useState('')
  const [phone, setPhone]             = useState('')
  const [saving, setSaving]           = useState(false)
  const [errors, setErrors]           = useState({})
  const [isEditing, setIsEditing]     = useState(false)
  const [logoUploading, setLogoUploading] = useState(false)

  const fileRef = useRef()

  // Single source of truth: profile from AuthContext.
  // logoPreview is derived here — never set manually after an upload.
  const logoPreview =
    profile?.logoPath && profile.logoPath.startsWith('http')
      ? profile.logoPath
      : null

  useEffect(() => {
    if (!profile) return
    setOwnerName(profile.ownerName   || '')
    setNurseryName(profile.nurseryName || '')
    setAddress(profile.address       || '')
    setPhone(profile.phone           || '')

    if (!profile.nurseryName) {
      setIsEditing(true)
    }
  }, [profile])

  function validate() {
    const errs = {}
    if (!ownerName.trim())   errs.ownerName   = 'Owner name is required'
    if (!nurseryName.trim()) errs.nurseryName = 'Nursery name is required'
    if (!address.trim())     errs.address     = 'Address is required'
    if (!phone.trim())       errs.phone       = 'Phone number is required'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

   async function handleSave(e) {
     e.preventDefault()
     if (!validate()) return
     setSaving(true)
     try {
       await profileService.update({
         ownerName:   ownerName.trim(),
         nurseryName: nurseryName.trim(),
         address:     address.trim(),
         phone:       phone.trim(),
         email:       user?.email || '',
       })
       await fetchProfile()
       toast.success('Profile saved!')
       setIsEditing(false)
     } catch (err) {
       const message = getError(err)
       if (message) toast.error(message)
     } finally {
       setSaving(false)
     }
   }

   async function handleLogoChange(e) {
     const file = e.target.files?.[0]
     if (!file) return

     if (!profile?.nurseryName) {
       toast.error('Please fill and save your nursery name first before uploading a logo.')
       return
     }

     setLogoUploading(true)
     const loadingToast = toast.loading('Compressing and uploading...')

     try {
       // Upload to Supabase and save URL via PUT /profile in one call
       const latestProfileReq = await profileService.get()
       const latestProfile = latestProfileReq.data?.data || profile

       await profileService.uploadLogo(file, latestProfile)

       // Refresh profile from backend — logoPreview is derived from this,
       // so the image will update automatically via the derived value above
       await fetchProfile()

       toast.dismiss(loadingToast)
       toast.success('Logo updated!')
     } catch (err) {
       toast.dismiss(loadingToast)
       const message = getError(err)
       if (message) toast.error(message)
     } finally {
       setLogoUploading(false)
       // Reset so the same file can be selected again
       if (fileRef.current) fileRef.current.value = ''
     }
   }

  function handleLogout() {
    if (!window.confirm('Are you sure you want to sign out?')) return
    logout()
    toast.success('Logged out successfully')
    navigate('/login')
  }

  function handleCancelEdit() {
    setIsEditing(false)
    setOwnerName(profile?.ownerName   || '')
    setNurseryName(profile?.nurseryName || '')
    setAddress(profile?.address       || '')
    setPhone(profile?.phone           || '')
    setErrors({})
  }

  return (
    <div className="max-w-3xl space-y-6 pb-12">
      <PageHeader
        title="Settings"
        subtitle="Manage your nursery profile and preferences."
      />

      <SectionCard title="Nursery Profile">
        {!isEditing && (
          <div className="flex justify-end mb-4">
            <Button variant="secondary" size="sm" onClick={() => setIsEditing(true)}>
              Edit Profile
            </Button>
          </div>
        )}

        <div className="flex items-center sm:items-start gap-6 flex-col sm:flex-row mb-6 pb-6 border-b border-gray-100">
          {/* Logo circle */}
          <div className="relative flex-shrink-0">
            <div className="w-24 h-24 rounded-full border border-gray-200 bg-gray-50 flex items-center justify-center overflow-hidden shadow-sm">
              {logoPreview ? (
                <img
                  src={logoPreview}
                  alt="Nursery logo"
                  className="w-full h-full object-cover"
                  onError={(e) => { e.target.style.display = 'none' }}
                />
              ) : (
                <span className="text-3xl text-gray-300">●</span>
              )}
            </div>

            {isEditing && (
              <button
                onClick={() => fileRef.current?.click()}
                disabled={logoUploading}
                className="absolute bottom-0 right-0 w-9 h-9 rounded-full border-2 border-white bg-[#5CB338] hover:bg-[#4d962e] flex items-center justify-center text-white transition-colors disabled:opacity-50 shadow-sm"
                title="Upload logo (optional)"
              >
                {logoUploading ? (
                  <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <Camera className="w-4 h-4" />
                )}
              </button>
            )}

            <input
              ref={fileRef}
              type="file"
              accept="image/jpeg, image/png, image/gif, image/webp"
              className="hidden"
              onChange={handleLogoChange}
            />
          </div>

          {/* Profile summary / change-logo link */}
          <div className="flex-1 mt-2 sm:mt-0 w-full flex flex-col items-center sm:items-start text-center sm:text-left">
            <p className="font-semibold text-ink text-lg">{nurseryName || 'Your Nursery'}</p>
            <p className="text-sm text-muted font-mono mt-1">{phone || 'No phone number'}</p>

            {isEditing ? (
              <button
                onClick={() => fileRef.current?.click()}
                className="text-sm text-[#5CB338] hover:text-[#4d962e] font-semibold mt-3 transition-colors"
              >
                Change logo
              </button>
            ) : (
              <div className="mt-4 w-full space-y-1.5 p-4 rounded-lg bg-gray-50 border border-gray-200 text-left">
                <p className="text-sm text-muted">
                  <span className="font-semibold text-ink">Owner:</span>{' '}
                  {ownerName || 'Not provided'}
                </p>
                <p className="text-sm text-muted">
                  <span className="font-semibold text-ink">Address:</span>{' '}
                  {address || 'Not provided'}
                </p>
              </div>
            )}
          </div>
        </div>

        {isEditing && (
          <form onSubmit={handleSave} className="space-y-5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <Input
                label="Owner Name"
                placeholder="Your full name"
                required
                value={ownerName}
                onChange={e => setOwnerName(e.target.value)}
                error={errors.ownerName}
              />
              <Input
                label="Nursery Name"
                placeholder="Business name"
                required
                value={nurseryName}
                onChange={e => setNurseryName(e.target.value)}
                error={errors.nurseryName}
              />
            </div>
            <Input
              label="Phone Number"
              type="tel"
              placeholder="Enter your phone number"
              required
              value={phone}
              onChange={e => setPhone(e.target.value)}
              error={errors.phone}
            />
            <Textarea
              label="Business Address"
              placeholder="Full address — appears on every quotation"
              required
              value={address}
              onChange={e => setAddress(e.target.value)}
              rows={3}
              error={errors.address}
            />

            <div className="bg-blue-50 border border-blue-100 rounded-lg p-4 mt-4">
              <p className="text-sm text-blue-700 font-medium">
                These details appear in the header of every PDF quotation and on the public
                share page. The logo is optional.
              </p>
            </div>

            <div className="flex flex-col-reverse sm:flex-row justify-end gap-3 pt-5 mt-2 border-t border-gray-100">
              {profile?.nurseryName && (
                <Button
                  type="button"
                  variant="secondary"
                  className="w-full sm:w-auto justify-center"
                  onClick={handleCancelEdit}
                >
                  Cancel
                </Button>
              )}
              <Button
                type="submit"
                loading={saving}
                className="w-full sm:w-auto justify-center"
                style={{ backgroundColor: '#5CB338' }}
              >
                Save Profile
              </Button>
            </div>
          </form>
        )}
      </SectionCard>

      <SectionCard title="Preferences">
        <div className="space-y-5">
          <div className="flex items-start sm:items-center justify-between gap-4">
            <div className="flex-1">
              <p className="font-semibold text-ink">Quotation Notifications</p>
              <p className="text-sm text-muted mt-0.5">
                Get notified when a customer views your quotation
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer flex-shrink-0 mt-1 sm:mt-0">
              <input type="checkbox" defaultChecked className="sr-only peer" />
              <div className="w-10 h-6 bg-gray-200 peer-focus:ring-2 peer-focus:ring-blue-500/20 rounded-full peer peer-checked:bg-[#5CB338] after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:after:translate-x-4 peer-checked:after:border-white" />
            </label>
          </div>

          <div className="flex items-start sm:items-center justify-between pt-5 border-t border-gray-100 gap-4">
            <div className="flex-1">
              <p className="font-semibold text-ink">PDF Caching</p>
              <p className="text-sm text-muted mt-0.5">Cache PDFs for faster re-downloads</p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer flex-shrink-0 mt-1 sm:mt-0">
              <input type="checkbox" defaultChecked className="sr-only peer" />
              <div className="w-10 h-6 bg-gray-200 peer-focus:ring-2 peer-focus:ring-blue-500/20 rounded-full peer peer-checked:bg-[#5CB338] after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:after:translate-x-4 peer-checked:after:border-white" />
            </label>
          </div>

          <div className="flex items-start sm:items-center justify-between pt-5 border-t border-gray-100 gap-4">
            <div className="flex-1">
              <p className="font-semibold text-ink">Account Security</p>
              <p className="text-sm text-muted mt-0.5">Google OAuth authentication</p>
            </div>
            <span className="text-[11px] sm:text-xs font-bold text-[#5CB338] bg-green-100 px-2.5 sm:px-3 py-1 rounded-md shrink-0 mt-1 sm:mt-0">
              Active
            </span>
          </div>
        </div>
      </SectionCard>

      <SectionCard title="About">
        <p className="text-sm text-ink">
          <span className="font-bold">QuotoNursery v1.0</span>
        </p>
        <p className="text-sm text-muted mt-2 leading-relaxed">
          A professional quotation management system for nursery businesses. Create, manage,
          and share quotations with your customers.
        </p>

        <div className="mt-8 pt-6 border-t border-gray-100">
          <h2 className="text-base font-semibold text-gray-900 mb-2">Sign Out</h2>
          <p className="text-sm text-muted mb-5">
            You will need to log in again with your Google account to access your account.
          </p>
          <div className="flex">
            <Button
              variant="danger"
              className="w-full sm:w-auto justify-center"
              onClick={handleLogout}
            >
              <LogOut className="w-4 h-4 mr-2" />
              Sign Out
            </Button>
          </div>
        </div>
      </SectionCard>
    </div>
  )
}