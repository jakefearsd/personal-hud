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

export interface PipelineRun {
  id: number;
  category: BriefingCategory;
  status: PipelineStatus;
  startTime: string;
  endTime: string | null;
  errorMessage: string | null;
}
