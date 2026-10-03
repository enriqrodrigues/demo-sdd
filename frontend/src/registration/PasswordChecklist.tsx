import { passwordCriteria } from './validation';

/** Checklist ao vivo dos critérios de força da senha. */
export default function PasswordChecklist({ password }: { password: string }) {
  return (
    <ul className="password-checklist" aria-label="Critérios da senha">
      {passwordCriteria(password).map((criterion) => (
        <li key={criterion.id} data-met={criterion.met} className={criterion.met ? 'met' : 'pending'}>
          <span aria-hidden="true">{criterion.met ? '✓' : '○'}</span> {criterion.label}
          <span className="visually-hidden">{criterion.met ? ' (atendido)' : ' (pendente)'}</span>
        </li>
      ))}
    </ul>
  );
}
