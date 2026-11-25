/* Desenho e animação da roleta (SVG). O resultado do giro sempre vem do servidor. */
(function () {
  'use strict';

  const SVG_NS = 'http://www.w3.org/2000/svg';
  const RAIO_FATIAS = 222;
  const RAIO_CENTRO = 74;
  const RAIO_ARO = 234;
  const RAIO_ICONE = 184;
  const RAIO_TEXTO = 124;
  const DURACAO_GIRO_MS = 7000;

  function el(nome, atributos, filhos) {
    const no = document.createElementNS(SVG_NS, nome);
    Object.entries(atributos || {}).forEach(([k, v]) => no.setAttribute(k, v));
    (filhos || []).forEach((f) => no.appendChild(f));
    return no;
  }

  function ponto(raio, graus) {
    const rad = (graus * Math.PI) / 180;
    return [raio * Math.sin(rad), -raio * Math.cos(rad)];
  }

  function caminhoFatia(inicio, fim) {
    const [x0, y0] = ponto(RAIO_CENTRO - 2, inicio);
    const [x1, y1] = ponto(RAIO_FATIAS, inicio);
    const [x2, y2] = ponto(RAIO_FATIAS, fim);
    const [x3, y3] = ponto(RAIO_CENTRO - 2, fim);
    return `M${x0},${y0} L${x1},${y1} A${RAIO_FATIAS},${RAIO_FATIAS} 0 0 1 ${x2},${y2} ` +
      `L${x3},${y3} A${RAIO_CENTRO - 2},${RAIO_CENTRO - 2} 0 0 0 ${x0},${y0} Z`;
  }

  /* Ícones desenhados em coordenadas locais (~48x48), "para cima" = para fora da roleta. */
  const ICONES = {
    DINHEIRO: () => el('text', { class: 'icone-texto', 'font-size': 46, y: 2 }, [document.createTextNode('$')]),
    BONUS: () => el('text', { class: 'icone-texto', 'font-size': 30, y: 2 }, [document.createTextNode('R$')]),
    FICHAS: () => el('g', { class: 'icone-linha' }, [-11, 0, 11].map((x) =>
      el('ellipse', { cx: x, cy: 0, rx: 8, ry: 16 }))),
    ROUPA: () => el('path', {
      class: 'icone-solido',
      d: 'M-9,-17 Q0,-10 9,-17 L22,-11 L27,1 L17,4 L15,-3 L15,18 L-15,18 L-15,-3 L-17,4 L-27,1 L-22,-11 Z'
    }),
    VEICULO: () => el('g', {}, [
      el('path', {
        class: 'icone-solido',
        d: 'M-25,8 L-25,-1 Q-25,-5 -20,-6 L-13,-7 L-7,-16 Q-5,-18 -2,-18 L9,-18 Q12,-18 14,-15 L19,-7 ' +
          'L23,-6 Q26,-5 26,-1 L26,8 Z'
      }),
      el('path', { class: 'icone-recorte', d: 'M-4,-14 L8,-14 L12,-8 L-8,-8 Z' }),
      el('circle', { class: 'icone-roda', cx: -14, cy: 9, r: 6 }),
      el('circle', { class: 'icone-roda', cx: 15, cy: 9, r: 6 })
    ]),
    TICKET: () => el('g', { transform: 'rotate(-12)' }, [
      el('path', {
        class: 'icone-solido',
        d: 'M-22,-14 L22,-14 L22,-5 Q17,0 22,5 L22,14 L-22,14 L-22,5 Q-17,0 -22,-5 Z'
      }),
      el('text', { class: 'icone-recorte-texto', 'font-size': 20, y: 1 }, [document.createTextNode('%')])
    ]),
    MISTERIO: () => {
      const pontos = [];
      for (let i = 0; i < 10; i++) {
        const r = i % 2 === 0 ? 24 : 10;
        const [x, y] = ponto(r, i * 36);
        pontos.push(`${x.toFixed(2)},${y.toFixed(2)}`);
      }
      return el('g', {}, [
        el('polygon', { class: 'icone-solido', points: pontos.join(' ') }),
        el('text', { class: 'icone-recorte-texto', 'font-size': 13, y: 2 }, [document.createTextNode('?')])
      ]);
    }
  };

  function textoDaFatia(premio) {
    switch (premio.tipo) {
      case 'DINHEIRO': return premio.rotulo.toUpperCase();
      case 'FICHAS': return premio.rotulo.split(' ')[0];
      case 'BONUS': return premio.rotulo.replace('Bônus de R$ ', '');
      case 'ROUPA': return 'ROUPA';
      case 'VEICULO': return 'VEÍCULO';
      case 'TICKET': return '2 TICKETS';
      case 'MISTERIO': return 'MISTÉRIO';
      default: return premio.rotulo;
    }
  }

  function criarDefs() {
    const gradiente = (id, cores) => el('radialGradient', {
      id, cx: 0, cy: 0, r: RAIO_FATIAS, gradientUnits: 'userSpaceOnUse'
    }, cores.map(([off, cor]) => el('stop', { offset: off, 'stop-color': cor })));
    const brilho = el('filter', { id: 'brilho-aro', x: '-20%', y: '-20%', width: '140%', height: '140%' }, [
      el('feGaussianBlur', { stdDeviation: 6, result: 'b' }),
      el('feMerge', {}, [el('feMergeNode', { in: 'b' }), el('feMergeNode', { in: 'SourceGraphic' })])
    ]);
    return el('defs', {}, [
      gradiente('fatia-a', [['0.30', '#bfe9ff'], ['0.55', '#5fb9ec'], ['0.80', '#2a7fc0'], ['1', '#1a4f88']]),
      gradiente('fatia-b', [['0.30', '#a9e0fb'], ['0.55', '#4aa8e0'], ['0.80', '#2272b3'], ['1', '#16457a']]),
      gradiente('centro', [['0', '#1b2c44'], ['1', '#070d18']]),
      brilho
    ]);
  }

  /** aoSelecionar = null cria uma roleta só decorativa (sem clique). */
  function criar(container, premios, aoSelecionar) {
    const interativa = typeof aoSelecionar === 'function';
    const passo = 360 / premios.length;
    const svg = el('svg', {
      viewBox: '-258 -276 516 534', class: 'roleta-svg', role: 'group',
      'aria-label': 'Roleta de prêmios com ' + premios.length + ' fatias'
    });
    svg.appendChild(criarDefs());

    const disco = el('g', { class: 'disco' });
    disco.appendChild(el('circle', { class: 'fundo-roleta', r: RAIO_ARO }));

    const fatias = premios.map((premio, i) => {
      const inicio = i * passo;
      const centro = inicio + passo / 2;
      const grupo = el('g', interativa ? {
        class: 'fatia', tabindex: 0, role: 'button', 'data-indice': i,
        'aria-label': 'Escolher prêmio ' + premio.rotulo
      } : { class: 'fatia', 'data-indice': i });
      grupo.appendChild(el('path', { class: 'fatia-fundo', d: caminhoFatia(inicio, inicio + passo),
        fill: i % 2 === 0 ? 'url(#fatia-a)' : 'url(#fatia-b)' }));
      const icone = ICONES[premio.tipo] ? ICONES[premio.tipo]() : el('g');
      grupo.appendChild(el('g', { transform: `rotate(${centro}) translate(0,${-RAIO_ICONE})` }, [icone]));
      const texto = textoDaFatia(premio);
      grupo.appendChild(el('text', {
        class: texto.length > 7 ? 'fatia-texto longo' : 'fatia-texto',
        transform: `rotate(${centro}) translate(0,${-RAIO_TEXTO}) rotate(-90)`
      }, [document.createTextNode(texto)]));
      grupo.appendChild(el('title', {}, [document.createTextNode(premio.rotulo)]));
      if (interativa) {
        const selecionar = () => aoSelecionar(i);
        grupo.addEventListener('click', selecionar);
        grupo.addEventListener('keydown', (e) => {
          if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); selecionar(); }
        });
      }
      disco.appendChild(grupo);
      return grupo;
    });

    // Divisórias e pinos do aro, por cima das fatias.
    for (let i = 0; i < premios.length; i++) {
      const [x0, y0] = ponto(RAIO_CENTRO, i * passo);
      const [x1, y1] = ponto(RAIO_ARO - 4, i * passo);
      disco.appendChild(el('line', { class: 'divisoria', x1: x0, y1: y0, x2: x1, y2: y1 }));
    }
    disco.appendChild(el('circle', { class: 'aro', r: RAIO_ARO, filter: 'url(#brilho-aro)' }));
    disco.appendChild(el('circle', { class: 'aro-interno', r: RAIO_FATIAS + 2 }));
    for (let i = 0; i < premios.length; i++) {
      const [x, y] = ponto(RAIO_ARO, i * passo);
      disco.appendChild(el('circle', { class: 'pino', cx: x, cy: y, r: 4.5 }));
    }
    svg.appendChild(disco);

    // Centro fixo (não gira): logo da roleta.
    const centro = el('g', { class: 'centro' }, [
      el('circle', { r: RAIO_CENTRO, fill: 'url(#centro)', class: 'centro-disco' }),
      el('text', { class: 'logo-pequeno', y: -30 }, [document.createTextNode('✦ ✦ ✦')]),
      el('text', { class: 'logo-script', y: 6 }, [document.createTextNode('Roleta')]),
      el('text', { class: 'logo-sub', y: 30 }, [document.createTextNode('DO SABER')]),
      el('text', { class: 'logo-mini', y: 44 }, [document.createTextNode('QUIZ & PRÊMIOS')])
    ]);
    svg.appendChild(centro);
    svg.appendChild(el('polygon', { class: 'ponteiro', points: '-15,-268 15,-268 0,-222', filter: 'url(#brilho-aro)' }));
    if (!interativa) svg.classList.add('travada');
    container.appendChild(svg);

    let rotacaoAtual = 0;

    return {
      destacar(indice) {
        fatias.forEach((f, i) => f.classList.toggle('selecionada', i === indice));
      },
      travar(travado) {
        svg.classList.toggle('travada', travado);
      },
      /** Gira até a fatia sorteada ficar sob o ponteiro. Resolve quando a animação termina. */
      girarPara(indice) {
        const centroFatia = indice * passo + passo / 2;
        const desvio = (Math.random() - 0.5) * (passo * 0.6);
        const destinoNormalizado = ((-centroFatia + desvio) % 360 + 360) % 360;
        const atualNormalizado = ((rotacaoAtual % 360) + 360) % 360;
        let delta = destinoNormalizado - atualNormalizado;
        if (delta < 0) delta += 360;
        rotacaoAtual += 360 * 6 + delta;
        return new Promise((resolve) => {
          disco.style.transition = `transform ${DURACAO_GIRO_MS}ms cubic-bezier(0.12, 0.65, 0.08, 1)`;
          disco.style.transform = `rotate(${rotacaoAtual}deg)`;
          setTimeout(resolve, DURACAO_GIRO_MS + 150);
        });
      }
    };
  }

  window.Roleta = { criar };
})();
