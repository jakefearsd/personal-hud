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
}
