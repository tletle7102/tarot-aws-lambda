const API_URL = 'https://3m2ywawvz1.execute-api.ap-northeast-2.amazonaws.com/reading';
const SITE_URL = location.origin;
const TOTAL_CARDS = 22;
const MAX_PICK = 3;

const form = document.getElementById('tarot-form');
const concernInput = document.getElementById('concern');
const charCount = document.getElementById('char-count');
const submitBtn = document.getElementById('submit-btn');
const readBtn = document.getElementById('read-btn');
const retryBtn = document.getElementById('retry-btn');
const shareBtn = document.getElementById('share-btn');
const shareToast = document.getElementById('share-toast');
const pickCount = document.getElementById('pick-count');
const fanContainer = document.getElementById('fan-container');

const inputSection = document.getElementById('input-section');
const pickSection = document.getElementById('pick-section');
const loadingSection = document.getElementById('loading-section');
const resultSection = document.getElementById('result-section');

let selectedCards = [];
let shuffledOrder = [];
let currentShareId = null;
let mysticInterval = null;

const MYSTIC_MESSAGES = [
    '카드의 기운을 느끼고 있습니다...',
    '별의 흐름을 읽고 있습니다...',
    '과거의 그림자를 들여다보고 있습니다...',
    '현재의 에너지를 감지하고 있습니다...',
    '미래의 실마리를 풀어내고 있습니다...',
    '카드가 당신에게 속삭이고 있습니다...',
    '우주의 메시지를 해독하고 있습니다...',
    '운명의 실타래를 풀고 있습니다...',
];

// 페이지 로드 시 공유 링크 확인
window.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(location.search);
    const shareId = params.get('r');
    if (shareId) {
        loadSharedReading(shareId);
    }
});

async function loadSharedReading(id) {
    showSection('loading');
    try {
        const response = await fetch(`${API_URL}/${id}`);
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || '결과를 찾을 수 없습니다');
        }

        currentShareId = id;
        renderResult(data);
        showSection('result');
    } catch (err) {
        alert(err.message);
        showSection('input');
    }
}

concernInput.addEventListener('input', () => {
    charCount.textContent = concernInput.value.length;
});

// Step 1 → Step 2
form.addEventListener('submit', (e) => {
    e.preventDefault();
    if (!concernInput.value.trim()) return;

    selectedCards = [];
    shuffledOrder = shuffle([...Array(TOTAL_CARDS).keys()]);
    renderFan();
    updatePickCount();
    readBtn.classList.add('hidden');
    showSection('pick');
});

// Step 2: 부채꼴 카드
function renderFan() {
    fanContainer.innerHTML = '';

    const totalAngle = 180;
    const startAngle = -totalAngle / 2;
    const step = totalAngle / (TOTAL_CARDS - 1);

    for (let i = 0; i < TOTAL_CARDS; i++) {
        const card = document.createElement('div');
        card.className = 'fan-card';
        card.dataset.index = i;
        card.dataset.cardId = shuffledOrder[i];

        const angle = startAngle + step * i;
        card.style.setProperty('--fan-rotate', `rotate(${angle}deg)`);
        card.style.transform = `rotate(${angle}deg)`;
        card.style.zIndex = i;

        const img = document.createElement('img');
        img.src = 'images/back.png';
        img.alt = '타로 카드';
        card.appendChild(img);

        card.addEventListener('click', () => onCardClick(card));
        fanContainer.appendChild(card);
    }
}

function onCardClick(card) {
    const index = parseInt(card.dataset.index);

    if (card.classList.contains('selected')) {
        card.classList.remove('selected');
        selectedCards = selectedCards.filter(c => c !== index);
        updatePickState();
        return;
    }

    if (selectedCards.length >= MAX_PICK) return;

    card.classList.add('selected');
    selectedCards.push(index);
    updatePickState();
}

function updatePickState() {
    updatePickCount();
    const allCards = fanContainer.querySelectorAll('.fan-card');

    if (selectedCards.length >= MAX_PICK) {
        allCards.forEach(c => {
            if (!c.classList.contains('selected')) c.classList.add('disabled');
        });
        readBtn.classList.remove('hidden');
    } else {
        allCards.forEach(c => c.classList.remove('disabled'));
        readBtn.classList.add('hidden');
    }
}

function updatePickCount() {
    pickCount.textContent = `(${selectedCards.length}/${MAX_PICK})`;
}

// Step 2 → Step 3: 리딩 요청
readBtn.addEventListener('click', async () => {
    const concern = concernInput.value.trim();
    const pickedCardIds = selectedCards.map(i => shuffledOrder[i]);

    showSection('loading');

    try {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ concern, cardIds: pickedCardIds })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || '알 수 없는 오류');
        }

        currentShareId = data.shareId;
        renderResult(data);
        showSection('result');
    } catch (err) {
        alert('오류가 발생했습니다: ' + err.message);
        showSection('pick');
    }
});

// Step 4: 결과 렌더링
function renderResult(data) {
    const positionMap = { past: 'card-past', present: 'card-present', future: 'card-future' };

    data.cards.forEach(card => {
        const slot = document.getElementById(positionMap[card.position]);

        const img = document.createElement('img');
        img.src = `images/${card.id}.png`;
        img.alt = card.name;
        // 이미지는 항상 정방향으로 표시

        const wrap = slot.querySelector('.card-img-wrap');
        wrap.innerHTML = '';
        wrap.appendChild(img);

        slot.querySelector('.card-name').textContent =
            card.name + (card.reversed ? ' (역방향)' : ' (정방향)');
        slot.querySelector('.card-meaning').textContent = card.meaning;
    });

    document.getElementById('reading-text').textContent = data.reading;
}

// 공유
shareBtn.addEventListener('click', async () => {
    if (!currentShareId) return;

    const shareUrl = `${SITE_URL}/?r=${currentShareId}`;
    const shareText = '타로셸에서 타로 리딩을 받았어요! 내 결과를 확인해보세요.';

    if (navigator.share) {
        try {
            await navigator.share({ title: '타로셸 리딩 결과', text: shareText, url: shareUrl });
        } catch (e) {
            // 사용자가 취소한 경우 무시
        }
    } else {
        await navigator.clipboard.writeText(shareUrl);
        shareToast.classList.remove('hidden');
        setTimeout(() => shareToast.classList.add('hidden'), 2000);
    }
});

// 다시 뽑기
retryBtn.addEventListener('click', () => {
    concernInput.value = '';
    charCount.textContent = '0';
    currentShareId = null;
    history.replaceState(null, '', location.pathname);
    showSection('input');
});

function startMysticMessages() {
    const el = document.getElementById('mystic-message');
    let idx = 0;
    el.textContent = MYSTIC_MESSAGES[0];
    mysticInterval = setInterval(() => {
        idx = (idx + 1) % MYSTIC_MESSAGES.length;
        el.style.opacity = 0;
        setTimeout(() => {
            el.textContent = MYSTIC_MESSAGES[idx];
            el.style.opacity = 1;
        }, 400);
    }, 2500);
}

function stopMysticMessages() {
    if (mysticInterval) {
        clearInterval(mysticInterval);
        mysticInterval = null;
    }
}

// 유틸
function showSection(name) {
    if (name === 'loading') startMysticMessages();
    else stopMysticMessages();
    inputSection.classList.toggle('hidden', name !== 'input');
    pickSection.classList.toggle('hidden', name !== 'pick');
    loadingSection.classList.toggle('hidden', name !== 'loading');
    resultSection.classList.toggle('hidden', name !== 'result');
}

function shuffle(arr) {
    for (let i = arr.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
}
