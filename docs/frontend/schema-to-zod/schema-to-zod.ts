/**
 * DraftFormSchema → Zod 스키마 변환 유틸리티
 *
 * 백엔드에서 전달받은 DraftFormSchema JSON을 Zod 스키마로 변환합니다.
 * Nuxt 4 프론트엔드에서 폼 유효성 검증에 사용합니다.
 *
 * @example
 * ```typescript
 * import { schemaToZod } from './schema-to-zod';
 * import type { DraftFormSchema } from './types';
 *
 * const schema: DraftFormSchema = await $fetch('/api/drafts/templates/1/schema');
 * const zodSchema = schemaToZod(schema);
 *
 * // 폼 데이터 유효성 검증
 * const result = zodSchema.safeParse(formData);
 * if (!result.success) {
 *   console.error(result.error.issues);
 * }
 * ```
 */

import { z, type ZodTypeAny, type ZodObject, type ZodRawShape } from 'zod';
import type {
  DraftFormSchema,
  FormField,
  TextField,
  NumberField,
  DateField,
  SelectField,
  CheckboxField,
  FileField,
  ArrayField,
  GroupField,
} from './types';

// ============================================================================
// 메인 변환 함수
// ============================================================================

/**
 * DraftFormSchema를 Zod 스키마로 변환합니다.
 *
 * @param schema - 백엔드에서 전달받은 DraftFormSchema
 * @returns Zod 객체 스키마
 */
export function schemaToZod(schema: DraftFormSchema): ZodObject<ZodRawShape> {
  const shape: ZodRawShape = {};

  for (const field of schema.fields) {
    shape[field.name] = fieldToZod(field);
  }

  return z.object(shape);
}

/**
 * 개별 FormField를 Zod 타입으로 변환합니다.
 *
 * @param field - FormField
 * @returns Zod 타입
 */
export function fieldToZod(field: FormField): ZodTypeAny {
  switch (field.type) {
    case 'text':
      return textFieldToZod(field);
    case 'number':
      return numberFieldToZod(field);
    case 'date':
      return dateFieldToZod(field);
    case 'select':
      return selectFieldToZod(field);
    case 'checkbox':
      return checkboxFieldToZod(field);
    case 'file':
      return fileFieldToZod(field);
    case 'array':
      return arrayFieldToZod(field);
    case 'group':
      return groupFieldToZod(field);
    default:
      // 알 수 없는 타입은 any로 처리
      return z.any();
  }
}

// ============================================================================
// 개별 필드 타입 변환
// ============================================================================

function textFieldToZod(field: TextField): ZodTypeAny {
  let schema = z.string();

  // 최소/최대 길이
  if (field.minLength != null) {
    schema = schema.min(field.minLength, {
      message: `${field.label}은(는) 최소 ${field.minLength}자 이상이어야 합니다.`,
    });
  }
  if (field.maxLength != null) {
    schema = schema.max(field.maxLength, {
      message: `${field.label}은(는) 최대 ${field.maxLength}자까지 입력할 수 있습니다.`,
    });
  }

  // 정규식 패턴
  if (field.pattern) {
    schema = schema.regex(new RegExp(field.pattern), {
      message: `${field.label} 형식이 올바르지 않습니다.`,
    });
  }

  // 입력 타입별 추가 검증
  if (field.inputType === 'email') {
    schema = schema.email({
      message: `올바른 이메일 주소를 입력해주세요.`,
    });
  } else if (field.inputType === 'url') {
    schema = schema.url({
      message: `올바른 URL을 입력해주세요.`,
    });
  }

  return wrapWithRequired(schema, field.required, field.label);
}

function numberFieldToZod(field: NumberField): ZodTypeAny {
  let schema = z.number({
    invalid_type_error: `${field.label}은(는) 숫자여야 합니다.`,
  });

  // 최소/최대값
  if (field.min != null) {
    schema = schema.min(field.min, {
      message: `${field.label}은(는) ${field.min} 이상이어야 합니다.`,
    });
  }
  if (field.max != null) {
    schema = schema.max(field.max, {
      message: `${field.label}은(는) ${field.max} 이하여야 합니다.`,
    });
  }

  // 정수 여부 (precision이 0이면 정수)
  if (field.precision === 0) {
    schema = schema.int({
      message: `${field.label}은(는) 정수여야 합니다.`,
    });
  }

  return wrapWithRequired(schema, field.required, field.label);
}

function dateFieldToZod(field: DateField): ZodTypeAny {
  // 날짜는 문자열(ISO format) 또는 Date 객체로 처리
  let schema = z.union([
    z.string().datetime({ message: `올바른 날짜 형식이 아닙니다.` }),
    z.date(),
  ]);

  // 날짜 범위 검증은 refine으로 처리
  if (field.minDate || field.maxDate) {
    schema = schema.refine(
      (val) => {
        const date = typeof val === 'string' ? new Date(val) : val;
        if (field.minDate && date < new Date(field.minDate)) return false;
        if (field.maxDate && date > new Date(field.maxDate)) return false;
        return true;
      },
      {
        message: `${field.label}이(가) 허용 범위를 벗어났습니다.`,
      }
    );
  }

  return wrapWithRequired(schema, field.required, field.label);
}

