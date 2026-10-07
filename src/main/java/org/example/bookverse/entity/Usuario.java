package org.example.bookverse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.example.bookverse.entity.enums.TipoConta;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 150)
	private String nome;

	@Column(nullable = false, length = 150, unique = true)
	private String email;

	@Column(name = "senha_hash", nullable = false)
	private String senhaHash;

	@Column(name = "data_nascimento", nullable = false)
	private LocalDate dataNascimento;

	@CreationTimestamp
	@Column(name = "data_cadastro", nullable = false, updatable = false)
	private LocalDateTime dataCadastro;

	@Column(name = "foto_perfil")
	private String fotoPerfil;

	@Column(columnDefinition = "text")
	private String bio;

	@Column(name = "tipo_conta", nullable = false, length = 20)
	private TipoConta tipoConta;

	public Usuario() {
	}

	public Usuario(String nome, String email, String senhaHash, LocalDate dataNascimento, TipoConta tipoConta) {
		this.nome = nome;
		this.email = email;
		this.senhaHash = senhaHash;
		this.dataNascimento = dataNascimento;
		this.tipoConta = tipoConta;
	}

	public UUID getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public void setSenhaHash(String senhaHash) {
		this.senhaHash = senhaHash;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	public LocalDateTime getDataCadastro() {
		return dataCadastro;
	}

	public String getFotoPerfil() {
		return fotoPerfil;
	}

	public void setFotoPerfil(String fotoPerfil) {
		this.fotoPerfil = fotoPerfil;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public TipoConta getTipoConta() {
		return tipoConta;
	}

	public void setTipoConta(TipoConta tipoConta) {
		this.tipoConta = tipoConta;
	}
}
