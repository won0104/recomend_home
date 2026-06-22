<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { defaultCenter, loadKakaoMap } from '../../utils/kakaoMap'

const emit = defineEmits(['select-place', 'next'])

const popularKeywords = ['강남구', '마포구', '성동구', '용산구', '송파구']

const keyword = ref('')
const places = ref([])
const pages = ref([])
const currentPage = ref(1)
const loading = ref(false)
const selectedPlace = ref(null)
const message = ref('회사 또는 학교, 관심 있는 건물명이나 동네를 입력해 보세요.')
const mapEl = ref(null)

let kakaoMap = null
let placeService = null
let paginationRef = null
let infoWindow = null
let markers = []

const selectedPlaceTitle = computed(() => selectedPlace.value?.place_name || '')
const selectedPlaceAddress = computed(() => selectedPlace.value?.road_address_name || selectedPlace.value?.address_name || '')

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function clearMarkers() {
  markers.forEach((marker) => marker.setMap(null))
  markers = []
  infoWindow?.close()
}

function markerImage(index) {
  const imageSrc = 'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_number_blue.png'
  const imageSize = new window.kakao.maps.Size(36, 37)
  const imageOptions = {
    spriteSize: new window.kakao.maps.Size(36, 691),
    spriteOrigin: new window.kakao.maps.Point(0, index * 46 + 10),
    offset: new window.kakao.maps.Point(13, 37)
  }
  return new window.kakao.maps.MarkerImage(imageSrc, imageSize, imageOptions)
}

function openInfoWindow(marker, place) {
  const content = `<div style="padding:6px 9px;z-index:1;font-size:13px;font-weight:700;">${escapeHtml(place.place_name)}</div>`
  infoWindow.setContent(content)
  infoWindow.open(kakaoMap, marker)
}

function selectPlace(place, index) {
  const marker = markers[index]
  selectedPlace.value = place
  emit('select-place', place)

  // 선택한 후보를 지도 중심으로 옮겨 사용자가 다음 단계의 기준 위치를 명확히 확인하게 합니다.
  if (marker) {
    kakaoMap.panTo(marker.getPosition())
    openInfoWindow(marker, place)
  }
}

function renderPlaces(items) {
  clearMarkers()
  places.value = items
  selectedPlace.value = null
  emit('select-place', null)

  const bounds = new window.kakao.maps.LatLngBounds()
  items.forEach((place, index) => {
    const position = new window.kakao.maps.LatLng(Number(place.y), Number(place.x))
    const marker = new window.kakao.maps.Marker({
      map: kakaoMap,
      position,
      image: markerImage(index)
    })

    window.kakao.maps.event.addListener(marker, 'click', () => selectPlace(place, index))
    markers.push(marker)
    bounds.extend(position)
  })

  if (markers.length) kakaoMap.setBounds(bounds)
}

function renderPagination(pagination) {
  paginationRef = pagination
  currentPage.value = pagination.current
  pages.value = Array.from({ length: pagination.last }, (_, index) => index + 1)
}

function searchPlaces() {
  const trimmed = keyword.value.trim()
  if (!trimmed) {
    message.value = '검색할 키워드를 입력해 주세요.'
    places.value = []
    pages.value = []
    selectedPlace.value = null
    emit('select-place', null)
    clearMarkers()
    return
  }

  if (!placeService) {
    message.value = '카카오 지도 서비스를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.'
    return
  }

  loading.value = true
  message.value = ''
  placeService.keywordSearch(trimmed, (data, status, pagination) => {
    loading.value = false
    if (status === window.kakao.maps.services.Status.OK) {
      renderPlaces(data)
      renderPagination(pagination)
      message.value = `${data.length}개의 장소를 찾았습니다. 기준 위치로 사용할 장소를 선택해 주세요.`
      return
    }

    places.value = []
    pages.value = []
    selectedPlace.value = null
    emit('select-place', null)
    clearMarkers()
    if (status === window.kakao.maps.services.Status.ZERO_RESULT) {
      message.value = '검색 결과가 없습니다. 건물명, 지하철역, 동네 이름으로 다시 검색해 보세요.'
    } else {
      message.value = '검색 결과를 불러오는 중 오류가 발생했습니다.'
    }
  })
}

function searchPopular(value) {
  keyword.value = value
  searchPlaces()
}

