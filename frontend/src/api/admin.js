import api from './client'

export async function fetchMembers() {
  const { data } = await api.get('/admin/members')
  return data
}

export async function changeMemberRole(userId, role) {
  const { data } = await api.patch(`/admin/members/${encodeURIComponent(userId)}/role`, { role })
  return data
}
