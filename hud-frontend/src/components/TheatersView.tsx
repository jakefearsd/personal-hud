import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { AlertTriangle, Shield, Globe, Activity } from 'lucide-react';
import { BriefingView } from './BriefingView';
import type { DailyBriefing } from './types';

interface TheatersViewProps {
  briefings: DailyBriefing[];
  loading: boolean;
  onTrigger?: () => void;
}

export function TheatersView({ briefings, loading, onTrigger }: TheatersViewProps) {
  return (
    <div className="flex flex-col gap-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-2">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-4">
            <CardTitle className="text-sm font-medium">Active Threat Theaters</CardTitle>
            <AlertTriangle className="h-4 w-4 text-destructive" />
          </CardHeader>
          <CardContent className="p-4 pt-0">
            <div className="text-2xl font-bold">12</div>
            <p className="text-xs text-muted-foreground">+2 since last hour</p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-4">
            <CardTitle className="text-sm font-medium">Global Readiness</CardTitle>
            <Shield className="h-4 w-4 text-emerald-500" />
          </CardHeader>
          <CardContent className="p-4 pt-0">
            <div className="text-2xl font-bold">98.5%</div>
            <p className="text-xs text-muted-foreground">Optimal operational capacity</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-4">
            <CardTitle className="text-sm font-medium">Active Deployments</CardTitle>
            <Globe className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent className="p-4 pt-0">
            <div className="text-2xl font-bold">4</div>
            <p className="text-xs text-muted-foreground">Unchanged across regions</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-4">
            <CardTitle className="text-sm font-medium">Critical Intel Streams</CardTitle>
            <Activity className="h-4 w-4 text-accent-foreground" />
          </CardHeader>
          <CardContent className="p-4 pt-0">
            <div className="text-2xl font-bold">84</div>
            <p className="text-xs text-muted-foreground">12 new sources connected</p>
          </CardContent>
        </Card>
      </div>

      <BriefingView 
        briefings={briefings} 
        type="theater"
        loading={loading} 
        onTrigger={onTrigger} 
      />
    </div>
  );
}
