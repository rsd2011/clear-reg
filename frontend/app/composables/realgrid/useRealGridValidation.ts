/**
 * RealGrid 유효성 검사 Composable
 *
 * 셀 단위 및 행 단위 유효성 검사:
 * - 필수 입력 검사
 * - 최소/최대값 검사
 * - 범위 검사
 * - 패턴 검사
 * - 커스텀 검증 함수
 */

import type { GridView, LocalDataProvider } from 'realgrid'
import type {
  RealGridValidationRule,
  RealGridColumnValidation,
  RealGridValidationResult,
  RealGridValidationOptions,
} from '~/types/realgrid'

// ============================================================================
// 유효성 검사 유틸리티
// ============================================================================

/**
 * 단일 값 검증
 */
const validateValue = (
  value: unknown,
  rules: RealGridValidationRule[],
  row: Record<string, unknown>,
): string | null => {
  for (const rule of rules) {
    switch (rule.type) {
      case 'required':
        if (value === null || value === undefined || value === '') {
          return rule.message
        }
        break

      case 'min':
        if (typeof value === 'number' && typeof rule.value === 'number') {
          if (value < rule.value) {
            return rule.message
          }
        }
        if (typeof value === 'string' && typeof rule.value === 'number') {
          if (value.length < rule.value) {
            return rule.message
          }
        }
        break

      case 'max':
        if (typeof value === 'number' && typeof rule.value === 'number') {
          if (value > rule.value) {
            return rule.message
          }
        }
        if (typeof value === 'string' && typeof rule.value === 'number') {
          if (value.length > rule.value) {
            return rule.message
          }
        }
        break

      case 'range':
        if (typeof value === 'number' && Array.isArray(rule.value) && rule.value.length === 2) {
          const [min, max] = rule.value as [number, number]
          if (value < min || value > max) {
            return rule.message
          }
        }
        break

      case 'pattern':
        if (typeof value === 'string' && rule.value instanceof RegExp) {
          if (!rule.value.test(value)) {
            return rule.message
          }
        }
        if (typeof value === 'string' && typeof rule.value === 'string') {
          const regex = new RegExp(rule.value)
          if (!regex.test(value)) {
            return rule.message
          }
        }
        break

      case 'custom':
        if (rule.validator && !rule.validator(value, row)) {
          return rule.message
        }
        break
    }
  }

  return null
}

/**
 * 행 데이터 검증
 */
const validateRow = (
  row: Record<string, unknown>,
  rowIndex: number,
  validations: RealGridColumnValidation[],
): RealGridValidationResult['errors'] => {
  const errors: RealGridValidationResult['errors'] = []

  for (const validation of validations) {
    const value = row[validation.column]
    const errorMessage = validateValue(value, validation.rules, row)

    if (errorMessage) {
      errors.push({
        row: rowIndex,
        column: validation.column,
        value,
        message: errorMessage,
      })
    }
  }

  return errors
}

// ============================================================================
// Composable
// ============================================================================

export interface UseRealGridValidationInput {
  validations: RealGridColumnValidation[]
  options?: RealGridValidationOptions
  onValidationError?: (result: RealGridValidationResult) => void
}

/**
 * RealGrid 유효성 검사 Composable
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * const { setupValidation, validateAll, validateRow, isValid, errors } = useRealGridValidation({
 *   validations: [
 *     {
 *       column: 'name',
 *       rules: [
 *         { type: 'required', message: '이름은 필수입니다.' },
 *         { type: 'max', value: 50, message: '이름은 50자 이하입니다.' }
 *       ]
 *     },
 *     {
 *       column: 'age',
 *       rules: [
 *         { type: 'range', value: [0, 120], message: '나이는 0-120 사이입니다.' }
 *       ]
 *     },
 *     {
 *       column: 'email',
 *       rules: [
 *         { type: 'pattern', value: /^[\w.-]+@[\w.-]+\.\w+$/, message: '이메일 형식이 올바르지 않습니다.' }
 *       ]
 *     }
 *   ],
 *   options: {
 *     validateOnEdit: true,
 *     showErrorTooltip: true
 *   }
 * })
 *
 * // 그리드 초기화 후
 * setupValidation(gridView, dataProvider)
 *
 * // 저장 전 전체 검증
 * const result = validateAll(dataProvider)
 * if (!result.valid) {
 *   showErrors(result.errors)
 * }
 * </script>
 * ```
 */
