/**
 * Módulo cadastro. Orquestra o cadastro (AD-4).
 */
@ApplicationModule(allowedDependencies = {"conta", "ativacao", "email", "compartilhado"})
package br.demo.usuarios.cadastro;

import org.springframework.modulith.ApplicationModule;
