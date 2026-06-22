<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { defaultCenter, loadKakaoMap } from '../utils/kakaoMap'

const props = defineProps({
  records: { type: Array, default: () => [] },
  centerText: { type: String, default: '' },
  marker: { type: Object, default: null },
  level: { type: Number, default: 6 },
  height: { type: String, default: '380px' }
})

const mapEl = ref(null)
const errorMessage = ref('')
let map = null
let geocoder = null
let markers = []
let infoWindows = []

function clearMarkers() {
  markers.forEach((marker) => marker.setMap(null))
  infoWindows.forEach((infoWindow) => infoWindow.close())
  markers = []
  infoWindows = []
}

function geocodeAddress(address) {
  return new Promise((resolve) => {
    if (!geocoder || !address) {
      resolve(null)
      return
    }
    geocoder.addressSearch(address, (result, status) => {
      if (status !== window.kakao.maps.services.Status.OK || !result?.length) {
        resolve(null)
        return
      }
      resolve({ lat: Number(result[0].y), lng: Number(result[0].x) })
    })
  })
}

function addMarker(position, content = '') {
  const marker = new window.kakao.maps.Marker({ map, position })
  markers.push(marker)
  if (content) {
    const infoWindow = new window.kakao.maps.InfoWindow({ content })
    window.kakao.maps.event.addListener(marker, 'click', () => {
      infoWindows.forEach((item) => item.close())
      infoWindow.open(map, marker)
    })
    infoWindows.push(infoWindow)
  }
}

async function renderMarker() {
  if (!map || !props.marker) return
  const lat = Number(props.marker.lat)
  const lng = Number(props.marker.lng)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return
  const position = new window.kakao.maps.LatLng(lat, lng)
  addMarker(position, props.marker.content || '')
  map.setCenter(position)
  map.setLevel(props.level)
}

async function renderRecords() {
  if (!map || props.marker || !props.records.length) return
  const bounds = new window.kakao.maps.LatLngBounds()
  const seen = new Set()
  const candidates = props.records.filter((record) => {
    const address = record.address || [record.sidoName, record.gugunName, record.dong || record.umdNm, record.name || record.aptNm].filter(Boolean).join(' ')
    if (!address || seen.has(address)) return false
    seen.add(address)
    record.__address = address
    return true
  }).slice(0, 40)

  for (const record of candidates) {
    const point = await geocodeAddress(record.__address)
    if (!point) continue
    const position = new window.kakao.maps.LatLng(point.lat, point.lng)
    const title = record.name || record.aptNm || '주택 정보'
    addMarker(position, `<div style="padding:8px 10px; min-width:160px; line-height:1.4;"><div style="font-weight:700;">${title}</div><div>${record.dong || record.umdNm || ''}</div></div>`)
    bounds.extend(position)
  }

  if (markers.length) map.setBounds(bounds)
}

async function moveToCenterText() {
  if (!map || !props.centerText) return
  const point = await geocodeAddress(props.centerText)
  if (!point) return
  map.setCenter(new window.kakao.maps.LatLng(point.lat, point.lng))
  map.setLevel(7)
}

async function render() {
  if (!map) return
  clearMarkers()
  await renderMarker()
  await renderRecords()
  await moveToCenterText()
}

async function init() {
  try {
    const kakao = await loadKakaoMap()
    await nextTick()
    const center = defaultCenter()
    map = new kakao.maps.Map(mapEl.value, {
      center: new kakao.maps.LatLng(center.lat, center.lng),
      level: props.level
    })
    geocoder = new kakao.maps.services.Geocoder()
    await render()
  } catch {
    errorMessage.value = 'Kakao 지도 SDK를 불러오지 못했습니다.'
  }
}

watch(() => [props.records, props.centerText, props.marker], render, { deep: true })

onBeforeUnmount(() => clearMarkers())
init()
</script>

<template>
  <div class="kakao-map-wrap" :style="{ height }">
    <div ref="mapEl" class="kakao-map"></div>
    <div v-if="errorMessage" class="map-fallback">{{ errorMessage }}</div>
  </div>
</template>
