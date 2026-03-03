const api = {
    base: '',
    async fetch(path, options = {}) {
        const res = await fetch(this.base + path, { ...options, credentials: 'include' });
        const text = await res.text();
        let data;
        try { data = text ? JSON.parse(text) : {}; } catch { data = {}; }
        if (!res.ok) throw new Error(data.error || res.statusText);
        return data;
    },
    async me() { return this.fetch('/api/auth/me'); },
    async logout() { return this.fetch('/api/auth/logout', { method: 'POST' }); },
    async getPets(type) { return this.fetch('/api/pets' + (type ? '?type=' + encodeURIComponent(type) : '')); },
    async getPet(id) { return this.fetch('/api/pets/' + id); },
    async createPet(pet) {
        return this.fetch('/api/pets', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(pet)
        });
    },
    async updatePet(id, pet) {
        return this.fetch('/api/pets/' + id, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(pet)
        });
    },
    async deletePet(id) {
        return this.fetch('/api/pets/' + id, { method: 'DELETE' });
    },
    async adoptPet(id, message) {
        return this.fetch('/api/pets/' + id + '/adopt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: message || '' })
        });
    },
    async addImage(petId, file, isPrimary = false) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('isPrimary', isPrimary.toString());
        return this.fetch('/api/pets/' + petId + '/images', {
            method: 'POST',
            body: formData
        });
    },
    async removeImage(petId, imageId) {
        return this.fetch('/api/pets/' + petId + '/images/' + imageId, { method: 'DELETE' });
    },
    async setPrimaryImage(petId, imageId) {
        return this.fetch('/api/pets/' + petId + '/images/' + imageId + '/primary', { method: 'PUT' });
    }
};
