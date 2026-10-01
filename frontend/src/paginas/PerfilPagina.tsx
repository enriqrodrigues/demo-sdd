import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useRef, useState } from 'react';
import { useForm, type Path } from 'react-hook-form';
import { useNavigate } from 'react-router';
import { ApiError, requisitar } from '../api/cliente';
import type { Perfil } from '../api/tipos';
import { Campo } from '../componentes/Campo';
import { CamposEndereco } from '../componentes/CamposEndereco';
import {
  perfilParaFormulario,
  perfilParaRequisicao,
  perfilSchema,
  type PerfilForm,
} from '../validacao/esquemas';
import { formatarData, mascaraCep, mascaraCpf, mascaraTelefone } from '../validacao/mascaras';

function formatarEndereco({ endereco: e }: Perfil): string {
  const complemento = e.complemento ? ` - ${e.complemento}` : '';
  return `${e.logradouro}, ${e.numero}${complemento} — ${e.bairro}, ${e.cidade}/${e.uf} — CEP ${mascaraCep(e.cep)}`;
}

interface FormularioContatoProps {
  perfil: Perfil;
  aoSalvar: (perfil: Perfil) => void;
  aoCancelar: () => void;
}

function FormularioContato({ perfil, aoSalvar, aoCancelar }: FormularioContatoProps) {
  const navigate = useNavigate();
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<PerfilForm>({
    resolver: zodResolver(perfilSchema),
    mode: 'onTouched',
    defaultValues: perfilParaFormulario(perfil),
  });

  async function salvar(dados: PerfilForm) {
    setErroGeral(null);
    try {
      aoSalvar(await requisitar<Perfil>('PUT', '/api/perfil', perfilParaRequisicao(dados)));
    } catch (erro) {
      if (erro instanceof ApiError && erro.status === 401) {
        navigate('/login', { replace: true });
      } else if (erro instanceof ApiError && erro.erros.length > 0) {
        for (const { campo, mensagem } of erro.erros) {
          setError(campo as Path<PerfilForm>, { type: 'server', message: mensagem });
        }
      } else {
        setErroGeral('Não foi possível salvar as alterações. Tente novamente.');
      }
    }
  }

  return (
    <form onSubmit={handleSubmit(salvar)} noValidate>
      {erroGeral && (
        <div className="alerta alerta-erro" role="alert">
          {erroGeral}
        </div>
      )}
      <Campo rotulo="Telefone" tipo="tel" registro={register('telefone')} erro={errors.telefone?.message}
        mascara={mascaraTelefone} autoComplete="tel" />
      <CamposEndereco registrar={(campo) => register(`endereco.${campo}`)} erros={errors.endereco} />
      <div className="acoes">
        <button type="submit" disabled={isSubmitting}>
          Salvar
        </button>
        <button type="button" className="secundario" onClick={aoCancelar}>
          Cancelar
        </button>
      </div>
    </form>
  );
}

export function PerfilPagina() {
  const navigate = useNavigate();
  const [perfil, setPerfil] = useState<Perfil | null>(null);
  const [editando, setEditando] = useState(false);
  const [mensagem, setMensagem] = useState<string | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [erroSaida, setErroSaida] = useState<string | null>(null);
  const [saindo, setSaindo] = useState(false);
  const saidaEmAndamento = useRef(false);

  useEffect(() => {
    let ativo = true;
    requisitar<Perfil>('GET', '/api/perfil')
      .then((dados) => ativo && setPerfil(dados))
      .catch((e: unknown) => {
        if (!ativo) return;
        if (e instanceof ApiError && e.status === 401) {
          navigate('/login', { replace: true });
        } else {
          setErro('Não foi possível carregar o perfil.');
        }
      });
    return () => {
      ativo = false;
    };
  }, [navigate]);

  async function sair() {
    if (saidaEmAndamento.current) return;
    saidaEmAndamento.current = true;
    setSaindo(true);
    setErroSaida(null);
    try {
      await requisitar<void>('POST', '/api/auth/logout');
      navigate('/login', { replace: true });
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        navigate('/login', { replace: true });
      } else {
        saidaEmAndamento.current = false;
        setSaindo(false);
        setErroSaida('Não foi possível sair. Tente novamente.');
      }
    }
  }

  if (erro) {
    return (
      <div className="alerta alerta-erro" role="alert">
        {erro}
      </div>
    );
  }
  if (!perfil) {
    return <p>Carregando…</p>;
  }

  return (
    <section className="cartao">
      <h1>Meu perfil</h1>
      {mensagem && <div className="alerta alerta-sucesso">{mensagem}</div>}
      {erroSaida && (
        <div className="alerta alerta-erro" role="alert">
          {erroSaida}
        </div>
      )}
      <dl className="dados">
        <dt>Nome</dt>
        <dd>{perfil.nome}</dd>
        <dt>CPF</dt>
        <dd>{mascaraCpf(perfil.cpf)}</dd>
        <dt>E-mail</dt>
        <dd>{perfil.email}</dd>
        <dt>Nascimento</dt>
        <dd>{formatarData(perfil.dataNascimento)}</dd>
        {!editando && (
          <>
            <dt>Telefone</dt>
            <dd>{mascaraTelefone(perfil.telefone)}</dd>
            <dt>Endereço</dt>
            <dd>{formatarEndereco(perfil)}</dd>
          </>
        )}
      </dl>
      {editando ? (
        <FormularioContato
          perfil={perfil}
          aoSalvar={(atualizado) => {
            setPerfil(atualizado);
            setEditando(false);
            setMensagem('Dados atualizados com sucesso.');
          }}
          aoCancelar={() => setEditando(false)}
        />
      ) : (
        <div className="acoes">
          <button
            type="button"
            onClick={() => {
              setMensagem(null);
              setEditando(true);
            }}
          >
            Editar contato
          </button>
          <button type="button" className="secundario" onClick={sair} disabled={saindo}>
            Sair
          </button>
        </div>
      )}
    </section>
  );
}
