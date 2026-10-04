import {useEffect,useState} from 'react'
import {Capacitor} from '@capacitor/core'
import {Bell,Download,ShieldCheck,Smartphone} from 'lucide-react'
import {applicationServerKey} from './push-config'
import {pushAction,stopAlerts} from './push'
export default function DeviceAlerts(){
 const [active,setActive]=useState(false),[busy,setBusy]=useState(false),[message,setMessage]=useState('')
 const native=Capacitor.isNativePlatform()
 const ios=/iPad|iPhone|iPod/.test(navigator.userAgent)||(navigator.platform==='MacIntel'&&navigator.maxTouchPoints>1)
 const android=/Android/.test(navigator.userAgent)
 let rememberedAndroidApp=false
 try{rememberedAndroidApp=sessionStorage.getItem('oak-android-app')==='1'}catch{/* Browser storage may be unavailable. */}
 const androidApp=android&&(new URLSearchParams(location.search).get('source')==='android-app'||rememberedAndroidApp||document.referrer.startsWith('android-app://com.oakandember.operator'))
 const installed=window.matchMedia('(display-mode: standalone)').matches||navigator.standalone
 const supported=!native&&'serviceWorker' in navigator&&'PushManager' in window&&'Notification' in window&&(!ios||installed)
 useEffect(()=>{if(supported)navigator.serviceWorker.register('/operator-sw.js',{scope:'/operator'}).then(r=>r.pushManager.getSubscription()).then(s=>setActive(Boolean(s)&&Notification.permission==='granted')).catch(()=>setMessage('No pudimos preparar las alertas. Revisá la conexión.'))},[supported])
 async function activate(){
  setBusy(true);setMessage('')
  try{
   const permission=await Notification.requestPermission()
   if(permission!=='granted')throw Error('El permiso no fue concedido. Podés revisarlo en los ajustes de notificaciones del dispositivo.')
   const registration=await navigator.serviceWorker.ready
   const key=Uint8Array.from(atob(applicationServerKey.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0))
   const subscription=await registration.pushManager.getSubscription()||await registration.pushManager.subscribe({userVisibleOnly:true,applicationServerKey:key})
   try{await pushAction('subscribe',{subscription:subscription.toJSON()})}catch(error){await subscription.unsubscribe();throw error}
   setActive(true);setMessage('Alertas activadas en este dispositivo. Usá la prueba para comprobar que aparece el aviso.')
  }catch(error){setMessage(error.message)}finally{setBusy(false)}
 }
 async function test(){setBusy(true);try{const r=await navigator.serviceWorker.ready;const s=await r.pushManager.getSubscription();await pushAction('test',{endpoint:s?.endpoint});setMessage('El proveedor aceptó la alerta de prueba. Comprobá el aviso en tu dispositivo; los modos de concentración pueden silenciarlo.')}catch(error){setMessage(error.message)}finally{setBusy(false)}}
 async function disable(){setBusy(true);try{await stopAlerts();setActive(false);setMessage('Alertas desactivadas en este dispositivo.')}catch{setActive(false);setMessage('La suscripción local se desactivó. No se pudo actualizar el servidor.')}finally{setBusy(false)}}
 return <section className="op-device"><div className="op-card"><span className="op-section-icon"><Smartphone size={24}/></span>{androidApp?<><h2>Tu app Android</h2><p>Oak &amp; Ember 1.1: tus consultas y presupuestos, con el mismo panel actualizado y un diseño más claro.</p><ol><li>Revisá las nuevas consultas en la bandeja.</li><li>Tocá <strong>Activar alertas</strong> y permití las notificaciones.</li><li>Tocá <strong>Probar alerta</strong> para comprobar el aviso.</li></ol><p className="op-hint"><Download size={16}/> Mantené Chrome actualizado y habilitá las notificaciones de Oak &amp; Ember en los ajustes de Android. Esta app necesita conexión.</p></>:android?<><h2>Oak &amp; Ember en tu Android</h2><p>Instalá la APK actualizada para abrir el panel desde su ícono.</p><a className="op-button" href="/downloads/oak-ember-1.1.apk" download>Descargar app Android 1.1</a><p className="op-hint"><Download size={16}/> Instalá la actualización sobre tu app actual, sin desinstalarla. También podés activar las alertas desde Chrome.</p></>:<><h2>Oak &amp; Ember en tu iPhone</h2><p>Tu bandeja de consultas, siempre a mano. Instalá la app desde Safari para abrirla a pantalla completa.</p><ol><li>Abrí <strong>oak-79f.pages.dev/operator</strong> en Safari.</li><li>Tocá <strong>Compartir → Agregar a pantalla de inicio</strong>.</li><li>Abrí el nuevo ícono, iniciá sesión y activá las alertas.</li></ol><p className="op-hint"><Download size={16}/> Requiere iOS 16.4 o posterior para notificaciones. No necesitás App Store.</p></>}</div><div className="op-card"><span className="op-section-icon"><Bell size={24}/></span><h2>Alertas de nuevas consultas</h2><p>Un aviso privado cuando llega una consulta, incluso con la app cerrada. Los datos del cliente se consultan después de iniciar sesión.</p><p className="op-alert-state">{active?'● Suscripción local activa':'○ Alertas sin activar'}</p>{supported?<div className="op-actions">{active?<><button className="op-button" disabled={busy} onClick={test}>Probar alerta</button><button disabled={busy} onClick={disable}>Desactivar</button></>:<button className="op-button" disabled={busy} onClick={activate}>{busy?'Conectando…':'Activar alertas'}</button>}</div>:<p className="op-install-hint">{native?'Para recibir estas alertas, usá la versión web instalada. Esta APK no incorpora notificaciones nativas.':ios?'Primero agregá la app a la pantalla de inicio y abrila desde su ícono.':'Este navegador no admite las alertas. Probá con un navegador compatible actualizado.'}</p>}<p role="status">{message}</p><p className="op-hint"><ShieldCheck size={16}/> Al cerrar sesión se desactivan las alertas de este dispositivo. Necesitás conexión para recibirlas.</p></div></section>
}
