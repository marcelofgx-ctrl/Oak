import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import Root from './Root.jsx'
import './styles.css'
if (window.location.pathname.startsWith('/operator')) document.title = 'Oak & Ember | Panel de gestión · Demo'
createRoot(document.getElementById('root')).render(<StrictMode><Root /></StrictMode>)
