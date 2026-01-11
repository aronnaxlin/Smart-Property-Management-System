/**
 * Dashboard Logic - Material Design & Optimized Chinese
 */
window.initDashboard = async function() {
    console.log('Initializing Dashboard...');

    try {
        const res = await window.api.get('/dashboard/stats');
        if (res.code === 200) {
            renderStats(res.data);
        } else {
            console.error('Failed to load stats:', res.message);
        }
    } catch (e) {
        console.error('Error fetching dashboard stats:', e);
    }
};

function renderStats(data) {
    // 1. Render Top Cards
    const rateData = data.collectionRate;
    const ratePercent = (rateData.rate * 100).toFixed(1) + '%';
    document.getElementById('stat-rate').innerText = ratePercent;
    document.getElementById('stat-rate-detail').innerText = `已缴: ${rateData.paid} / 总计: ${rateData.total}`;

    const incomeData = data.incomeDistribution;
    let totalIncome = 0;
    incomeData.forEach(item => totalIncome += item.total_amount);
    document.getElementById('stat-income').innerText = '¥ ' + totalIncome.toLocaleString();

    const arrearsData = data.arrearsByBuilding;
    if (arrearsData.length > 0) {
        document.getElementById('stat-arrears').innerText = arrearsData[0].building_no + '#'; // Most arrears building
    } else {
        document.getElementById('stat-arrears').innerText = '无';
    }

    // 2. Render ECharts
    renderIncomeChart(incomeData);
    renderArrearsChart(arrearsData);
}

function translateFeeType(type) {
    const map = {
        'PROPERTY_FEE': '物业费',
        'HEATING_FEE': '取暖费',
        'WATER': '水费',
        'WATER_FEE': '水费',
        'ELECTRICITY': '电费',
        'ELECTRICITY_FEE': '电费',
        'PARKING_FEE': '停车费'
    };
    return map[type] || type;
}

function renderIncomeChart(data) {
    const chartDom = document.getElementById('incomeChart');
    if (!chartDom) return;

    // Clear previous instance to prevent styling conflicts
    echarts.dispose(chartDom);
    const myChart = echarts.init(chartDom);

    const option = {
        tooltip: { trigger: 'item' },
        legend: {
            bottom: '5%',
            left: 'center'
        },
        color: ['#006064', '#0097A7', '#4DD0E1', '#B2EBF2', '#FF5722'], // Material Palette
        series: [
            {
                name: '收入来源',
                type: 'pie',
                radius: ['40%', '70%'],
                avoidLabelOverlap: false,
                itemStyle: {
                    borderRadius: 5,
                    borderColor: '#fff',
                    borderWidth: 2
                },
                label: {
                    show: false,
                    position: 'center'
                },
                // Removed emphasis center text as requested
                emphasis: {
                    label: {
                        show: false
                    }
                },
                labelLine: {
                    show: false
                },
                data: data.map(item => ({
                    value: item.total_amount,
                    name: translateFeeType(item.fee_type)
                }))
            }
        ]
    };
    myChart.setOption(option);
}

function renderArrearsChart(data) {
    const chartDom = document.getElementById('arrearsChart');
    if (!chartDom) return;

    echarts.dispose(chartDom);
    const myChart = echarts.init(chartDom);

    const option = {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: [
            {
                type: 'category',
                data: data.map(item => item.building_no + '#'),
                axisTick: { alignWithLabel: true },
                axisLabel: { color: '#757575' }
            }
        ],
        yAxis: [
            {
                type: 'value',
                axisLabel: { color: '#757575' },
                splitLine: { lineStyle: { color: '#EEEEEE' } } // Subtle grid
            }
        ],
        series: [
            {
                name: '欠费数量',
                type: 'bar',
                barWidth: '50%',
                data: data.map(item => ({
                    value: item.unpaid_count,
                    itemStyle: {
                        color: '#D32F2F' // Solid Material Red
                    }
                }))
            }
        ]
    };
    myChart.setOption(option);
}
