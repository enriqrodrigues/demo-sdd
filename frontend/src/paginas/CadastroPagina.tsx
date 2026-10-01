import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm, type Path } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { ApiError, requisitar } from '../api/cliente';
import type { CadastroResposta } from '../api/tipos';
import { Campo } from '../componentes/Campo';
import { CamposEndereco } from '../componentes/CamposEndereco';
import { IndicadorForcaSenha } from '../componentes/IndicadorForcaSenha';
import { cadastroParaRequisicao, cadastroSchema, ENDERECO_VAZIO, type CadastroForm } from '../validacao/esquemas';
import { mascaraCpf, mascaraTelefone } from '../validacao/mascaras';

const VALORES_INICIAIS: CadastroForm = {
  nome: '',
  cpf: '',
  email: '',
  dataNascimento: '',
  senha: '',
  confirmacaoSenha: '',
  telefone: '',
  endereco: ENDERECO_VAZIO,
};

export function CadastroPagina() {
  const navigate = useNavigate();
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CadastroForm>({
    resolver: zodResolver(cadastroSchema),
    mode: 'onTouched',
    defaultValues: VALORES_INICIAIS,
  });

  async function enviar(dados: CadastroForm) {
    setErroGeral(null);
    try {
      const resposta = await requisitar<CadastroResposta>('POST', '/api/usuarios', cadastroParaRequisicao(dados));
      navigate('/cadastro/concluido', { state: { email: resposta.email } });
    } catch (erro) {
      if (erro instanceof ApiError && erro.erros.length > 0) {
        for (const { campo, mensagem } of erro.erros) {
          setError(campo as Path<CadastroForm>, { type: 'server', message: mensagem });
        }
        return;
      }
      setErroGeral(erro instanceof ApiError ? erro.message : 'Não foi possível concluir o cadastro. Tente novamente.');
    }
  }

  return (
    <section className="cartao">
      <h1>Criar conta</h1>
      {erroGeral && (
        <div className="alerta alerta-erro" role="alert">
          {erroGeral}
        </div>
      )}
      <form onSubmit={handleSubmit(enviar)} noValidate>
        <Campo rotulo="Nome completo" registro={register('nome')} erro={errors.nome?.message} autoComplete="name" />
        <Campo rotulo="CPF" registro={register('cpf')} erro={errors.cpf?.message} mascara={mascaraCpf}
          inputMode="numeric" />
        <Campo rotulo="E-mail" tipo="email" registro={register('email')} erro={errors.email?.message}
          autoComplete="email" />
        <Campo rotulo="Data de nascimento" tipo="date" registro={register('dataNascimento')}
          erro={errors.dataNascimento?.message} autoComplete="bday" />
        <Campo rotulo="Senha" tipo="password" registro={register('senha')} erro={errors.senha?.message}
          autoComplete="new-password" />
        <IndicadorForcaSenha senha={watch('senha')} />
        <Campo rotulo="Confirmação de senha" tipo="password" registro={register('confirmacaoSenha')}
          erro={errors.confirmacaoSenha?.message} autoComplete="new-password" />
        <Campo rotulo="Telefone" tipo="tel" registro={register('telefone')} erro={errors.telefone?.message}
          mascara={mascaraTelefone} autoComplete="tel" />
        <CamposEndereco registrar={(campo) => register(`endereco.${campo}`)} erros={errors.endereco} />
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Enviando…' : 'Cadastrar'}
        </button>
      </form>
      <p>
        Já tem conta? <Link to="/login">Entrar</Link>
      </p>
    </section>
  );
}
