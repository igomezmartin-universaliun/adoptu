const rsaCrypto = {
    _publicKey: null,
    
    async importPublicKey(pemKey) {
        const keyData = this._base64ToArrayBuffer(this._pemToBase64(pemKey));
        return crypto.subtle.importKey(
            'spki',
            keyData,
            {
                name: 'RSA-OAEP',
                hash: 'SHA-256',
                mgf1: {
                    name: 'MGF1',
                    hash: 'SHA-1'
                }
            },
            false,
            ['encrypt']
        );
    },
    
    _pemToBase64(pem) {
        const pemContents = pem
            .replace(/-----BEGIN PUBLIC KEY-----/, '')
            .replace(/-----END PUBLIC KEY-----/, '')
            .replace(/\s/g, '');
        return pemContents;
    },
    
    _base64ToArrayBuffer(base64) {
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes.buffer;
    },
    
    async encrypt(plaintext, publicKeyPem) {
        try {
            const publicKey = await this.importPublicKey(publicKeyPem);
            const encoder = new TextEncoder();
            const data = encoder.encode(plaintext);
            
            const encrypted = await crypto.subtle.encrypt(
                {
                    name: 'RSA-OAEP',
                    hash: 'SHA-256',
                    mgf1: {
                        name: 'MGF1',
                        hash: 'SHA-256'
                    }
                },
                publicKey,
                data
            );
            
            return this._arrayBufferToBase64(encrypted);
        } catch (e) {
            console.error('Encryption failed:', e);
            return null;
        }
    },
    
    _arrayBufferToBase64(buffer) {
        const bytes = new Uint8Array(buffer);
        let binary = '';
        for (let i = 0; i < bytes.length; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary);
    }
};

let cachedPublicKey = null;

async function getPublicKey() {
    if (cachedPublicKey) {
        return cachedPublicKey;
    }
    const response = await fetch('/api/auth/encryption-key');
    const data = await response.json();
    cachedPublicKey = data.publicKey;
    return cachedPublicKey;
}
