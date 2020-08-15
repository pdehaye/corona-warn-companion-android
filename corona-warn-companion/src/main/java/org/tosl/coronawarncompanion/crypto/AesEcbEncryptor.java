/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Michael Huebler, 2020-08: As required by the license
 * ("You must cause any modified files to carry prominent notices stating that You changed the files")
 * I hereby state that I changed this file.
 */

package org.tosl.coronawarncompanion.crypto;

import android.annotation.SuppressLint;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encryptor that encapsulates aes/ecb encryption, but separates initialization and encryption steps.
 *
 * <p>If used repeatedly, call {@link #AesEcbEncryptor()} and {@link #init(byte[])} sparingly.
 */
public final class AesEcbEncryptor {

    private final Cipher cipher;

    @SuppressLint("GetInstance")
    public AesEcbEncryptor() throws CryptoException {
        try {
            this.cipher = Cipher.getInstance("AES/ECB/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Initializes encryption with provided key.
     */
    public void init(byte[] key) throws CryptoException {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        } catch (InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Encrypts provided data.
     */
    public byte[] encrypt(byte[] data) throws CryptoException {
        try {
            return cipher.doFinal(data);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Encrypts provided data to a given output buffer.
     */
    public byte[] encrypt(byte[] data, byte[] output) throws CryptoException {
        try {
            cipher.doFinal(data, /*inputOffset =*/ 0, data.length, output, /* outputOffset =*/ 0);
            return output;
        } catch (BadPaddingException | IllegalBlockSizeException | ShortBufferException e) {
            throw new CryptoException(e);
        }
    }
}