import { useState } from 'react';
import {
  EMPTY_VALUES,
  FIELDS,
  validateField,
  validateForm,
  type FieldErrors,
  type FieldName,
  type RegistrationValues,
} from './validation';

/**
 * Estado do formulário com validação em tempo real (RF02): cada campo é
 * validado ao perder o foco e, depois de mostrar um erro, a cada alteração.
 */
export function useFormValidation(initial: RegistrationValues = EMPTY_VALUES) {
  const [values, setValues] = useState<RegistrationValues>(initial);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [touched, setTouched] = useState<Partial<Record<FieldName, boolean>>>({});

  function setFieldError(field: FieldName, error: string | undefined) {
    setErrors((current) => {
      const next = { ...current };
      if (error) next[field] = error;
      else delete next[field];
      return next;
    });
  }

  function change(field: FieldName, value: string) {
    const nextValues = { ...values, [field]: value };
    setValues(nextValues);
    if (touched[field] || errors[field]) {
      setFieldError(field, validateField(field, nextValues));
    }
  }

  function blur(field: FieldName) {
    setTouched((current) => ({ ...current, [field]: true }));
    setFieldError(field, validateField(field, values));
  }

  /** Valida todos os campos; devolve o primeiro campo inválido ou `undefined`. */
  function validateAll(): FieldName | undefined {
    const allErrors = validateForm(values);
    setErrors(allErrors);
    setTouched(Object.fromEntries(FIELDS.map((field) => [field, true])));
    return FIELDS.find((field) => allErrors[field]);
  }

  /** Aplica os erros por campo devolvidos pelo servidor. */
  function applyServerErrors(fieldErrors: Record<string, string[]>) {
    setErrors((current) => {
      const next = { ...current };
      for (const [field, messages] of Object.entries(fieldErrors)) {
        if ((FIELDS as readonly string[]).includes(field)) {
          next[field as FieldName] = messages.join(' ');
        }
      }
      return next;
    });
  }

  return { values, errors, change, blur, validateAll, applyServerErrors };
}
