export type StatusMensalidade = "PAGO" | "A_PAGAR" | "ATRASADO";
export type DiaSemana = "SEGUNDA" | "TERCA" | "QUARTA" | "QUINTA" | "SEXTA" | "SABADO" | "DOMINGO";
export type TipoHorario = "FIXO" | "AVULSO";

export interface Aluno {
  id: string;
  nome: string;
  email: string;
  telefone?: string;
  nomeResponsavel?: string;
  telefoneResponsavel?: string;
  cpfPagador?: string;
  valorMensalidade?: number;
  diaVencimento?: number;
  dataCadastro: string;
}

export interface Horario {
  id: string;
  alunoId: string;
  diaSemana: DiaSemana;
  horaInicio: string;
  horaFim: string;
  tipo: TipoHorario;
}

export interface Mensalidade {
  id: string;
  alunoId: string;
  mesReferencia: string;
  valor: number;
  diaVencimento: number;
  status: StatusMensalidade;
  dataPagamento?: string;
  observacao?: string;
}

export interface DashboardAlunoItem {
  alunoId: string;
  nome: string;
  telefone?: string;
  horaInicio: string;
  horaFim: string;
  statusMensalidade: StatusMensalidade;
  mesReferencia: string;
}
