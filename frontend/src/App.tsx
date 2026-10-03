import { Navigate, Route, Routes } from 'react-router';
import { AtivacaoPagina } from './paginas/AtivacaoPagina';
import { CadastroConcluidoPagina } from './paginas/CadastroConcluidoPagina';
import { CadastroPagina } from './paginas/CadastroPagina';
import { InicioPagina } from './paginas/InicioPagina';
import { LoginPagina } from './paginas/LoginPagina';
import { PerfilPagina } from './paginas/PerfilPagina';

export function App() {
  return (
    <div className="layout">
      <header>
        <h1>Cadastro de Usuários</h1>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<InicioPagina />} />
          <Route path="/cadastro" element={<CadastroPagina />} />
          <Route path="/cadastro/concluido" element={<CadastroConcluidoPagina />} />
          <Route path="/ativar" element={<AtivacaoPagina />} />
          <Route path="/login" element={<LoginPagina />} />
          <Route path="/perfil" element={<PerfilPagina />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
