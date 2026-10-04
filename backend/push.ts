import {createClient} from 'npm:@supabase/supabase-js@2.57.4'
import {validSubscription,notificationPayload} from './push-policy.js'
import {sender,deliver,dispatchWithRetry} from './push-delivery.ts'
Deno.serve(async(req:Request)=>{
 const origin=req.headers.get('origin')||''
 const allowed=['https://oak-79f.pages.dev','http://127.0.0.1:5173','http://localhost:5173']
 const headers={'Access-Control-Allow-Origin':allowed.includes(origin)?origin:allowed[0],'Access-Control-Allow-Headers':'authorization,apikey,content-type,x-client-info','Access-Control-Allow-Methods':'POST,OPTIONS','Content-Type':'application/json','Cache-Control':'no-store','Vary':'Origin'}
 const reply=(value:any,status=200)=>new Response(JSON.stringify(value),{status,headers})
 if(req.method==='OPTIONS')return new Response(null,{status:204,headers})
 if(origin&&!allowed.includes(origin))return reply({error:'Origin not allowed'},403)
 if(req.method!=='POST')return reply({error:'Use POST'},405)
 const db=createClient(Deno.env.get('SUPABASE_URL')!,Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,{auth:{persistSession:false}})
 const {data:{user},error}=await db.auth.getUser((req.headers.get('authorization')||'').replace(/^Bearer /i,''))
 if(error||!user)return reply({error:'Iniciá sesión para gestionar alertas.'},401)
 const member=await db.from('oak_operators').select('user_id').eq('user_id',user.id).maybeSingle()
 if(member.error||!member.data)return reply({error:'Acceso no autorizado.'},403)
 try {
  const text=await req.text();if(text.length>8192)return reply({error:'Solicitud demasiado grande.'},413)
  const body=JSON.parse(text)
  if(body.action==='subscribe') {
   if(!validSubscription(body.subscription))return reply({error:'Dispositivo de notificaciones no válido.'},400)
   const sub=body.subscription
   try {const raw=Uint8Array.from(atob(sub.keys.p256dh.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0));await crypto.subtle.importKey('raw',raw,{name:'ECDH',namedCurve:'P-256'},false,[])}catch{return reply({error:'Clave del dispositivo no válida.'},400)}
   const existing=await db.from('oak_push_subscriptions').select('user_id').eq('endpoint',sub.endpoint).maybeSingle()
   if(existing.data&&existing.data.user_id!==user.id)return reply({error:'Este dispositivo está asociado a otro acceso. Desactivá sus alertas primero.'},409)
   const count=await db.from('oak_push_subscriptions').select('endpoint',{count:'exact',head:true}).eq('user_id',user.id)
   if(count.error||(!existing.data&&(count.count||0)>=10))return reply({error:'Límite de dispositivos alcanzado.'},409)
   const result=await db.from('oak_push_subscriptions').upsert({endpoint:sub.endpoint,user_id:user.id,subscription:sub})
   if(result.error)throw result.error
   return reply({ok:true})
  }
  if(body.action==='unsubscribe') {const r=await db.from('oak_push_subscriptions').delete().eq('endpoint',String(body.endpoint)).eq('user_id',user.id);if(r.error)throw r.error;return reply({ok:true})}
  if(body.action==='drain'){EdgeRuntime.waitUntil(dispatchWithRetry(db));return reply({ok:true})}
  if(body.action==='test') {
   const r=await db.from('oak_push_subscriptions').select('*').eq('endpoint',String(body.endpoint)).eq('user_id',user.id).maybeSingle()
   if(r.error||!r.data)return reply({error:'Primero activá las alertas en este dispositivo.'},400)
   const claim=await db.from('oak_push_subscriptions').update({last_test:new Date().toISOString()}).eq('endpoint',r.data.endpoint).or(`last_test.is.null,last_test.lt.${new Date(Date.now()-30000).toISOString()}`).select('endpoint')
   if(claim.error||!claim.data?.length)return reply({error:'Esperá 30 segundos antes de repetir la prueba.'},429)
   if(!await deliver(db,await sender(db),r.data,notificationPayload('',true)))return reply({error:'El proveedor no aceptó la alerta. Volvé a activar las notificaciones.'},502)
   const active=await db.from('oak_push_subscriptions').select('endpoint').eq('endpoint',r.data.endpoint).maybeSingle()
   if(!active.data)return reply({error:'Esta suscripción venció. Activá nuevamente las alertas.'},410)
   return reply({ok:true})
  }
  return reply({error:'Acción no válida.'},400)
 }catch{return reply({error:'No pudimos completar la operación de alertas.'},500)}
})
