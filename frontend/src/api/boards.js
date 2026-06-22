import api from './client'

export async function fetchBoards(keyword = '') {
  const { data } = await api.get('/boards', { params: { keyword: keyword || undefined } })
  return data
}

export async function fetchBoard(id) {
  const { data } = await api.get(`/boards/${id}`)
  return data
}

export async function fetchMyBoards() {
  const { data } = await api.get('/boards/me')
  return data
}

export async function createBoard(payload) {
  const { data } = await api.post('/boards', payload)
  return data
}

export async function updateBoard(id, payload) {
  const { data } = await api.put(`/boards/${id}`, payload)
  return data
}

export async function deleteBoard(id) {
  const { data } = await api.delete(`/boards/${id}`)
  return data
}
