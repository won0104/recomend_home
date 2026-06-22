import api from './client'

export async function fetchHouses(params = {}) {
  const { data } = await api.get('/houses', { params })
  return data
}

export async function fetchHouse(aptSeq) {
  const { data } = await api.get(`/houses/${encodeURIComponent(aptSeq)}`)
  return data
}

export async function fetchHouseDeals(aptSeq, limit = 20) {
  const { data } = await api.get(`/houses/${encodeURIComponent(aptSeq)}/deals`, { params: { limit } })
  return data
}

export async function updateHouse(aptSeq, payload) {
  const { data } = await api.put(`/admin/houses/${encodeURIComponent(aptSeq)}`, payload)
  return data
}
