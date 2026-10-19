import { useEffect, useId, useRef, type ReactNode } from 'react'

interface Props {
  titulo: string
  children: ReactNode
  rotuloConfirmar: string
  aoConfirmar: () => void
  aoCancelar: () => void
}

/** Diálogo modal de confirmação: foco inicial em Cancelar e Esc cancela. */
export function DialogoConfirmacao({ titulo, children, rotuloConfirmar, aoConfirmar, aoCancelar }: Props) {
  const idTitulo = useId()
  const cancelar = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    cancelar.current?.focus()
  }, [])

  return (
    <div className="fundo-dialogo">
      <div
        className="dialogo"
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
        onKeyDown={(evento) => {
          if (evento.key === 'Escape') {
            aoCancelar()
          }
        }}
      >
        <h2 id={idTitulo}>{titulo}</h2>
        <div>{children}</div>
        <div className="formulario-acoes">
          <button type="button" className="botao-perigo" onClick={aoConfirmar}>
            {rotuloConfirmar}
          </button>
          <button type="button" ref={cancelar} onClick={aoCancelar}>
            Cancelar
          </button>
        </div>
      </div>
    </div>
  )
}