export const useRealGridValidation = (input: UseRealGridValidationInput) => {
  const validations = ref<RealGridColumnValidation[]>(input.validations)

  const options = ref<RealGridValidationOptions>(
    input.options || {
      validateOnEdit: true,
      validateOnCommit: true,
      showErrorTooltip: true,
      errorClassName: 'rg-validation-error',
    },
  )

  // 현재 에러 상태
  const errors = ref<RealGridValidationResult['errors']>([])
  const isValid = computed(() => errors.value.length === 0)

  // 에러가 있는 셀 Set
  const errorCells = ref<Set<string>>(new Set())

  /**
   * 셀 키 생성
   */
  const getCellKey = (row: number, column: string): string => {
    return `${row}:${column}`
  }

  /**
   * 에러 셀 스타일 적용
   */
  const applyErrorStyle = (grid: GridView, row: number, column: string, hasError: boolean): void => {
    const cellKey = getCellKey(row, column)

    if (hasError) {
      errorCells.value.add(cellKey)
      // RealGrid 동적 스타일 적용 (setCellStyle은 타입 정의에 없을 수 있음)
      const g = grid as unknown as Record<string, unknown>
      if (typeof g.setCellStyle === 'function') {
        g.setCellStyle(row, column, options.value.errorClassName || 'rg-validation-error')
      }
    }
    else {
      errorCells.value.delete(cellKey)
      const g = grid as unknown as Record<string, unknown>
      if (typeof g.setCellStyle === 'function') {
        g.setCellStyle(row, column, '')
      }
    }
  }

  /**
   * 전체 데이터 검증
   */
  const validateAll = (provider: LocalDataProvider): RealGridValidationResult => {
    const allErrors: RealGridValidationResult['errors'] = []
    const rowCount = provider.getRowCount()

    for (let i = 0; i < rowCount; i++) {
      const row = provider.getJsonRow(i)
      const rowErrors = validateRow(row, i, validations.value)
      allErrors.push(...rowErrors)
    }

    errors.value = allErrors

    const result: RealGridValidationResult = {
      valid: allErrors.length === 0,
      errors: allErrors,
    }

    if (!result.valid && input.onValidationError) {
      input.onValidationError(result)
    }

    return result
  }

  /**
   * 단일 행 검증
   */
  const validateSingleRow = (
    provider: LocalDataProvider,
    rowIndex: number,
  ): RealGridValidationResult => {
    const row = provider.getJsonRow(rowIndex)
    const rowErrors = validateRow(row, rowIndex, validations.value)

    // 기존 에러에서 해당 행 에러 제거 후 새로 추가
    errors.value = errors.value.filter(e => e.row !== rowIndex)
    errors.value.push(...rowErrors)

    const result: RealGridValidationResult = {
      valid: rowErrors.length === 0,
      errors: rowErrors,
    }

    if (!result.valid && input.onValidationError) {
      input.onValidationError(result)
    }

    return result
  }

  /**
   * 단일 셀 검증
   */
  const validateCell = (
    provider: LocalDataProvider,
    rowIndex: number,
    columnName: string,
  ): string | null => {
    const validation = validations.value.find(v => v.column === columnName)
    if (!validation) {
      return null
    }

    const row = provider.getJsonRow(rowIndex)
    const value = row[columnName]
    return validateValue(value, validation.rules, row)
  }

  /**
   * 유효성 검사 설정
   */
  const setupValidation = (grid: GridView, provider: LocalDataProvider): void => {
    // 편집 완료 시 검증
    if (options.value.validateOnEdit) {
      grid.onEditCommit = (_g, index, oldValue, newValue) => {
        // 해당 셀 검증
        const dataRow = (index as unknown as { dataRow?: number }).dataRow ?? 0
        const fieldName = (index as unknown as { fieldName?: string }).fieldName ?? ''
        const row = provider.getJsonRow(dataRow)
        row[fieldName] = newValue

        const validation = validations.value.find(v => v.column === fieldName)
        if (validation) {
          const errorMessage = validateValue(newValue, validation.rules, row)
          applyErrorStyle(grid, dataRow, fieldName, !!errorMessage)

          if (errorMessage && options.value.showErrorTooltip) {
            // 툴팁 표시 (RealGrid의 커스텀 툴팁 또는 alert)
            console.warn(`Validation error at [${dataRow}, ${fieldName}]: ${errorMessage}`)
          }
        }

        return true
      }
    }

    // 행 커밋 시 검증
    if (options.value.validateOnCommit) {
      grid.onValidateRow = ((_g, itemIndex, dataRow, inserting, values) => {
        const row = values as unknown as Record<string, unknown>
        const rowErrors = validateRow(row, dataRow, validations.value)

        if (rowErrors.length > 0) {
          // 첫 번째 에러 메시지 반환
          const firstError = rowErrors[0]
          return { level: 'error', message: firstError?.message ?? '' }
        }

        return { level: 'ignore', message: '' }
      }) as typeof grid.onValidateRow
    }
  }

  /**
   * 유효성 검사 해제
   */
  const teardownValidation = (grid: GridView): void => {
    grid.onEditCommit = undefined
    grid.onValidateRow = undefined
  }

  /**
   * 에러 클리어
   */
  const clearErrors = (): void => {
    errors.value = []
    errorCells.value.clear()
  }

  /**
   * 특정 컬럼의 검증 규칙 추가
   */
  const addValidation = (validation: RealGridColumnValidation): void => {
    const existingIndex = validations.value.findIndex(v => v.column === validation.column)
    if (existingIndex >= 0) {
      validations.value[existingIndex] = validation
    }
    else {
      validations.value.push(validation)
    }
  }

  /**
   * 특정 컬럼의 검증 규칙 제거
   */
  const removeValidation = (columnName: string): void => {
    validations.value = validations.value.filter(v => v.column !== columnName)
  }

  /**
   * 검증 규칙에 규칙 추가
   */
  const addRule = (columnName: string, rule: RealGridValidationRule): void => {
    const validation = validations.value.find(v => v.column === columnName)
    if (validation) {
      validation.rules.push(rule)
    }
    else {
      validations.value.push({
        column: columnName,
        rules: [rule],
      })
    }
  }

  /**
   * 첫 번째 에러 셀로 이동
   */
  const goToFirstError = (grid: GridView): void => {
    if (errors.value.length > 0) {
      const firstError = errors.value[0]
      if (firstError) {
        grid.setCurrent({ itemIndex: firstError.row, column: firstError.column })
        grid.setFocus()
      }
    }
  }

  /**
   * 옵션 업데이트
   */
  const updateOptions = (newOptions: Partial<RealGridValidationOptions>): void => {
    options.value = { ...options.value, ...newOptions }
  }

  return {
    // 설정
    setupValidation,
    teardownValidation,

    // 검증
    validateAll,
    validateSingleRow,
    validateCell,

    // 에러 관리
    clearErrors,
    goToFirstError,

    // 규칙 관리
    addValidation,
    removeValidation,
    addRule,

    // 상태
    errors: readonly(errors),
    isValid,
    errorCells: readonly(errorCells),

    // 옵션
    updateOptions,
    options: readonly(options),
    validations: readonly(validations),
  }
}

export default useRealGridValidation
