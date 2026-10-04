import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { getProfile, updateProfile, type Profile } from '../api';
import FormField from '../registration/FormField';
import { maskCep, maskCpf, maskPhone } from '../registration/masks';
import { useFormValidation } from '../registration/useFormValidation';
import { CONTACT_FIELDS, MAX_LENGTH, UFS, type ContactField, type ContactValues } from '../registration/validation';

type TextFieldConfig = {
  field: ContactField;
  label: string;
  type?: string;
  autoComplete?: string;
  inputMode?: 'numeric' | 'tel' | 'text';
  placeholder?: string;
  mask?: (value: string) => string;
  optional?: boolean;
};

const PHONE_FIELD: TextFieldConfig = {
  field: 'phone',
  label: 'Telefone (com DDD)',
  type: 'tel',
  inputMode: 'tel',
  placeholder: '(00) 00000-0000',
  mask: maskPhone,
  autoComplete: 'tel-national',
};

const ADDRESS_FIELDS: TextFieldConfig[] = [
  { field: 'cep', label: 'CEP', inputMode: 'numeric', placeholder: '00000-000', mask: maskCep, autoComplete: 'postal-code' },
  { field: 'street', label: 'Logradouro', autoComplete: 'address-line1' },
  { field: 'number', label: 'Número' },
  { field: 'complement', label: 'Complemento', optional: true, autoComplete: 'address-line2' },
  { field: 'district', label: 'Bairro' },
  { field: 'city', label: 'Cidade', autoComplete: 'address-level2' },
];

/** Valores do formulário a partir do perfil do servidor, já com as máscaras. */
function toContactValues(profile: Profile): ContactValues {
  return {
    phone: maskPhone(profile.phone),
    cep: maskCep(profile.cep),
    street: profile.street,
    number: profile.number,
    complement: profile.complement ?? '',
    district: profile.district,
    city: profile.city,
    state: profile.state,
  };
}

/** AAAA-MM-DD para dd/mm/aaaa, sem passar por Date (evita deslocamento de fuso). */
function formatDate(iso: string): string {
  const [year, month, day] = iso.split('-');
  return `${day}/${month}/${year}`;
}

type LoadState = { kind: 'loading' } | { kind: 'loaded'; profile: Profile } | { kind: 'error'; message: string };

/** Perfil do usuário autenticado (RF07): dados pessoais somente leitura (RN01) e edição de contato. */
export default function ProfilePage() {
  const navigate = useNavigate();
  const [state, setState] = useState<LoadState>({ kind: 'loading' });

  useEffect(() => {
    let active = true;
    getProfile().then((result) => {
      if (!active) return;
      if (result.ok) {
        setState({ kind: 'loaded', profile: result.data });
      } else if (result.problem.status === 401) {
        navigate('/login', { replace: true });
      } else {
        setState({ kind: 'error', message: result.problem.detail });
      }
    });
    return () => {
      active = false;
    };
  }, [navigate]);

  if (state.kind === 'loading') {
    return (
      <main className="container">
        <p role="status">Carregando...</p>
      </main>
    );
  }

  if (state.kind === 'error') {
    return (
      <main className="container">
        <div role="alert" className="alert alert--error">
          {state.message}
        </div>
        <BackLink />
      </main>
    );
  }

  return <ProfileView initialProfile={state.profile} />;
}

function BackLink() {
  return (
    <p className="form-footer">
      <Link to="/inicio">Voltar ao início</Link>
    </p>
  );
}

function ProfileView({ initialProfile }: { initialProfile: Profile }) {
  const navigate = useNavigate();
  const [profile, setProfile] = useState(initialProfile);
  const form = useFormValidation(CONTACT_FIELDS, toContactValues(initialProfile));
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string }>();

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
            onChange={(event) => form.change(field, mask ? mask(event.target.value) : event.target.value)}
            onBlur={() => form.blur(field)}
          />
        )}
      </FormField>
    );
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setMessage(undefined);

    const firstInvalid = form.validateAll();
    if (firstInvalid) {
      document.getElementById(firstInvalid)?.focus();
      return;
    }

    setSubmitting(true);
    const result = await updateProfile(form.values);
    setSubmitting(false);

    if (result.ok) {
      setProfile(result.data);
      form.reset(toContactValues(result.data));
      setMessage({ kind: 'success', text: 'Dados atualizados com sucesso.' });
      return;
    }

    const { problem } = result;
    if (problem.status === 401) {
      navigate('/login', { replace: true });
      return;
    }
    form.applyServerErrors(problem.fieldErrors);
    setMessage({
      kind: 'error',
      text: problem.code === 'VALIDATION_ERROR' ? 'Corrija os campos destacados e tente novamente.' : problem.detail,
    });
  }

  function handleDiscard() {
    setMessage(undefined);
    form.reset(toContactValues(profile));
  }

  return (
    <main className="container">
      <h1>Meu perfil</h1>

      {message && (
        <div role={message.kind === 'success' ? 'status' : 'alert'} className={`alert alert--${message.kind}`}>
          {message.text}
        </div>
      )}

      <section aria-labelledby="personal-data-title" className="profile-data">
        <h2 id="personal-data-title">Dados pessoais</h2>
        <dl>
          <dt>Nome</dt>
          <dd>{profile.name}</dd>
          <dt>CPF</dt>
          <dd>{maskCpf(profile.cpf)}</dd>
          <dt>E-mail</dt>
          <dd>{profile.email}</dd>
          <dt>Data de nascimento</dt>
          <dd>{formatDate(profile.birthDate)}</dd>
        </dl>
        <p className="profile-note">Estes dados não podem ser alterados.</p>
      </section>

      <form onSubmit={handleSubmit} noValidate>
        <fieldset>
          <legend>Contato</legend>
          {renderTextField(PHONE_FIELD)}
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

        <div className="form-actions">
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? 'Salvando...' : 'Salvar alterações'}
          </button>
          <button type="button" className="button button--secondary" onClick={handleDiscard} disabled={submitting}>
            Descartar alterações
          </button>
        </div>
      </form>

      <BackLink />
    </main>
  );
}
