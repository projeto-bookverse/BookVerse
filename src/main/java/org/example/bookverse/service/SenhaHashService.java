package org.example.bookverse.service;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class SenhaHashService {

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 600_000;
	private static final int SALT_LENGTH_BYTES = 16;
	private static final int KEY_LENGTH_BITS = 256;

	private final SecureRandom secureRandom = new SecureRandom();

	public String hash(String senha) {
		byte[] salt = new byte[SALT_LENGTH_BYTES];
		secureRandom.nextBytes(salt);

		char[] senhaCaracteres = senha.toCharArray();
		PBEKeySpec keySpec = new PBEKeySpec(senhaCaracteres, salt, ITERATIONS, KEY_LENGTH_BITS);
		try {
			byte[] hash = SecretKeyFactory.getInstance(ALGORITHM)
					.generateSecret(keySpec)
					.getEncoded();
			Base64.Encoder encoder = Base64.getEncoder();
			return "pbkdf2-sha256$" + ITERATIONS + "$"
					+ encoder.encodeToString(salt) + "$"
					+ encoder.encodeToString(hash);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Não foi possível proteger a senha.", exception);
		} finally {
			keySpec.clearPassword();
			Arrays.fill(senhaCaracteres, '\0');
		}
	}
}
