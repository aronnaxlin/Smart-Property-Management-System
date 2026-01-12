/**
 * 钱包页面初始化
 * 加载钱包信息和交易记录
 */
window.initWallet = function() {
    loadWalletInfo();
    loadTransactions();
};

/**
 * 获取当前用户ID
 * 从本地存储中读取用户信息
 *
 * @returns {number} 用户ID
 */
function getUserId() {
    const userStr = localStorage.getItem('user_info');
    if (userStr) {
        try {
            const user = JSON.parse(userStr);
            return user.userId || 1;
        } catch (e) {
            console.error('用户信息解析失败:', e);
            return 1;
        }
    }
    return 1; // Fallback
}

/**
 * 加载钱包余额信息
 */
async function loadWalletInfo() {
    const userId = getUserId();
    const balanceElement = document.getElementById('walletBalance');

    if (!balanceElement) {
        console.error('walletBalance element not found');
        return;
    }

    try {
        const res = await window.api.get('/wallet/info', { userId: userId });
        if (res.code === 200) {
            // 更新余额显示
            balanceElement.innerText = res.data.toFixed(2);
        } else {
            balanceElement.innerText = 'Error';
            console.error('加载余额失败:', res.message);
        }
    } catch (e) {
        console.error('系统错误:', e);
        balanceElement.innerText = 'Error';
        alert('系统错误，请稍后重试');
    }
}

/**
 * 钱包充值
 * 包含输入验证和加载状态管理
 */
async function rechargeWallet() {
    const userId = getUserId();
    const amountInput = document.getElementById('walletRechargeAmount');
    const amount = amountInput.value.trim();

    // 输入验证：金额不能为空
    if (!amount) {
        alert('请输入充值金额');
        return;
    }

    // 输入验证：金额必须为正数
    const amountNum = parseFloat(amount);
    if (isNaN(amountNum) || amountNum <= 0) {
        alert('请输入有效的充值金额（大于0）');
        return;
    }

    // 输入验证：金额上限检查
    if (amountNum > 1000000) {
        alert('单次充值金额不能超过100万元');
        return;
    }

    try {
        const res = await window.api.postForm('/wallet/recharge', { userId: userId, amount: amountNum });
        if (res.code === 200) {
            alert('充值成功');
            // 重新加载数据
            loadWalletInfo();
            loadTransactions();
            // 清空输入框
            amountInput.value = '';
        } else {
            alert('充值失败: ' + res.message);
        }
    } catch (e) {
        console.error('充值错误:', e);
        alert('系统错误，请稍后重试');
    }
}

/**
 * 加载交易记录列表
 */
async function loadTransactions() {
    const userId = getUserId();
    const tbody = document.getElementById('walletTransBody');
    if (!tbody) {
        console.error('walletTransBody element not found');
        return;
    }
    tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">加载中...</td></tr>';

    try {
        const res = await window.api.get('/wallet/transactions', { userId: userId });
        if (res.code === 200) {
            const list = res.data;
            if (!list || list.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;">暂无记录</td></tr>';
                return;
            }

            // 渲染交易记录
            let html = '';
            list.forEach(t => {
                // 根据交易类型设置颜色和符号
                const colorClass = t.transType === 'RECHARGE' ? 'text-cyan' : 'text-pink';
                const sign = t.transType === 'RECHARGE' ? '+' : '-';

                html += `
                    <tr>
                        <td>#${t.transId}</td>
                        <td>${t.transType}</td>
                        <td class="${colorClass}">${sign}¥${t.amount.toFixed(2)}</td>
                        <td>${t.description}</td>
                        <td>${new Date(t.transTime).toLocaleString('zh-CN')}</td>
                    </tr>
                `;
            });
            tbody.innerHTML = html;
        } else {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--accent-pink);">加载失败</td></tr>';
        }
    } catch (e) {
        console.error('加载交易记录失败:', e);
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--accent-pink);">系统错误</td></tr>';
    }
}
