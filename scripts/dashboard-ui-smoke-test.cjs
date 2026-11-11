const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const elements = new Map();
function makeElement(tagOrId) {
  return {
    id: tagOrId, value: '', textContent: '', hidden: false, disabled: false, title: '',
    style: {}, children: [], listeners: {}, className: '',
    classList: { toggle() {} },
    addEventListener(type, callback) { this.listeners[type] = callback; },
    append(...children) { this.children.push(...children); },
    replaceChildren(...children) { this.children = [...children]; },
    getContext() { return {}; }
  };
}
const document = {
  getElementById(id) {
    if (!elements.has(id)) elements.set(id, makeElement(id));
    return elements.get(id);
  },
  createElement(tag) { return makeElement(tag); }
};

let fetchUrl = '';
let intervalCallback;
const resposta = {
  periodo: { inicio: '2026-10-29', fim: '2026-10-30' },
  statusSelecionado: null,
  granularidade: 'DIA',
  indicadores: { faturamento: 100, totalPedidos: 2, ticketMedio: 50, pedidosEmAndamento: 1, pedidosCancelados: 0 },
  vendas: [{ periodo: '2026-10-30', rotulo: '30/10', faturamento: 100, pedidos: 2 }],
  pedidosPorStatus: [{ codigo: 'RECEBIDO', nome: 'Recebido', quantidade: 2 }],
  produtosMaisVendidos: [{ produtoId: 'p1', nome: 'Pizza de queijo', quantidade: 3, faturamento: 100 }],
  pedidosRecentes: [{ id: 'pedido-123456', cliente: 'Ana Souza', status: 'RECEBIDO', statusNome: 'Recebido', dataHoraEntrada: '2026-10-30T12:00:00Z', valorTotal: 100 }],
  atualizadoEm: '2026-10-30T12:30:00Z'
};

class Chart {
  static instances = [];
  constructor(context, config) { this.config = config; Chart.instances.push(this); }
  destroy() { this.destroyed = true; }
}

const context = {
  document,
  Chart,
  URLSearchParams,
  Intl,
  Date,
  Number,
  String,
  Math,
  fetch: async (url) => {
    fetchUrl = url;
    return { ok: true, headers: { get: () => 'application/json' }, json: async () => resposta };
  },
  window: { setInterval(callback) { intervalCallback = callback; } }
};

vm.runInNewContext(fs.readFileSync(process.argv[2], 'utf8'), context);
setImmediate(() => {
  try {
    assert.match(fetchUrl, /^\/api\/dashboard\/resumo\?/);
    assert.ok(new URLSearchParams(fetchUrl.split('?')[1]).get('inicio'));
    assert.ok(new URLSearchParams(fetchUrl.split('?')[1]).get('fim'));
    assert.match(document.getElementById('indicador-faturamento').textContent, /100,00/);
    assert.equal(document.getElementById('indicador-pedidos').textContent, '2');
    assert.equal(document.getElementById('lista-produtos').children.length, 1);
    assert.equal(document.getElementById('lista-pedidos').children.length, 1);
    assert.equal(document.getElementById('legenda-status').children.length, 1);
    assert.equal(Chart.instances.length, 2);
    assert.equal(document.getElementById('dashboard-erro').hidden, true);
    assert.equal(typeof intervalCallback, 'function');
    console.log('Dashboard UI smoke test: PASS (filtros, KPIs, gráficos, ranking, pedidos e atualização automática)');
  } catch (error) {
    console.error(error);
    process.exitCode = 1;
  }
});
