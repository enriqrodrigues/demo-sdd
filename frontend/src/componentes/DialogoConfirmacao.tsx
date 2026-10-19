import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react'

interface Props {
  titulo: string
  children: ReactNode
  rotuloConfirmar: string
  aoConfirmar: () => void
  aoCancelar: () => void
}

/**
 * Diálogo modal de confirmação: foco inicial em Cancelar, Tab preso entre os botões, Esc cancela
 * (de qualquer ponto da página) e, ao fechar, o foco volta para quem abriu o diálogo.
 */
export function DialogoConfirmacao({ titulo, children, rotuloConfirmar, aoConfirmar, aoCancelar }: Props) {
  const idTitulo = useId()
  const confirmar = useRef<HTMLButtonElement>(null)
  const cancelar = useRef<HTMLButtonElement>(null)
  const aoCancelarAtual = useRef(aoCancelar)

  useEffect(() => {
    aoCancelarAtual.current = aoCancelar
  })

  useEffect(() => {
    const quemAbriu = document.activeElement instanceof HTMLElement ? document.activeElement : null
    cancelar.current?.focus()
    const aoTeclar = (evento: globalThis.KeyboardEvent) => {
      if (evento.key === 'Escape') {
        aoCancelarAtual.current()
      }
    }
    document.addEventListener('keydown', aoTeclar)
    return () => {
      document.removeEventListener('keydown', aoTeclar)
      // Se a linha foi excluída, o botão que abriu o diálogo já não existe.
      if (quemAbriu?.isConnected) {
        quemAbriu.focus()
      }
    }
  }, [])

  function prenderFoco(evento: KeyboardEvent) {
    if (evento.key !== 'Tab') {
      return
    }
    const primeiro = confirmar.current
    const ultimo = cancelar.current
    if (evento.shiftKey && document.activeElement === primeiro) {
      evento.preventDefault()
      ultimo?.focus()
    } else if (!evento.shiftKey && document.activeElement === ultimo) {
      evento.preventDefault()
      primeiro?.focus()
    }
  }

  return (
    <div className="fundo-dialogo">
      <div
        className="dialogo"
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
        onKeyDown={prenderFoco}
      >
        <h2 id={idTitulo}>{titulo}</h2>
        <div>{children}</div>
        <div className="formulario-acoes">
          <button type="button" ref={confirmar} className="botao-perigo" onClick={aoConfirmar}>
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
