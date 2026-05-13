const API_URL = 'https://3m2ywawvz1.execute-api.ap-northeast-2.amazonaws.com/reading';

const form = document.getElementById('tarot-form');
const concernInput = document.getElementById('concern');
const charCount = document.getElementById('char-count');
const submitBtn = document.getElementById('submit-btn');
const retryBtn = document.getElementById('retry-btn');

const inputSection = document.getElementById('input-section');
const loadingSection = document.getElementById('loading-section');
const resultSection = document.getElementById('result-section');

// 카드 이모지 매핑
const CARD_EMOJI = {
    'The Fool': '🃏', 'The Magician': '🎩', 'The High Priestess': '🌙',
    'The Empress': '👑', 'The Emperor': '🏛️', 'The Hierophant': '📿',
    'The Lovers': '💕', 'The Chariot': '⚔️', 'Strength': '🦁',
    'The Hermit': '🏮', 'Wheel of Fortune': '🎡', 'Justice': '⚖️',
    'The Hanged Man': '🔮', 'Death': '🦋', 'Temperance': '🏺',
    'The Devil': '🔥', 'The Tower': '⚡', 'The Star': '⭐',
    'The Moon': '🌙', 'The Sun': '☀️', 'Judgement': '📯',
    'The World': '🌍'
};

concernInput.addEventListener('input', () => {
    charCount.textContent = concernInput.value.length;
});

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const concern = concernInput.value.trim();
    if (!concern) return;

    showSection('loading');

    try {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ concern })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || '알 수 없는 오류');
        }

        renderResult(data);
        showSection('result');
    } catch (err) {
        alert('오류가 발생했습니다: ' + err.message);
        showSection('input');
    }
});

retryBtn.addEventListener('click', () => {
    concernInput.value = '';
    charCount.textContent = '0';
    showSection('input');
});

function showSection(name) {
    inputSection.classList.toggle('hidden', name !== 'input');
    loadingSection.classList.toggle('hidden', name !== 'loading');
    resultSection.classList.toggle('hidden', name !== 'result');
}

function renderResult(data) {
    const positionMap = { past: 'card-past', present: 'card-present', future: 'card-future' };

    data.cards.forEach(card => {
        const slot = document.getElementById(positionMap[card.position]);
        const emoji = CARD_EMOJI[card.nameEn] || '🃏';

        slot.className = 'card-slot' + (card.reversed ? ' card-reversed' : '');
        slot.querySelector('.card-face').textContent = emoji;
        slot.querySelector('.card-name').textContent =
            card.name + (card.reversed ? ' (역방향)' : ' (정방향)');
        slot.querySelector('.card-meaning').textContent = card.meaning;
    });

    document.getElementById('reading-text').textContent = data.reading;
}
