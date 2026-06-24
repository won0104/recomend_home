import api from './client'

export async function sendChatMessage(payload) {
  const { data } = await api.post('/chat', payload)
  return data
}
