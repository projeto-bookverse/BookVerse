package org.example.bookverse.entity.enums;

public enum StatusLeitura {
	QUERO_LER("quero_ler"),
	LENDO("lendo"),
	LIDO("lido"),
	ABANDONADO("abandonado");

	private final String valor;

	StatusLeitura(String valor) {
		this.valor = valor;
	}

	public String getValor() {
		return valor;
	}

	public static StatusLeitura fromValor(String valor) {
		for (StatusLeitura status : values()) {
			if (status.valor.equals(valor)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Status de leitura inválido: " + valor);
	}
}
