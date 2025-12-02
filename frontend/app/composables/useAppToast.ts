import { useToast } from 'primevue/usetoast'

type ToastSeverity = 'success' | 'info' | 'warn' | 'error' | 'secondary' | 'contrast'

interface ToastOptions {
  /** 토스트 제목 */
  summary?: string
  /** 토스트 내용 */
  detail?: string
  /** 자동 닫힘 시간 (ms), 0이면 수동 닫기 */
  life?: number
  /** 닫기 버튼 표시 여부 */
  closable?: boolean
  /** 토스트 그룹 */
  group?: string
}

const DEFAULT_LIFE = 3000

/**
 * 애플리케이션 토스트 알림을 위한 composable
 *
 * @example
 * ```typescript
 * const toast = useAppToast()
 *
 * toast.success('저장되었습니다')
 * toast.error('오류가 발생했습니다', '상세 메시지')
 * toast.info({ summary: '알림', detail: '내용', life: 5000 })
 * ```
 */
export function useAppToast() {
  const toast = useToast()

  function show(severity: ToastSeverity, options: ToastOptions | string, detail?: string) {
    const config = typeof options === 'string'
      ? { summary: options, detail }
      : options

    toast.add({
      severity,
      summary: config.summary,
      detail: config.detail,
      life: config.life ?? DEFAULT_LIFE,
      closable: config.closable ?? true,
      group: config.group,
    })
  }

  return {
    /**
     * 성공 토스트 표시
     */
    success(options: ToastOptions | string, detail?: string) {
      show('success', options, detail)
    },

    /**
     * 정보 토스트 표시
     */
    info(options: ToastOptions | string, detail?: string) {
      show('info', options, detail)
    },

    /**
     * 경고 토스트 표시
     */
    warn(options: ToastOptions | string, detail?: string) {
      show('warn', options, detail)
    },

    /**
     * 오류 토스트 표시
     */
    error(options: ToastOptions | string, detail?: string) {
      show('error', options, detail)
    },

    /**
     * 모든 토스트 제거
     */
    clear(group?: string) {
      toast.removeAllGroups()
      if (group) {
        // PrimeVue Toast는 그룹별 제거를 직접 지원하지 않으므로 전체 제거
        toast.removeAllGroups()
      }
    },

    /**
     * 원본 PrimeVue toast 인스턴스 접근
     */
    raw: toast,
  }
}
