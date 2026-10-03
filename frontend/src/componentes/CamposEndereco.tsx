import { useId } from 'react';
import type { UseFormRegisterReturn } from 'react-hook-form';
import type { EnderecoForm } from '../validacao/esquemas';
import { mascaraCep } from '../validacao/mascaras';
import { UFS } from '../validacao/regras';
import { Campo } from './Campo';

interface CamposEnderecoProps {
  registrar: (campo: keyof EnderecoForm) => UseFormRegisterReturn;
  erros?: Partial<Record<keyof EnderecoForm, { message?: string }>>;
}

export function CamposEndereco({ registrar, erros }: CamposEnderecoProps) {
  const idUf = useId();
  const erroUf = erros?.uf?.message;
  return (
    <fieldset>
      <legend>Endereço</legend>
      <Campo rotulo="CEP" registro={registrar('cep')} erro={erros?.cep?.message} mascara={mascaraCep}
        inputMode="numeric" autoComplete="postal-code" />
      <Campo rotulo="Logradouro" registro={registrar('logradouro')} erro={erros?.logradouro?.message}
        autoComplete="address-line1" />
      <Campo rotulo="Número" registro={registrar('numero')} erro={erros?.numero?.message} />
      <Campo rotulo="Complemento (opcional)" registro={registrar('complemento')}
        erro={erros?.complemento?.message} autoComplete="address-line2" />
      <Campo rotulo="Bairro" registro={registrar('bairro')} erro={erros?.bairro?.message} />
      <Campo rotulo="Cidade" registro={registrar('cidade')} erro={erros?.cidade?.message}
        autoComplete="address-level2" />
      <div className="campo">
        <label htmlFor={idUf}>UF</label>
        <select id={idUf} aria-invalid={erroUf ? true : undefined} {...registrar('uf')}>
          <option value="">Selecione</option>
          {UFS.map((uf) => (
            <option key={uf} value={uf}>
              {uf}
            </option>
          ))}
        </select>
        {erroUf && (
          <span className="erro-campo" role="alert">
            {erroUf}
          </span>
        )}
      </div>
    </fieldset>
  );
}
