import { ApplicationServer, importVapidKeys, Urgency, PushMessageError } from 'jsr:@negrel/webpush@0.5.0'
import { notificationPayload,validSubscription } from './push-policy.js'
export async function sender(db:any) {
 const {data,error}=await db.rpc('oak_push_keys'); if(error||!data)throw Error('Push configuration unavailable')
 return ApplicationServer.new({contactInformation:'https://oak-79f.pages.dev',vapidKeys:await importVapidKeys(data)})
}
export async function deliver(db:any,server:any,row:any,payload:any) {
 if(!validSubscription(row.subscription))return false
 try {await server.subscribe(row.subscription).pushTextMessage(JSON.stringify(payload),{urgency:Urgency.High,ttl:3600});return true}
 catch(error) {if(error instanceof PushMessageError && [404,410].includes(error.response.status)) {await db.from('oak_push_subscriptions').delete().eq('endpoint',row.endpoint);return true}return false}
}
export async function dispatchPending(db:any) {
 const {data:events,error}=await db.rpc('oak_claim_push');if(error||!events?.length)return
 const server=await sender(db)
 const {data:rows,error:subscriptionError}=await db.from('oak_push_subscriptions').select('*')
 if(subscriptionError)throw Error('Subscriptions unavailable')
 for(const event of events) {
  const delivered=new Set<string>(event.delivered)
  let failed=false
  for(const row of rows||[]) {
   // Membership is also enforced by the cascading foreign key.
   if(delivered.has(row.endpoint))continue
   if(await deliver(db,server,row,notificationPayload(event.request_id)))delivered.add(row.endpoint);else failed=true
  }
  await db.from('oak_push_events').update({status:failed?'pending':'sent',delivered:[...delivered]}).eq('request_id',event.request_id)
 }
}

export async function dispatchWithRetry(db:any) {
 for(const delay of [0,5000,20000]) {
  if(delay)await new Promise(resolve=>setTimeout(resolve,delay))
  try{await dispatchPending(db)}catch{console.error('Push dispatch unavailable; retained in queue')}
 }
}
