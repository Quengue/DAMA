// Gráficos com Chart.js (servido pelo BFF em /vendor). Cores alinhadas ao tema escuro.

const ativos = new Set();

export const CORES = ['#ff3b00', '#5aa9ff', '#2fd67b', '#ffa726', '#c77dff', '#ff4d6a', '#4dd0e1', '#f06292'];

export function destruirGraficos() {
    for (const grafico of ativos) grafico.destroy();
    ativos.clear();
}

export function grafico(container, tipo, dados, opcoes = {}) {
    if (!window.Chart) {
        container.textContent = 'Biblioteca de gráficos indisponível.';
        return null;
    }
    const canvas = document.createElement('canvas');
    container.replaceChildren(canvas);
    const Chart = window.Chart;
    Chart.defaults.color = '#9a9a9a';
    Chart.defaults.borderColor = '#1f1f1f';
    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;

    const instancia = new Chart(canvas, {
        type: tipo,
        data: dados,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: { labels: { boxWidth: 12, boxHeight: 12 } },
                tooltip: { backgroundColor: '#151515', borderColor: '#333', borderWidth: 1 }
            },
            ...opcoes
        }
    });
    ativos.add(instancia);
    return instancia;
}

export function serie(rotulo, valores, indice = 0, extra = {}) {
    const cor = CORES[indice % CORES.length];
    return {
        label: rotulo,
        data: valores,
        borderColor: cor,
        backgroundColor: cor + '33',
        pointRadius: 3,
        tension: .3,
        ...extra
    };
}
