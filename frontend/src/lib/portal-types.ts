import { StatusMensalidade } from "./types";

export interface PortalPerfilResponse {
  id: string;
  nome: string;
  email: string;
  telefone?: string;
  nomeResponsavel?: string;
  telefoneResponsavel?: string;
  valorMensalidade?: number;
  diaVencimento?: number;
}

export interface MensalidadeResponse {
  id: string;
  alunoId: string;
  mesReferencia: string;
  valor: number;
  diaVencimento: number;
  status: StatusMensalidade;
  dataPagamento?: string;
  observacao?: string;
}
