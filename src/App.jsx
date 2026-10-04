import { useState } from 'react'
import { ArrowRight, CalendarDays, Check, ClipboardCheck, Flame, Hammer, Home, Menu, SearchCheck, ShieldCheck, Sparkles, X } from 'lucide-react'

import RequestWizard from './RequestWizard'
import PhotoGallery from './PhotoGallery'
import './request.css'

const services = [
  { icon: Sparkles, title: 'Chimney Cleaning', text: 'Thoughtful removal of soot and buildup, with care for the hearth and surrounding home.' },
  { icon: SearchCheck, title: 'Chimney Inspection', text: 'A careful visual assessment to help identify visible concerns and practical next steps.' },
  { icon: Hammer, title: 'Repair & Maintenance', text: 'Maintenance and repair planning for masonry, caps, dampers and other chimney components.' },
  { icon: Home, title: 'Fireplace Installation', text: 'Fireplace options considered around your space, preferences and the character of your home.' },
  { icon: Flame, title: 'Stove Installation', text: 'Wood and gas stove installation planning with placement and everyday comfort in mind.' },
]

function Logo() { return <a className="logo" href="#top" aria-label="Oak and Ember home"><img src="/logo.svg" alt="Oak & Ember — Chimney & Fireplace Services" /></a> }

function Header() {
  const [open, setOpen] = useState(false)
  const close = () => setOpen(false)
  return <header className="site-header">
    <div className="nav-wrap"><Logo />
      <button className="menu-button" aria-expanded={open} aria-controls="site-nav" onClick={() => setOpen(!open)}>{open ? <X /> : <Menu />}<span className="sr-only">{open ? 'Close menu' : 'Open menu'}</span></button>
      <nav id="site-nav" className={open ? 'nav open' : 'nav'} aria-label="Primary navigation">
        <a onClick={close} href="#services">Services</a><a onClick={close} href="#about">Our approach</a><a onClick={close} href="#gallery">Gallery</a><a onClick={close} href="#process">What to expect</a><a onClick={close} className="button nav-cta" href="#request">Request an estimate</a>
      </nav>
    </div>
  </header>
}

export default function App() {
  return <><a className="skip-link" href="#main">Skip to content</a><Header/><main id="main">
    <section className="hero" id="top"><div className="hero-art"><img src="/images/fireplace-room-1280.webp" srcSet="/images/fireplace-room-640.webp 640w, /images/fireplace-room-1280.webp 1280w" sizes="100vw" alt="White fireplace mantel, warm glowing insert and wood floor" width="1280" height="1707" fetchPriority="high"/></div><div className="hero-shade"></div><div className="hero-content"><p className="eyebrow light">Chimney & fireplace services · Atlanta, Georgia</p><h1>Safe chimneys.<br/><em>Warm homes.</em></h1><p className="hero-copy">Considered care for the place your family gathers—from seasonal upkeep to a hearth made new.</p><div className="hero-actions"><a className="button copper" href="#request">Request an estimate <ArrowRight /></a><a className="button ghost" href="#services">Explore services</a></div><div className="hero-trust"><span><ShieldCheck/>Safety-minded service</span><span><Home/>Care for your home</span></div></div><a className="scroll" href="#services">Discover <span>↓</span></a></section>
    <section className="intro section" id="services"><div className="section-heading"><div><p className="eyebrow">Care for every kind of hearth</p><h2>From chimney top<br/>to fireside.</h2></div><p>Whether you’re preparing for the season, addressing a concern or imagining a new gathering place, we begin by listening.</p></div><div className="services-grid">{services.map(({icon:Icon,title,text},i)=><article className={`service-card ${i===0?'featured':''}`} key={title}><span className="service-number">0{i+1}</span><Icon/><h3>{title}</h3><p>{text}</p><a href="#request" onClick={() => window.dispatchEvent(new CustomEvent('oak-select-service', { detail: ['cleaning','inspection','repair','fireplace','stove'][i] }))} aria-label={`Request ${title}`}>Discuss this service <ArrowRight/></a></article>)}</div></section>
    <section className="georgia" id="about"><div className="georgia-image"><img src="/images/chimney-detail-640.webp" srcSet="/images/chimney-detail-640.webp 640w, /images/chimney-detail-1280.webp 1280w" sizes="(max-width:699px) 100vw, 50vw" alt="Stone chimney and dark chimney cap against a blue sky" loading="lazy" width="1280" height="1845"/></div><div className="georgia-copy"><p className="eyebrow light">Rooted in Georgia</p><h2>Local care, with your home at heart.</h2><p>Oak & Ember is a family business being built to serve the greater Atlanta community. We believe good service should feel clear, respectful and personal—from the first conversation to the final walkthrough.</p><ul><li><Check/>A careful, home-conscious approach</li><li><Check/>Clear next steps, without pressure</li><li><Check/>Recommendations shaped around your needs</li></ul><a className="text-button light" href="#process">See what to expect <ArrowRight/></a></div></section>
    <PhotoGallery/>
    <section className="process section" id="process"><div className="process-intro"><p className="eyebrow light">Simple by design</p><h2>A clear path<br/>to a warmer home.</h2><p>Share what you need, tell us what works for your household, and we’ll follow up before anything is scheduled.</p></div><ol><li><span>01</span><ClipboardCheck/><div><h3>Send a request</h3><p>Tell us about your home, service needs and any concerns you’ve noticed.</p></div></li><li><span>02</span><CalendarDays/><div><h3>Connect with the team</h3><p>Share your contact details and ZIP. The team would discuss your needs before proposing an assessment or estimate.</p></div></li><li><span>03</span><ShieldCheck/><div><h3>Confirm together</h3><p>A team member would review the details and follow up to confirm scope and availability.</p></div></li></ol></section>
    <section className="request section" id="request"><div className="request-heading"><p className="eyebrow">Request an estimate</p><h2>Let’s talk about<br/>your project.</h2><p>Tell us what you need and how to reach you. In the live service, the team will follow up to discuss the scope and next steps. This preview is demo only.</p><div className="privacy-callout"><ShieldCheck/><span><strong>Privacy by design</strong>Backend-ready structure, with no active storage or transfer.</span></div></div><RequestWizard/></section>
  </main><footer><div className="footer-main"><Logo/><p>Thoughtful chimney and fireplace care for warm, welcoming homes.</p><nav aria-label="Footer navigation"><a href="#services">Services</a><a href="#about">Our approach</a><a href="#gallery">Gallery</a><a href="#request">Request an estimate</a></nav></div><div className="footer-bottom"><span>© {new Date().getFullYear()} Oak & Ember. Provisional brand.</span><span>Serving the greater Atlanta, Georgia area · Exact coverage to be confirmed.</span></div></footer><nav className="mobile-dock" aria-label="Quick navigation"><a href="#top"><Home aria-hidden="true"/><span>Home</span></a><a href="#services"><Flame aria-hidden="true"/><span>Services</span></a><a href="#request"><ClipboardCheck aria-hidden="true"/><span>Request</span></a></nav></>
}
