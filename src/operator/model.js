export const statuses = ['Nueva', 'Por contactar', 'Esperando información', 'Presupuesto enviado', 'Aceptada', 'Cerrada']

export function validateVisit(visit, visits) {
  if (!visit.date || !visit.time || !visit.duration || !visit.requestId) return 'Completá fecha, hora, duración y consulta.'
  const start = new Date(`${visit.date}T${visit.time}`).getTime()
  if (!Number.isFinite(start) || start < Date.now()) return 'Elegí una fecha y hora futuras.'
  const duration = Number(visit.duration)
  if (!Number.isFinite(duration) || duration < 30 || duration > 480) return 'La duración debe ser de 30 a 480 minutos.'
  const end = start + duration * 60000
  for (const other of visits.filter(item => item.state !== 'Cancelada')) {
    const otherStart = new Date(`${other.date}T${other.time}`).getTime()
    const otherEnd = otherStart + Number(other.duration) * 60000
    if (start < otherEnd + 30 * 60000 && end + 30 * 60000 > otherStart) return 'El horario se superpone con otra visita o su margen de traslado de 30 minutos.'
  }
  return ''
}

export function validateQuote(amount, scope) {
  if (!scope.trim()) return 'Describí el alcance del presupuesto.'
  if (!Number.isFinite(Number(amount)) || Number(amount) <= 0) return 'Ingresá un importe positivo en USD.'
  return ''
}
