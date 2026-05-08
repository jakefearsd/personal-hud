export type BriefingCategory = 
  | 'WORLD_NEWS' 
  | 'US_NEWS' 
  | 'FINANCE' 
  | 'TECHNOLOGY' 
  | 'GLOBAL_SITREP' 
  | 'THEATER_UKRAINE' 
  | 'THEATER_MIDDLE_EAST';

export interface DailyBriefing {
  id: number;
  generatedAt: string;
  category: BriefingCategory;
  markdownContent: string;
  htmlContent?: string;
  modelName?: string;
}

export type PipelineStatus = 'PENDING' | 'SUCCESS' | 'FAILED';
export type LlmProvider = 'OLLAMA' | 'GEMINI';

export interface LlmConfig {
  id?: number;
  name: string;
  provider: LlmProvider;
  baseUrl: string;
  modelName: string;
  apiKey: string;
  numCtx: number;
  active: boolean;
  updatedAt?: string;
}

export interface PipelineRun {
  id: number;
  category: BriefingCategory;
  status: PipelineStatus;
  startTime: string;
  endTime: string | null;
  errorMessage: string | null;
  inputTokens?: number;
  outputTokens?: number;
}

export interface MacroMetric {
  ticker: string;
  label: string;
  price: number;
  change: number;
  changePercent: number;
  updatedAt: string;
}

export interface MetricHistory {
  id: number;
  ticker: string;
  price: number;
  changePercent: number;
  timestamp: string;
}

export interface MarketEvent {
  id: number;
  ticker: string;
  timestamp: string;
  title: string;
  rationale: string;
}

export interface MarketPrediction {
  id: number;
  ticker: string;
  generationDate: string;
  targetDate: string;
  predictedPrice: number;
  actualPrice: number | null;
  rationale: string;
  synthesis: string;
  modelName: string;
}