function selectFieldToZod(field: SelectField): ZodTypeAny {
  const validValues = field.options.map((opt) => opt.value);

  if (field.multiple) {
    // 다중 선택
    let schema = z.array(z.enum(validValues as [string, ...string[]]));

    if (field.required) {
      schema = schema.min(1, {
        message: `${field.label}을(를) 하나 이상 선택해주세요.`,
      });
    }

    return schema;
  } else {
    // 단일 선택
    const schema = z.enum(validValues as [string, ...string[]], {
      errorMap: () => ({
        message: `${field.label}에서 올바른 옵션을 선택해주세요.`,
      }),
    });

    return wrapWithRequired(schema, field.required, field.label);
  }
}

function checkboxFieldToZod(field: CheckboxField): ZodTypeAny {
  let schema = z.boolean();

  // 필수 체크박스 (동의 필수 등)
  if (field.required) {
    schema = schema.refine((val) => val === true, {
      message: `${field.checkLabel || field.label}에 동의해주세요.`,
    });
  }

  return schema;
}

function fileFieldToZod(field: FileField): ZodTypeAny {
  // 파일은 File 객체 또는 파일 정보 객체로 처리
  const fileSchema = z.union([
    z.instanceof(File),
    z.object({
      name: z.string(),
      size: z.number(),
      type: z.string(),
    }),
  ]);

  if (field.multiple) {
    let schema = z.array(fileSchema);

    if (field.maxFiles != null) {
      schema = schema.max(field.maxFiles, {
        message: `파일은 최대 ${field.maxFiles}개까지 업로드할 수 있습니다.`,
      });
    }

    if (field.required) {
      schema = schema.min(1, {
        message: `${field.label}을(를) 업로드해주세요.`,
      });
    }

    return schema;
  } else {
    return wrapWithRequired(fileSchema, field.required, field.label);
  }
}

function arrayFieldToZod(field: ArrayField): ZodTypeAny {
  // 배열 항목의 스키마 생성
  const itemShape: ZodRawShape = {};
  for (const itemField of field.itemFields) {
    itemShape[itemField.name] = fieldToZod(itemField);
  }
  const itemSchema = z.object(itemShape);

  let schema = z.array(itemSchema);

  // 최소/최대 항목 수
  if (field.minItems != null) {
    schema = schema.min(field.minItems, {
      message: `${field.label}은(는) 최소 ${field.minItems}개 이상 입력해야 합니다.`,
    });
  }
  if (field.maxItems != null) {
    schema = schema.max(field.maxItems, {
      message: `${field.label}은(는) 최대 ${field.maxItems}개까지 입력할 수 있습니다.`,
    });
  }

  if (field.required && field.minItems == null) {
    schema = schema.min(1, {
      message: `${field.label}을(를) 하나 이상 입력해주세요.`,
    });
  }

  return schema;
}

function groupFieldToZod(field: GroupField): ZodTypeAny {
  // 그룹은 중첩 객체로 처리
  const shape: ZodRawShape = {};
  for (const nestedField of field.fields) {
    shape[nestedField.name] = fieldToZod(nestedField);
  }

  return z.object(shape);
}

// ============================================================================
// 유틸리티 함수
// ============================================================================

/**
 * 필수 여부에 따라 스키마를 래핑합니다.
 */
function wrapWithRequired(
  schema: ZodTypeAny,
  required: boolean,
  label: string
): ZodTypeAny {
  if (required) {
    // 문자열의 경우 빈 문자열도 에러로 처리
    if (schema instanceof z.ZodString) {
      return schema.min(1, { message: `${label}을(를) 입력해주세요.` });
    }
    return schema;
  }

  return schema.optional().nullable();
}

// ============================================================================
// 추가 유틸리티
// ============================================================================

/**
 * 스키마에서 특정 필드의 Zod 타입만 추출합니다.
 */
export function getFieldZodType(
  schema: DraftFormSchema,
  fieldName: string
): ZodTypeAny | null {
  const field = schema.fields.find((f) => f.name === fieldName);
  if (!field) return null;
  return fieldToZod(field);
}

/**
 * 폼 데이터의 초기값을 스키마 기반으로 생성합니다.
 */
export function createInitialValues(
  schema: DraftFormSchema
): Record<string, unknown> {
  const values: Record<string, unknown> = { ...schema.defaultValues };

  for (const field of schema.fields) {
    if (!(field.name in values)) {
      values[field.name] = getDefaultValue(field);
    }
  }

  return values;
}

/**
 * 필드 타입별 기본값을 반환합니다.
 */
function getDefaultValue(field: FormField): unknown {
  switch (field.type) {
    case 'text':
      return '';
    case 'number':
      return null;
    case 'date':
      return null;
    case 'select':
      return field.multiple ? [] : null;
    case 'checkbox':
      return field.defaultValue;
    case 'file':
      return field.multiple ? [] : null;
    case 'array':
      return [];
    case 'group': {
      const groupValues: Record<string, unknown> = {};
      for (const nestedField of field.fields) {
        groupValues[nestedField.name] = getDefaultValue(nestedField);
      }
      return groupValues;
    }
    default:
      return null;
  }
}
