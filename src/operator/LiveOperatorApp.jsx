import { useEffect, useState } from 'react'
import { backend } from '../backend/client'
import { statuses, validateQuote } from './model'
import { serviceOptions } from '../requestModel'
import './operator.css'

export default function LiveOperatorApp() {
 const [user,setUser] = useState(null)
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
  backend.auth.getUser().then(({data}) => {setUser(data.user);refresh(data.user)})
  const {data:{subscription}} = backend.auth.onAuthStateChange((_event,session) => {
   setUser(session?.user || null);setRequests([]);setImages([])
   if (session?.user) setTimeout(()=>refresh(session.user),0)
  })
  return () => subscription.unsubscribe()
 }, [])
 useEffect(() => {
  let active = true
  if (selected) Promise.all(selected.photos.map(async photo => {
   const {data,error} = await backend.storage.from('oak-request-photos').createSignedUrl(photo.path,300)
   return error ? null : data.signedUrl
  })).then(urls => {if (active) setImages(urls.filter(Boolean))})
  return () => {active=false}
 }, [selected])
 async function login(event) {
  event.preventDefault();setBusy(true);setMessage('')
  const form = new FormData(event.currentTarget)
  const {error} = await backend.auth.signInWithPassword({email:String(form.get('email')).trim(),password:String(form.get('password'))})
  if (error) setMessage('No pudimos iniciar sesión. Revisá el email y la contraseña de tu acceso autorizado.')
  setBusy(false)
 }
 async function update(patch) {
  setBusy(true)
  const {data,error} = await backend.from('oak_requests').update(patch).eq('id',selectedId).select().single()
  if (error) setMessage('No pudimos guardar. Los cambios no fueron confirmados; intentá nuevamente.')
  else {setRequests(previous=>previous.map(item=>item.id===data.id?data:item));setMessage('Cambios guardados. No se envió un mensaje al cliente.')}
  setBusy(false)
 }
 const visible = requests.filter(item=>(filter==='Todas'||item.status===filter)&&`${item.payload.name} ${item.id} ${item.payload.service}`.toLowerCase().includes(search.toLowerCase()))
 return <div className="operator-app"><header className="operator-header"><a href="/" className="operator-brand">OAK & EMBER · Panel privado</a>{user&&<button onClick={()=>backend.auth.signOut()}>Cerrar sesión</button>}</header><main className="operator-main"><h1>Solicitudes de presupuesto</h1><p className="op-message" role="status">{message}</p>{!user?<form className="op-card op-form" onSubmit={login}><h2>Acceso del equipo</h2><p>Solo las cuentas autorizadas pueden consultar solicitudes y fotografías. No se muestran datos de clientes sin iniciar sesión.</p><label>Email<input type="email" name="email" autoComplete="username" required/></label><label>Contraseña<input type="password" name="password" autoComplete="current-password" required/></label><button className="op-button" disabled={busy}>{busy?'Ingresando…':'Entrar'}</button><p>El acceso inicial debe habilitarlo el responsable de Oak &amp; Ember. No hay registro público.</p></form>:<><button className="op-button" disabled={busy} onClick={()=>refresh(user)}>{busy?'Cargando…':'Actualizar solicitudes'}</button><div className="op-filters"><label>Buscar<input value={search} onChange={e=>setSearch(e.target.value)} type="search"/></label><label>Estado<select value={filter} onChange={e=>setFilter(e.target.value)}><option>Todas</option>{statuses.map(status=><option key={status}>{status}</option>)}</select></label></div><div className="op-columns"><section className="op-inquiries" aria-label="Solicitudes">{visible.map(item=><button className="op-inquiry" key={item.id} onClick={()=>{setImages([]);setSelectedId(item.id)}}><span className="op-badge">{item.status}</span><strong>{item.payload.name}</strong><span>{serviceOptions.find(s=>s.id===item.payload.service)?.title}</span><small>{item.id.slice(0,8).toUpperCase()} · {new Date(item.created_at).toLocaleString('es-UY',{timeZone:'America/New_York'})} (Atlanta)</small></button>)}{!visible.length&&<p>No hay solicitudes para mostrar.</p>}</section>{selected&&<section className="op-card op-detail" key={selectedId}><h2>{selected.payload.name}</h2><p>{selected.payload.details}</p><dl><dt>Contacto preferido</dt><dd>{selected.payload.contact} · {selected.payload.time}</dd><dt>Teléfono</dt><dd>{selected.payload.phone?<a href={`tel:${selected.payload.phone.replace(/[^+\d]/g,'')}`}>{selected.payload.phone}</a>:'No proporcionado'}</dd><dt>Email</dt><dd>{selected.payload.email?<a href={`mailto:${selected.payload.email}`}>{selected.payload.email}</a>:'No proporcionado'}</dd><dt>Dirección / ZIP</dt><dd>{selected.payload.address||'Por acordar'} · {selected.payload.zip}</dd><dt>Equipo</dt><dd>{selected.payload.appliance}</dd></dl><div className="wizard-photos">{images.map((url,index)=><a key={url} href={url} target="_blank" rel="noreferrer"><img src={url} alt={`Foto privada del cliente ${index+1}`} style={{width:'100%',maxWidth:240}}/></a>)}</div><label>Estado<select disabled={busy} value={selected.status} onChange={e=>update({status:e.target.value})}>{statuses.map(status=><option key={status}>{status}</option>)}</select></label><form onSubmit={event=>{event.preventDefault();update({notes:String(new FormData(event.currentTarget).get('notes'))})}}><label>Notas internas<textarea name="notes" defaultValue={selected.notes} maxLength={10000}/></label><button className="op-button" disabled={busy}>Guardar notas</button></form><form className="op-quote" onSubmit={event=>{event.preventDefault();const form=new FormData(event.currentTarget);const amount=String(form.get('amount'));const scope=String(form.get('scope'));const error=validateQuote(amount,scope);if(error)setMessage(error);else update({quote:{amount:Number(amount),scope}})}}><h3>Presupuesto interno</h3><label>Alcance<textarea name="scope" defaultValue={selected.quote?.scope||''} maxLength={3000} required/></label><label>Importe USD<input name="amount" type="number" min="0.01" step="0.01" defaultValue={selected.quote?.amount||''} required/></label><button className="op-button" disabled={busy}>Guardar presupuesto</button><p>Guardar no lo envía al cliente ni confirma un trabajo. Cambiá a “Presupuesto enviado” después de comunicarlo.</p></form><p>La visita se acuerda después de revisar el proyecto y contactar al cliente. No se reserva automáticamente.</p></section>}</div></>}</main></div>
}
