<script setup>
import { formatDateTime, authorName } from '../utils/format'

defineProps({
  items: { type: Array, default: () => [] },
  basePath: { type: String, required: true },
  emptyText: { type: String, required: true }
})
</script>

<template>
  <table class="table table-hover">
    <thead>
      <tr>
        <th style="width:70px;" class="text-center">No</th>
        <th>제목</th>
        <th style="width:140px;">작성자</th>
        <th style="width:90px;" class="text-end">조회</th>
        <th style="width:160px;">작성일</th>
      </tr>
    </thead>
    <tbody>
      <tr v-if="items.length === 0">
        <td colspan="5" class="empty-state">{{ emptyText }}</td>
      </tr>
      <tr v-for="item in items" :key="item.id">
        <td class="text-center mono small text-muted">{{ item.id }}</td>
        <td>
          <RouterLink :to="`${basePath}/${item.id}`" class="fw-semibold article-link">{{ item.title }}</RouterLink>
        </td>
        <td>{{ authorName(item) }}</td>
        <td class="text-end mono">{{ item.viewCount ?? 0 }}</td>
        <td class="small text-muted mono">{{ formatDateTime(item.createdAt) }}</td>
      </tr>
    </tbody>
  </table>
</template>
