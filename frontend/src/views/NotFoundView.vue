<script setup>
import { RouterLink } from 'vue-router'
import PageHero from '../components/PageHero.vue'
</script>

<template>
  <PageHero :crumbs="['홈', '오류']" title="페이지를 찾을 수 없습니다." lede="요청하신 SPA 경로가 존재하지 않습니다." />
  <main class="container page-container">
    <div class="card">
      <div class="card-body empty-state">
        <RouterLink to="/" class="btn btn-primary btn-sm">메인으로 이동</RouterLink>
      </div>
    </div>
  </main>
</template>
