/**
 * Perfil do usuário autenticado (RF07, RN01): consulta e edição de telefone e
 * endereço. Depende só de {@code user} (entidade), {@code shared} (validação e
 * erros) e {@code auth} (o principal da sessão).
 */
@ApplicationModule(allowedDependencies = { "user", "shared", "auth" })
package br.com.demosdd.profile;

import org.springframework.modulith.ApplicationModule;
