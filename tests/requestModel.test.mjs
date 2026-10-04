import test from 'node:test'
import assert from 'node:assert/strict'
import { emptyRequest, serviceOptions, todayLocal, validateStep, validatePhotos } from '../src/requestModel.js'
const valid = { ...emptyRequest, service:'cleaning', details:'Seasonal chimney cleaning', name:'Sample Name', phone:'+1 (404) 555-0100', email:'sample@example.com', address:'Sample address', zip:'30301', acknowledge:true }
test('all offered services have a clear request route', () => { for (const service of serviceOptions) { assert.equal(Object.keys(validateStep(0,{...valid,service:service.id})).length,0); assert.ok(service.route); assert.ok(service.detail) } assert.ok(validateStep(0,{...valid,service:'made-up'}).service) })
test('required project and contact fields block progress', () => { assert.ok(validateStep(1,emptyRequest).details); assert.equal(Object.keys(validateStep(2,emptyRequest)).length,3); for(let step=0;step<4;step++) assert.deepEqual(validateStep(step,valid),{}) })
test('phone email ZIP and past-date checks', () => { const errors=validateStep(2,{...valid,phone:'123',email:'bad',zip:'12',date:'2026-10-01'},'2026-10-04'); assert.equal(Object.keys(errors).length,4); assert.deepEqual(validateStep(2,{...valid,zip:'30301-1234',date:'2026-10-04'},'2026-10-04'),{}) })
test('finishing requires explicit demo acknowledgement', () => { assert.ok(validateStep(3,{...valid,acknowledge:false}).acknowledge) })
test('photo limits enforce format, aggregate count, size and empty files', () => { const photo={type:'image/jpeg',size:100}; assert.equal(validatePhotos([photo]),''); assert.ok(validatePhotos(Array(6).fill(photo))); assert.ok(validatePhotos([{type:'image/svg+xml',size:100}])); assert.ok(validatePhotos([{...photo,size:0}])); assert.ok(validatePhotos([{...photo,size:8*1024*1024+1}])); assert.equal(validatePhotos([{type:'image/webp',size:8*1024*1024}]),'') })
test('date uses local calendar components', () => assert.equal(todayLocal(new Date(2026,9,4)),'2026-10-04'))

test("estimate requests require only the chosen contact method, name and ZIP", () => { assert.deepEqual(validateStep(2,{...emptyRequest,name:"Demo",zip:"30301",phone:"4045550100"}),{}); assert.deepEqual(validateStep(2,{...emptyRequest,name:"Demo",zip:"30301",contact:"Email",email:"demo@example.com"}),{}); assert.ok(validateStep(2,{...emptyRequest,name:"Demo",zip:"30301",contact:"Email"}).email) })
