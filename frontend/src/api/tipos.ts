export interface ErroCampo {
  campo: string;
  mensagem: string;
}

export interface EnderecoDados {
  cep: string;
  logradouro: string;
  numero: string;
  complemento: string | null;
  bairro: string;
  cidade: string;
  uf: string;
}

export interface Perfil {
  nome: string;
  cpf: string;
  email: string;
  dataNascimento: string;
  telefone: string;
  endereco: EnderecoDados;
  status: string;
}

export interface UsuarioLogado {
  nome: string;
  email: string;
}

export interface CadastroResposta {
  id: string;
  email: string;
  status: string;
}
