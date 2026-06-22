import api from './client'

function normalizeCategories(items) {
  return (Array.isArray(items) ? items : [])
    .map((item) => ({
      code: String(item?.code || '').trim(),
      name: String(item?.name || '').trim()
    }))
    .filter((item) => item.code && item.name)
}

export async function fetchCommercialCategories() {
  const { data } = await api.get('/commercials/categories')
  return normalizeCategories(data.data)
}

export async function fetchCommercialSubcategories(categoryLCode) {
  const { data } = await api.get('/commercials/subcategories', {
    params: { categoryLCode: categoryLCode || undefined }
  })
  return normalizeCategories(data.data)
}

export async function searchCommercials(condition) {
  const { data } = await api.get('/commercials', {
    params: {
      lawdCd: condition.lawdCd || undefined,
      dong: condition.dong || undefined,
      categoryL: condition.categoryL || undefined,
      categoryM: condition.categoryM || undefined,
      keyword: condition.keyword || undefined,
      limit: condition.limit || 200
    }
  })
  return Array.isArray(data.data) ? data.data : []
}
