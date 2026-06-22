import api from './client'

export async function fetchFavorites() {
  const { data } = await api.get('/favorites')
  return data
}

export async function addFavorite(payload) {
  const { data } = await api.post('/favorites', payload)
  return data
}

export async function deleteFavorite(key) {
  const { data } = await api.delete(`/favorites/${encodeURIComponent(key)}`)
  return data
}
