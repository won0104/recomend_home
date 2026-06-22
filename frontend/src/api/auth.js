import api from './client'

export async function login(payload) {
  const { data } = await api.post('/auth/login', payload)
  return data
}

export async function logout() {
  const { data } = await api.post('/auth/logout')
  return data
}

export async function fetchMe() {
  const { data } = await api.get('/auth/me')
  return data
}

export async function registerMember(payload) {
  const { data } = await api.post('/members', payload)
  return data
}

export async function updateMe(payload) {
  const { data } = await api.put('/members/me', payload)
  return data
}

export async function deleteMe() {
  const { data } = await api.delete('/members/me')
  return data
}
