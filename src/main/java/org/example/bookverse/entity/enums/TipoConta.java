package org.example.bookverse.entity.enums;

public enum TipoConta {
	LEITOR("leitor"),
	ADMIN("admin");

	private final String valor;

	TipoConta(String valor) {
		this.valor = valor;
	}

	public String getValor() {
		return valor;
	}

	public static TipoConta fromValor(String valor) {
		for (TipoConta tipo : values()) {
			if (tipo.valor.equals(valor)) {
				return tipo;
			}
		}
		throw new IllegalArgumentException("Tipo de conta inválido: " + valor);
	}
}
