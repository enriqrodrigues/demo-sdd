import { setupServer } from 'msw/node'

/** API simulada pelo MSW; cada teste registra os handlers de que precisa. */
export const servidor = setupServer()
