<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { loadKakaoMap } from '../../utils/kakaoMap'

const props = defineProps({
  houses: { type: Array, default: () => [] },
  dongAddress: { type: String, default: '' },
  highlightedAptSeq: { type: String, default: null },
})

const emit = defineEmits(['select'])

const mapEl = ref(null)
const errorMessage = ref('')
let map = null
let geocoder = null
let overlayMap = {} // aptSeq → {overlay, el}

function clearOverlays() {
  Object.values(overlayMap).forEach(({ overlay }) => overlay.setMap(null))
  overlayMap = {}
}

function makeDot(house, highlighted) {
  const color = highlighted ? '#079455' : '#6b7280'
  const size = highlighted ? '18px' : '14px'
  const el = document.createElement('div')
  el.style.cssText = `
    width:${size};height:${size};border-radius:50%;
    background:${color};border:2px solid #fff;
    box-shadow:0 1px 4px rgba(0,0,0,.35);
    cursor:pointer;transition:transform .15s;
  `
  el.title = house.aptNm ?? ''
  el.addEventListener('click', () => emit('select', house.aptSeq))
  el.addEventListener('mouseenter', () => { el.style.transform = 'scale(1.5)' })
  el.addEventListener('mouseleave', () => { el.style.transform = '' })
  return el
}

async function render() {
  if (!map) return
  clearOverlays()

  const located = props.houses.filter(h => h.latitude && h.longitude)

  if (located.length === 0) {
    if (geocoder && props.dongAddress) {
      geocoder.addressSearch(props.dongAddress, (res, status) => {
        if (status === window.kakao.maps.services.Status.OK && res.length) {
          map.setCenter(new window.kakao.maps.LatLng(Number(res[0].y), Number(res[0].x)))
          map.setLevel(5)
        }
      })
    }
    return
  }

  const bounds = new window.kakao.maps.LatLngBounds()
  located.forEach(house => {
    const pos = new window.kakao.maps.LatLng(house.latitude, house.longitude)
    const highlighted = house.aptSeq === props.highlightedAptSeq
    const el = makeDot(house, highlighted)
    const overlay = new window.kakao.maps.CustomOverlay({
      map, position: pos, content: el,
      yAnchor: 0.5, xAnchor: 0.5,
      zIndex: highlighted ? 10 : 1,
    })
    overlayMap[house.aptSeq] = { overlay, el }
    bounds.extend(pos)
  })

  if (!bounds.isEmpty()) map.setBounds(bounds, 60)
}

async function init() {
  try {
    const kakao = await loadKakaoMap()
    await nextTick()
    map = new kakao.maps.Map(mapEl.value, {
      center: new kakao.maps.LatLng(37.5665, 126.978),
      level: 6,
    })
    geocoder = new kakao.maps.services.Geocoder()
    await render()
  } catch {
    errorMessage.value = '지도를 불러오지 못했습니다.'
  }
}

watch(() => props.houses, render, { deep: true })

watch(() => props.highlightedAptSeq, (next, prev) => {
  // 이전 하이라이트 해제
  if (prev && overlayMap[prev]) {
    const el = overlayMap[prev].el
    el.style.width = '14px'; el.style.height = '14px'
    el.style.background = '#6b7280'
  }
  // 새 하이라이트 적용
  if (next && overlayMap[next]) {
    const el = overlayMap[next].el
    el.style.width = '18px'; el.style.height = '18px'
    el.style.background = '#079455'
    // 지도 중심 이동
    const house = props.houses.find(h => h.aptSeq === next)
    if (house?.latitude && house?.longitude) {
      map.panTo(new window.kakao.maps.LatLng(house.latitude, house.longitude))
    }
  }
})

onBeforeUnmount(clearOverlays)
init()
</script>

<template>
  <div class="nr-detail-mapbox">
    <div ref="mapEl" class="nr-detail-mapbox__map"></div>
    <div v-if="errorMessage" class="nr-map-error">{{ errorMessage }}</div>
  </div>
</template>
