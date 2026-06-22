import api from './client'

export async function fetchNotices(keyword = '') {
  const { data } = await api.get('/notices', { params: { keyword: keyword || undefined } })
  return data
}

export async function fetchNotice(id) {
  const { data } = await api.get(`/notices/${id}`)
  return data
}

export async function createNotice(payload) {
  const { data } = await api.post('/notices', payload)
  return data
}

export async function updateNotice(id, payload) {
  const { data } = await api.put(`/notices/${id}`, payload)
  return data
}

export async function deleteNotice(id) {
  const { data } = await api.delete(`/notices/${id}`)
  return data
}
