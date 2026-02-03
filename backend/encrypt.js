/**
 * AES/CBC/PKCS5Padding encryption - must match app ConfigDecryptor.
 * Key: ByteArray(32) { it.toByte() } = bytes 0,1,...,31
 * Output: base64(IV_16bytes + ciphertext)
 */
const crypto = require('crypto');

const ALGORITHM = 'aes-256-cbc';
const KEY_LENGTH = 32;
const IV_LENGTH = 16;

// Same key as in app: AppContainer.kt → ByteArray(32) { it.toByte() }
const KEY = Buffer.from(Array.from({ length: 32 }, (_, i) => i));

function encrypt(plainText) {
  const iv = crypto.randomBytes(IV_LENGTH);
  const cipher = crypto.createCipheriv(ALGORITHM, KEY, iv);
  const encrypted = Buffer.concat([cipher.update(plainText, 'utf8'), cipher.final()]);
  return Buffer.concat([iv, encrypted]).toString('base64');
}

module.exports = { encrypt };
