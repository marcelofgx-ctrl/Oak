import test from 'node:test'
import assert from 'node:assert/strict'
import {validSubscription,notificationPayload} from '../backend/push-policy.js'
const keys={p256dh:'B'.repeat(87),auth:'A'.repeat(22)}
test('push rejects arbitrary hosts, credentials, ports and malformed keys',()=>{
 for(const endpoint of ['http://web.push.apple.com/x','https://evil.example/x','https://web.push.apple.com.evil.example/x','https://user@web.push.apple.com/x','https://web.push.apple.com:444/x','https://web.push.apple.com/x#fragment'])assert.equal(validSubscription({endpoint,keys}),false)
 assert.equal(validSubscription({endpoint:'https://web.push.apple.com/x',keys:{...keys,auth:'bad'}}),false)
})
test('push accepts only supported HTTPS providers',()=>{for(const endpoint of ['https://web.push.apple.com/x','https://fcm.googleapis.com/fcm/send/x','https://updates.push.services.mozilla.com/wpush/v2/x'])assert.equal(validSubscription({endpoint,keys}),true)})
test('notification payload contains no customer data and uses private inbox route',()=>{const p=notificationPayload('reference');assert.deepEqual(Object.keys(p).sort(),['body','tag','title','url']);assert.equal(p.url,'/operator');assert.equal(p.body,'Nueva consulta. Abrí la app para revisarla.');assert.equal(notificationPayload('',true).tag,'oak-test')})
