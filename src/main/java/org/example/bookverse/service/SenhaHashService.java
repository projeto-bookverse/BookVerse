package org.example.bookverse.service;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class SenhaHashService {

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final String PREFIXO = "pbkdf2-sha256";
	private static final int ITERATIONS = 600_000;
	private static final int SALT_LENGTH_BYTES = 16;
	private static final int KEY_LENGTH_BITS = 256;

	private final SecureRandom secureRandom = new SecureRandom();

	public String hash(String senha) {
		byte[] salt = new byte[SALT_LENGTH_BYTES];
		secureRandom.nextBytes(salt);

		byte[] hash = calcular(senha, salt, ITERATIONS, KEY_LENGTH_BITS);
		Base64.Encoder encoder = Base64.getEncoder();
		return PREFIXO + "$" + ITERATIONS + "$"
				+ encoder.encodeToString(salt) + "$"
				+ encoder.encodeToString(hash);
	}

	/**
	 * Confere se a senha digitada corresponde ao hash armazenado: recalcula o hash com o mesmo sal e o mesmo
	 * número de iterações gravados no hash e compara em tempo constante. Hash em formato desconhecido = senha errada.
	 */
	public boolean verificar(String senha, String hashArmazenado) {
		if (senha == null || hashArmazenado == null) {
			return false;
		}
		String[] partes = hashArmazenado.split("\\$");
		if (partes.length != 4 || !PREFIXO.equals(partes[0])) {
			return false;
		}
		try {
			int iteracoes = Integer.parseInt(partes[1]);
			Base64.Decoder decoder = Base64.getDecoder();
			byte[] salt = decoder.decode(partes[2]);
			byte[] esperado = decoder.decode(partes[3]);
			byte[] calculado = calcular(senha, salt, iteracoes, esperado.length * 8);
			return MessageDigest.isEqual(esperado, calculado);
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private static byte[] calcular(String senha, byte[] salt, int iteracoes, int tamanhoEmBits) {
		char[] senhaCaracteres = senha.toCharArray();
		PBEKeySpec keySpec = new PBEKeySpec(senhaCaracteres, salt, iteracoes, tamanhoEmBits);
		try {
			return SecretKeyFactory.getInstance(ALGORITHM)
					.generateSecret(keySpec)
					.getEncoded();
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Não foi possível proteger a senha.", exception);
		} finally {
			keySpec.clearPassword();
			Arrays.fill(senhaCaracteres, '\0');
		}
	}
}
