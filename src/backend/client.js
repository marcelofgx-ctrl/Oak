import { createClient } from '@supabase/supabase-js'
import { backendUrl, publicKey, intakeKey } from './config'

export const backend = createClient(backendUrl, publicKey)

export async function sendRequest(data, photos, id) {
  if (!navigator.onLine) throw new Error('You appear to be offline. Reconnect and try again; your form is still here.')

  const form = new FormData()
  form.append('request', JSON.stringify({ ...data, id }))
  for (const photo of photos) form.append('photos', photo.file)

  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), 90_000)

  try {
    const response = await fetch(`${backendUrl}/functions/v1/oak-intake`, {
      method: 'POST',
      headers: {
        apikey: intakeKey,
        Authorization: `Bearer ${intakeKey}`,
        Accept: 'application/json',
      },
      body: form,
      signal: controller.signal,
    })

    let result = {}
    try { result = await response.json() } catch { /* A generic message is shown below. */ }
    if (!response.ok || !result.reference) {
      throw new Error(result.error || 'We could not save your request. Please retry; your form is still here.')
    }
    return result.reference
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw new Error('The upload took too long. Check your connection and try again; your form is still here.', { cause: error })
    }
    if (error instanceof TypeError) {
      throw new Error('We could not reach Oak & Ember. Check your connection and try again; your form is still here.', { cause: error })
    }
    throw error
  } finally {
    window.clearTimeout(timeout)
  }
}
