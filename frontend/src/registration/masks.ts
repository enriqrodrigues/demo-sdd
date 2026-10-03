// Máscaras aplicadas enquanto o usuário digita. O backend aceita os valores
// com ou sem máscara.

function digits(value: string, max: number): string {
  return value.replace(/\D/g, '').slice(0, max);
}

/** 000.000.000-00 */
export function maskCpf(value: string): string {
  const d = digits(value, 11);
  if (d.length <= 3) return d;
  if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`;
  if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`;
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
}

/** (00) 0000-0000 para fixo e (00) 00000-0000 para celular, conforme a quantidade de dígitos. */
export function maskPhone(value: string): string {
  const d = digits(value, 11);
  if (d.length === 0) return '';
  if (d.length <= 2) return `(${d}`;
  const ddd = d.slice(0, 2);
  const number = d.slice(2);
  if (number.length <= 4) return `(${ddd}) ${number}`;
  const split = number.length === 9 ? 5 : 4;
  return `(${ddd}) ${number.slice(0, split)}-${number.slice(split)}`;
}

/** 00000-000 */
export function maskCep(value: string): string {
  const d = digits(value, 8);
  return d.length <= 5 ? d : `${d.slice(0, 5)}-${d.slice(5)}`;
}
