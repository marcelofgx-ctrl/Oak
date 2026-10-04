import { createClient } from 'npm:@supabase/supabase-js@2.57.4'
import { validateStep, validatePhotos } from './requestModel.js'
const allowed = ['https://oak-79f.pages.dev', 'http://127.0.0.1:5173', 'http://localhost:5173']
Deno.serve(async (req: Request) => {
 const origin = req.headers.get('origin') || ''
 const headers = { 'Access-Control-Allow-Origin': allowed.includes(origin) ? origin : allowed[0], 'Access-Control-Allow-Headers':'authorization, apikey, content-type, x-client-info', 'Access-Control-Allow-Methods':'POST, OPTIONS', 'Vary':'Origin', 'Content-Type':'application/json', 'Cache-Control':'no-store' }
 const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers })
 if (req.method === 'OPTIONS') return new Response(null, { status:204, headers })
 if (origin && !allowed.includes(origin)) return reply({error:'Origin not allowed.'},403)
 if (req.method !== 'POST') return reply({error:'Use POST.'},405)
 if (Number(req.headers.get('content-length') || 0) > 43 * 1024 * 1024) return reply({error:'Files are too large.'},413)
 const db = createClient(Deno.env.get('SUPABASE_URL')!, Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!, {auth:{persistSession:false}})
 let id = ''
 let owned = false
 const uploaded: string[] = []
 try {
  const reader = req.body?.getReader()
  if (!reader) return reply({error:'Request body required.'},400)
  const chunks: Uint8Array[] = []
  let size = 0
  while (true) {
   const next = await reader.read()
   if (next.done) break
   size += next.value.byteLength
   if (size > 43 * 1024 * 1024) { await reader.cancel(); return reply({error:'Files are too large.'},413) }
   chunks.push(next.value)
  }
  const form = await new Response(new Blob(chunks),{headers:{'Content-Type':req.headers.get('content-type') || ''}}).formData()
  const raw = JSON.parse(String(form.get('request') || '{}'))
  if (raw.website) return reply({error:'Unable to accept this request.'},400)
  id = String(raw.id || '')
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(id)) return reply({error:'Invalid request reference.'},400)
  const data: Record<string, unknown> = {}
  const bounds = {service:30,appliance:100,reason:100,details:3000,name:100,phone:30,email:254,address:250,zip:10,contact:20,time:20}
  for (const [key,max] of Object.entries(bounds)) {
   if (typeof raw[key] !== 'string' || raw[key].length > max) return reply({error:'Please check your request fields.'},400)
   data[key] = raw[key].trim()
  }
  data.acknowledge = raw.acknowledge === true
  data.date = ''
  if (!['Phone call','Email'].includes(String(data.contact)) || !['Flexible','Morning','Afternoon'].includes(String(data.time))) return reply({error:'Invalid contact preferences.'},400)
  const errors = Object.assign({}, ...[0,1,2,3].map(step => validateStep(step,data)))
  if (Object.keys(errors).length) return reply({error:'Please check the request fields.',fields:errors},400)
  const files = form.getAll('photos')
  if (files.some(file => !(file instanceof File))) return reply({error:'Invalid photo.'},400)
  const photoError = validatePhotos(files)
  if (photoError) return reply({error:photoError},400)
  for (const file of files as File[]) {
   const b = new Uint8Array(await file.slice(0,12).arrayBuffer())
   const jpeg = b[0]===255 && b[1]===216 && b[2]===255
   const png = [137,80,78,71,13,10,26,10].every((v,i)=>b[i]===v)
   const webp = String.fromCharCode(...b.slice(0,4))==='RIFF' && String.fromCharCode(...b.slice(8,12))==='WEBP'
   if (!(file.type==='image/jpeg' && jpeg || file.type==='image/png' && png || file.type==='image/webp' && webp)) return reply({error:'A photo does not match its image format.'},400)
  }
  const existing = await db.from('oak_requests').select('id,ready').eq('id',id).maybeSingle()
  if (existing.error) throw existing.error
  if (existing.data?.ready) return reply({reference:id})
  if (existing.data) return reply({error:'This request is still processing. Please retry shortly.'},409)
  const fingerprint = Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(String(data.contact==='Email'?data.email:String(data.phone).replace(/\D/g,'')))))).map(v=>v.toString(16).padStart(2,'0')).join('')
  const limit = await db.rpc('oak_claim_intake',{fingerprint})
  if (limit.error) throw limit.error
  if (!limit.data) return reply({error:'Too many requests. Please try again in an hour.'},429)
  const inserted = await db.from('oak_requests').insert({id,payload:data})
  if (inserted.error) throw inserted.error
  owned = true
  const photos = []
  for (const file of files as File[]) {
   const path = `${id}/${crypto.randomUUID()}.${file.type==='image/jpeg'?'jpg':file.type==='image/png'?'png':'webp'}`
   const result = await db.storage.from('oak-request-photos').upload(path,file,{contentType:file.type,upsert:false})
   if (result.error) throw result.error
   uploaded.push(path); photos.push({path,size:file.size,type:file.type})
  }
  const finished = await db.from('oak_requests').update({photos,ready:true}).eq('id',id)
  if (finished.error) throw finished.error
  return reply({reference:id},201)
 } catch {
  if (uploaded.length) await db.storage.from('oak-request-photos').remove(uploaded)
  if (owned) await db.from('oak_requests').delete().eq('id',id).eq('ready',false)
  return reply({error:'We could not save your request. Your details are still here so you can retry.'},500)
 }
})
