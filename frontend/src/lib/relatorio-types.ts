export interface RelatorioResumoResponse {
  mesReferencia: string;
  totalAlunos: number;
  totalEsperado: number;
  totalRecebido: number;
  totalAPagar: number;
  totalAtrasado: number;
  percentualRecebido: number;
}

export interface EvolucaoMensalItem {
  mes: string;
  esperado: number;
  recebido: number;
}

export interface InadimplenteItem {
  alunoId: string;
  nome: string;
  telefone?: string;
  mesesAtrasados: number;
  totalAtrasado: number;
}

export interface PortalRelatorioResponse {
  totalMeses: number;
  totalPago: number;
  totalAPagar: number;
  totalAtrasado: number;
  totalGeral: number;
}
