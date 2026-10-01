import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest';
import { servidor } from './servidor';

beforeAll(() => servidor.listen({ onUnhandledRequest: 'error' }));
beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=token-teste';
});
afterEach(() => {
  cleanup();
  servidor.resetHandlers();
});
afterAll(() => servidor.close());
