import api from './client'

function normalize(items) {
  return (Array.isArray(items) ? items : [])
    .map((item) => ({
      code: String(item?.code || '').trim(),
      name: String(item?.name || '').trim()
    }))
    .filter((item) => item.code && item.name)
    .sort((a, b) => a.name.localeCompare(b.name, 'ko'))
}

export async function fetchSidoList() {
  const { data } = await api.get('/regions/sido')
  return normalize(data.data)
}

export async function fetchSigunguList(sidoCode) {
  if (!sidoCode) return []
  const { data } = await api.get('/regions/sigungu', { params: { sidoCode } })
  return normalize(data.data)
}

export async function fetchEmdList(sigunguCode) {
  if (!sigunguCode) return []
  const { data } = await api.get('/regions/emd', { params: { sigunguCode } })
  return normalize(data.data)
}
