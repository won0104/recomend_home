<script setup>
import { nextTick, onBeforeUnmount, watch, ref } from 'vue'
import { defaultCenter, loadKakaoMap } from '../utils/kakaoMap'

const props = defineProps({
  candidates: { type: Array, default: () => [] }
})

const mapEl = ref(null)
const errorMessage = ref('')
let map = null
let markers = []
let infoWindows = []

function clearMarkers() {
  markers.forEach((marker) => marker.setMap(null))
  infoWindows.forEach((infoWindow) => infoWindow.close())
  markers = []
  infoWindows = []
}

function markerContent(candidate, index) {
  return `
    <div style="padding:8px 10px; min-width:150px; line-height:1.35;">
      <div style="font-weight:700;">${index + 1}. ${candidate.name || ''}</div>
      <div style="font-size:12px; color:#475569;">${candidate.address || ''}</div>
    </div>
  `
}

function renderCandidates() {
  if (!map) return
  clearMarkers()

  const valid = props.candidates
    .map((candidate, index) => ({ candidate, index }))
    .filter(({ candidate }) => Number.isFinite(Number(candidate.latitude)) && Number.isFinite(Number(candidate.longitude)))

  if (!valid.length) return

  const bounds = new window.kakao.maps.LatLngBounds()
  valid.forEach(({ candidate, index }) => {
    const position = new window.kakao.maps.LatLng(Number(candidate.latitude), Number(candidate.longitude))
    const marker = new window.kakao.maps.Marker({ map, position })
    const infoWindow = new window.kakao.maps.InfoWindow({
      content: markerContent(candidate, index),
      disableAutoPan: true
    })

    const openInfo = () => {
      infoWindows.forEach((item) => item.close())
      infoWindow.open(map, marker)
    }
    window.kakao.maps.event.addListener(marker, 'click', openInfo)
    window.kakao.maps.event.addListener(marker, 'mouseover', openInfo)
    window.kakao.maps.event.addListener(marker, 'mouseout', () => {
      infoWindow.close()
    })

    markers.push(marker)
    infoWindows.push(infoWindow)
    bounds.extend(position)
  })

  if (valid.length === 1) {
    map.setCenter(bounds.getSouthWest())
    map.setLevel(4)
  } else {
    map.setBounds(bounds)
  }
}

async function init() {
  try {
    const kakao = await loadKakaoMap()
    await nextTick()
    const center = defaultCenter()
    map = new kakao.maps.Map(mapEl.value, {
      center: new kakao.maps.LatLng(center.lat, center.lng),
      level: 6
    })
    renderCandidates()
  } catch {
    errorMessage.value = '지도를 불러오지 못했습니다.'
  }
}

watch(() => props.candidates, renderCandidates, { deep: true })
onBeforeUnmount(clearMarkers)
init()
</script>

<template>
  <div class="chat-map">
    <div ref="mapEl" class="chat-map-canvas"></div>
    <p v-if="errorMessage" class="chat-map-error">{{ errorMessage }}</p>
  </div>
</template>
