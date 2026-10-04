import { useEffect, useRef, useState } from 'react'
import { ArrowLeft, ArrowRight, Check, Upload, X } from 'lucide-react'
import { emptyRequest, serviceOptions, validatePhotos, validateStep } from './requestModel'
import { sendRequest } from './backend/client'
const steps = ['Service', 'Your hearth', 'Contact', 'Review']

export default function RequestWizard() {
  const [step, setStep] = useState(0)
  const [data, setData] = useState({ ...emptyRequest })
  const [errors, setErrors] = useState({})
  const [photos, setPhotos] = useState([])
  const [photoError, setPhotoError] = useState('')
  const [status, setStatus] = useState('idle')
  const heading = useRef(null)
  const moved = useRef(false)
  const photoResources = useRef([])
  const requestId = useRef(crypto.randomUUID())
  const [reference, setReference] = useState('')
  const [sendError, setSendError] = useState('')
  const selected = serviceOptions.find(service => service.id === data.service)
  useEffect(() => {
    if (moved.current) heading.current?.focus()
  }, [step, status])
  useEffect(() => {
    function select(event) {
      const service = serviceOptions.find(option => option.id === event.detail)
      if (!service) return
      setData(previous => ({ ...previous, service: service.id }))
      setStep(0); setStatus('idle'); setErrors({}); moved.current = true
      requestAnimationFrame(() => heading.current?.focus())
    }
    window.addEventListener('oak-select-service', select)
    return () => window.removeEventListener('oak-select-service', select)
  }, [])
  useEffect(() => () => { photoResources.current.forEach(photo => URL.revokeObjectURL(photo.url)) }, [])
  const update = event => {
    const { name, value, checked, type } = event.target
    setData(previous => ({ ...previous, [name]: type === 'checkbox' ? checked : value }))
    setErrors(previous => ({ ...previous, [name]: undefined }))
  }
  const error = key => errors[key] && <p className="field-error" id={`request-${key}-error`}>{errors[key]}</p>
  const attrs = key => ({ id: `request-${key}`, name: key, value: data[key], onChange: update, 'aria-invalid': !!errors[key], 'aria-describedby': errors[key] ? `request-${key}-error` : undefined })
  const field = (key, label, type = 'text', extra = {}) => <div className="field"><label htmlFor={`request-${key}`}>{label}</label><input {...attrs(key)} type={type} {...extra}/>{error(key)}</div>
  const select = (key, label, options) => <div className="field"><label htmlFor={`request-${key}`}>{label}</label><select {...attrs(key)}>{options.map(option => <option key={option}>{option}</option>)}</select>{error(key)}</div>
  function addPhotos(event) {
    const files = Array.from(event.target.files)
    const message = validatePhotos([...photos.map(photo => photo.file), ...files])
    setPhotoError(message)
    if (!message) {
      const added = files.map(file => ({ file, url: URL.createObjectURL(file), id: crypto.randomUUID() }))
      const next = [...photos, ...added]; photoResources.current = next; setPhotos(next)
    }
    event.target.value = ''
  }
  function removePhoto(id) {
    const removed = photos.find(photo => photo.id === id)
    URL.revokeObjectURL(removed.url)
    const next = photos.filter(photo => photo.id !== id); photoResources.current = next; setPhotos(next); setPhotoError('')
  }
  function clearPhotos() { photoResources.current.forEach(photo => URL.revokeObjectURL(photo.url)); photoResources.current = []; setPhotos([]) }
  function move(next) { moved.current = true; setErrors({}); setStep(next) }
  async function submit(event) {
    event.preventDefault()
    if (status === 'loading') return
    const next = validateStep(step, data); setErrors(next)
    if (Object.keys(next).length) { requestAnimationFrame(() => document.getElementById(`request-${Object.keys(next)[0]}`)?.focus()); return }
    if (step < 3) { move(step + 1); return }
    moved.current = true; setStatus('loading'); setSendError('')
    try {
      const receipt = await sendRequest(data, photos, requestId.current)
      setReference(receipt); clearPhotos(); setData({ ...emptyRequest }); setStatus('success')
    } catch (error) { setSendError(error.message); setStatus('idle') }
  }
  if (status === 'success') return <div className="wizard-success"><h3 ref={heading} tabIndex={-1}><Check aria-hidden="true"/> Request received.</h3><p role="status">Your request and selected photos were saved securely for the team to review.</p><p>Reference: <strong>{reference.slice(0,8).toUpperCase()}</strong></p><p>This is a request for contact. No price, payment or appointment has been confirmed. The team will contact you using your preferred method.</p><button type="button" className="button" onClick={() => { requestId.current = crypto.randomUUID(); move(0); setStatus('idle') }}>Make another request <ArrowRight aria-hidden="true"/></button></div>
  return <form className="request-wizard" onSubmit={submit} noValidate aria-busy={status === 'loading'}>
    <div className="wizard-demo"><strong>REQUEST AN ESTIMATE</strong><span>Your request goes to the Oak &amp; Ember team. No appointment is booked.</span></div>
    <ol className="wizard-progress" aria-label="Request progress">{steps.map((label, index) => <li key={label} aria-current={index === step ? 'step' : undefined} className={index <= step ? 'reached' : ''}><span aria-hidden="true">{index < step ? '✓' : index + 1}</span><small>{label}</small></li>)}</ol>
    <p className="eyebrow">STEP {step + 1} OF 4</p>
    <h3 className="wizard-title" ref={heading} tabIndex={-1}>{['What does your home need?', 'Tell us about your hearth.', 'How can we reach you?', 'A final look before you finish.'][step]}</h3>
    {Object.values(errors).some(Boolean) && <p className="error-summary" role="alert">Please check the highlighted fields.</p>}
    {step === 0 && <fieldset className="service-choices" id="request-service" tabIndex={-1} aria-describedby={errors.service ? 'request-service-error' : undefined}><legend className="sr-only">Choose a service</legend>{serviceOptions.map(option => <label key={option.id} className={`service-choice ${data.service === option.id ? 'selected' : ''}`}><input type="radio" name="service" value={option.id} checked={data.service === option.id} onChange={update}/><span><strong>{option.title}</strong><small>{option.description}</small></span></label>)}{error('service')}<p className="field-help">Not for emergencies. If there is an active fire or immediate danger, call 911.</p></fieldset>}
    {step === 1 && <fieldset><legend className="sr-only">Project details</legend><div className="route-note"><strong>{selected?.route}</strong><p>{selected?.detail}</p></div>{select('appliance', 'What do you have?', ['Not sure', 'Wood-burning fireplace', 'Gas fireplace', 'Wood stove', 'Gas stove', 'No existing fireplace or stove'])}{data.service === 'inspection' && select('reason','Why are you requesting an inspection?', ['','Routine check','Buying or selling a home','A concern or recent change','Other'])}<div className="field"><label htmlFor="request-details">What can we help with? *</label><textarea {...attrs('details')} rows={4} maxLength={3000} placeholder={['fireplace','stove'].includes(data.service) ? 'Tell us about the room, equipment and project you have in mind.' : 'Describe any concerns, recent changes or work you would like.'}/>{error('details')}</div><div className="wizard-upload"><label htmlFor="request-photos"><Upload aria-hidden="true"/><strong>Add helpful photos</strong><small>Camera or gallery · JPG, PNG, WebP · Up to 5 · 8 MB each</small></label><input id="request-photos" type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={addPhotos} aria-invalid={!!photoError} aria-describedby="photo-help photo-error"/></div><p className="field-help" id="photo-help">Photos are previewed locally until you submit. Submitting sends them securely with your request; only authorized team members can access them. Avoid including personal documents or people.</p><p className="field-error" id="photo-error" role="alert">{photoError}</p><p className="field-help" aria-live="polite">{photos.length} of 5 photos selected</p><ul className="wizard-photos">{photos.map(photo => <li key={photo.id}><img src={photo.url} alt={`Selected: ${photo.file.name}`} onError={() => setPhotoError('One photo could not be displayed. Remove it and choose a different image.')}/><span>{photo.file.name}</span><button type="button" onClick={() => removePhoto(photo.id)} aria-label={`Remove ${photo.file.name}`}><X aria-hidden="true"/></button></li>)}</ul></fieldset>}
    {step === 2 && <fieldset><legend className="sr-only">Contact for an estimate</legend><p className="field-help">Fields marked * are required. We’ll discuss the project before arranging a visit.</p><div className="form-grid">{field('name','Full name *','text',{ autoComplete:'name',maxLength:100 })}{field('phone',data.contact === 'Phone call' ? 'Phone *' : 'Phone (optional)','tel',{ autoComplete:'tel',maxLength:30 })}{field('email',data.contact === 'Email' ? 'Email *' : 'Email (optional)','email',{ autoComplete:'email',maxLength:254 })}{field('zip','ZIP code *','text',{ autoComplete:'postal-code',inputMode:'numeric',maxLength:10 })}</div>{field('address','Service address, including city (optional)','text',{ autoComplete:'street-address',maxLength:250 })}<p className="field-help">Coverage has not been confirmed. Entering a ZIP does not establish service availability.</p><div className="form-grid">{select('contact','Preferred contact method',['Phone call','Email'])}{select('time','Best time to contact you (optional)',['Flexible','Morning','Afternoon'])}</div></fieldset>}
    {step === 3 && <fieldset><legend className="sr-only">Review your request</legend><div className="route-note"><strong>{selected?.route}</strong><p>No price or appointment is confirmed. The team will confirm scope, coverage and availability with you.</p></div><div className="review-group"><h4>Your service <button type="button" onClick={() => move(0)}>Edit service</button></h4><p>{selected?.title}</p></div><div className="review-group"><h4>Your hearth <button type="button" onClick={() => move(1)}>Edit details</button></h4><p>{data.appliance}{data.reason && ` · ${data.reason}`}</p><p className="review-description">{data.details}</p><p>{photos.length} selected photo{photos.length !== 1 ? 's' : ''}</p></div><div className="review-group"><h4>Contact & timing <button type="button" onClick={() => move(2)}>Edit contact</button></h4><p>{data.name}<br/>{data.phone || 'No phone provided'}<br/>{data.email || 'No email provided'}<br/>{data.address || 'Address to be discussed'} · {data.zip}</p><p>Contact: {data.contact} · {data.time}</p></div><label className="wizard-consent"><input id="request-acknowledge" name="acknowledge" type="checkbox" checked={data.acknowledge} onChange={update} aria-invalid={!!errors.acknowledge} aria-describedby={errors.acknowledge ? 'request-acknowledge-error' : undefined}/><span>I agree to sharing these details and photos with Oak & Ember so the team can contact me about this request. This does not book an appointment. *</span></label>{error('acknowledge')}</fieldset>}
    {sendError && <p className="error-summary" role="alert">{sendError}</p>}<div className="wizard-actions">{step > 0 && <button className="wizard-back" type="button" disabled={status === 'loading'} onClick={() => move(step - 1)}><ArrowLeft aria-hidden="true"/> Back</button>}<button type="submit" className="button" disabled={status === 'loading'}>{status === 'loading' ? 'Sending request…' : step === 3 ? 'Send request' : 'Continue'}<ArrowRight aria-hidden="true"/></button></div><p className="submit-note">No account required · No payment · No confirmed booking</p>
  </form>
}
