(function () {
    'use strict';

    const form = document.getElementById('dashboard-filtros');
    const inicioInput = document.getElementById('filtro-inicio');
    const fimInput = document.getElementById('filtro-fim');
    const statusInput = document.getElementById('filtro-status');
    const aplicarButton = document.getElementById('aplicar-filtros');
    const atualizarButton = document.getElementById('atualizar-dashboard');
    const errorBox = document.getElementById('dashboard-erro');
    const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
    const number = new Intl.NumberFormat('pt-BR');
    const statusColors = ['#e8a454', '#cf8055', '#24a06c', '#6391c4', '#8a73be', '#458761', '#cf5a5a'];
    const statusLabels = {
        RECEBIDO: 'Recebido', EM_PREPARACAO: 'Em preparo', PRONTO: 'Pronto',
        SAIU_PARA_ENTREGA: 'Saiu para entrega', RETIRADO: 'Retirado',
        FINALIZADO: 'Finalizado', CANCELADO: 'Cancelado'
    };
    let salesChart = null;
    let statusChart = null;
    let loading = false;

    function saoPauloToday() {
        const parts = new Intl.DateTimeFormat('en-US', {
            timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit'
        }).formatToParts(new Date());
        const part = (type) => parts.find((value) => value.type === type).value;
        return `${part('year')}-${part('month')}-${part('day')}`;
    }

    function dateOffset(isoDate, offset) {
        const [year, month, day] = isoDate.split('-').map(Number);
        const date = new Date(Date.UTC(year, month - 1, day + offset));
        return date.toISOString().slice(0, 10);
    }

    function moneyValue(value) {
        return money.format(Number(value || 0));
    }

    function setText(id, value) {
        document.getElementById(id).textContent = value;
    }

    function node(tag, className, text) {
        const element = document.createElement(tag);
        if (className) element.className = className;
        if (text !== undefined) element.textContent = text;
        return element;
    }

    function setBusy(value) {
        loading = value;
        aplicarButton.disabled = value;
        atualizarButton.disabled = value;
        aplicarButton.textContent = value ? 'Atualizando…' : '⌕  Aplicar filtros';
        atualizarButton.classList.toggle('is-loading', value);
    }

    function formatDate(value) {
        if (!value) return '—';
        return new Intl.DateTimeFormat('pt-BR', {
            timeZone: 'America/Sao_Paulo', day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
        }).format(new Date(value));
    }

    async function loadDashboard(event) {
        if (event) event.preventDefault();
        if (loading) return;
        setBusy(true);
        errorBox.hidden = true;
        const params = new URLSearchParams({ inicio: inicioInput.value, fim: fimInput.value });
        if (statusInput.value) params.set('status', statusInput.value);

        try {
            const response = await fetch(`/api/dashboard/resumo?${params.toString()}`, {
                credentials: 'same-origin', cache: 'no-store', headers: { Accept: 'application/json' }
            });
            if (!response.headers.get('content-type')?.includes('application/json')) {
                throw new Error('Sua sessão pode ter expirado. Entre novamente no sistema para atualizar o dashboard.');
            }
            const data = await response.json();
            if (!response.ok) throw new Error(data.mensagem || data.message || 'Não foi possível carregar os dados do dashboard.');
            renderDashboard(data);
            setText('ultima-atualizacao', `Atualizado às ${new Intl.DateTimeFormat('pt-BR', {
                timeZone: 'America/Sao_Paulo', hour: '2-digit', minute: '2-digit'
            }).format(new Date(data.atualizadoEm))}`);
        } catch (error) {
            errorBox.textContent = error.message || 'Falha ao consultar o serviço. Tente novamente.';
            errorBox.hidden = false;
            setText('ultima-atualizacao', 'Falha na última atualização');
        } finally {
            setBusy(false);
        }
    }

    function renderDashboard(data) {
        const indicadores = data.indicadores || {};
        setText('indicador-faturamento', moneyValue(indicadores.faturamento));
        setText('indicador-pedidos', number.format(indicadores.totalPedidos || 0));
        setText('indicador-ticket', moneyValue(indicadores.ticketMedio));
        setText('indicador-andamento', number.format(indicadores.pedidosEmAndamento || 0));
        setText('indicador-cancelados', number.format(indicadores.pedidosCancelados || 0));
        setText('legenda-periodo', `Exibindo ${data.granularidade === 'DIA' ? 'por dia' : data.granularidade === 'SEMANA' ? 'por semana' : 'por mês'} · ${data.periodo.inicio} a ${data.periodo.fim}`);
        renderSales(data.vendas || []);
        renderStatuses(data.pedidosPorStatus || []);
        renderProducts(data.produtosMaisVendidos || []);
        renderRecentOrders(data.pedidosRecentes || []);
    }

    function renderSales(points) {
        const canvas = document.getElementById('grafico-vendas');
        document.getElementById('sem-vendas').hidden = points.some((point) => point.pedidos > 0);
        if (salesChart) salesChart.destroy();
        salesChart = new Chart(canvas.getContext('2d'), {
            type: 'line',
            data: {
                labels: points.map((point) => point.rotulo),
                datasets: [
                    {
                        label: 'Faturamento', data: points.map((point) => Number(point.faturamento || 0)),
                        yAxisID: 'vendas', borderColor: '#16865b', backgroundColor: 'rgba(22,134,91,.10)',
                        borderWidth: 2, pointRadius: points.length > 60 ? 0 : 3, pointHoverRadius: 5,
                        pointBackgroundColor: '#fff', pointBorderWidth: 2, lineTension: .32, fill: true
                    },
                    {
                        label: 'Pedidos', data: points.map((point) => Number(point.pedidos || 0)),
                        yAxisID: 'pedidos', borderColor: '#e89b55', backgroundColor: '#e89b55',
                        borderWidth: 1.5, pointRadius: points.length > 60 ? 0 : 2, pointHoverRadius: 4,
                        pointBackgroundColor: '#e89b55', lineTension: .2, fill: false
                    }
                ]
            },
            options: {
                responsive: true, maintainAspectRatio: false, legend: { display: false },
                tooltips: { mode: 'index', intersect: false, callbacks: {
                    label: (item, chartData) => {
                        const name = chartData.datasets[item.datasetIndex].label;
                        return name === 'Faturamento' ? ` Faturamento: ${moneyValue(item.yLabel)}` : ` Pedidos: ${number.format(item.yLabel || 0)}`;
                    }
                } },
                hover: { mode: 'nearest', intersect: false },
                scales: {
                    xAxes: [{ gridLines: { display: false }, ticks: { maxTicksLimit: 10, maxRotation: 0, fontColor: '#909b93', fontSize: 10 } }],
                    yAxes: [
                        { id: 'vendas', position: 'left', ticks: { beginAtZero: true, fontColor: '#909b93', fontSize: 9, callback: (value) => `R$ ${number.format(value)}` }, gridLines: { color: '#edf1ed', zeroLineColor: '#edf1ed' } },
                        { id: 'pedidos', position: 'right', ticks: { beginAtZero: true, precision: 0, fontColor: '#b58b64', fontSize: 9 }, gridLines: { drawOnChartArea: false } }
                    ]
                }
            }
        });
    }

    function renderStatuses(statuses) {
        const legend = document.getElementById('legenda-status');
        legend.replaceChildren();
        const values = statuses.map((status) => Number(status.quantidade || 0));
        statuses.forEach((status, index) => {
            const row = node('li');
            const dot = node('span');
            dot.style.backgroundColor = statusColors[index % statusColors.length];
            const label = node('span', '', status.nome || statusLabels[status.codigo] || status.codigo);
            const count = node('strong', '', number.format(status.quantidade || 0));
            row.append(dot, label, count);
            legend.append(row);
        });
        const canvas = document.getElementById('grafico-status');
        if (statusChart) statusChart.destroy();
        statusChart = new Chart(canvas.getContext('2d'), {
            type: 'doughnut',
            data: { labels: statuses.map((status) => status.nome), datasets: [{
                data: values, backgroundColor: statusColors, borderWidth: 2, borderColor: '#fff', hoverBorderWidth: 2
            }] },
            options: {
                responsive: true, maintainAspectRatio: false, cutoutPercentage: 72,
                legend: { display: false }, tooltips: { callbacks: {
                    label: (item, chartData) => ` ${chartData.labels[item.index]}: ${number.format(values[item.index])}`
                } }
            }
        });
    }

    function renderProducts(products) {
        const container = document.getElementById('lista-produtos');
        const empty = document.getElementById('sem-produtos');
        container.replaceChildren();
        empty.hidden = products.length > 0;
        const maximum = Math.max(1, ...products.map((item) => Number(item.quantidade || 0)));
        products.forEach((product, index) => {
            const row = node('div', 'product-row');
            row.append(node('span', 'product-name', `${index + 1}. ${product.nome || 'Produto'}`));
            row.append(node('span', 'product-quantity', `${number.format(product.quantidade || 0)} un.`));
            const track = node('div', 'product-track');
            const bar = node('div', 'product-bar');
            bar.style.width = `${Math.max(4, Number(product.quantidade || 0) / maximum * 100)}%`;
            track.append(bar);
            row.append(track);
            row.append(node('span', 'product-revenue', `${moneyValue(product.faturamento)} em vendas`));
            container.append(row);
        });
    }

    function renderRecentOrders(orders) {
        const body = document.getElementById('lista-pedidos');
        const empty = document.getElementById('sem-pedidos');
        body.replaceChildren();
        empty.hidden = orders.length > 0;
        orders.forEach((order) => {
            const row = node('tr');
            const code = node('td', '', `#${String(order.id || '').slice(-6).toUpperCase() || '—'}`);
            const customer = node('td', '', order.cliente || 'Cliente');
            customer.title = order.cliente || 'Cliente';
            const statusCell = node('td');
            const badge = node('span', `status-badge status-${String(order.status || '').toLowerCase()}`, order.statusNome || statusLabels[order.status] || 'Não informado');
            statusCell.append(badge);
            statusCell.title = formatDate(order.dataHoraEntrada);
            row.append(code, customer, statusCell, node('td', '', moneyValue(order.valorTotal)));
            body.append(row);
        });
    }

    form.addEventListener('submit', loadDashboard);
    atualizarButton.addEventListener('click', loadDashboard);
    document.getElementById('limpar-filtros').addEventListener('click', () => {
        const hoje = saoPauloToday();
        inicioInput.value = dateOffset(hoje, -6);
        fimInput.value = hoje;
        statusInput.value = '';
        loadDashboard();
    });

    const hoje = saoPauloToday();
    inicioInput.value = dateOffset(hoje, -6);
    fimInput.value = hoje;
    loadDashboard();
    window.setInterval(() => loadDashboard(), 60000);
}());
