window.initAIChat = function() {
    // Check if marked.js is loaded
    if (typeof marked === 'undefined') {
        console.warn('Marked.js library not loaded. Markdown rendering will be disabled.');
    } else {
        console.log('Marked.js loaded successfully. Version:', marked.version || 'unknown');
    }

    // Focus input
    document.getElementById('chatInput').focus();
};

function handleChatKey(e) {
    if (e.key === 'Enter') sendChat();
}

async function sendChat() {
    const input = document.getElementById('chatInput');
    const msg = input.value.trim();
    if (!msg) return;

    appendMsg(msg, 'user');
    input.value = '';

    // Show typing
    const typingId = 'typing-' + Date.now();
    const history = document.getElementById('chatHistory');
    history.insertAdjacentHTML('beforeend', `
        <div class="msg bot" id="${typingId}">
            <div class="bubble">...</div>
        </div>
    `);
    history.scrollTop = history.scrollHeight;

    try {
        const res = await window.api.post('/ai/chat', { message: msg });

        // Remove typing
        const typingEl = document.getElementById(typingId);
        if (typingEl) typingEl.remove();

        if (res.code === 200) {
            appendMsg(res.data, 'bot');
        } else {
            appendMsg('Error: ' + res.message, 'bot');
        }
    } catch (e) {
        document.getElementById(typingId)?.remove();
        appendMsg('Connection Error', 'bot');
    }
}

function appendMsg(text, type) {
    const history = document.getElementById('chatHistory');
    const div = document.createElement('div');
    div.className = `msg ${type}`;

    const bubble = document.createElement('div');
    bubble.className = 'bubble';

    if (type === 'bot' && typeof marked !== 'undefined') {
        try {
            console.log('Attempting to render markdown...');
            console.log('Text to render:', text.substring(0, 100) + '...');

            let rendered;

            // Try different marked API versions
            if (typeof marked === 'function') {
                // marked v4.x and earlier - marked is a function
                console.log('Using marked as function');
                rendered = marked(text, {
                    breaks: true,
                    gfm: true,
                    headerIds: false,
                    mangle: false
                });
            } else if (typeof marked.parse === 'function') {
                // marked v5.x and later - marked.parse is the function
                console.log('Using marked.parse');
                rendered = marked.parse(text, {
                    breaks: true,
                    gfm: true,
                    headerIds: false,
                    mangle: false
                });
            } else {
                throw new Error('Unable to find marked rendering function');
            }

            console.log('Rendered HTML:', rendered.substring(0, 100) + '...');
            bubble.innerHTML = rendered;
            console.log('✅ Markdown rendered successfully');
        } catch (error) {
            console.error('❌ Markdown rendering failed:', error);
            console.error('Original text:', text);
            // Fallback to plain text if markdown fails
            bubble.textContent = text;
        }
    } else {
        // Plain text for user messages (escape HTML)
        // or when marked.js is not available
        bubble.textContent = text;

        if (type === 'bot' && typeof marked === 'undefined') {
            console.warn('⚠️ Marked.js not available, displaying plain text');
        }
    }

    div.appendChild(bubble);
    history.appendChild(div);
    history.scrollTop = history.scrollHeight;
}
