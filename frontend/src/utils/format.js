export function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').substring(0, 19)
}

export function formatDate(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').substring(0, 10)
}

export function number(value, fallback = '0') {
  if (value === null || value === undefined || value === '') return fallback
  const n = Number(value)
  return Number.isFinite(n) ? n.toLocaleString() : fallback
}

export function authorName(item) {
  return item?.authorName || item?.authorUserId || ''
}
