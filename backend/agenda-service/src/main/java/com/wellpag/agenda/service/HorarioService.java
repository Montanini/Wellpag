package com.wellpag.agenda.service;

import com.wellpag.agenda.dto.HorarioRequest;
import com.wellpag.agenda.dto.HorarioResponse;
import com.wellpag.agenda.model.DiaSemana;
import com.wellpag.agenda.model.Horario;
import com.wellpag.agenda.repository.AlunoRepository;
import com.wellpag.agenda.repository.HorarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final AlunoRepository alunoRepository;

    public HorarioResponse criar(String professorId, HorarioRequest request) {
        alunoRepository.findByIdAndProfessorId(request.alunoId(), professorId)
            .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));

        Horario horario = new Horario();
        horario.setAlunoId(request.alunoId());
        horario.setProfessorId(professorId);
        horario.setDiaSemana(request.diaSemana());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFim(request.horaFim());
        horario.setTipo(request.tipo());

        return HorarioResponse.from(horarioRepository.save(horario));
    }

    public List<HorarioResponse> listarPorProfessor(String professorId) {
        return horarioRepository.findByProfessorId(professorId)
            .stream()
            .map(HorarioResponse::from)
            .toList();
    }

    public List<HorarioResponse> listarPorAluno(String alunoId) {
        return horarioRepository.findByAlunoId(alunoId)
            .stream()
            .map(HorarioResponse::from)
            .toList();
    }

    public List<HorarioResponse> listarPorDia(String professorId, DiaSemana diaSemana) {
        return horarioRepository.findByProfessorIdAndDiaSemana(professorId, diaSemana)
            .stream()
            .map(HorarioResponse::from)
            .toList();
    }

    /**
     * Horários do aluno autenticado no portal (pode ter mais de um vínculo Aluno
     * para o mesmo usuarioId). Porte fiel de AlunoPortalService.horarios() do
     * monolito: para cada Aluno vinculado, busca os horários por alunoId e
     * achata tudo em uma única lista.
     */
    public List<HorarioResponse> listarParaUsuario(String usuarioId) {
        return alunoRepository.findByUsuarioId(usuarioId)
            .stream()
            .flatMap(aluno -> horarioRepository.findByAlunoId(aluno.getId()).stream())
            .map(HorarioResponse::from)
            .toList();
    }

    public void remover(String horarioId, String professorId) {
        Horario horario = horarioRepository.findById(horarioId)
            .filter(h -> h.getProfessorId().equals(professorId))
            .orElseThrow(() -> new IllegalArgumentException("Horário não encontrado"));
        horarioRepository.delete(horario);
    }
}
