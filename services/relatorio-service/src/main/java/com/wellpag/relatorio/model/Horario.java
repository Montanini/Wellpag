package com.wellpag.relatorio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalTime;

/**
 * Modelo read-only temporario: subconjunto dos campos de Horario realmente lidos
 * pelo Dashboard. Aponta para a mesma collection "horarios" do monolito.
 * Ponte transitoria ate o futuro agenda-service existir de verdade e isso virar
 * uma chamada REST — este servico nunca escreve nesta collection.
 */
@Data
@Document(collection = "horarios")
public class Horario {

    @Id
    private String id;

    private String alunoId;
    private String professorId;

    private DiaSemana diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFim;
}
