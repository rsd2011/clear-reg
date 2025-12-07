// RealGrid 라이선스 키와 전역 기본 설정을 지연 초기화
// 🚀 성능 최적화: 필요 시점까지 RealGrid 로드 지연

let isInitialized = false

// RealGrid 초기화 함수 (지연 로딩)
export async function initializeRealGrid() {
  if (isInitialized) return

  // 동적 import로 RealGrid와 JSZip 로드
  const [RealGrid, JSZip] = await Promise.all([
    import('realgrid'),
    import('jszip'),
  ])

  // RealGrid Excel 내보내기를 위해 JSZip을 글로벌로 노출
  if (typeof window !== 'undefined') {
    (window as Window & { JSZip?: typeof JSZip.default }).JSZip = JSZip.default
  }

  // 라이선스 키 설정
  RealGrid.default.setLicenseKey('upVcPE+wPOmtLjqyBIh9RkM/nBOseBrflwxYpzGZyYm9cY8amGDkiMnVeQKUHJDjW2y71jtk+wulbe73I/Iwr4bU8JyIza+HDufv8SUhNy7xHp5M+wl2RGSe3PbBoyaMRPbok3NSHhx/ITZOhMrz/SkjqYJvMeObkxValpjT3ck=')

  // 권장 전역 설정 적용
  RealGrid.default.setDefault({
    edit: {
      commitByCell: true,
      commitWhenLeave: true,
      crossWhenExitLast: true,
      exceptDataClickWhenButton: true,
    },
    display: {
      editItemMerging: true,
      rowResizable: true,
    },
    checkBar: {
      syncHeadCheck: true,
    },
  } as Parameters<typeof RealGrid.default.setDefault>[0])

  isInitialized = true
}

// RealGrid 초기화 상태 확인
export function isRealGridInitialized() {
  return isInitialized
}

export default defineNuxtPlugin({
  name: 'realgrid',
  parallel: true, // 병렬 로딩 허용 (다른 플러그인 블로킹 안함)
  async setup() {
    // 🚀 플러그인 등록 시점에는 초기화하지 않음
    // RealGrid 컴포넌트가 마운트될 때 initializeRealGrid() 호출
    // 이렇게 하면 RealGrid를 사용하지 않는 페이지에서는 로드되지 않음
  },
})
