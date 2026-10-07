package org.example.bookverse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.example.bookverse.entity.enums.StatusLeitura;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "estante_usuario",
		uniqueConstraints = @UniqueConstraint(name = "uq_usuario_livro", columnNames = {"usuario_id", "livro_id"}))
public class EstanteUsuario {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "livro_id", nullable = false)
	private Livro livro;

	@Column(nullable = false, length = 20)
	private StatusLeitura status;

	@Column(name = "data_inicio")
	private LocalDate dataInicio;

	@Column(name = "data_fim")
	private LocalDate dataFim;

	public EstanteUsuario() {
	}

	public EstanteUsuario(Usuario usuario, Livro livro, StatusLeitura status) {
		this.usuario = usuario;
		this.livro = livro;
		this.status = status;
	}

	public UUID getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public Livro getLivro() {
		return livro;
	}

	public void setLivro(Livro livro) {
		this.livro = livro;
	}

	public StatusLeitura getStatus() {
		return status;
	}

	public void setStatus(StatusLeitura status) {
		this.status = status;
	}

	public LocalDate getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(LocalDate dataInicio) {
		this.dataInicio = dataInicio;
	}

	public LocalDate getDataFim() {
		return dataFim;
	}

	public void setDataFim(LocalDate dataFim) {
		this.dataFim = dataFim;
	}
}
