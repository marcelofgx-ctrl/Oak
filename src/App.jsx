import { useRef, useState } from 'react'
import { ArrowRight, CalendarDays, Check, ChevronDown, ClipboardCheck, Flame, Hammer, Home, Menu, SearchCheck, ShieldCheck, Sparkles, Upload, X } from 'lucide-react'

const services = [
  { icon: Sparkles, title: 'Chimney Cleaning', text: 'Thoughtful removal of soot and buildup, with care for the hearth and surrounding home.' },
  { icon: SearchCheck, title: 'Chimney Inspection', text: 'A careful visual assessment to help identify visible concerns and practical next steps.' },
  { icon: Hammer, title: 'Repair & Maintenance', text: 'Maintenance and repair planning for masonry, caps, dampers and other chimney components.' },
  { icon: Home, title: 'Fireplace Installation', text: 'Fireplace options considered around your space, preferences and the character of your home.' },
  { icon: Flame, title: 'Stove Installation', text: 'Wood and gas stove installation planning with placement and everyday comfort in mind.' },
]

const gallery = [
  { src: '/images/hearth-concept.svg', alt: 'Concept illustration of a warmly lit stone fireplace', label: 'Warm gathering place', className: 'wide' },
  { src: '/images/chimney-concept.svg', alt: 'Concept illustration of a brick chimney on a Georgia home', label: 'Home from the outside in' },
  { src: '/images/stove-concept.svg', alt: 'Concept illustration of a freestanding stove with a glowing fire', label: 'Comfort, considered' },
]

function Logo() { return <a className="logo" href="#top" aria-label="Oak and Ember home"><img src="/logo.svg" alt="Oak & Ember — Chimney & Fireplace Services" /></a> }

function Header() {
  const [open, setOpen] = useState(false)
  const close = () => setOpen(false)
  return <header className="site-header">
    <div className="nav-wrap"><Logo />
      <button className="menu-button" aria-expanded={open} aria-controls="site-nav" onClick={() => setOpen(!open)}>{open ? <X /> : <Menu />}<span className="sr-only">{open ? 'Close menu' : 'Open menu'}</span></button>
      <nav id="site-nav" className={open ? 'nav open' : 'nav'} aria-label="Primary navigation">
        <a onClick={close} href="#services">Services</a><a onClick={close} href="#about">Our approach</a><a onClick={close} href="#gallery">Inspiration</a><a onClick={close} href="#process">What to expect</a><a onClick={close} className="button nav-cta" href="#request">Request service</a>
      </nav>
    </div>
  </header>
}

