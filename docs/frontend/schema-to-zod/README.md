# DraftFormSchema → Zod 변환 유틸리티

백엔드에서 전달받은 `DraftFormSchema` JSON을 Zod 스키마로 변환하여
Nuxt 4 프론트엔드에서 폼 유효성 검증에 사용합니다.

## 설치

```bash
# Zod 설치
pnpm add zod
```

## 파일 복사

이 디렉토리의 파일들을 프론트엔드 프로젝트에 복사합니다:

```
frontend/
└── utils/
    └── form-schema/
        ├── types.ts          # TypeScript 타입 정의
        ├── schema-to-zod.ts  # Zod 변환 유틸리티
        └── index.ts          # 내보내기
```

## 사용법

### 기본 사용

```typescript
import { schemaToZod, createInitialValues } from '@/utils/form-schema';
import type { DraftFormSchema } from '@/utils/form-schema/types';

// 1. 백엔드에서 스키마 조회
const schema: DraftFormSchema = await $fetch('/api/drafts/templates/1/schema');

// 2. Zod 스키마로 변환
const zodSchema = schemaToZod(schema);

// 3. 초기값 생성
const initialValues = createInitialValues(schema);

// 4. 폼 데이터 유효성 검증
const result = zodSchema.safeParse(formData);
if (!result.success) {
  console.error(result.error.issues);
}
```

### VeeValidate + Zod 통합

```typescript
import { useForm } from 'vee-validate';
import { toTypedSchema } from '@vee-validate/zod';
import { schemaToZod, createInitialValues } from '@/utils/form-schema';

const props = defineProps<{
  schema: DraftFormSchema;
}>();

const zodSchema = computed(() => schemaToZod(props.schema));
const validationSchema = computed(() => toTypedSchema(zodSchema.value));

const { handleSubmit, errors } = useForm({
  validationSchema,
  initialValues: createInitialValues(props.schema),
});

const onSubmit = handleSubmit((values) => {
  // 유효성 검증 통과 후 제출
  console.log(values);
});
```

### FormKit + Zod 통합

```typescript
import { createZodPlugin } from '@formkit/zod';
import { schemaToZod } from '@/utils/form-schema';

const zodPlugin = createZodPlugin(schemaToZod(schema));
```

## 지원 필드 타입

| 필드 타입 | Zod 스키마 | 비고 |
|----------|-----------|------|
| `text` | `z.string()` | email, url, tel 등 inputType 지원 |
| `number` | `z.number()` | min, max, 정수 검증 지원 |
| `date` | `z.string().datetime() \| z.date()` | 날짜 범위 검증 지원 |
| `select` | `z.enum()` / `z.array(z.enum())` | 단일/다중 선택 지원 |
| `checkbox` | `z.boolean()` | 필수 동의 검증 지원 |
| `file` | `z.instanceof(File)` | 파일 개수 제한 지원 |
| `array` | `z.array(z.object(...))` | 중첩 필드 지원 |
| `group` | `z.object(...)` | 중첩 필드 그룹 지원 |

## 에러 메시지 커스터마이징

기본 한국어 에러 메시지가 포함되어 있습니다.
커스터마이징이 필요한 경우 `schema-to-zod.ts`를 수정하세요:

```typescript
// 예: 이메일 에러 메시지 변경
if (field.inputType === 'email') {
  schema = schema.email({
    message: `이메일 형식이 올바르지 않습니다. (예: example@domain.com)`,
  });
}
```

## 타입 가드 사용

```typescript
import { isTextField, isSelectField, isGroupField } from '@/utils/form-schema/types';

function renderField(field: FormField) {
  if (isTextField(field)) {
    return <TextInput field={field} />;
  }
  if (isSelectField(field)) {
    return <SelectInput field={field} options={field.options} />;
  }
  if (isGroupField(field)) {
    return (
      <FieldGroup title={field.label}>
        {field.fields.map(renderField)}
      </FieldGroup>
    );
  }
}
```

## 백엔드 스키마 예시

```json
{
  "version": "1.0",
  "fields": [
    {
      "type": "text",
      "name": "title",
      "label": "제목",
      "required": true,
      "minLength": 1,
      "maxLength": 100
    },
    {
      "type": "select",
      "name": "category",
      "label": "카테고리",
      "required": true,
      "options": [
        { "value": "A", "label": "유형 A" },
        { "value": "B", "label": "유형 B" }
      ]
    }
  ],
  "layout": {
    "columns": 1,
    "sections": []
  },
  "attachmentConfig": {
    "enabled": true,
    "required": false,
    "maxFiles": 5
  }
}
```

## 관련 백엔드 클래스

- `DraftFormSchema` - 메인 스키마 클래스
- `FormField` - sealed interface (TextField, NumberField 등)
- `FormSchemaBuilders` - 업무 유형별 사전 정의 스키마
