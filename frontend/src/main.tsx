import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import Rotas from './Rotas.tsx'
import { inicializar } from './inicializacao.ts'

void inicializar().then(() => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <BrowserRouter>
        <Rotas />
      </BrowserRouter>
    </StrictMode>,
  )
})
