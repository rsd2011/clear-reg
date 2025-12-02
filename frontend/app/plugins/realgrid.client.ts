// RealGrid 라이선스 키와 전역 기본 설정을 앱 시작 시 초기화
import RealGrid from 'realgrid'

export default defineNuxtPlugin(() => {
  // 라이선스 키 설정
  RealGrid.setLicenseKey('upVcPE+wPOmtLjqyBIh9RkM/nBOseBrflwxYpzGZyYm9cY8amGDkiMnVeQKUHJDjW2y71jtk+wulbe73I/Iwr4bU8JyIza+HDufv8SUhNy7xHp5M+wl2RGSe3PbBoyaMRPbok3NSHhx/ITZOhMrz/SkjqYJvMeObkxValpjT3ck=')

  // 권장 전역 설정 적용
  // copyOptions는 ViewOptions에 포함되지 않으므로 별도 타입 단언 사용
  RealGrid.setDefault({
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
  } as Parameters<typeof RealGrid.setDefault>[0])
})
