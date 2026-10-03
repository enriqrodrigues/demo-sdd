import '@testing-library/jest-dom/vitest';

export const TEST_XSRF_TOKEN = 'token-xsrf-de-teste';

// Como no navegador depois da primeira resposta do servidor: o cookie anti-CSRF
// já existe. Os testes do api.ts que cobrem a ausência do cookie o apagam.
beforeEach(() => {
  document.cookie = `XSRF-TOKEN=${TEST_XSRF_TOKEN}; path=/`;
});
