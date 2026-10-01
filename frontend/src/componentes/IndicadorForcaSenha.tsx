import { CRITERIOS_SENHA } from '../validacao/regras';

export function IndicadorForcaSenha({ senha }: { senha: string }) {
  return (
    <ul className="forca-senha" aria-label="Requisitos da senha">
      {CRITERIOS_SENHA.map((criterio) => {
        const atendido = criterio.teste(senha ?? '');
        return (
          <li key={criterio.rotulo} data-ok={atendido} className={atendido ? 'ok' : undefined}>
            {atendido ? '✓' : '•'} {criterio.rotulo}
          </li>
        );
      })}
    </ul>
  );
}
