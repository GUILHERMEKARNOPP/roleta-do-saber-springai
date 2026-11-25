/* Fluxo do jogo: escolher prêmio -> comprar ticket -> responder -> girar. */
(function () {
  'use strict';

  const csrfToken = document.querySelector('meta[name="_csrf"]').content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
  const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 });
  const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

  const $ = (id) => document.getElementById(id);
  const ui = {
    saldo: $('saldo'), ajuda: $('ajuda'),
    abaRoleta: $('aba-roleta'), abaHistorico: $('aba-historico'),
    painelRoleta: $('painel-roleta'), painelHistorico: $('painel-historico'),
    selectPremio: $('select-premio'), selectCategoria: $('select-categoria'),
    premioNome: $('premio-escolhido'), chanceAtual: $('chance-atual'),
    btnComprar: $('btn-comprar'), custo: $('custo-ticket'),
    etapaEscolha: $('etapa-escolha'), etapaCarregando: $('etapa-carregando'),
    etapaPergunta: $('etapa-pergunta'), etapaResultado: $('etapa-resultado'), etapaGiro: $('etapa-giro'),
    dificuldade: $('dificuldade'), categoria: $('categoria-pergunta'), bonus: $('bonus-info'),
    timer: $('timer'), timerBarra: $('timer-barra'),
    pergunta: $('pergunta'), alternativas: $('alternativas'),
    resultadoTitulo: $('resultado-titulo'), resultadoTexto: $('resultado-texto'), explicacao: $('explicacao'),
    medidor: $('medidor'), medidorBase: $('medidor-base'), medidorBonus: $('medidor-bonus'),
    medidorTexto: $('medidor-texto'), btnGirar: $('btn-girar'), btnNovaRodada: $('btn-nova-rodada'),
    giroTitulo: $('giro-titulo'), giroTexto: $('giro-texto'), btnJogarNovamente: $('btn-jogar-novamente'),
    mensagem: $('mensagem'),
    historicoLista: $('historico-lista'), historicoVazio: $('historico-vazio'),
    pagAnterior: $('pag-anterior'), pagProxima: $('pag-proxima'), pagInfo: $('pag-info')
  };

  let estado = null;
  let roleta = null;
  let premioSelecionado = null;
  let rodadaAtual = null;
  let cronometro = null;
  let paginaHistorico = 0;

  async function api(metodo, url, corpo) {
    const opcoes = { method: metodo, headers: { Accept: 'application/json' }, credentials: 'same-origin' };
    if (metodo !== 'GET') {
      opcoes.headers[csrfHeader] = csrfToken;
      opcoes.headers['Content-Type'] = 'application/json';
      opcoes.body = JSON.stringify(corpo || {});
    }
    const resposta = await fetch(url, opcoes);
    if (resposta.status === 401 || resposta.status === 403) {
      window.location.href = '/login';
      throw new Error('Sessão expirada.');
    }
    const json = await resposta.json().catch(() => ({}));
    if (!resposta.ok) throw new Error(json.mensagem || 'Erro inesperado. Tente novamente.');
    return json;
  }

  function mostrarMensagem(texto) {
    ui.mensagem.textContent = texto || '';
  }

  const AJUDAS = {
    'etapa-escolha': 'Clique numa fatia da roleta para escolher o prêmio que você quer ganhar.',
    'etapa-carregando': 'Aguarde: a IA está criando sua pergunta.',
    'etapa-pergunta': 'Responda antes que o tempo acabe.',
    'etapa-resultado': '',
    'etapa-giro': 'Escolha um novo prêmio para jogar de novo.'
  };

  function ajuda(texto) {
    ui.ajuda.textContent = texto;
    ui.ajuda.hidden = !texto;
  }

  function mostrarEtapa(etapa) {
    [ui.etapaEscolha, ui.etapaCarregando, ui.etapaPergunta, ui.etapaResultado, ui.etapaGiro]
      .forEach((e) => { e.hidden = e !== etapa; });
    const escolhendo = etapa === ui.etapaEscolha;
    roleta.travar(!escolhendo);
    ui.selectPremio.disabled = !escolhendo;
    if (AJUDAS[etapa.id]) ajuda(AJUDAS[etapa.id]);
  }

  function atualizarCarteira(saldo) {
    if (saldo !== undefined && saldo !== null) {
      estado.saldo = saldo;
      ui.saldo.textContent = moeda.format(saldo);
    }
    ui.btnComprar.disabled = premioSelecionado === null || estado.saldo < estado.custoTicket;
  }

  function selecionarPremio(indice) {
    if (ui.etapaEscolha.hidden) return;
    premioSelecionado = indice;
    roleta.destacar(indice);
    ui.selectPremio.value = String(indice);
    ui.premioNome.textContent = estado.premios[indice].rotulo;
    ui.chanceAtual.textContent = estado.chanceBase + '%';
    atualizarCarteira();
    if (estado.saldo < estado.custoTicket) {
      mostrarMensagem('Saldo insuficiente para comprar um ticket.');
    } else {
      mostrarMensagem('');
    }
  }

  function preencherSelects() {
    estado.premios.forEach((p) => {
      const opcao = document.createElement('option');
      opcao.value = String(p.indice);
      opcao.textContent = p.rotulo;
      ui.selectPremio.appendChild(opcao);
    });
    estado.categorias.forEach((c) => {
      const opcao = document.createElement('option');
      opcao.value = c.codigo;
      opcao.textContent = c.nome;
      ui.selectCategoria.appendChild(opcao);
    });
    ui.selectPremio.addEventListener('change', () => {
      if (ui.selectPremio.value !== '') selecionarPremio(Number(ui.selectPremio.value));
    });
  }

  function nomeDificuldade(d) {
    return { FACIL: 'Fácil', MEDIO: 'Médio', DIFICIL: 'Difícil', MUITO_DIFICIL: 'Muito difícil' }[d] || d;
  }

  function exibirPergunta(rodada) {
    rodadaAtual = rodada;
    premioSelecionado = rodada.premioAlvo;
    roleta.destacar(rodada.premioAlvo);
    ui.premioNome.textContent = estado.premios[rodada.premioAlvo].rotulo;
    ui.dificuldade.textContent = nomeDificuldade(rodada.dificuldade);
    ui.dificuldade.className = 'dificuldade dificuldade-' + rodada.dificuldade.toLowerCase();
    ui.categoria.textContent = rodada.categoria;
    ui.bonus.textContent = 'Acertando, sua chance de ganhar "' + estado.premios[rodada.premioAlvo].rotulo +
      '" sobe de ' + rodada.chanceBase + '% para ' + Math.min(rodada.chanceBase + rodada.bonusChance, 95) + '%.';
    ui.pergunta.textContent = rodada.pergunta;
    ui.alternativas.replaceChildren();
    ['A', 'B', 'C', 'D'].forEach((letra, i) => {
      const botao = document.createElement('button');
      botao.type = 'button';
      botao.className = 'linha alternativa';
      botao.dataset.letra = letra;
      const marcador = document.createElement('span');
      marcador.className = 'letra';
      marcador.textContent = letra;
      const texto = document.createElement('span');
      texto.textContent = rodada.alternativas[i];
      botao.append(marcador, texto);
      botao.addEventListener('click', () => responder(letra));
      ui.alternativas.appendChild(botao);
    });
    mostrarEtapa(ui.etapaPergunta);
    iniciarCronometro(rodada.segundosRestantes);
  }

  function iniciarCronometro(segundos) {
    pararCronometro();
    const total = estado.prazoRespostaSegundos;
    const fim = Date.now() + segundos * 1000;
    const atualizar = () => {
      const restante = Math.max(0, Math.ceil((fim - Date.now()) / 1000));
      ui.timer.textContent = String(restante);
      ui.timerBarra.style.transform = 'scaleX(' + Math.max(0, restante / total) + ')';
      ui.timer.classList.toggle('urgente', restante <= 10);
      if (restante <= 0) {
        pararCronometro();
        tempoEsgotado();
      }
    };
    atualizar();
    cronometro = setInterval(atualizar, 250);
  }

  function pararCronometro() {
    if (cronometro) clearInterval(cronometro);
    cronometro = null;
  }

  async function tempoEsgotado() {
    travarAlternativas();
    // Dá a folga do servidor passar e consulta o estado, que encerra a rodada vencida.
    setTimeout(async () => {
      try {
        const novo = await api('GET', '/api/estado');
        atualizarCarteira(novo.saldo);
        mostrarResultado({ acertou: false, tempoEsgotado: true, respostaCorreta: null, explicacao: null });
        carregarHistorico(0);
      } catch (e) {
        mostrarMensagem(e.message);
      }
    }, 6000);
  }

  function travarAlternativas() {
    ui.alternativas.querySelectorAll('button').forEach((b) => { b.disabled = true; });
  }

  async function comprarTicket() {
    if (premioSelecionado === null) return;
    mostrarMensagem('');
    ui.btnComprar.disabled = true;
    mostrarEtapa(ui.etapaCarregando);
    try {
      const rodada = await api('POST', '/api/rodadas', {
        premioIndice: premioSelecionado, categoria: ui.selectCategoria.value
      });
      atualizarCarteira(estado.saldo - estado.custoTicket);
      exibirPergunta(rodada);
      carregarHistorico(0);
    } catch (e) {
      mostrarEtapa(ui.etapaEscolha);
      atualizarCarteira();
      mostrarMensagem(e.message);
    }
  }

  async function responder(letra) {
    pararCronometro();
    travarAlternativas();
    try {
      const resultado = await api('POST', '/api/rodadas/' + rodadaAtual.id + '/resposta', { alternativa: letra });
      ui.alternativas.querySelectorAll('button').forEach((b) => {
        if (b.dataset.letra === resultado.respostaCorreta) b.classList.add('correta');
        else if (b.dataset.letra === letra) b.classList.add('errada');
      });
      atualizarCarteira(resultado.saldo);
      setTimeout(() => mostrarResultado(resultado), 1300);
      carregarHistorico(0);
    } catch (e) {
      mostrarMensagem(e.message);
    }
  }

  function mostrarResultado(resultado) {
    ui.explicacao.textContent = resultado.explicacao || '';
    ui.explicacao.hidden = !resultado.explicacao;
    if (resultado.acertou) {
      ui.resultadoTitulo.textContent = 'Resposta certa!';
      ui.resultadoTitulo.className = 'menu-titulo sucesso';
      ui.resultadoTexto.textContent = 'Você ganhou o direito de girar a roleta.';
      mostrarMedidor(resultado.chanceBase, resultado.chanceFinal);
      ui.btnGirar.hidden = false;
      ui.btnNovaRodada.hidden = true;
      ajuda('Clique em GIRAR A ROLETA.');
    } else {
      ui.resultadoTitulo.textContent = resultado.tempoEsgotado ? 'Tempo esgotado!' : 'Resposta errada!';
      ui.resultadoTitulo.className = 'menu-titulo falha';
      ui.resultadoTexto.textContent = 'Você perdeu ' + moeda.format(estado.custoTicket) + ' do ticket.' +
        (resultado.respostaCorreta ? ' A resposta certa era a letra ' + resultado.respostaCorreta + '.' : '');
      ui.medidor.hidden = true;
      ui.btnGirar.hidden = true;
      ui.btnNovaRodada.hidden = false;
      ajuda('Você perdeu o ticket. Escolha um prêmio e tente de novo.');
    }
    mostrarEtapa(ui.etapaResultado);
  }

  function mostrarMedidor(base, final) {
    ui.medidor.hidden = false;
    ui.medidorBase.style.transform = 'scaleX(' + base / 100 + ')';
    ui.medidorBonus.style.transform = 'scaleX(0)';
    requestAnimationFrame(() => requestAnimationFrame(() => {
      ui.medidorBonus.style.transform = 'scaleX(' + final / 100 + ')';
    }));
    ui.medidorTexto.textContent = 'Chance de ganhar "' + estado.premios[premioSelecionado].rotulo + '": ' +
      base + '% → ' + final + '%';
  }

  async function girar() {
    ui.btnGirar.disabled = true;
    mostrarMensagem('');
    try {
      const giro = await api('POST', '/api/rodadas/' + rodadaAtual.id + '/giro');
      ui.btnGirar.hidden = true;
      document.body.classList.add('girando');
      ajuda('Boa sorte!');
      await roleta.girarPara(giro.premioSorteado);
      document.body.classList.remove('girando');
      roleta.destacar(giro.premioSorteado);
      ui.giroTitulo.textContent = giro.ganhouPremioEscolhido ? 'JACKPOT! Você ganhou o prêmio escolhido!'
        : 'Você ganhou: ' + estado.premios[giro.premioSorteado].rotulo;
      ui.giroTitulo.className = 'menu-titulo ' + (giro.ganhouPremioEscolhido ? 'jackpot' : 'sucesso');
      ui.giroTexto.textContent = giro.descricaoGanho;
      atualizarCarteira(giro.saldo);
      mostrarEtapa(ui.etapaGiro);
      carregarHistorico(0);
    } catch (e) {
      document.body.classList.remove('girando');
      mostrarMensagem(e.message);
    } finally {
      ui.btnGirar.disabled = false;
    }
  }

  function novaRodada() {
    rodadaAtual = null;
    mostrarMensagem('');
    mostrarEtapa(ui.etapaEscolha);
    if (premioSelecionado !== null) selecionarPremio(premioSelecionado);
  }

  async function carregarHistorico(pagina) {
    try {
      const dados = await api('GET', '/api/historico?pagina=' + pagina);
      paginaHistorico = dados.pagina;
      ui.historicoLista.replaceChildren();
      dados.itens.forEach((item) => {
        const li = document.createElement('li');
        const topo = document.createElement('div');
        topo.className = 'historico-topo';
        const premio = document.createElement('span');
        premio.textContent = item.premioEscolhido;
        const resultado = document.createElement('span');
        resultado.className = 'status-' + item.status.toLowerCase();
        resultado.textContent = item.resultado;
        topo.append(premio, resultado);
        const detalhe = document.createElement('div');
        detalhe.className = 'historico-detalhe';
        detalhe.textContent = dataHora.format(new Date(item.data)) + ' · ' + item.categoria + ' · ' +
          nomeDificuldade(item.dificuldade);
        li.append(topo, detalhe);
        ui.historicoLista.appendChild(li);
      });
      ui.historicoVazio.hidden = dados.itens.length > 0;
      ui.pagInfo.textContent = dados.totalPaginas > 0 ? (dados.pagina + 1) + ' / ' + dados.totalPaginas : '';
      ui.pagAnterior.disabled = dados.pagina <= 0;
      ui.pagProxima.disabled = dados.pagina + 1 >= dados.totalPaginas;
    } catch (e) {
      mostrarMensagem(e.message);
    }
  }

  function mostrarAba(historico) {
    ui.painelRoleta.hidden = historico;
    ui.painelHistorico.hidden = !historico;
    ui.abaRoleta.classList.toggle('ativa', !historico);
    ui.abaHistorico.classList.toggle('ativa', historico);
    if (historico) carregarHistorico(0);
  }

  async function iniciar() {
    try {
      estado = await api('GET', '/api/estado');
    } catch (e) {
      mostrarMensagem(e.message);
      return;
    }
    roleta = window.Roleta.criar($('roleta'), estado.premios, selecionarPremio);
    preencherSelects();
    ui.custo.textContent = moeda.format(estado.custoTicket);
    atualizarCarteira(estado.saldo);

    const rodada = estado.rodadaAtual;
    if (rodada && rodada.status === 'AGUARDANDO_RESPOSTA') {
      exibirPergunta(rodada);
    } else if (rodada && rodada.status === 'LIBERADA_PARA_GIRO') {
      rodadaAtual = rodada;
      premioSelecionado = rodada.premioAlvo;
      roleta.destacar(rodada.premioAlvo);
      ui.premioNome.textContent = estado.premios[rodada.premioAlvo].rotulo;
      mostrarResultado({ acertou: true, chanceBase: rodada.chanceBase, chanceFinal: rodada.chanceFinal });
      ui.resultadoTexto.textContent = 'Você já acertou a pergunta. Gire a roleta!';
    } else {
      mostrarEtapa(ui.etapaEscolha);
    }

    ui.btnComprar.addEventListener('click', comprarTicket);
    ui.btnGirar.addEventListener('click', girar);
    ui.btnNovaRodada.addEventListener('click', novaRodada);
    ui.btnJogarNovamente.addEventListener('click', novaRodada);
    ui.abaRoleta.addEventListener('click', () => mostrarAba(false));
    ui.abaHistorico.addEventListener('click', () => mostrarAba(true));
    ui.pagAnterior.addEventListener('click', () => carregarHistorico(paginaHistorico - 1));
    ui.pagProxima.addEventListener('click', () => carregarHistorico(paginaHistorico + 1));
    carregarHistorico(0);
  }

  iniciar();
})();
