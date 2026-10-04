import { createClient } from '@supabase/supabase-js'
import { backendUrl, publicKey, intakeKey } from './config'
export const backend = createClient(backendUrl, publicKey)
export async function sendRequest(data, photos, id) {
 const form = new FormData()
 form.append('request',JSON.stringify({...data,id}))
 for (const photo of photos) form.append('photos',photo.file)
 const response = await fetch(`${backendUrl}/functions/v1/oak-intake`, {method:'POST',headers:{apikey:intakeKey,Authorization:`Bearer ${intakeKey}`},body:form})
 const result = await response.json()
 if (!response.ok || !result.reference) throw new Error(result.error || 'We could not save your request. Please retry.')
 return result.reference
}
