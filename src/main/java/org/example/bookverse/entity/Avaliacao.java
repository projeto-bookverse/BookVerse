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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "avaliacoes",
		uniqueConstraints = @UniqueConstraint(name = "uq_avaliacao_usuario_livro", columnNames = {"usuario_id", "livro_id"}))
public class Avaliacao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "livro_id", nullable = false)
	private Livro livro;

	/** Nota de 1 a 5 (garantida por CHECK no banco). */
	@Column(nullable = false)
	private Short nota;

	@Column(name = "texto_resenha", columnDefinition = "text")
	private String textoResenha;

	@Column(name = "flag_spoiler", nullable = false)
	private boolean flagSpoiler = false;

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private LocalDateTime criadoEm;

	public Avaliacao() {
	}

	public Avaliacao(Usuario usuario, Livro livro, Short nota) {
		this.usuario = usuario;
		this.livro = livro;
		this.nota = nota;
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

	public Short getNota() {
		return nota;
	}

	public void setNota(Short nota) {
		this.nota = nota;
	}

	public String getTextoResenha() {
		return textoResenha;
	}

	public void setTextoResenha(String textoResenha) {
		this.textoResenha = textoResenha;
	}

	public boolean isFlagSpoiler() {
		return flagSpoiler;
	}

	public void setFlagSpoiler(boolean flagSpoiler) {
		this.flagSpoiler = flagSpoiler;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}
}
