import { lazy, Suspense } from 'react'
import App from './App.jsx'

const OperatorApp = lazy(() => import('./operator/LiveOperatorApp.jsx'))

export default function Root() {
  const operator = import.meta.env.VITE_APP_MODE === 'operator' || window.location.pathname.startsWith('/operator')
  return <Suspense fallback={<p role="status" className="route-loading">Loading Oak &amp; Ember…</p>}>{operator ? <OperatorApp /> : <App />}</Suspense>
}
