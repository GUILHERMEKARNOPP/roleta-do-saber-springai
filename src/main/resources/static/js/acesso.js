/* Roleta decorativa (girando devagar) no fundo das telas de login e cadastro. */
(function () {
  'use strict';
  const alvo = document.getElementById('roleta-decorativa');
  const itens = document.querySelectorAll('#premios-decorativos li');
  if (!alvo || !itens.length || !window.Roleta) return;
  const premios = Array.from(itens).map((li, indice) => ({
    indice, tipo: li.dataset.tipo, rotulo: li.textContent
  }));
  window.Roleta.criar(alvo, premios, null);
})();
