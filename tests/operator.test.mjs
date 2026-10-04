import test from 'node:test'
import assert from 'node:assert/strict'
import {validateQuote,validateVisit} from '../src/operator/model.js'
const visit={requestId:'DEMO',date:'2099-10-04',time:'10:00',duration:60}
test('agenda permits a visit without automatically confirming it',()=>assert.equal(validateVisit(visit,[]),''))
test('agenda prevents overlaps including travel buffer',()=>{assert.ok(validateVisit({...visit,time:'10:45'},[visit]));assert.ok(validateVisit({...visit,time:'11:15'},[visit]));assert.equal(validateVisit({...visit,time:'11:30'},[visit]),'');assert.equal(validateVisit(visit,[{...visit,state:'Cancelada'}]),'')})
test('agenda rejects past or incomplete visits',()=>{assert.ok(validateVisit({...visit,date:'2000-01-01'},[]));assert.ok(validateVisit({...visit,duration:-10},[]));assert.ok(validateVisit({...visit,requestId:''},[]))})
test('quote requires scope and a positive amount',()=>{assert.ok(validateQuote('','Assessment'));assert.ok(validateQuote('100',''));assert.ok(validateQuote('-10','Assessment'));assert.equal(validateQuote('100.50','Sample scope'),'')})
