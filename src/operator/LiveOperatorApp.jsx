import { useEffect, useState } from 'react'
import { Capacitor } from '@capacitor/core'
import { App as NativeApp } from '@capacitor/app'
import { backend } from '../backend/client'
import { statuses, validateQuote } from './model'
import { serviceOptions } from '../requestModel'
import './operator.css'
import DeviceAlerts from './DeviceAlerts'
import {pushAction,stopAlerts} from './push'
import {Inbox,Smartphone,Flame} from 'lucide-react'

export default function LiveOperatorApp() {
 const [tab,setTab] = useState('inbox')
 const [user,setUser] = useState(null)
 const [initializing,setInitializing] = useState(true)
 const [online,setOnline] = useState(navigator.onLine)
 const native = Capacitor.isNativePlatform()
 const [requests,setRequests] = useState([])
 const [selectedId,setSelectedId] = useState('')
 const [message,setMessage] = useState('')
 const [busy,setBusy] = useState(false)
 const [filter,setFilter] = useState('Todas')
 const [search,setSearch] = useState('')
 const [images,setImages] = useState([])
 const selected = requests.find(item => item.id === selectedId)
 async function refresh(currentUser) {
  if (!currentUser) return
  setBusy(true)
  const member = await backend.from('oak_operators').select('user_id').eq('user_id',currentUser.id).maybeSingle()
  if (member.error || !member.data) {setMessage('Esta cuenta todavía no está autorizada para Oak & Ember.');setRequests([]);setBusy(false);return}
  const {data,error} = await backend.from('oak_requests').select('*').eq('ready',true).order('created_at',{ascending:false})
  if (error) setMessage('No pudimos cargar las solicitudes. Volvé a intentar.')
  else {setRequests(data);setMessage(`${data.length} solicitudes recibidas.`)}
  setBusy(false)
 }
 useEffect(() => {
  document.documentElement.lang='es';document.title='Oak & Ember · Equipo'
  if(!native&&!document.querySelector('link[rel=manifest]')){
   const manifest=document.createElement('link');manifest.rel='manifest';manifest.href='/operator.webmanifest';document.head.appendChild(manifest)
   const apple=document.createElement('link');apple.rel='apple-touch-icon';apple.href='/operator-icon-180.png';document.head.appendChild(apple)
   const viewport=document.querySelector('meta[name=viewport]');if(viewport)viewport.content='width=device-width, initial-scale=1.0, viewport-fit=cover'
   const theme=document.querySelector('meta[name=theme-color]');if(theme)theme.content='#17372d'
   navigator.serviceWorker?.register('/operator-sw.js',{scope:'/operator'}).catch(()=>{})
  }
  backend.auth.getUser().then(({data}) => {setUser(data.user);refresh(data.user)}).finally(()=>setInitializing(false))
  const {data:{subscription}} = backend.auth.onAuthStateChange((_event,session) => {
   setUser(session?.user || null);if(!session?.user){setRequests([]);setImages([])}
   if (session?.user) setTimeout(()=>refresh(session.user),0)
  })
  return () => subscription.unsubscribe()
 }, [native])
 useEffect(() => {
  let active = true
  if (selected) Promise.all(selected.photos.map(async photo => {
   const {data,error} = await backend.storage.from('oak-request-photos').createSignedUrl(photo.path,300)
   return error ? null : data.signedUrl
  })).then(urls => {if (active) setImages(urls.filter(Boolean))})
  return () => {active=false}
 }, [selected])
 useEffect(() => {
  const onOnline = () => {setOnline(true);setMessage('Conexión restablecida. Actualizá las solicitudes para ver los últimos cambios.')}
  const onOffline = () => {setOnline(false);setMessage('Sin conexión. Necesitás internet para cargar o guardar cambios.')}
  window.addEventListener('online',onOnline);window.addEventListener('offline',onOffline)
  return () => {window.removeEventListener('online',onOnline);window.removeEventListener('offline',onOffline)}
 }, [])
 useEffect(() => {
  if (!native) return
  const listener = NativeApp.addListener('backButton', () => {
   if (selectedId) {setSelectedId('');setImages([])}
   else NativeApp.exitApp()
  })
  return () => {listener.then(handle=>handle.remove())}
 }, [native,selectedId])
 useEffect(()=>{if(!user||native)return;const refreshVisible=()=>{if(document.visibilityState==='visible'&&navigator.onLine){refresh(user);pushAction('drain').catch(()=>{})}};const onMessage=e=>{if(e.data?.type==='oak-new-request')refreshVisible()};const interval=setInterval(refreshVisible,60000);window.addEventListener('focus',refreshVisible);navigator.serviceWorker?.addEventListener('message',onMessage);return()=>{clearInterval(interval);window.removeEventListener('focus',refreshVisible);navigator.serviceWorker?.removeEventListener('message',onMessage)}},[user,native])
 async function login(event) {
  event.preventDefault();if(!online){setMessage('Necesitás internet para iniciar sesión.');return}setBusy(true);setMessage('')
  const form = new FormData(event.currentTarget)
  const {error} = await backend.auth.signInWithPassword({email:String(form.get('email')).trim(),password:String(form.get('password'))})
  if (error) setMessage('No pudimos iniciar sesión. Revisá el email y la contraseña de tu acceso autorizado.')
  setBusy(false)
 }
 async function update(patch) {
  if(!online){setMessage('Sin conexión. Los cambios no se guardaron.');return}
  setBusy(true)
  const {data,error} = await backend.from('oak_requests').update(patch).eq('id',selectedId).select().single()
  if (error) setMessage('No pudimos guardar. Los cambios no fueron confirmados; intentá nuevamente.')
  else {setRequests(previous=>previous.map(item=>item.id===data.id?data:item));setMessage('Cambios guardados. No se envió un mensaje al cliente.')}
  setBusy(false)
 }
 const visible = requests.filter(item=>(filter==='Todas'||item.status===filter)&&`${item.payload.name} ${item.id} ${item.payload.service}`.toLowerCase().includes(search.toLowerCase()))
 return <div className={`operator-app ${native ? 'operator-native' : ''}`}><header className="operator-header"><a href="/operator" className="operator-brand"><img src="/operator-logo.svg" alt="" width="38" height="38"/><span>OAK &amp; EMBER<small>GESTIÓN DE SERVICIOS</small></span></a>{user&&<button onClick={async()=>{try{await stopAlerts()}catch{/* Local unsubscribe attempted before logout. */}const result=await backend.auth.signOut({scope:'local'});if(result.error)setMessage('No se pudo cerrar la sesión. Volvé a intentar.');else {setSelectedId('');setMessage('Sesión cerrada.')}}}>Cerrar sesión</button>}</header><main className="operator-main"><div className="op-hero"><span className="operator-eyebrow"><Flame size={14}/> OAK &amp; EMBER · EQUIPO</span><h1>{user?'Cada hogar, bien atendido.':'Cuidamos el próximo paso.'}</h1><p className="op-subtitle">Revisá el proyecto, contactá al cliente y acordá el próximo paso.</p></div><p className="op-message" role="status">{message}</p>{initializing?<p role="status">Comprobando tu acceso…</p>:!user?<form className="op-card op-form" onSubmit={login}><h2>Acceso del equipo</h2><p>Solo las cuentas autorizadas pueden consultar solicitudes y fotografías. No se muestran datos de clientes sin iniciar sesión.</p><label>Email<input type="email" name="email" autoComplete="username" required/></label><label>Contraseña<input type="password" name="password" autoComplete="current-password" required/></label><button className="op-button" disabled={busy || !online}>{busy?'Ingresando…':'Entrar'}</button><p>El acceso inicial debe habilitarlo el responsable de Oak &amp; Ember. No hay registro público.</p></form>:<><nav className="operator-tabs" aria-label="Panel"><button aria-current={tab==='inbox'?'page':undefined} onClick={()=>setTab('inbox')}><Inbox size={19}/> Consultas</button><button aria-current={tab==='device'?'page':undefined} onClick={()=>setTab('device')}><Smartphone size={19}/> App y alertas</button></nav>{tab==='device'?<DeviceAlerts/>:<><div className="op-metrics"><div><strong>{requests.filter(r=>r.status==='Nueva').length}</strong><span>Nuevas consultas</span></div><div><strong>{requests.filter(r=>!['Nueva','Cerrada'].includes(r.status)).length}</strong><span>En seguimiento</span></div><div><strong>{requests.length}</strong><span>Total recibidas</span></div></div><button className="op-button" disabled={busy || !online} onClick={()=>refresh(user)}>{busy?'Cargando…':'Actualizar solicitudes'}</button><div className="op-filters"><label>Buscar<input value={search} onChange={e=>setSearch(e.target.value)} type="search"/></label><label>Estado<select value={filter} onChange={e=>setFilter(e.target.value)}><option>Todas</option>{statuses.map(status=><option key={status}>{status}</option>)}</select></label></div><div className={`op-columns ${selected ? 'has-detail' : ''}`}><section className="op-inquiries" aria-label="Solicitudes">{visible.map(item=><button className={`op-inquiry ${selectedId===item.id?'active':''}`} key={item.id} onClick={()=>{setImages([]);setSelectedId(item.id)}}><span className="op-badge">{item.status}</span><strong>{item.payload.name}</strong><span>{serviceOptions.find(s=>s.id===item.payload.service)?.title}</span><small>{item.id.slice(0,8).toUpperCase()} · {new Date(item.created_at).toLocaleString('es-UY',{timeZone:'America/New_York'})} (Atlanta)</small></button>)}{!visible.length&&<p>No hay solicitudes para mostrar.</p>}</section>{selected&&<section className="op-card op-detail" key={selectedId}><button className="op-back" type="button" onClick={()=>{setSelectedId('');setImages([])}}>← Volver a consultas</button><h2>{selected.payload.name}</h2><p>{selected.payload.details}</p><dl><dt>Contacto preferido</dt><dd>{selected.payload.contact} · {selected.payload.time}</dd><dt>Teléfono</dt><dd>{selected.payload.phone?<a href={`tel:${selected.payload.phone.replace(/[^+\d]/g,'')}`}>{selected.payload.phone}</a>:'No proporcionado'}</dd><dt>Email</dt><dd>{selected.payload.email?<a href={`mailto:${selected.payload.email}`}>{selected.payload.email}</a>:'No proporcionado'}</dd><dt>Dirección / ZIP</dt><dd>{selected.payload.address||'Por acordar'} · {selected.payload.zip}</dd><dt>Equipo</dt><dd>{selected.payload.appliance}</dd></dl><div className="wizard-photos">{images.map((url,index)=><a key={url} href={url} target="_blank" rel="noreferrer"><img src={url} alt={`Foto privada del cliente ${index+1}`} style={{width:'100%',maxWidth:240}}/></a>)}</div><label>Estado<select aria-label="Estado de la consulta" disabled={busy || !online} value={selected.status} onChange={e=>update({status:e.target.value})}>{statuses.map(status=><option key={status}>{status}</option>)}</select></label><form onSubmit={event=>{event.preventDefault();update({notes:String(new FormData(event.currentTarget).get('notes'))})}}><label>Notas internas<textarea name="notes" defaultValue={selected.notes} maxLength={10000}/></label><button className="op-button" disabled={busy || !online}>Guardar notas</button></form><form className="op-quote" onSubmit={event=>{event.preventDefault();const form=new FormData(event.currentTarget);const amount=String(form.get('amount'));const scope=String(form.get('scope'));const error=validateQuote(amount,scope);if(error)setMessage(error);else update({quote:{amount:Number(amount),scope}})}}><h3>Presupuesto interno</h3><label>Alcance<textarea name="scope" defaultValue={selected.quote?.scope||''} maxLength={3000} required/></label><label>Importe USD<input name="amount" type="number" min="0.01" step="0.01" defaultValue={selected.quote?.amount||''} required/></label><button className="op-button" disabled={busy || !online}>Guardar presupuesto</button><p>Guardar no lo envía al cliente ni confirma un trabajo. Cambiá a “Presupuesto enviado” después de comunicarlo.</p></form><p>La visita se acuerda después de revisar el proyecto y contactar al cliente. No se reserva automáticamente.</p></section>}</div></>}</>}</main></div>
}
