/* Never cache authenticated requests, customer photographs or API responses. */
self.addEventListener('install',()=>self.skipWaiting())
self.addEventListener('activate',event=>event.waitUntil(self.clients.claim()))
self.addEventListener('push',event=>{
 let data={};try{data=event.data?.json()||{}}catch{/* Generic notification for malformed payload. */}
 const test=data.tag==='oak-test'
 event.waitUntil(Promise.all([self.registration.showNotification('Oak & Ember',{body:test?'Las alertas están conectadas.':'Nueva consulta. Abrí la app para revisarla.',icon:'/operator-icon-192.png',badge:'/operator-icon-192.png',tag:typeof data.tag==='string'?data.tag.slice(0,64):'oak-new',data:{url:'/operator'}}),self.clients.matchAll({type:'window',includeUncontrolled:true}).then(clients=>clients.forEach(client=>client.postMessage({type:'oak-new-request'})))]))
})
self.addEventListener('notificationclick',event=>{
 event.notification.close()
 event.waitUntil(self.clients.matchAll({type:'window',includeUncontrolled:true}).then(async clients=>{const client=clients.find(c=>new URL(c.url).pathname.startsWith('/operator'));if(client)return client.focus();return self.clients.openWindow('/operator')}))
})
