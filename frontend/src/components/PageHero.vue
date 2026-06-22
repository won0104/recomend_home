<script setup>
defineProps({
  crumbs: { type: Array, default: () => [] },
  title: { type: String, required: true },
  lede: { type: String, default: '' }
})
</script>

<template>
  <section class="page-hero">
    <div class="container">
      <div class="flex-between page-hero-row">
        <div>
          <div v-if="crumbs.length" class="crumbs">
            <template v-for="(crumb, index) in crumbs" :key="`${crumb}-${index}`">
              <span v-if="index > 0" class="sep">›</span>
              <span>{{ crumb }}</span>
            </template>
          </div>
          <h1>{{ title }}</h1>
          <p v-if="lede" class="lede">{{ lede }}</p>
        </div>
        <div v-if="$slots.right" class="page-hero-actions">
          <slot name="right" />
        </div>
      </div>
    </div>
  </section>
</template>
