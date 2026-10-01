import { useId, type HTMLAttributes } from 'react';
import type { UseFormRegisterReturn } from 'react-hook-form';

interface CampoProps {
  rotulo: string;
  registro: UseFormRegisterReturn;
  erro?: string;
  tipo?: string;
  mascara?: (valor: string) => string;
  autoComplete?: string;
  inputMode?: HTMLAttributes<HTMLInputElement>['inputMode'];
}

export function Campo({ rotulo, registro, erro, tipo = 'text', mascara, autoComplete, inputMode }: CampoProps) {
  const id = useId();
  const idErro = `${id}-erro`;
  return (
    <div className="campo">
      <label htmlFor={id}>{rotulo}</label>
      <input
        id={id}
        type={tipo}
        autoComplete={autoComplete}
        inputMode={inputMode}
        aria-invalid={erro ? true : undefined}
        aria-describedby={erro ? idErro : undefined}
        {...registro}
        onChange={(evento) => {
          if (mascara) {
            evento.target.value = mascara(evento.target.value);
          }
          return registro.onChange(evento);
        }}
      />
      {erro && (
        <span id={idErro} className="erro-campo" role="alert">
          {erro}
        </span>
      )}
    </div>
  );
}
