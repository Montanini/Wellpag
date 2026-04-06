package com.wellpag.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalTime;

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
    private TipoHorario tipo;
}
