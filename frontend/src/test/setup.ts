import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { servidor } from './servidor'

// Com vários arquivos em paralelo, digitar com user-event pode passar do 1 s padrão dos findBy*.
configure({ asyncUtilTimeout: 5000 })

beforeAll(() => servidor.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  cleanup()
  servidor.resetHandlers()
})
afterAll(() => servidor.close())
