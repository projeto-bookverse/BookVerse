package org.example.bookverse.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.bookverse.entity.enums.TipoConta;

/**
 * Converte o enum para os valores minúsculos exigidos pelo CHECK da coluna usuarios.tipo_conta.
 */
@Converter(autoApply = true)
public class TipoContaConverter implements AttributeConverter<TipoConta, String> {

	@Override
	public String convertToDatabaseColumn(TipoConta tipoConta) {
		return tipoConta == null ? null : tipoConta.getValor();
	}

	@Override
	public TipoConta convertToEntityAttribute(String valor) {
		return valor == null ? null : TipoConta.fromValor(valor);
	}
}
