import { useRef } from 'react'
import { X, ZoomIn } from 'lucide-react'
import './photos.css'
const photographs = [
  { file:'fireplace-room', alt:'White mantel framing a dark fireplace insert, stone hearth and wood floor', title:'A welcoming hearth', label:'Fireplace' },
  { file:'fireplace-detail', alt:'Close view of the fireplace insert with glowing logs and its dark surround', title:'Warmth in the details', label:'Fireplace detail' },
  { file:'chimney-detail', alt:'Stone chimney and dark cap viewed from below against a blue sky', title:'From stone to chimney top', label:'Chimney detail' },
]
const comparison = [
  { file:'chimney-before', alt:'Before: stone chimney with a worn metal cap and a ladder alongside', title:'Before', label:'Chimney — before' },
  { file:'chimney-after', alt:'After: chimney with a dark cap, viewed across the shingled roof', title:'After', label:'Chimney — after' },
]
export default function PhotoGallery() {
  const dialog = useRef(null)
  const fullImage = useRef(null)
  const caption = useRef(null)
  function open(photo) { fullImage.current.src = `/images/${photo.file}-1280.webp`; fullImage.current.alt = photo.alt; caption.current.textContent = photo.label; dialog.current.showModal() }
  const card = (photo, index = 0) => <figure key={photo.file} className={index === 0 ? 'photo-featured' : ''}><button type="button" className="photo-open" onClick={() => open(photo)} aria-label={`Enlarge ${photo.label.toLowerCase()}`}><img src={`/images/${photo.file}-640.webp`} srcSet={`/images/${photo.file}-640.webp 640w, /images/${photo.file}-1280.webp 1280w`} sizes="(max-width: 699px) 90vw, (max-width: 980px) 45vw, 40vw" alt={photo.alt} loading="lazy" width="1280" height={photo.file === 'chimney-detail' ? 1845 : 1707}/><span className="photo-zoom" aria-hidden="true"><ZoomIn/></span></button><figcaption><span>{photo.label}</span><strong>{photo.title}</strong></figcaption></figure>
  return <section className="gallery section real-photos" id="gallery"><div className="section-heading"><div><p className="eyebrow">A closer look</p><h2>From chimney top<br/>to the heart of home.</h2></div><p>A fireplace, its details, and chimney care—through photographs shared with Oak & Ember.</p></div><div className="real-photo-grid">{photographs.map(card)}</div><div className="comparison-heading"><p className="eyebrow">BEFORE & AFTER</p><h3>A change at the chimney top.</h3><p>The same chimney, photographed from different angles before and after the work.</p></div><div className="comparison-grid">{comparison.map(card)}</div><p className="photo-note">Supplied photographs, retouched for presentation. Tap any image for a closer look.</p><dialog ref={dialog} className="photo-dialog" aria-labelledby="photo-dialog-caption"><button className="photo-close" type="button" onClick={() => dialog.current.close()} aria-label="Close photograph"><X/></button><img ref={fullImage} alt=""/><p ref={caption} id="photo-dialog-caption"/></dialog></section>
}
