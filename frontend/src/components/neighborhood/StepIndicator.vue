<script setup>
const props = defineProps({
  currentStep: { type: Number, required: true }
})

const steps = [
  { no: 1, label: '위치 설정' },
  { no: 2, label: '거래·주거' },
  { no: 3, label: '예산 설정' },
  { no: 4, label: '생활 설정' }
]

function stepText(stepNo) {
  return stepNo < props.currentStep ? '✓' : String(stepNo)
}
</script>

<template>
  <div class="condition-stepper">
    <div
      v-for="step in steps"
      :key="step.no"
      class="condition-step"
      :class="{ 'is-active': currentStep === step.no, 'is-complete': step.no < currentStep }"
    >
      <span>{{ stepText(step.no) }}</span>
      <strong>{{ step.label }}</strong>
    </div>
  </div>
</template>
