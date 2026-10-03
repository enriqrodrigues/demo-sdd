// Regras de validação do cadastro (RF02), espelhando o backend. O servidor
// valida de novo e é quem decide; aqui elas servem para dar retorno imediato.

export const FIELDS = [
  'name',
  'cpf',
  'email',
  'birthDate',
  'password',
  'phone',
  'cep',
  'street',
  'number',
  'complement',
  'district',
  'city',
  'state',
] as const;

export type FieldName = (typeof FIELDS)[number];
export type RegistrationValues = Record<FieldName, string>;
export type FieldErrors = Partial<Record<FieldName, string>>;

export const REQUIRED = 'Campo obrigatório';

export const UFS = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA',
  'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
] as const;

export const MAX_LENGTH: Partial<Record<FieldName, number>> = {
  name: 150,
  email: 254,
  street: 200,
  number: 20,
  complement: 100,
  district: 100,
  city: 100,
};

const OPTIONAL_FIELDS: ReadonlySet<FieldName> = new Set(['complement']);

export const EMPTY_VALUES: RegistrationValues = Object.fromEntries(
  FIELDS.map((field) => [field, '']),
) as RegistrationValues;

// --- Normalização (mesmas regras do backend) ---

/** Remove apenas caracteres de máscara; outros caracteres permanecem e invalidam o valor. */
function removeMask(value: string, mask: RegExp): string {
  return value.trim().replace(mask, '');
}

export const normalize = {
  cpf: (value: string) => removeMask(value, /[.\-\s]/g),
  phone: (value: string) => removeMask(value, /[()\-\s]/g),
  cep: (value: string) => removeMask(value, /[-\s]/g),
  email: (value: string) => value.trim().toLowerCase(),
};

// --- CPF ---

export function isValidCpf(value: string): boolean {
  const cpf = normalize.cpf(value);
  if (!/^\d{11}$/.test(cpf) || /^(\d)\1{10}$/.test(cpf)) {
    return false;
  }
  const digits = [...cpf].map(Number);
  const checkDigit = (length: number) => {
    const sum = digits.slice(0, length).reduce((acc, digit, i) => acc + digit * (length + 1 - i), 0);
    const remainder = sum % 11;
    return remainder < 2 ? 0 : 11 - remainder;
  };
  return checkDigit(9) === digits[9] && checkDigit(10) === digits[10];
}

// --- Senha ---

export const PASSWORD_MIN = 8;
export const PASSWORD_MAX = 64;

export type PasswordCriterion = { id: string; label: string; met: boolean };

/** Critérios de força da senha, na ordem exibida no checklist do formulário. */
export function passwordCriteria(password: string): PasswordCriterion[] {
  const length = [...password].length;
  return [
    {
      id: 'length',
      label: `Entre ${PASSWORD_MIN} e ${PASSWORD_MAX} caracteres`,
      met: length >= PASSWORD_MIN && length <= PASSWORD_MAX,
    },
    { id: 'upper', label: 'Uma letra maiúscula', met: /\p{Lu}/u.test(password) },
    { id: 'lower', label: 'Uma letra minúscula', met: /\p{Ll}/u.test(password) },
    { id: 'digit', label: 'Um dígito', met: /\p{Nd}/u.test(password) },
    { id: 'special', label: 'Um caractere especial', met: /[^\p{L}\p{Nd}]/u.test(password) },
  ];
}

function passwordError(password: string): string | undefined {
  const length = [...password].length;
  if (length < PASSWORD_MIN) return `A senha deve ter ao menos ${PASSWORD_MIN} caracteres`;
  if (length > PASSWORD_MAX) return `A senha deve ter no máximo ${PASSWORD_MAX} caracteres`;
  const unmet = passwordCriteria(password).find((criterion) => !criterion.met);
  return unmet ? `A senha deve conter: ${unmet.label.toLowerCase()}` : undefined;
}

// --- Data ---

/** Data local de hoje no formato do input date (AAAA-MM-DD). */
export function todayIso(now: Date = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

// --- Validação por campo ---

/** Mensagem de erro do campo, ou `undefined` se o valor é válido. */
export function validateField(field: FieldName, values: RegistrationValues, today = todayIso()): string | undefined {
  const raw = values[field] ?? '';
  const value = field === 'password' ? raw : raw.trim();

  if (field === 'password' ? raw.trim() === '' : value === '') {
    return OPTIONAL_FIELDS.has(field) ? undefined : REQUIRED;
  }

  const max = MAX_LENGTH[field];
  if (max !== undefined && value.length > max) {
    return `Máximo de ${max} caracteres`;
  }

  switch (field) {
    case 'cpf':
      return isValidCpf(value) ? undefined : 'CPF inválido';
    case 'email':
      return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(normalize.email(value)) ? undefined : 'E-mail inválido';
    case 'birthDate':
      if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return 'Data inválida';
      return value > today ? 'A data de nascimento não pode ser futura' : undefined;
    case 'password':
      return passwordError(raw);
    case 'phone':
      return /^\d{10,11}$/.test(normalize.phone(value))
        ? undefined
        : 'O telefone deve ter 10 dígitos (fixo) ou 11 dígitos (celular), incluindo o DDD';
    case 'cep':
      return /^\d{8}$/.test(normalize.cep(value)) ? undefined : 'O CEP deve ter 8 dígitos';
    case 'state':
      return (UFS as readonly string[]).includes(value.toUpperCase()) ? undefined : 'UF inválida';
    default:
      return undefined;
  }
}

export function validateForm(values: RegistrationValues, today = todayIso()): FieldErrors {
  const errors: FieldErrors = {};
  for (const field of FIELDS) {
    const error = validateField(field, values, today);
    if (error) errors[field] = error;
  }
  return errors;
}
