import api from './client'

export async function recommendNeighborhood(payload) {
  const { data } = await api.post('/neighborhoods/recommend', payload)
  return data
}

export async function summarizeNeighborhood(payload) {
  const { data } = await api.post('/neighborhoods/summary', payload)
  return data
}
