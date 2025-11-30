/**
 * DraftFormSchema TypeScript 타입 정의
 *
 * 백엔드 Java 클래스와 1:1 대응되는 TypeScript 타입입니다.
 * 프론트엔드에서 스키마 JSON을 타입 안전하게 다룰 수 있습니다.
 *
 * @see backend/admin/src/main/java/com/example/admin/draft/domain/schema/
 */

// ============================================================================
// 기본 필드 타입
// ============================================================================

export type FieldType =
  | 'text'
  | 'number'
  | 'date'
  | 'select'
  | 'checkbox'
  | 'file'
  | 'array'
  | 'group';

export interface BaseField {
  type: FieldType;
  name: string;
  label: string;
  required: boolean;
  description?: string | null;
}

// ============================================================================
// 개별 필드 타입
// ============================================================================

export interface TextField extends BaseField {
  type: 'text';
  placeholder?: string | null;
  minLength?: number | null;
  maxLength?: number | null;
  pattern?: string | null;
  multiline: boolean;
  inputType: 'text' | 'email' | 'url' | 'tel';
}

export interface NumberField extends BaseField {
  type: 'number';
  min?: number | null;
  max?: number | null;
  step?: number | null;
  precision?: number | null;
  currency?: string | null;
}

export interface DateField extends BaseField {
  type: 'date';
  dateType: 'date' | 'datetime' | 'time';
  minDate?: string | null; // ISO date string
  maxDate?: string | null;
  format?: string | null;
  includeTime: boolean;
}

export interface SelectOption {
  value: string;
  label: string;
  description?: string | null;
  disabled?: boolean;
}

export interface SelectField extends BaseField {
  type: 'select';
  options: SelectOption[];
  multiple: boolean;
  searchable: boolean;
  optionsSource?: string | null;
  displayType: 'dropdown' | 'radio' | 'checkbox';
}

export interface CheckboxField extends BaseField {
  type: 'checkbox';
  defaultValue: boolean;
  checkLabel?: string | null;
}

export interface FileField extends BaseField {
  type: 'file';
  acceptedTypes: string[];
  maxFileSize?: number | null;
  maxFiles?: number | null;
  multiple: boolean;
}

export interface ArrayField extends BaseField {
  type: 'array';
  itemFields: FormField[];
  minItems?: number | null;
  maxItems?: number | null;
  addLabel?: string | null;
}

export interface GroupField extends BaseField {
  type: 'group';
  fields: FormField[];
  collapsible: boolean;
  collapsed: boolean;
}

// ============================================================================
// 통합 필드 타입
// ============================================================================

export type FormField =
  | TextField
  | NumberField
  | DateField
  | SelectField
  | CheckboxField
  | FileField
  | ArrayField
  | GroupField;

// ============================================================================
// 레이아웃 및 첨부파일 설정
// ============================================================================

export interface FormSection {
  name: string;
  title: string;
  description?: string | null;
  fieldNames: string[];
  collapsible: boolean;
}

export interface FormLayout {
  columns: number;
  sections: FormSection[];
  fieldWidths: Record<string, number>;
  fieldOrder: string[];
}

export interface AttachmentCategory {
  code: string;
  name: string;
  required: boolean;
  description?: string | null;
}

export interface AttachmentConfig {
  enabled: boolean;
  required: boolean;
  maxFiles?: number | null;
  maxTotalSize?: number | null;
  maxFileSize?: number | null;
  acceptedTypes: string[];
  categories: AttachmentCategory[];
}

export interface ValidationRule {
  name: string;
  expression: string;
  message: string;
  targetFields: string[];
}

// ============================================================================
// 메인 스키마 타입
// ============================================================================

export interface DraftFormSchema {
  version: string;
  fields: FormField[];
  layout: FormLayout;
  attachmentConfig: AttachmentConfig;
  defaultValues: Record<string, unknown>;
  validationRules: ValidationRule[];
}

// ============================================================================
// 타입 가드 함수
// ============================================================================

export function isTextField(field: FormField): field is TextField {
  return field.type === 'text';
}

export function isNumberField(field: FormField): field is NumberField {
  return field.type === 'number';
}

export function isDateField(field: FormField): field is DateField {
  return field.type === 'date';
}

export function isSelectField(field: FormField): field is SelectField {
  return field.type === 'select';
}

export function isCheckboxField(field: FormField): field is CheckboxField {
  return field.type === 'checkbox';
}

export function isFileField(field: FormField): field is FileField {
  return field.type === 'file';
}

export function isArrayField(field: FormField): field is ArrayField {
  return field.type === 'array';
}

export function isGroupField(field: FormField): field is GroupField {
  return field.type === 'group';
}
