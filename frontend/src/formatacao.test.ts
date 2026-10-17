import { describe, expect, it } from 'vitest';
import { formatarData, formatarPercentual } from './formatacao';

describe('formatarData', () => {
  it('converte ISO em dd/mm/aaaa sem passar por Date (sem deslocamento de fuso)', () => {
    expect(formatarData('2026-10-16')).toBe('16/10/2026');
    expect(formatarData('2026-01-01')).toBe('01/01/2026');
  });
});

describe('formatarPercentual', () => {
  it('usa vírgula e sempre duas casas', () => {
    expect(formatarPercentual(66.67)).toBe('66,67');
    expect(formatarPercentual(80)).toBe('80,00');
    expect(formatarPercentual(100)).toBe('100,00');
  });
});
