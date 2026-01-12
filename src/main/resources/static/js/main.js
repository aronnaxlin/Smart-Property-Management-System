document.addEventListener('DOMContentLoaded', () => {
    initClock();
    setupNavigation();
    loadView('dashboard'); // Default view
});

function initClock() {
    const clockEl = document.getElementById('clock');
    setInterval(() => {
        const now = new Date();
        clockEl.innerText = now.toLocaleTimeString('zh-CN', { hour12: false });
    }, 1000);
}

function setupNavigation() {
    const userStr = localStorage.getItem('user_info');
    if (!userStr) {
        window.location.href = 'login.html';
        return;
    }
    const user = JSON.parse(userStr);
    document.getElementById('current-user').innerText = user.name || user.userName;

    // Role based filtering
    const role = user.userType || 'OWNER';
    const navItems = document.querySelectorAll('.nav-links li');

    navItems.forEach(item => {
        const view = item.dataset.view;
        let show = true;

        if (role === 'OWNER') {
             // Owner can see utility, wallet, fees, and AI chat
             if (view !== 'utility' && view !== 'wallet' && view !== 'fees' && view !== 'ai-chat') {
                 show = false;
             }
        }

        if (show) {
            item.style.display = 'flex';
            item.addEventListener('click', () => {
                navItems.forEach(nav => nav.classList.remove('active'));
                item.classList.add('active');
                loadView(view);
            });
        } else {
            item.style.display = 'none';
        }
    });

    // Default Load Logic
    if (role === 'OWNER') {
        // Load utility by default for owners as they can't see dashboard
        const active = document.querySelector('.nav-links li[data-view="utility"]');
        if(active) active.classList.add('active');
        loadView('utility');
    } else {
        // Admin default
        loadView('dashboard');
    }

    // Logout Setup
    const logoutBtn = document.createElement('div');
    logoutBtn.className = 'logout-btn';
    logoutBtn.innerHTML = '<span class="icon" style="margin-right:10px;"><svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M10.09 15.59L11.5 17l5-5-5-5-1.41 1.41L12.67 11H3v2h9.67l-2.58 2.59zM19 3H5a2 2 0 00-2 2v4h2V5h14v14H5v-4H3v4a2 2 0 002 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/></svg></span> 退出登录';
    logoutBtn.style.cursor = 'pointer';
    logoutBtn.style.padding = '15px 25px';
    logoutBtn.style.color = 'var(--text-secondary)';
    logoutBtn.onclick = handleLogout;

    document.querySelector('.sidebar').appendChild(logoutBtn);
}

function handleLogout() {
    // Clear local storage
    localStorage.removeItem('user_info');
    // Call backend logout
    fetch('/api/auth/logout', { method: 'POST' })
    .then(() => {
        window.location.href = 'login.html';
    });
}

function loadView(viewName) {
    const contentArea = document.getElementById('content-area');
    const pageTitle = document.getElementById('page-title');

    // Update simple loading state
    contentArea.innerHTML = '<div class="loading-glitch">Loading Module: ' + viewName.toUpperCase() + '...</div>';

    // Set Title
    const titleMap = {
        'dashboard': '数据看板',
        'owners': '用户/业主管理',
        'properties': '房产管理',
        'fees': '费用管理',
        'utility': '水电服务',
        'wallet': '我的钱包',
        'ai-chat': 'AI 助手'
    };
    pageTitle.innerText = titleMap[viewName] || 'Unknown Module';

    // Fetch view content (simulated for now, normally we'd fetch HTML partials)
    // In a real SPA, we might use fetch('views/' + viewName + '.html')
    fetch(`views/${viewName}.html`)
        .then(response => {
            if (!response.ok) throw new Error('Module load failed');
            return response.text();
        })
        .then(html => {
            contentArea.innerHTML = html;
            initViewScript(viewName); // Initialize specific JS for the view
        })
        .catch(err => {
            console.error(err);
            contentArea.innerHTML = `<div class="error-panel">Error loading module: ${viewName}<br>Please check system logs.</div>`;
        });
}

function initViewScript(viewName) {
    // Dynamic script initialization based on view
    if (viewName === 'dashboard') {
        if (window.initDashboard) window.initDashboard();
    } else if (viewName === 'owners') {
        if (window.initOwners) window.initOwners();
    } else if (viewName === 'properties') {
        if (window.initProperties) window.initProperties();
    } else if (viewName === 'fees') {
        if (window.initFees) window.initFees();
    } else if (viewName === 'utility') {
        if (window.initUtility) window.initUtility();
    } else if (viewName === 'wallet') {
        if (window.initWallet) window.initWallet();
    } else if (viewName === 'ai-chat') {
        if (window.initAIChat) window.initAIChat();
    }
}
