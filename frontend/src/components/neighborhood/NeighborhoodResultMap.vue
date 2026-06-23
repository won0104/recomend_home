<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { loadKakaoMap } from '../../utils/kakaoMap'

const props = defineProps({
  baseLocation: { type: Object, default: null }, // { lat, lng }
  neighborhoods: { type: Array, default: () => [] }, // [{ rank, sidoName, sigunguName, dongName }]
})

const RANK_COLORS = ['#079455', '#22C55E', '#F97316']

const mapEl = ref(null)
const errorMessage = ref('')
let map = null
let geocoder = null
let overlays = []
let baseMarker = null

function clearOverlays() {
  overlays.forEach((o) => o.setMap(null))
  overlays = []
  if (baseMarker) {
    baseMarker.setMap(null)
    baseMarker = null
  }
}

function addNumberOverlay(position, rank) {
  const color = RANK_COLORS[rank - 1] ?? '#888'
  const content = `<div style="
    width:36px;height:36px;border-radius:50%;
    background:${color};color:#fff;font-weight:800;font-size:15px;
    display:flex;align-items:center;justify-content:center;
    border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.25);
    cursor:default;
  ">${rank}</div>`
  const overlay = new window.kakao.maps.CustomOverlay({
    map,
    position,
    content,
    yAnchor: 0.5,
    xAnchor: 0.5,
  })
  overlays.push(overlay)
}

function addBaseMarker(position) {
  const content = `<div style="
    display:flex;flex-direction:column;align-items:center;gap:2px;
  ">
    <div style="
      background:#EF4444;color:#fff;font-size:11px;font-weight:700;
      padding:3px 7px;border-radius:6px;white-space:nowrap;
      box-shadow:0 1px 4px rgba(0,0,0,.25);
    ">내 위치</div>
    <div style="
      width:0;height:0;border-left:6px solid transparent;
      border-right:6px solid transparent;border-top:8px solid #EF4444;
    "></div>
  </div>`
  baseMarker = new window.kakao.maps.CustomOverlay({
    map,
    position,
    content,
    yAnchor: 1,
    xAnchor: 0.5,
  })
}

function geocodeAddress(address) {
  return new Promise((resolve) => {
    if (!geocoder || !address) { resolve(null); return }
    geocoder.addressSearch(address, (result, status) => {
      if (status !== window.kakao.maps.services.Status.OK || !result?.length) {
        resolve(null); return
      }
      resolve(new window.kakao.maps.LatLng(Number(result[0].y), Number(result[0].x)))
    })
  })
}

async function render() {
  if (!map) return
  clearOverlays()

  const bounds = new window.kakao.maps.LatLngBounds()

  if (props.baseLocation?.lat && props.baseLocation?.lng) {
    const pos = new window.kakao.maps.LatLng(props.baseLocation.lat, props.baseLocation.lng)
    addBaseMarker(pos)
    bounds.extend(pos)
  }

  for (const hood of props.neighborhoods) {
    const addr = [hood.sidoName, hood.sigunguName, hood.dongName].filter(Boolean).join(' ')
    const pos = await geocodeAddress(addr)
    if (!pos) continue
    addNumberOverlay(pos, hood.rank)
    bounds.extend(pos)
  }

  if (!bounds.isEmpty()) map.setBounds(bounds, 80)
}

async function init() {
  try {
    const kakao = await loadKakaoMap()
    await nextTick()
    map = new kakao.maps.Map(mapEl.value, {
      center: new kakao.maps.LatLng(37.5665, 126.978),
      level: 7,
    })
    geocoder = new kakao.maps.services.Geocoder()
    await render()
  } catch {
    errorMessage.value = '지도를 불러오지 못했습니다.'
  }
}

watch(() => [props.baseLocation, props.neighborhoods], render, { deep: true })
onBeforeUnmount(clearOverlays)
init()
</script>

<template>
  <div class="nr-map-wrap">
    <div ref="mapEl" class="nr-map"></div>
    <div v-if="errorMessage" class="nr-map-error">{{ errorMessage }}</div>
  </div>
</template>
