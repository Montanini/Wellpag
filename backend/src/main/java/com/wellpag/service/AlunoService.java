package com.wellpag.service;

import com.wellpag.dto.AlunoAutoCadastroRequest;
import com.wellpag.dto.AlunoComplementoRequest;
import com.wellpag.dto.AlunoResponse;
import com.wellpag.model.Aluno;
import com.wellpag.repository.AlunoRepository;
import com.wellpag.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final UsuarioRepository usuarioRepository;

    /** Chamado pelo próprio aluno — não requer autenticação. */
    public AlunoResponse autoCadastrar(AlunoAutoCadastroRequest request) {
        if (!usuarioRepository.existsById(request.professorId())) {
            throw new IllegalArgumentException("Professor não encontrado");
        }
        if (alunoRepository.existsByEmailAndProfessorId(request.email(), request.professorId())) {
            throw new IllegalArgumentException("Aluno já cadastrado para este professor");
        }

        Aluno aluno = new Aluno();
        aluno.setNome(request.nome());
        aluno.setEmail(request.email());
        aluno.setTelefone(request.telefone());
        aluno.setNomeResponsavel(request.nomeResponsavel());
        aluno.setTelefoneResponsavel(request.telefoneResponsavel());
        aluno.setProfessorId(request.professorId());

        return AlunoResponse.from(alunoRepository.save(aluno));
    }

    /** Professor complementa o aluno com mensalidade e vencimento. */
    public AlunoResponse complementar(String alunoId, String professorId, AlunoComplementoRequest request) {
        Aluno aluno = buscarAlunoDoProfessor(alunoId, professorId);
        aluno.setValorMensalidade(request.valorMensalidade());
        aluno.setDiaVencimento(request.diaVencimento());
        return AlunoResponse.from(alunoRepository.save(aluno));
    }

    public List<AlunoResponse> listarPorProfessor(String professorId) {
        return alunoRepository.findByProfessorId(professorId)
            .stream()
            .map(AlunoResponse::from)
            .toList();
    }

    public AlunoResponse buscar(String alunoId, String professorId) {
        return AlunoResponse.from(buscarAlunoDoProfessor(alunoId, professorId));
    }

    public void remover(String alunoId, String professorId) {
        Aluno aluno = buscarAlunoDoProfessor(alunoId, professorId);
        alunoRepository.delete(aluno);
    }

    private Aluno buscarAlunoDoProfessor(String alunoId, String professorId) {
        return alunoRepository.findByIdAndProfessorId(alunoId, professorId)
            .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));
    }
}
