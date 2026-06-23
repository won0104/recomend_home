import api from '../api/client'

export async function startNeighborhoodChat(sessionId) {
  const { data } = await api.post('/chat/neighborhood/start', null, {
    params: sessionId ? { sessionId } : {}
  })
  return data.data
}

export async function sendNeighborhoodChatMessage(payload) {
  const { data } = await api.post('/chat/neighborhood', payload)
  return data.data
}
