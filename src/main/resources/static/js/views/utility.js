let userCards = []; // 存储用户的卡片列表

window.initUtility = function() {
    const userStr = localStorage.getItem('user_info');
    if (!userStr) {
        alert('用户未登录');
        return;
    }

    const user = JSON.parse(userStr);

    if (user.userType === 'OWNER') {
        initOwnerView();
    } else {
        initAdminView();
    }
};

/**
 * 初始化业主视角
 */
async function initOwnerView() {
    // 显示业主专属区域
    document.getElementById('ownerCardsSection').style.display = 'block';
    document.getElementById('ownerTopupForm').style.display = 'block';

    // 加载业主的所有水电卡
    await loadMyCards();
}

/**
 * 初始化管理员视角
 */
function initAdminView() {
    // 显示管理员表单
    document.getElementById('adminTopupForm').style.display = 'block';
}

/**
 * 加载业主的所有水电卡
 */
async function loadMyCards() {
    const tbody = document.getElementById('myCardsBody');
    tbody.innerHTML = '<tr><td colspan="5" class="text-center">加载中...</td></tr>';

    try {
        const res = await window.api.get('/utility/my-cards');
        if (res.code === 200) {
            userCards = res.data;
            renderMyCards(res.data);
            populateCardSelector(res.data);
        } else {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center text-pink">${res.message}</td></tr>`;
        }
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-pink">加载失败</td></tr>';
    }
}

/**
 * 渲染水电卡列表表格
 */
function renderMyCards(cards) {
    const tbody = document.getElementById('myCardsBody');

    if (!cards || cards.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-dim">暂无水电卡</td></tr>';
        return;
    }

    let html = '';
    cards.forEach(card => {
        const location = `${card.buildingNo}-${card.unitNo}-${card.roomNo}`;
        const cardType = card.cardType === 'WATER' ? '水卡' : '电卡';
        html += `
            <tr>
                <td>${location}</td>
                <td class="text-cyan">${cardType}</td>
                <td>#${card.cardId}</td>
                <td class="text-pink">¥${card.balance || 0}</td>
                <td>
                    <button class="sci-btn" onclick="selectCardForTopup(${card.cardId})">充值</button>
                </td>
            </tr>
        `;
    });
    tbody.innerHTML = html;
}

/**
 * 填充卡片选择下拉框
 */
function populateCardSelector(cards) {
    const select = document.getElementById('ucSelectCard');
    select.innerHTML = '<option value="">请选择要充值的卡片...</option>';

    cards.forEach(card => {
        const location = `${card.buildingNo}-${card.unitNo}-${card.roomNo}`;
        const cardType = card.cardType === 'WATER' ? '水卡' : '电卡';
        const option = document.createElement('option');
        option.value = card.cardId;
        option.text = `${location} - ${cardType} (¥${card.balance || 0})`;
        select.appendChild(option);
    });
}

/**
 * 从表格中选择卡片充值
 */
function selectCardForTopup(cardId) {
    document.getElementById('ucSelectCard').value = cardId;
    // 滚动到充值表单
    document.getElementById('ownerTopupForm').scrollIntoView({ behavior: 'smooth' });
}

/**
 * 业主充值自己的水电卡
 */
async function topUpMyCard() {
    const cardId = document.getElementById('ucSelectCard').value;
    const amount = document.getElementById('ucOwnerAmount').value;
    const resultDiv = document.getElementById('ucOwnerResult');
    const alertBox = document.getElementById('ucArrearsAlert');

    resultDiv.innerHTML = '';
    alertBox.style.display = 'none';

    if (!cardId) {
        resultDiv.innerHTML = '<span class="text-pink">请选择要充值的卡片</span>';
        return;
    }

    if (!amount || amount <= 0) {
        resultDiv.innerHTML = '<span class="text-pink">请输入有效的充值金额</span>';
        return;
    }

    try {
        // 调用API（不需要传userId，后端从session获取）
        const res = await window.api.postForm('/utility/card/topup', {
            cardId: cardId,
            amount: amount
        });

        if (res.code === 200) {
            resultDiv.innerHTML = '<span class="text-cyan">✅ 充值成功! 已从钱包扣款</span>';
            document.getElementById('ucOwnerAmount').value = '';
            // 刷新卡片列表
            await loadMyCards();
        } else {
            if (res.message && res.message.includes("欠费")) {
                document.getElementById('ucArrearsMsg').innerText = res.message;
                alertBox.style.display = 'block';
            } else {
                resultDiv.innerHTML = `<span class="text-pink">❌ ${res.message}</span>`;
            }
        }
    } catch (e) {
        resultDiv.innerHTML = '<span class="text-pink">System Error</span>';
    }
}

/**
 * 管理员充值水电卡
 */
async function adminTopUpCard() {
    const cardId = document.getElementById('ucAdminCardId').value;
    const amount = document.getElementById('ucAdminAmount').value;
    const resultDiv = document.getElementById('ucAdminResult');
    const alertBox = document.getElementById('ucArrearsAlert');

    resultDiv.innerHTML = '';
    alertBox.style.display = 'none';

    if (!cardId || cardId <= 0) {
        resultDiv.innerHTML = '<span class="text-pink">请输入有效的卡号</span>';
        return;
    }

    if (!amount || amount <= 0) {
        resultDiv.innerHTML = '<span class="text-pink">请输入有效的充值金额</span>';
        return;
    }

    try {
        const res = await window.api.postForm('/utility/card/topup', {
            cardId: cardId,
            amount: amount
        });

        if (res.code === 200) {
            resultDiv.innerHTML = '<span class="text-cyan">✅ 充值成功! 已从业主钱包扣款</span>';
            document.getElementById('ucAdminCardId').value = '';
            document.getElementById('ucAdminAmount').value = '';
        } else {
            if (res.message && res.message.includes("欠费")) {
                document.getElementById('ucArrearsMsg').innerText = res.message;
                alertBox.style.display = 'block';
            } else {
                resultDiv.innerHTML = `<span class="text-pink">❌ ${res.message}</span>`;
            }
        }
    } catch (e) {
        resultDiv.innerHTML = '<span class="text-pink">System Error</span>';
    }
}

/**
 * 查询卡片余额（通用）
 */
async function checkCardBalance() {
    const cardId = document.getElementById('ucSearchId').value;
    const resEl = document.getElementById('ucBalanceResult');

    if (!cardId) return;

    try {
        const res = await window.api.get(`/utility/card/${cardId}`);
        if (res.code === 200) {
            resEl.innerText = `余额 (Balance): ¥${res.data}`;
        } else {
            resEl.innerText = 'Card not found';
        }
    } catch (e) {
        resEl.innerText = 'Error';
    }
}
