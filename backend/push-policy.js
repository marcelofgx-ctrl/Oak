export function validSubscription(s) {
 try {
  const u=new URL(s.endpoint)
  const pathAllowed=u.hostname==='web.push.apple.com'?u.pathname.length>1:u.hostname==='fcm.googleapis.com'?/^\/(fcm\/send|wp)\//.test(u.pathname):/^\/wpush\/v[12]\//.test(u.pathname)
  const allowed=u.hostname==='web.push.apple.com'||u.hostname==='fcm.googleapis.com'||u.hostname==='updates.push.services.mozilla.com'
  return allowed && pathAllowed && !u.search && u.protocol==='https:' && !u.username && !u.password && (!u.port||u.port==='443') && !u.hash && s.endpoint.length<=2048 && /^[A-Za-z0-9_-]{87}$/.test(s.keys?.p256dh||'') && /^[A-Za-z0-9_-]{22}$/.test(s.keys?.auth||'')
 } catch {return false}
}
export function notificationPayload(id,test=false) {
 return {title:'Oak & Ember',body:test?'Las alertas están conectadas.':'Nueva consulta. Abrí la app para revisarla.',tag:test?'oak-test':`oak-${id}`,url:'/operator'}
}
