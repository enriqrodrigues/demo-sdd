import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { register } from '../api';
import FormField from './FormField';
import PasswordChecklist from './PasswordChecklist';
import { maskCep, maskCpf, maskPhone } from './masks';
import { useFormValidation } from './useFormValidation';
import { MAX_LENGTH, UFS, todayIso, type FieldName } from './validation';

type TextFieldConfig = {
  field: FieldName;
  label: string;
  type?: string;
  autoComplete?: string;
  inputMode?: 'numeric' | 'email' | 'tel' | 'text';
  placeholder?: string;
  mask?: (value: string) => string;
  optional?: boolean;
};

const PERSONAL_FIELDS: TextFieldConfig[] = [
  { field: 'name', label: 'Nome completo', autoComplete: 'name' },
  { field: 'cpf', label: 'CPF', inputMode: 'numeric', placeholder: '000.000.000-00', mask: maskCpf },
  { field: 'email', label: 'E-mail', type: 'email', autoComplete: 'email', inputMode: 'email' },
  { field: 'birthDate', label: 'Data de nascimento', type: 'date', autoComplete: 'bday' },
];

const ADDRESS_FIELDS: TextFieldConfig[] = [
  { field: 'cep', label: 'CEP', inputMode: 'numeric', placeholder: '00000-000', mask: maskCep, autoComplete: 'postal-code' },
  { field: 'street', label: 'Logradouro', autoComplete: 'address-line1' },
  { field: 'number', label: 'Número' },
  { field: 'complement', label: 'Complemento', optional: true, autoComplete: 'address-line2' },
  { field: 'district', label: 'Bairro' },
  { field: 'city', label: 'Cidade', autoComplete: 'address-level2' },
];

/** Formulário de cadastro (RF01) com validação em tempo real (RF02). */
export default function RegistrationPage() {
  const navigate = useNavigate();
  const form = useFormValidation();
  const [submitting, setSubmitting] = useState(false);
  const [generalMessage, setGeneralMessage] = useState<string>();

  function renderTextField(config: TextFieldConfig) {
    const { field, label, type = 'text', mask, optional, ...inputProps } = config;
    return (
      <FormField key={field} id={field} label={label} error={form.errors[field]} optional={optional}>
        {(a11y) => (
          <input
            {...a11y}
            {...inputProps}
            name={field}
            type={type}
            value={form.values[field]}
            maxLength={MAX_LENGTH[field]}
            max={field === 'birthDate' ? todayIso() : undefined}
            onChange={(event) => form.change(field, mask ? mask(event.target.value) : event.target.value)}
            onBlur={() => form.blur(field)}
          />
        )}
      </FormField>
    );
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setGeneralMessage(undefined);

    const firstInvalid = form.validateAll();
    if (firstInvalid) {
      document.getElementById(firstInvalid)?.focus();
      return;
    }

    setSubmitting(true);
    const result = await register(form.values);
    setSubmitting(false);

    if (result.ok) {
      navigate('/cadastro/sucesso', { state: { email: result.data.email } });
      return;
    }

    const { problem } = result;
    form.applyServerErrors(problem.fieldErrors);
    setGeneralMessage(
      problem.code === 'VALIDATION_ERROR' ? 'Corrija os campos destacados e tente novamente.' : problem.detail,
    );
  }

  return (
    <main className="container">
      <h1>Crie sua conta</h1>

      {generalMessage && (
        <div role="alert" className="alert alert--error">
          {generalMessage}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <fieldset>
          <legend>Dados pessoais</legend>
          {PERSONAL_FIELDS.map(renderTextField)}

          <FormField
            id="password"
            label="Senha"
            error={form.errors.password}
            hint={<PasswordChecklist password={form.values.password} />}
          >
            {(a11y) => (
              <input
                {...a11y}
                name="password"
                type="password"
                autoComplete="new-password"
                value={form.values.password}
                onChange={(event) => form.change('password', event.target.value)}
                onBlur={() => form.blur('password')}
              />
            )}
          </FormField>
        </fieldset>

        <fieldset>
          <legend>Contato</legend>
          {renderTextField({
            field: 'phone',
            label: 'Telefone (com DDD)',
            type: 'tel',
            inputMode: 'tel',
            placeholder: '(00) 00000-0000',
            mask: maskPhone,
            autoComplete: 'tel-national',
          })}
        </fieldset>

        <fieldset>
          <legend>Endereço</legend>
          {ADDRESS_FIELDS.map(renderTextField)}

          <FormField id="state" label="UF" error={form.errors.state}>
            {(a11y) => (
              <select
                {...a11y}
                name="state"
                value={form.values.state}
                onChange={(event) => form.change('state', event.target.value)}
                onBlur={() => form.blur('state')}
              >
                <option value="">Selecione</option>
                {UFS.map((uf) => (
                  <option key={uf} value={uf}>
                    {uf}
                  </option>
                ))}
              </select>
            )}
          </FormField>
        </fieldset>

        <button type="submit" className="button" disabled={submitting}>
          {submitting ? 'Enviando...' : 'Cadastrar'}
        </button>
      </form>
    </main>
  );
}
