export type StatusNotificacao = "PENDENTE" | "VINCULADA" | "IGNORADA";
export type BancoIntegracao = "PIX_GENERICO" | "ASAAS" | "INTER" | "SICOOB" | "EFIPAY" | "GENERICO";

export interface NotificacaoResponse {
  id: string;
  banco: BancoIntegracao;
  valor?: number;
  nomePagador?: string;
  documentoPagador?: string;
  dataTransacao?: string;
  endToEndId?: string;
  descricao?: string;
  status: StatusNotificacao;
  mensalidadeId?: string;
  alunoId?: string;
  recebidaEm: string;
}

export interface WebhookConfiguracaoResponse {
  token: string;
  urls: Record<string, string>;
}
