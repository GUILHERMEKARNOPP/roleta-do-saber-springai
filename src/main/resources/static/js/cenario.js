/* Cristais flutuantes do fundo (puramente decorativo). */
(function () {
  'use strict';
  const alvo = document.querySelector('.cristais');
  if (!alvo) return;
  const quantidade = window.matchMedia('(max-width: 700px)').matches ? 10 : 22;
  for (let i = 0; i < quantidade; i++) {
    const cristal = document.createElement('span');
    cristal.className = 'cristal';
    const tamanho = 8 + Math.random() * 22;
    cristal.style.width = tamanho + 'px';
    cristal.style.height = tamanho * (0.8 + Math.random() * 0.6) + 'px';
    cristal.style.left = Math.random() * 100 + '%';
    cristal.style.top = Math.random() * 70 + '%';
    cristal.style.animationDelay = (-Math.random() * 12).toFixed(2) + 's';
    cristal.style.animationDuration = (9 + Math.random() * 8).toFixed(2) + 's';
    cristal.style.setProperty('--giro', Math.round(Math.random() * 360) + 'deg');
    alvo.appendChild(cristal);
  }
})();
