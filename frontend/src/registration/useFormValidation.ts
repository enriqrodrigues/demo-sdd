import { useState } from 'react';
import {
  EMPTY_VALUES,
  FIELDS,
  validateField,
  validateForm,
  type FieldErrors,
  type FieldName,
} from './validation';

/**
 * Estado do formulário com validação em tempo real (RF02): cada campo é
 * validado ao perder o foco e, depois de mostrar um erro, a cada alteração.
 * Por padrão cobre todos os campos do cadastro; o perfil usa só os de contato.
 */
export function useFormValidation<F extends FieldName = FieldName>(
  fields: readonly F[] = FIELDS as readonly FieldName[] as readonly F[],
  initial: Record<F, string> = EMPTY_VALUES,
) {
  const [values, setValues] = useState<Record<F, string>>(initial);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [touched, setTouched] = useState<Partial<Record<F, boolean>>>({});

  function setFieldError(field: F, error: string | undefined) {
    setErrors((current) => {
      const next = { ...current };
      if (error) next[field] = error;
      else delete next[field];
      return next;
    });
  }

  function change(field: F, value: string) {
    const nextValues = { ...values, [field]: value };
    setValues(nextValues);
    if (touched[field] || errors[field]) {
      setFieldError(field, validateField(field, nextValues));
    }
  }

  function blur(field: F) {
    setTouched((current) => ({ ...current, [field]: true }));
    setFieldError(field, validateField(field, values));
  }

  /** Valida todos os campos; devolve o primeiro campo inválido ou `undefined`. */
  function validateAll(): F | undefined {
    const allErrors = validateForm(values, undefined, fields);
    setErrors(allErrors);
    setTouched(Object.fromEntries(fields.map((field) => [field, true])) as Partial<Record<F, boolean>>);
    return fields.find((field) => allErrors[field]);
  }

  /** Aplica os erros por campo devolvidos pelo servidor. */
  function applyServerErrors(fieldErrors: Record<string, string[]>) {
    setErrors((current) => {
      const next = { ...current };
      for (const [field, messages] of Object.entries(fieldErrors)) {
        if ((fields as readonly string[]).includes(field)) {
          next[field as F] = messages.join(' ');
        }
      }
      return next;
    });
  }

  /** Troca todos os valores e limpa erros e campos visitados. */
  function reset(nextValues: Record<F, string>) {
    setValues(nextValues);
    setErrors({});
    setTouched({});
  }

  return { values, errors, change, blur, validateAll, applyServerErrors, reset };
}
