import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import Root from './Root.jsx'
import './styles.css'
import './site-upgrades.css'

const operator = window.location.pathname.startsWith('/operator')
document.documentElement.lang = operator ? 'es' : 'en'
if (operator) document.title = 'Oak & Ember | Team Service Manager'

createRoot(document.getElementById('root')).render(<StrictMode><Root /></StrictMode>)
