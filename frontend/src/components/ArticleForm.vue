<script setup>
defineProps({
  title: { type: String, default: '' },
  content: { type: String, default: '' },
  pageTitle: { type: String, required: true },
  submitText: { type: String, required: true },
  cancelTo: { type: String, required: true },
  submitting: { type: Boolean, default: false }
})
const emit = defineEmits(['submit'])

function submit(event) {
  const form = new FormData(event.currentTarget)
  emit('submit', {
    title: String(form.get('title') || '').trim(),
    content: String(form.get('content') || '').trim()
  })
}
</script>

<template>
  <main class="container page-container">
    <form @submit.prevent="submit">
      <div class="card form-card">
        <div class="card-header section-head">
          <div class="title"><span class="bar"></span>{{ pageTitle }}</div>
        </div>
        <div class="card-body grid article-form-body">
          <div>
            <label for="title" class="form-label">제목</label>
            <input id="title" name="title" maxlength="200" required class="form-control" :value="title" />
          </div>
          <div>
            <label for="content" class="form-label">내용</label>
            <textarea id="content" name="content" rows="14" required class="form-control article-textarea" :value="content"></textarea>
          </div>
        </div>
        <div class="card-footer flex-between">
          <RouterLink :to="cancelTo" class="btn btn-outline-secondary">취소</RouterLink>
          <button type="submit" class="btn btn-primary" :disabled="submitting">{{ submitText }}</button>
        </div>
      </div>
    </form>
  </main>
</template>