function ServiceForm() {
  const [errors, setErrors] = useState({})
  const [status, setStatus] = useState('idle')
  const formRef = useRef(null)
  const validate = (form) => {
    const data = new FormData(form); const next = {}
    ;['name','phone','email','address','zip','service','details','date','time'].forEach(k => { if (!String(data.get(k) || '').trim()) next[k] = 'Please complete this field.' })
    if (data.get('email') && !/^\S+@\S+\.\S+$/.test(data.get('email'))) next.email = 'Enter a valid email address.'
    if (data.get('phone') && String(data.get('phone')).replace(/\D/g,'').length < 10) next.phone = 'Enter a 10-digit phone number.'
    if (data.get('zip') && !/^\d{5}$/.test(data.get('zip'))) next.zip = 'Enter a 5-digit ZIP code.'
    return next
  }
  const submit = (e) => {
    e.preventDefault(); const next = validate(e.currentTarget); setErrors(next)
    if (Object.keys(next).length) { setStatus('error'); requestAnimationFrame(() => document.querySelector('[aria-invalid="true"]')?.focus()); return }
    setStatus('loading'); window.setTimeout(() => setStatus('success'), 900)
  }
  const field = (name, label, type='text', props={}) => <div className="field"><label htmlFor={name}>{label}<span aria-hidden="true"> *</span></label><input id={name} name={name} type={type} aria-invalid={!!errors[name]} aria-describedby={errors[name] ? `${name}-error` : undefined} {...props}/>{errors[name] && <p className="field-error" id={`${name}-error`}>{errors[name]}</p>}</div>
  if (status === 'success') return <div className="success" role="status"><span className="success-icon"><Check /></span><p className="eyebrow">Demo complete</p><h3>Your request was tested successfully.</h3><p>No information or photos were sent or saved, and no appointment was made. When the real service is connected, a team member would follow up to confirm details and availability.</p><button className="text-button" onClick={() => { setStatus('idle'); setErrors({}); formRef.current?.reset() }}>Start another demo request <ArrowRight /></button></div>
  return <form ref={formRef} onSubmit={submit} noValidate>
    <div className="demo-note"><ShieldCheck /><div><strong>Demo request form</strong><p>This form is for demonstration only. It will not send or save your information, upload photos, or confirm an appointment.</p></div></div>
    {status === 'error' && <div className="error-summary" role="alert"><strong>There are a few details to review.</strong><span>Check the highlighted fields below.</span></div>}
    <fieldset><legend>About you and your home</legend><div className="form-grid">{field('name','Full name')}{field('phone','Phone','tel',{autoComplete:'tel',placeholder:'(000) 000-0000'})}{field('email','Email','email',{autoComplete:'email'})}{field('address','Service address','text',{autoComplete:'street-address'})}{field('zip','ZIP code','text',{inputMode:'numeric',maxLength:5,autoComplete:'postal-code'})}<div className="field"><label htmlFor="service">Service needed <span aria-hidden="true">*</span></label><select id="service" name="service" defaultValue="" aria-invalid={!!errors.service} aria-describedby={errors.service ? 'service-error':undefined}><option value="" disabled>Select a service</option>{services.map(s=><option key={s.title}>{s.title}</option>)}<option>Not sure yet</option></select><ChevronDown aria-hidden="true" className="select-arrow"/>{errors.service && <p className="field-error" id="service-error">{errors.service}</p>}</div></div></fieldset>
    <fieldset><legend>Tell us what you have in mind</legend><div className="field"><label htmlFor="details">Description <span aria-hidden="true">*</span></label><textarea id="details" name="details" rows="5" placeholder="What are you noticing, planning or hoping to improve?" aria-invalid={!!errors.details} aria-describedby={errors.details ? 'details-error':'details-help'}></textarea><p className="field-help" id="details-help">Please don’t include sensitive personal information.</p>{errors.details && <p className="field-error" id="details-error">{errors.details}</p>}</div><div className="form-grid">{field('date','Preferred date','date')}<div className="field"><label htmlFor="time">Preferred time window <span aria-hidden="true">*</span></label><select id="time" name="time" defaultValue="" aria-invalid={!!errors.time}><option value="" disabled>Choose a time window</option><option>Morning</option><option>Afternoon</option><option>Flexible</option></select><ChevronDown aria-hidden="true" className="select-arrow"/>{errors.time && <p className="field-error">{errors.time}</p>}</div></div>
    <div className="field upload-field"><span className="label">Photos <span className="optional">Optional · demo only</span></span><label className="upload" htmlFor="photos"><Upload/><span><strong>Choose photos</strong><small>Photos are not uploaded or saved in demo mode.</small></span></label><input id="photos" name="photos" type="file" accept="image/*" multiple disabled /></div></fieldset>
    <button className="button submit" disabled={status==='loading'}>{status==='loading' ? <><span className="spinner"/>Testing request…</> : <>Test request <ArrowRight /></>}</button><p className="submit-note">Submitting this demo does not create an appointment. Future scheduling will require follow-up confirmation.</p>
  </form>
}

