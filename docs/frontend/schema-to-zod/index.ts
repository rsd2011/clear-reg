/**
 * DraftFormSchema → Zod 변환 유틸리티 모듈
 *
 * @example
 * ```typescript
 * import {
 *   schemaToZod,
 *   fieldToZod,
 *   createInitialValues,
 *   getFieldZodType,
 * } from '@/utils/form-schema';
 *
 * import type {
 *   DraftFormSchema,
 *   FormField,
 *   TextField,
 *   SelectField,
 * } from '@/utils/form-schema';
 * ```
 */

// 변환 유틸리티
export {
  schemaToZod,
  fieldToZod,
  createInitialValues,
  getFieldZodType,
} from './schema-to-zod';

// 타입 정의
export type {
  DraftFormSchema,
  FormField,
  FieldType,
  BaseField,
  TextField,
  NumberField,
  DateField,
  SelectField,
  SelectOption,
  CheckboxField,
  FileField,
  ArrayField,
  GroupField,
  FormLayout,
  FormSection,
  AttachmentConfig,
  AttachmentCategory,
  ValidationRule,
} from './types';

// 타입 가드
export {
  isTextField,
  isNumberField,
  isDateField,
  isSelectField,
  isCheckboxField,
  isFileField,
  isArrayField,
  isGroupField,
} from './types';
