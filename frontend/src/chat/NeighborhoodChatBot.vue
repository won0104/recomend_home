<script setup>
import { nextTick, onMounted, ref } from 'vue'
import ChatCandidateMap from './ChatCandidateMap.vue'
import { sendNeighborhoodChatMessage, startNeighborhoodChat } from './neighborhoodChatApi'

const open = ref(false)
const loading = ref(false)
const input = ref('')
const sessionId = ref(localStorage.getItem('neighborhoodChatSessionId') || '')
const messages = ref([])
const scrollEl = ref(null)

onMounted(start)

async function start() {
  loading.value = true
  try {
    const response = await startNeighborhoodChat(sessionId.value)
    sessionId.value = response.sessionId
    localStorage.setItem('neighborhoodChatSessionId', response.sessionId)
    messages.value = [{ role: 'assistant', text: response.message }]
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

async function submit() {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', text })
  input.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const response = await sendNeighborhoodChatMessage({
      sessionId: sessionId.value,
      message: text
    })
    sessionId.value = response.sessionId
    localStorage.setItem('neighborhoodChatSessionId', response.sessionId)
    messages.value.push({
      role: 'assistant',
      text: response.message,
      candidates: response.placeCandidates || [],
      recommendation: response.recommendation
    })
  } catch (error) {
    messages.value.push({
      role: 'assistant',
      text: error?.response?.data?.message || '응답을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

async function resetChat() {
  input.value = '처음부터 다시'
  await submit()
}

function pickCandidate(index) {
  input.value = `${index + 1}번`
  submit()
}

function quickSend(text) {
  input.value = text
  submit()
}

function scrollToBottom() {
  nextTick(() => {
    if (scrollEl.value) {
      scrollEl.value.scrollTop = scrollEl.value.scrollHeight
    }
  })
}
</script>

<template>
  <div class="neighborhood-chat">
    <button type="button" class="chat-launcher" @click="open = !open">
      <span aria-hidden="true">💬</span>
      동네추천
    </button>

    <section v-if="open" class="chat-panel" aria-label="동네추천 챗봇">
      <header class="chat-header">
        <div>
          <strong>동네추천 챗봇</strong>
          <span>위치, 반경, 예산, 생활환경을 대화로 입력합니다.</span>
        </div>
        <button type="button" @click="resetChat">초기화</button>
      </header>

      <div ref="scrollEl" class="chat-messages">
        <article
          v-for="(message, index) in messages"
          :key="index"
          class="chat-message"
          :class="`is-${message.role}`"
        >
          <p>{{ message.text }}</p>

          <template v-if="message.candidates?.length > 1">
            <ChatCandidateMap
              :candidates="message.candidates"
              @select="pickCandidate"
            />

            <div class="candidate-actions">
              <button
                v-for="(candidate, candidateIndex) in message.candidates"
                :key="candidate.id || candidateIndex"
                type="button"
                @click="pickCandidate(candidateIndex)"
              >
                <strong>{{ candidateIndex + 1 }}. {{ candidate.name }}</strong>
                <span>{{ candidate.address }}</span>
              </button>
            </div>
          </template>

          <ol v-if="message.recommendation?.legalDongCodes?.length" class="recommendation-list">
            <li
              v-for="code in message.recommendation.legalDongCodes.slice(0, 3)"
              :key="code"
            >
              {{ code }}
            </li>
          </ol>
        </article>

        <article v-if="loading" class="chat-message is-assistant">
          <p>잠시만요. 조건을 확인하고 있어요...</p>
        </article>
      </div>

      <div class="chat-quick-actions">
        <button type="button" @click="quickSend('반경 다시')">반경 다시</button>
        <button type="button" @click="quickSend('예산 다시')">예산 다시</button>
        <button type="button" @click="quickSend('생활환경 다시')">생활환경 다시</button>
      </div>

      <form class="chat-input" @submit.prevent="submit">
        <input
          v-model="input"
          type="text"
          placeholder="예: 5키로 정도, 8억 이하, 반려동물 공원 의료"
        />
        <button type="submit" :disabled="loading">전송</button>
      </form>
    </section>
  </div>
</template>
