window.searchPhotographers = async function() {
    const country = document.getElementById('search-country').value;
    const state = document.getElementById('search-state').value;
    const city = document.getElementById('search-city').value;
    const zip = document.getElementById('search-zip').value;
    const neighborhood = document.getElementById('search-neighborhood').value;
    
    if (!country) {
        document.getElementById('photographers').innerHTML = '<p>'+t('pleaseSelectCountry')+'</p>';
        return;
    }
    
    const params = new URLSearchParams();
    params.append('country', country);
    if (state) params.append('state', state);
    if (city) params.append('city', city);
    if (zip) params.append('zip', zip);
    if (neighborhood) params.append('neighborhood', neighborhood);
    
    loadPhotographers('/api/photographers?' + params.toString());
}

async function loadPhotographers(url) {
    try {
        const output = await fetch(url);
        if (!output.ok) throw new Error('Failed to load photographers');
        const photographers = await output.json();
        const container = document.getElementById('photographers');
        if (!photographers || photographers.length === 0) {
            container.innerHTML = '<p>'+t('noPhotographersAvailable')+'</p>';
            return;
        }
        container.innerHTML = photographers.map(p => {
            const fee = p.photographerFee ? p.photographerFee + ' ' + (p.photographerCurrency || 'USD') : 'Free';
            const location = [p.photographerCity, p.photographerState, p.photographerCountry].filter(Boolean).join(', ');
            return '<div class="photographer-card">' +
                '<div class="photographer-info">' +
                '<h3>'+p.displayName+'</h3>' +
                (location ? '<p class="photographer-location">'+location+'</p>' : '') +
                '<p class="photographer-fee">'+t('sessionFee')+': <strong>'+fee+'</strong></p>' +
                '</div>' +
                '<button class="btn request-btn" data-id="'+p.userId+'" data-name="'+p.displayName+'" data-fee="'+fee+'">'+t('requestPhotoSession')+'</button>' +
                '</div>';
        }).join('');
        
        document.querySelectorAll('.request-btn').forEach(btn => {
            btn.onclick = () => {
                const photographerId = parseInt(btn.dataset.id);
                const photographerName = btn.dataset.name;
                const fee = btn.dataset.fee;
                showRequestModal(photographerId, photographerName, fee);
            };
        });
    } catch (err) {
        document.getElementById('photographers').innerHTML = '<p>'+t('errorLoadingPhotographers')+'</p>';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    createRequestModal();
    const searchBtn = document.getElementById('search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', window.searchPhotographers);
    }
    const stateInput = document.getElementById('search-state');
    if (stateInput) {
        stateInput.addEventListener('input', debounce(window.searchPhotographers, 500));
    }
    const cityInput = document.getElementById('search-city');
    if (cityInput) {
        cityInput.addEventListener('input', debounce(window.searchPhotographers, 500));
    }
    const zipInput = document.getElementById('search-zip');
    if (zipInput) {
        zipInput.addEventListener('input', debounce(window.searchPhotographers, 500));
    }
    const neighborhoodInput = document.getElementById('search-neighborhood');
    if (neighborhoodInput) {
        neighborhoodInput.addEventListener('input', debounce(window.searchPhotographers, 500));
    }
});

function createRequestModal() {
    const modal = document.createElement('div');
    modal.id = 'request-modal';
    modal.className = 'form-modal';
    modal.style.display = 'none';
    modal.innerHTML = `
        <div class="form-modal-content card-bg">
            <h2 id="modal-title">${t('requestPhotoSession')}</h2>
            <p id="modal-photographer" class="photographer-fee"></p>
            <form id="request-form">
                <div class="form-group">
                    <label for="request-message">${t('message')}</label>
                    <textarea id="request-message" rows="4" class="form-control"></textarea>
                </div>
                <div class="form-actions">
                    <button type="submit" class="btn btn-primary">${t('sendRequest')}</button>
                    <button type="button" class="btn" id="cancel-request">${t('cancel')}</button>
                </div>
            </form>
        </div>
    `;
    document.body.appendChild(modal);

    document.getElementById('cancel-request').addEventListener('click', () => {
        modal.style.display = 'none';
    });

    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.style.display = 'none';
        }
    });

    document.getElementById('request-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const message = document.getElementById('request-message').value;
        const photographerId = parseInt(modal.dataset.photographerId);
        try {
            await api.createPhotographyRequest(photographerId, null, message || '');
            modal.style.display = 'none';
            alert(t('requestSentSuccessfully'));
        } catch (err) {
            alert('Error: ' + err.message);
        }
    });
}

function showRequestModal(photographerId, photographerName, fee) {
    const modal = document.getElementById('request-modal');
    document.getElementById('modal-title').textContent = t('requestPhotoSession');
    document.getElementById('modal-photographer').innerHTML = `${t('requestTo')}: <strong>${photographerName}</strong><br>${t('sessionFee')}: <strong>${fee}</strong>`;
    document.getElementById('request-message').placeholder = t('enterMessage').replace('{name}', photographerName);
    document.getElementById('request-message').value = '';
    modal.dataset.photographerId = photographerId;
    modal.style.display = 'flex';
}

window.debounce = function(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}