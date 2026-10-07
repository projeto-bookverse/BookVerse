package org.example.bookverse.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.bookverse.entity.enums.StatusLeitura;

/**
 * Converte o enum para os valores minúsculos exigidos pelo CHECK da coluna estante_usuario.status.
 */
@Converter(autoApply = true)
public class StatusLeituraConverter implements AttributeConverter<StatusLeitura, String> {

	@Override
	public String convertToDatabaseColumn(StatusLeitura status) {
		return status == null ? null : status.getValor();
	}

	@Override
	public StatusLeitura convertToEntityAttribute(String valor) {
		return valor == null ? null : StatusLeitura.fromValor(valor);
	}
}
