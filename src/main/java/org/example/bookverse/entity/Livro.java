package org.example.bookverse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "livros")
public class Livro {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String titulo;

	@Column(nullable = false, length = 150)
	private String autor;

	@Column(length = 20, unique = true)
	private String isbn;

	@Column(name = "capa_url")
	private String capaUrl;

	@Column(columnDefinition = "text")
	private String sinopse;

	@Column(nullable = false, length = 50)
	private String genero;

	@Column(name = "ano_publicacao")
	private Integer anoPublicacao;

	@Column(name = "num_paginas")
	private Integer numPaginas;

	public Livro() {
	}

	public Livro(String titulo, String autor, String genero) {
		this.titulo = titulo;
		this.autor = autor;
		this.genero = genero;
	}

	public UUID getId() {
		return id;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getAutor() {
		return autor;
	}

	public void setAutor(String autor) {
		this.autor = autor;
	}

	public String getIsbn() {
		return isbn;
	}

	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}

	public String getCapaUrl() {
		return capaUrl;
	}

	public void setCapaUrl(String capaUrl) {
		this.capaUrl = capaUrl;
	}

	public String getSinopse() {
		return sinopse;
	}

	public void setSinopse(String sinopse) {
		this.sinopse = sinopse;
	}

	public String getGenero() {
		return genero;
	}

	public void setGenero(String genero) {
		this.genero = genero;
	}

	public Integer getAnoPublicacao() {
		return anoPublicacao;
	}

	public void setAnoPublicacao(Integer anoPublicacao) {
		this.anoPublicacao = anoPublicacao;
	}

	public Integer getNumPaginas() {
		return numPaginas;
	}

	public void setNumPaginas(Integer numPaginas) {
		this.numPaginas = numPaginas;
	}
}
