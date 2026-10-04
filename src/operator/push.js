import {backend} from '../backend/client'
import {backendUrl,publicKey} from '../backend/config'
export async function pushAction(action,values={}) {
 const {data}=await backend.auth.getSession()
 if(!data.session)throw Error('Iniciá sesión para activar las alertas.')
 const response=await fetch(`${backendUrl}/functions/v1/oak-push`,{method:'POST',headers:{apikey:publicKey,Authorization:`Bearer ${data.session.access_token}`,'Content-Type':'application/json'},body:JSON.stringify({action,...values})})
 const result=await response.json()
 if(!response.ok)throw Error(result.error||'No pudimos gestionar las alertas.')
 return result
}
export async function stopAlerts() {
 if(!('serviceWorker' in navigator))return
 const registration=await navigator.serviceWorker.getRegistration('/operator')
 const subscription=await registration?.pushManager.getSubscription()
 if(subscription){try{await pushAction('unsubscribe',{endpoint:subscription.endpoint})}finally{await subscription.unsubscribe()}}
}