function movePage(page) {
  if (!paginationRef || page === currentPage.value) return
  paginationRef.gotoPage(page)
}

function goNext() {
  if (!selectedPlace.value) {
    message.value = '다음 단계로 이동하려면 먼저 기준 위치를 선택해 주세요.'
    return
  }
  emit('next', selectedPlace.value)
}

async function initMap() {
  try {
    const kakao = await loadKakaoMap()
    await nextTick()
    const center = defaultCenter()
    kakaoMap = new kakao.maps.Map(mapEl.value, {
      center: new kakao.maps.LatLng(center.lat, center.lng),
      level: 3
    })
    placeService = new kakao.maps.services.Places()
    infoWindow = new kakao.maps.InfoWindow({ zIndex: 1 })
  } catch {
    message.value = '카카오 지도 SDK를 불러오지 못했습니다.'
  }
}

onBeforeUnmount(() => clearMarkers())
initMap()
</script>

<template>
  <section class="kakao-place-search">
    <div class="place-search-top">
      <div class="place-search-guide">
        <strong>기준 위치를 먼저 정해보세요</strong>
        <p>다니게 될 회사나 학교, 자주 방문하는 장소를 입력하면 그 위치를 기준으로 어울리는 동네를 찾아드릴게요.</p>
      </div>

      <form class="place-search-form" @submit.prevent="searchPlaces">
        <label class="visually-hidden" for="kakao-place-keyword">건물 또는 동네 검색</label>
        <div class="place-search-input-row">
          <span class="place-search-icon" aria-hidden="true">⌕</span>
          <input
            id="kakao-place-keyword"
            v-model="keyword"
            class="form-control"
            type="text"
            placeholder="회사 또는 학교 주소, 건물명, 동네를 입력해 주세요"
          />
          <button class="btn btn-primary" type="submit" :disabled="loading">
            {{ loading ? '검색 중' : '장소 검색' }}
          </button>
        </div>
      </form>

      <div class="place-popular-row" aria-label="인기 지역">
        <span>인기 지역:</span>
        <button
          v-for="item in popularKeywords"
          :key="item"
          type="button"
          class="place-popular-button"
          @click="searchPopular(item)"
        >
          {{ item }}
        </button>
      </div>
    </div>

    <div class="place-search-workspace">
      <div class="place-search-map" ref="mapEl"></div>

      <aside class="place-search-panel">
        <div class="place-panel-head">
          <strong>검색 결과</strong>
          <span class="badge text-bg-secondary">{{ places.length }}곳</span>
        </div>
        <p class="place-search-message small text-muted">{{ message }}</p>

        <ol class="place-result-list">
          <li
            v-for="(place, index) in places"
            :key="place.id || `${place.place_name}-${place.x}-${place.y}`"
            class="place-result-item"
            :class="{ 'is-selected': selectedPlace?.id === place.id }"
            @click="selectPlace(place, index)"
            @keydown.enter="selectPlace(place, index)"
            tabindex="0"
          >
            <span class="place-marker-no">{{ index + 1 }}</span>
            <div class="place-result-info">
              <strong>{{ place.place_name }}</strong>
              <span>{{ place.road_address_name || place.address_name }}</span>
              <small v-if="place.road_address_name && place.address_name">{{ place.address_name }}</small>
              <small v-if="place.phone">{{ place.phone }}</small>
            </div>
          </li>
        </ol>

        <div class="selected-place-box" :class="{ 'is-empty': !selectedPlace }">
          <template v-if="selectedPlace">
            <span class="small text-muted">선택한 기준 위치</span>
            <strong>{{ selectedPlaceTitle }}</strong>
            <small>{{ selectedPlaceAddress }}</small>
          </template>
          <template v-else>
            <span class="small text-muted">지도 또는 목록에서 기준 위치를 선택해 주세요.</span>
          </template>
        </div>

        <nav v-if="pages.length > 1" class="place-pagination" aria-label="Kakao place result pages">
          <button
            v-for="page in pages"
            :key="page"
            type="button"
            class="page"
            :class="{ 'is-active': page === currentPage }"
            @click="movePage(page)"
          >
            {{ page }}
          </button>
        </nav>

        <div class="place-next-footer">
          <button type="button" class="btn btn-primary" :disabled="!selectedPlace" @click="goNext">
            다음 →
          </button>
        </div>
      </aside>
    </div>
  </section>
</template>
