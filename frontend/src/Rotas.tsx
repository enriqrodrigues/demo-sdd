import { Route, Routes } from 'react-router'
import Ativacao from './paginas/Ativacao.tsx'
import Cadastro from './paginas/Cadastro.tsx'
import Inicio from './paginas/Inicio.tsx'
import Login from './paginas/Login.tsx'
import Perfil from './paginas/Perfil.tsx'

export default function Rotas() {
  return (
    <Routes>
      <Route path="/cadastro" element={<Cadastro />} />
      <Route path="/ativacao" element={<Ativacao />} />
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<Inicio />} />
      <Route path="/perfil" element={<Perfil />} />
    </Routes>
  )
}
