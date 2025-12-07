<script setup lang="ts">
/**
 * RealGrid 상태바 컴포넌트
 *
 * 선택 영역의 요약 정보(합계, 평균, 최대, 최소, 개수)를 표시합니다.
 */

import type { RealGridSelectionSummary } from '~/types/realgrid'

interface Props {
  /** 선택 요약 정보 */
  summary: RealGridSelectionSummary
}

defineProps<Props>()
</script>

<template>
  <div
    v-if="summary.numericCount > 0"
    class="realgrid-status-bar"
  >
    <span class="realgrid-status-bar__item">
      <strong>합계:</strong> {{ summary.sum.toLocaleString() }}
    </span>
    <span class="realgrid-status-bar__item">
      <strong>평균:</strong> {{ summary.average.toFixed(2) }}
    </span>
    <span class="realgrid-status-bar__item">
      <strong>최대:</strong> {{ summary.max.toLocaleString() }}
    </span>
    <span class="realgrid-status-bar__item">
      <strong>최소:</strong> {{ summary.min.toLocaleString() }}
    </span>
    <span class="realgrid-status-bar__item">
      <strong>개수:</strong> {{ summary.count }}
    </span>
  </div>
</template>

<style scoped>
@import './styles/realgrid-status-bar.css';
</style>
