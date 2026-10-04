import { lazy, Suspense } from 'react'
import App from './App.jsx'
const OperatorApp = lazy(() => import('./operator/LiveOperatorApp.jsx'))
export default function Root() {
  const operator = window.location.pathname === '/operator' || window.location.pathname === '/operator/'
  return <Suspense fallback={<p role="status">Loading…</p>}>{operator ? <OperatorApp/> : <App/>}</Suspense>
}
