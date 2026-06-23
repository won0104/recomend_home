import api from './client'

export async function recommendNeighborhood(payload) {
  const { data } = await api.post('/neighborhoods/recommend', payload)
  return data
}
