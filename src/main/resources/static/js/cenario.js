/* Elementos decorativos do cassino: cristais na parede e caça-níqueis. */
(function () {
  'use strict';

  const SVG_NS = 'http://www.w3.org/2000/svg';
  const celular = window.matchMedia('(max-width: 1000px)').matches;

  function el(nome, atributos, filhos) {
    const no = document.createElementNS(SVG_NS, nome);
    Object.entries(atributos || {}).forEach(([k, v]) => no.setAttribute(k, v));
    (filhos || []).forEach((f) => no.appendChild(f));
    return no;
  }

  function texto(conteudo, atributos) {
    const t = el('text', atributos);
    t.textContent = conteudo;
    return t;
  }

  /* Cristais: lascas irregulares, concentradas nas laterais da parede. */
  function criarCristais() {
    const alvo = document.querySelector('.cristais');
    if (!alvo) return;
    const quantidade = celular ? 12 : 34;
    for (let i = 0; i < quantidade; i++) {
      const cristal = document.createElement('span');
      cristal.className = 'cristal';
      // concentra os cristais na parede esquerda, como na referência, com alguns à direita
      const lateralEsquerda = i % 3 !== 0;
      const x = lateralEsquerda ? 22 + Math.random() * 18 : 82 + Math.random() * 16;
      const tamanho = 7 + Math.random() * 20;
      cristal.style.left = x + '%';
      cristal.style.top = 6 + Math.random() * 52 + '%';
      cristal.style.width = tamanho + 'px';
      cristal.style.height = tamanho * (0.7 + Math.random() * 0.8) + 'px';
      const pontos = [];
      const lados = 5 + Math.floor(Math.random() * 3);
      for (let p = 0; p < lados; p++) {
        const ang = (p / lados) * Math.PI * 2 + Math.random() * 0.5;
        const raio = 35 + Math.random() * 15;
        pontos.push((50 + Math.cos(ang) * raio).toFixed(1) + '% ' + (50 + Math.sin(ang) * raio).toFixed(1) + '%');
      }
      cristal.style.clipPath = 'polygon(' + pontos.join(',') + ')';
      cristal.style.setProperty('--giro', Math.round(Math.random() * 360) + 'deg');
      cristal.style.animationDelay = (-Math.random() * 14).toFixed(2) + 's';
      cristal.style.animationDuration = (10 + Math.random() * 8).toFixed(2) + 's';
      alvo.appendChild(cristal);
    }
  }

  /* Caça-níquel desenhado em SVG (gabinete, letreiro, tela com rolos e painel). */
  function maquina(titulo, corA, corB, simbolos) {
    const id = 'maq-' + Math.random().toString(36).slice(2, 8);
    const svg = el('svg', { viewBox: '0 0 140 300', class: 'maquina', focusable: 'false' });
    svg.appendChild(el('defs', {}, [
      el('linearGradient', { id: id + '-topo', x1: 0, y1: 0, x2: 0, y2: 1 }, [
        el('stop', { offset: 0, 'stop-color': corA }), el('stop', { offset: 1, 'stop-color': corB })
      ]),
      el('linearGradient', { id: id + '-tela', x1: 0, y1: 0, x2: 0, y2: 1 }, [
        el('stop', { offset: 0, 'stop-color': '#fff6fb' }), el('stop', { offset: 1, 'stop-color': corA })
      ])
    ]));
    // gabinete
    svg.appendChild(el('rect', { x: 12, y: 62, width: 116, height: 230, rx: 10, fill: '#191c26' }));
    svg.appendChild(el('rect', { x: 12, y: 62, width: 8, height: 230, fill: corA, opacity: 0.55 }));
    svg.appendChild(el('rect', { x: 120, y: 62, width: 8, height: 230, fill: corA, opacity: 0.55 }));
    // letreiro arredondado
    svg.appendChild(el('path', { d: 'M14,78 Q14,10 70,8 Q126,10 126,78 Z', fill: 'url(#' + id + '-topo)' }));
    svg.appendChild(el('path', { d: 'M22,74 Q22,20 70,18 Q118,20 118,74 Z', fill: 'none', stroke: '#fff', 'stroke-opacity': 0.5, 'stroke-width': 2 }));
    svg.appendChild(texto(titulo, { x: 70, y: 56, class: 'maquina-titulo' }));
    // tela com 3 rolos
    svg.appendChild(el('rect', { x: 24, y: 90, width: 92, height: 66, rx: 6, fill: 'url(#' + id + '-tela)' }));
    simbolos.forEach((s, i) => {
      svg.appendChild(el('rect', { x: 30 + i * 28, y: 100, width: 24, height: 46, rx: 3, fill: '#fff' }));
      svg.appendChild(texto(s, { x: 42 + i * 28, y: 130, class: 'maquina-simbolo', fill: corB }));
    });
    // painel de botões
    svg.appendChild(el('rect', { x: 18, y: 166, width: 104, height: 20, rx: 3, fill: '#2a2e3b' }));
    ['#ff4d5e', '#ffd34d', '#4dd2ff'].forEach((c, i) => {
      svg.appendChild(el('circle', { cx: 40 + i * 30, cy: 176, r: 5, fill: c }));
    });
    // painel inferior iluminado
    svg.appendChild(el('rect', { x: 22, y: 196, width: 96, height: 60, rx: 6, fill: 'url(#' + id + '-topo)', opacity: 0.85 }));
    svg.appendChild(texto('$$$', { x: 70, y: 233, class: 'maquina-rodape' }));
    svg.appendChild(el('rect', { x: 18, y: 264, width: 104, height: 26, rx: 3, fill: '#0f1118' }));
    svg.style.setProperty('--brilho', corA);
    return svg;
  }

  function criarMaquinas() {
    const esquerda = document.querySelector('.maquinas-esquerda');
    const direita = document.querySelector('.maquinas-direita');
    if (!esquerda || !direita || celular) return;
    esquerda.append(
      maquina('SORTE', '#c64bff', '#5a1490', ['7', '★', '7']),
      maquina('777', '#ff3b47', '#7a0710', ['7', '7', '7']));
    direita.append(
      maquina('NEON', '#ff4fb8', '#7d0f55', ['$', '★', '$']),
      maquina('JACKPOT', '#3ee07a', '#0d6b33', ['★', '7', '★']));
  }

  criarCristais();
  criarMaquinas();
})();
