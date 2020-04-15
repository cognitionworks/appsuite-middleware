/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the OX Software GmbH group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2016-2020 OX Software GmbH
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.saml.spi;

import java.security.Key;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Collections;
import java.util.List;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.crypto.KeySupport;


/**
 * A {@link CredentialProvider} that uses {@link Key} instances directly to provide
 * the credentials. The keys are generated on provided {@link KeySpec} instances.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.6.1
 */
public class KeySpecCredentialProvider extends AbstractCredentialProvider {

    public enum Algorithm {
        RSA,
        DSA
    }

    /**
     * A simple container to hold a private key, a public or both and the according algorithm.
     */
    public static class SpecContainer {

        private final KeySpec publicKeySpec;

        private final KeySpec privateKeySpec;

        private final Algorithm algorithm;

        /**
         * Initializes a new {@link SpecContainer}. The values to be passed in depend
         * on the usage of the according keys:
         * <ul>
         *  <li><strong>Verify IDP signatures:</strong> The IDPs public key must be set.</li>
         *  <li><strong>Sign request objects:</strong> The private key for signing is mandatory. If the public key is also provided, it will be part of the SPs metadata XML.</li>
         *  <li><strong>Decrypt response objects:</strong> The private key for decryption is mandatory. If the public key is also provided, it will be part of the SPs metadata XML.</li>
         * </ul>
         *
         * @param publicKeySpec The public key spec or <code>null</code>
         * @param privateKeySpec The private key spec or <code>null</code>
         * @param algorithm The algorithm
         */
        public SpecContainer(KeySpec publicKeySpec, KeySpec privateKeySpec, Algorithm algorithm) {
            super();
            this.publicKeySpec = publicKeySpec;
            this.privateKeySpec = privateKeySpec;
            this.algorithm = algorithm;
        }

        public KeySpec getPublicKeySpec() {
            return publicKeySpec;
        }

        public KeySpec getPrivateKeySpec() {
            return privateKeySpec;
        }

        public Algorithm getAlgorithm() {
            return algorithm;
        }

    }

    private KeySpecCredentialProvider(List<Credential> idpPublicKeyCredentials, Credential signingPrivateKeyCredential, Credential decryptionPrivateKeyCredential) {
        super(idpPublicKeyCredentials, signingPrivateKeyCredential, decryptionPrivateKeyCredential);
    }

    /**
     * Initializes a new instance of {@link KeySpecCredentialProvider}.
     *
     * @param idpPublicSpec The container holding the IDPs public key for signature verification.
     * @param signingKeySpec The container holding the SPs private key and optionally the according public key to sign request objects.
     *                       Can be <code>null</code> if request objects shall not be signed. In case the key is not DSA or RSA, the according
     *                       <strong>public key spec must be contained</strong>.
     * @param decryptionKeySpec The container holding the SPs private key and optionally the according public key to decrypt encrypted
     *                          response data or encryption keys. Can be <code>null</code> if request objects will not be encrypted.
     *                          In case the key is not DSA or RSA, the according <strong>public key spec must be contained</strong>.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws KeyException If in case of a given signing or decryption key spec without according public key spec deriving the public key
     *                      fails.
     */
    public static KeySpecCredentialProvider newInstance(SpecContainer idpPublicSpec, SpecContainer signingKeySpec, SpecContainer decryptionKeySpec) throws NoSuchAlgorithmException, InvalidKeySpecException, KeyException {
        BasicCredential idpPublicKeyCredential = null;
        BasicCredential signingPrivateKeyCredential = null;
        BasicCredential decryptionPrivateKeyCredential = null;
        if (idpPublicSpec != null) {
            KeyFactory keyFactory = KeyFactory.getInstance(idpPublicSpec.getAlgorithm().name());
            PublicKey idpPublicKey = keyFactory.generatePublic(idpPublicSpec.getPublicKeySpec());
            idpPublicKeyCredential = new BasicCredential(idpPublicKey);
            idpPublicKeyCredential.setUsageType(UsageType.SIGNING);
        }

        if (signingKeySpec != null) {
            KeyFactory keyFactory = KeyFactory.getInstance(signingKeySpec.getAlgorithm().name());
            PrivateKey signingKey = keyFactory.generatePrivate(signingKeySpec.getPrivateKeySpec());
            PublicKey verificationKey = null;
            if (signingKeySpec.getPublicKeySpec() != null) {
                verificationKey = keyFactory.generatePublic(signingKeySpec.getPublicKeySpec());
            } else {
                verificationKey = KeySupport.derivePublicKey(signingKey);
            }
            signingPrivateKeyCredential = new BasicCredential(verificationKey, signingKey);
            signingPrivateKeyCredential.setUsageType(UsageType.SIGNING);
        }

        if (decryptionKeySpec != null) {
            KeyFactory keyFactory = KeyFactory.getInstance(decryptionKeySpec.getAlgorithm().name());
            PrivateKey decryptionKey = keyFactory.generatePrivate(decryptionKeySpec.getPrivateKeySpec());
            PublicKey encryptionKey = null;
            if (decryptionKeySpec.getPublicKeySpec() != null) {
                encryptionKey = keyFactory.generatePublic(decryptionKeySpec.getPublicKeySpec());
            } else {
                encryptionKey = KeySupport.derivePublicKey(decryptionKey);
            }
            decryptionPrivateKeyCredential = new BasicCredential(encryptionKey, decryptionKey);
            decryptionPrivateKeyCredential.setUsageType(UsageType.ENCRYPTION);
        }

        return new KeySpecCredentialProvider(Collections.singletonList(idpPublicKeyCredential), signingPrivateKeyCredential, decryptionPrivateKeyCredential);
    }
}
