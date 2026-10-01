export const UFS = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA',
  'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
] as const;

export const LIMITE_BYTES_SENHA = 72;

export const CRITERIOS_SENHA: { rotulo: string; teste: (senha: string) => boolean }[] = [
  { rotulo: 'Mínimo de 8 caracteres', teste: (s) => s.length >= 8 },
  { rotulo: 'Letra maiúscula', teste: (s) => /[A-Z]/.test(s) },
  { rotulo: 'Letra minúscula', teste: (s) => /[a-z]/.test(s) },
  { rotulo: 'Número', teste: (s) => /\d/.test(s) },
  { rotulo: 'Caractere especial', teste: (s) => /[^A-Za-z0-9]/.test(s) },
];

export function soDigitos(valor: string): string {
  return valor.replace(/\D/g, '');
}

export function cpfValido(valor: string): boolean {
  const d = soDigitos(valor);
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) {
    return false;
  }
  const digito = (quantidade: number) => {
    let soma = 0;
    for (let i = 0; i < quantidade; i++) {
      soma += Number(d[i]) * (quantidade + 1 - i);
    }
    const resto = (soma * 10) % 11;
    return resto === 10 ? 0 : resto;
  };
  return digito(9) === Number(d[9]) && digito(10) === Number(d[10]);
}

export function senhaForte(senha: string): boolean {
  return CRITERIOS_SENHA.every((criterio) => criterio.teste(senha));
}

export function senhaExcedeLimite(senha: string): boolean {
  return new TextEncoder().encode(senha).length > LIMITE_BYTES_SENHA;
}

function hojeIso(): string {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${agora.getFullYear()}-${mes}-${dia}`;
}

export function dataPassada(iso: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(iso) && iso < hojeIso();
}

export function emailValido(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
