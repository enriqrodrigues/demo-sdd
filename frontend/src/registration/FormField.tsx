import type { ReactNode } from 'react';

type FormFieldProps = {
  id: string;
  label: string;
  error?: string;
  optional?: boolean;
  hint?: ReactNode;
  children: (props: { id: string; 'aria-invalid': boolean; 'aria-describedby'?: string }) => ReactNode;
};

/** Rótulo, controle e mensagem de erro de um campo, ligados por id para acessibilidade. */
export default function FormField({ id, label, error, optional, hint, children }: FormFieldProps) {
  const errorId = `${id}-error`;
  return (
    <div className={`field${error ? ' field--invalid' : ''}`}>
      <label htmlFor={id}>
        {label}
        {optional && <span className="optional"> (opcional)</span>}
      </label>
      {children({ id, 'aria-invalid': Boolean(error), 'aria-describedby': error ? errorId : undefined })}
      {hint}
      {error && (
        <p id={errorId} className="field-error">
          {error}
        </p>
      )}
    </div>
  );
}
