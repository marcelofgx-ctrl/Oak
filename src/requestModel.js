export const serviceOptions = [
  { id: 'cleaning', title: 'Chimney cleaning', description: 'Seasonal care and soot buildup.', route: 'Service request', detail: 'The team will review your chimney and confirm the scope before scheduling.' },
  { id: 'inspection', title: 'Chimney inspection', description: 'Understand a concern or your chimney’s condition.', route: 'Inspection request', detail: 'The reason for the inspection helps the team determine the appropriate assessment.' },
  { id: 'repair', title: 'Repairs & maintenance', description: 'Masonry, leaks, caps, dampers or visible damage.', route: 'Evaluation request', detail: 'Photos can help with the initial review. A site assessment may be needed before a quote.' },
  { id: 'fireplace', title: 'Fireplace installation', description: 'A new hearth or an update to your space.', route: 'Project consultation', detail: 'Equipment, venting and your space need to be assessed before pricing or scheduling installation.' },
  { id: 'stove', title: 'Stove installation', description: 'Plan a stove and its place in your home.', route: 'Project consultation', detail: 'The team will review your equipment, placement and venting needs before proposing the next step.' },
  { id: 'unsure', title: 'Help me choose', description: 'Tell us what you’re noticing. Start there.', route: 'Advice request', detail: 'Describe the concern so the team can help identify the appropriate service.' },
]
export const emptyRequest = { service: '', appliance: 'Not sure', reason: '', details: '', name: '', phone: '', email: '', address: '', zip: '', contact: 'Phone call', date: '', time: 'Flexible', acknowledge: false }
export function todayLocal(date = new Date()) { return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}` }
export function validateStep(step, data, today = todayLocal()) {
  const errors = {}
  if (step === 0 && !serviceOptions.some(option => option.id === data.service)) errors.service = 'Choose a service to continue.'
  if (step === 1 && !data.details.trim()) errors.details = 'Tell us a little about what you need.'
  if (step === 1 && data.details.length > 3000) errors.details = 'Keep the description within 3,000 characters.'
  if (step === 2) {
    for (const key of ['name','zip',data.contact === 'Email' ? 'email' : 'phone']) if (!data[key].trim()) errors[key] = 'Please complete this field.'
    const digits = data.phone.replace(/\D/g,'')
    if (data.phone && !(digits.length === 10 || (digits.length === 11 && digits.startsWith('1')))) errors.phone = 'Enter a 10-digit US phone number, optionally with +1.'
    if (data.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email)) errors.email = 'Enter a valid email address.'
    if (data.zip && !/^\d{5}(-\d{4})?$/.test(data.zip)) errors.zip = 'Enter a 5-digit ZIP code or ZIP+4.'
    if (data.date && (!/^\d{4}-\d{2}-\d{2}$/.test(data.date) || data.date < today)) errors.date = 'Choose today or a future date, or leave it blank.'
  }
  if (step === 3 && !data.acknowledge) errors.acknowledge = 'Please acknowledge that this is a local demo.'
  return errors
}
export function validatePhotos(files) {
  if (files.length > 5) return 'Choose up to 5 photos in total.'
  if (files.some(file => !['image/jpeg','image/png','image/webp'].includes(file.type))) return 'Use JPG, PNG or WebP photos only.'
  if (files.some(file => file.size <= 0 || file.size > 8 * 1024 * 1024)) return 'Each photo must be non-empty and no larger than 8 MB.'
  return ''
}
