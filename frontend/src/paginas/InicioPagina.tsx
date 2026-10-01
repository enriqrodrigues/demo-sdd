import { useEffect } from 'react';
import { useNavigate } from 'react-router';
import { requisitar } from '../api/cliente';

export function InicioPagina() {
  const navigate = useNavigate();

  useEffect(() => {
    let ativo = true;
    requisitar('GET', '/api/perfil')
      .then(() => ativo && navigate('/perfil', { replace: true }))
      .catch(() => ativo && navigate('/login', { replace: true }));
    return () => {
      ativo = false;
    };
  }, [navigate]);

  return <p>Carregando…</p>;
}
