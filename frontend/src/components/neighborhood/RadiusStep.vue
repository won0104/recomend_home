<script setup>
import { computed } from 'vue'

const props = defineProps({
  selectedPlace: { type: Object, default: null },
  selectedAddress: { type: String, default: '' },
  commuteRadius: { type: Number, required: true }
})

const emit = defineEmits(['update:commuteRadius', 'prev', 'next'])

const radius = computed({
  get: () => props.commuteRadius,
  set: (value) => emit('update:commuteRadius', Number(value))
})
</script>

<template>
  <div class="condition-card">
    <div class="condition-card-head">
      <div>
        <h2>중요 반경 범위 설정</h2>
        <p>입력한 회사/학교 위치를 기준으로 원하는 반경 범위를 설정해 주세요.</p>
      </div>
      <div class="condition-tip">
        직선거리 기준으로 최대 30km까지 설정할 수 있습니다.<br />
        실제 이동 거리와 지도/교통 환경 차이가 있을 수 있어요.
      </div>
    </div>

    <div class="selected-origin">
      <span class="pin-dot" aria-hidden="true"></span>
      <div>
        <strong>{{ selectedPlace?.place_name }}</strong>
        <small>{{ selectedAddress }}</small>
      </div>
    </div>

    <!-- 반경 값은 부모 View에 보관되어 다음 단계와 추천 API payload에서 함께 사용됩니다. -->
    <div class="radius-control">
      <div class="radius-control-head">
        <strong>최대 반경 거리 <span>(직선거리 기준)</span></strong>
        <em>{{ radius }}km 이내</em>
      </div>
      <input
        v-model.number="radius"
        class="radius-slider"
        type="range"
        min="5"
        max="30"
        step="5"
        aria-label="최대 반경 거리"
      />
      <!-- 각 left% = (값 - 5) / (30 - 5) * 100 -->
      <div class="radius-marks" aria-hidden="true">
        <span style="left: 0%">5km</span>
        <span style="left: 20%">10km</span>
        <span style="left: 60%">20km</span>
        <span style="left: 100%">30km</span>
      </div>
    </div>

    <p class="condition-example">
      예) {{ radius }}km 설정 시, 입력한 위치로부터 직선거리 {{ radius }}km 이내의 매물을 검색합니다.
    </p>

    <div class="condition-actions">
      <button type="button" class="btn btn-outline-secondary" @click="emit('prev')">이전</button>
      <button type="button" class="btn btn-primary" @click="emit('next')">다음 →</button>
    </div>
  </div>
</template>