export default function App() {
  return <><a className="skip-link" href="#main">Skip to content</a><Header/><main id="main">
    <section className="hero" id="top"><div className="hero-art" role="img" aria-label="A warm conceptual fireplace scene"></div><div className="hero-shade"></div><div className="hero-content"><p className="eyebrow light">Chimney & fireplace services · Atlanta, Georgia</p><h1>Safe chimneys.<br/><em>Warm homes.</em></h1><p className="hero-copy">Considered care for the place your family gathers—from seasonal upkeep to a hearth made new.</p><div className="hero-actions"><a className="button copper" href="#request">Request service <ArrowRight /></a><a className="button ghost" href="#services">Explore services</a></div><div className="hero-trust"><span><ShieldCheck/>Safety-minded service</span><span><Home/>Care for your home</span></div></div><a className="scroll" href="#services">Discover <span>↓</span></a></section>
    <section className="intro section" id="services"><div className="section-heading"><div><p className="eyebrow">Care for every kind of hearth</p><h2>From chimney top<br/>to fireside.</h2></div><p>Whether you’re preparing for the season, addressing a concern or imagining a new gathering place, we begin by listening.</p></div><div className="services-grid">{services.map(({icon:Icon,title,text},i)=><article className={`service-card ${i===0?'featured':''}`} key={title}><span className="service-number">0{i+1}</span><Icon/><h3>{title}</h3><p>{text}</p><a href="#request" aria-label={`Request ${title}`}>Request this service <ArrowRight/></a></article>)}</div></section>
    <section className="georgia" id="about"><div className="georgia-image"><img src="/images/chimney-concept.svg" alt="Concept illustration of a home and chimney among Georgia-inspired green hills"/><span className="concept-tag">Conceptual image</span></div><div className="georgia-copy"><p className="eyebrow light">Rooted in Georgia</p><h2>Local care, with your home at heart.</h2><p>Oak & Ember is a family business being built to serve the greater Atlanta community. We believe good service should feel clear, respectful and personal—from the first conversation to the final walkthrough.</p><ul><li><Check/>A careful, home-conscious approach</li><li><Check/>Clear next steps, without pressure</li><li><Check/>Recommendations shaped around your needs</li></ul><a className="text-button light" href="#process">See what to expect <ArrowRight/></a></div></section>
    <section className="gallery section" id="gallery"><div className="section-heading"><div><p className="eyebrow">Hearth inspiration</p><h2>Spaces made<br/>for gathering.</h2></div><p>Visual direction for the warmth, materials and craftsmanship we value. These conceptual images are inspiration only—not completed Oak & Ember projects.</p></div><div className="gallery-grid">{gallery.map(item=><figure className={item.className||''} key={item.label}><img src={item.src} alt={item.alt}/><figcaption><span>Conceptual image</span><strong>{item.label}</strong></figcaption></figure>)}</div></section>
    <section className="process section" id="process"><div className="process-intro"><p className="eyebrow light">Simple by design</p><h2>A clear path<br/>to a warmer home.</h2><p>Share what you need, tell us what works for your household, and we’ll follow up before anything is scheduled.</p></div><ol><li><span>01</span><ClipboardCheck/><div><h3>Send a request</h3><p>Tell us about your home, service needs and any concerns you’ve noticed.</p></div></li><li><span>02</span><CalendarDays/><div><h3>Share preferences</h3><p>Suggest a date and time window that suits your routine. It’s a preference, not a booking.</p></div></li><li><span>03</span><ShieldCheck/><div><h3>Confirm together</h3><p>A team member would review the details and follow up to confirm scope and availability.</p></div></li></ol></section>
    <section className="request section" id="request"><div className="request-heading"><p className="eyebrow">Request service</p><h2>Tell us about<br/>your hearth.</h2><p>Use this demo to explore the planned request experience. No data leaves your browser.</p><div className="privacy-callout"><ShieldCheck/><span><strong>Privacy by design</strong>Backend-ready structure, with no active storage or transfer.</span></div></div><ServiceForm/></section>
  </main><footer><div className="footer-main"><Logo/><p>Thoughtful chimney and fireplace care for warm, welcoming homes.</p><nav aria-label="Footer navigation"><a href="#services">Services</a><a href="#about">Our approach</a><a href="#gallery">Inspiration</a><a href="#request">Request service</a></nav></div><div className="footer-bottom"><span>© {new Date().getFullYear()} Oak & Ember. Provisional brand.</span><span>Serving the greater Atlanta, Georgia area · Exact coverage to be confirmed.</span></div></footer></>
}
