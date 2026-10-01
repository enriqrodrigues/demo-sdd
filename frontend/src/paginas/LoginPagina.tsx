import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm, type Path } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { ApiError, requisitar } from '../api/cliente';
import type { UsuarioLogado } from '../api/tipos';
import { Campo } from '../componentes/Campo';
import { loginSchema, type LoginForm } from '../validacao/esquemas';

export function LoginPagina() {
  const navigate = useNavigate();
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    mode: 'onTouched',
    defaultValues: { email: '', senha: '' },
  });

  async function entrar(dados: LoginForm) {
    setErroGeral(null);
    try {
      await requisitar<UsuarioLogado>('POST', '/api/auth/login', { email: dados.email.trim(), senha: dados.senha });
      navigate('/perfil');
    } catch (erro) {
      if (erro instanceof ApiError && erro.status === 401) {
        setErroGeral('E-mail ou senha inválidos.');
      } else if (erro instanceof ApiError && erro.status === 403) {
        setErroGeral('Sua conta ainda não foi ativada. Verifique seu e-mail.');
      } else if (erro instanceof ApiError && erro.erros.length > 0) {
        for (const { campo, mensagem } of erro.erros) {
          setError(campo as Path<LoginForm>, { type: 'server', message: mensagem });
        }
      } else {
        setErroGeral('Não foi possível entrar. Tente novamente.');
      }
    }
  }

  return (
    <section className="cartao">
      <h1>Entrar</h1>
      {erroGeral && (
        <div className="alerta alerta-erro" role="alert">
          {erroGeral}
        </div>
      )}
      <form onSubmit={handleSubmit(entrar)} noValidate>
        <Campo rotulo="E-mail" tipo="email" registro={register('email')} erro={errors.email?.message}
          autoComplete="email" />
        <Campo rotulo="Senha" tipo="password" registro={register('senha')} erro={errors.senha?.message}
          autoComplete="current-password" />
        <button type="submit" disabled={isSubmitting}>
          Entrar
        </button>
      </form>
      <p>
        Ainda não tem conta? <Link to="/cadastro">Criar conta</Link>
      </p>
    </section>
  );
}
