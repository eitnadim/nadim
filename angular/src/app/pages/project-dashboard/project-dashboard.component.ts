import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { interval, Subscription } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { SidebarComponent } from '../../components/sidebar/sidebar.component';
import { ProjectDbConfigComponent } from '../project-db-config/project-db-config.component';
import { ProjectSettingsComponent } from '../project-settings/project-settings.component';
import { ProjectService } from '../../services/project.service';
import { Project, BuildResult, DeployResult } from '../../models/project.model';
import { environment } from '../../../environments/environment';

export interface BuildLog {
  id:          string;
  projectId:   string;
  status:      'SUCCESS' | 'FAILED' | 'RUNNING';
  logs:        string;
  commitHash?: string;
  branch?:     string;
  startedAt:   string;
  finishedAt?: string;
  triggeredBy?: string;
}

@Component({
  selector: 'app-project-dashboard',
  standalone: true,
  imports: [CommonModule, SidebarComponent, RouterLink, ProjectDbConfigComponent, ProjectSettingsComponent],
  templateUrl: './project-dashboard.component.html',
  styleUrls: ['./project-dashboard.component.scss']
})
export class ProjectDashboardComponent implements OnInit, OnDestroy {

  projectId        = '';
  project?:        Project;
  buildResult?:    BuildResult;
  deployResult?:   DeployResult;

  // ── Build history ──────────────────────────────────────────
  buildLogs:       BuildLog[] = [];
  selectedLog?:    BuildLog;
  loadingLogs      = false;

  // ── Loading states ─────────────────────────────────────────
  loadingProject   = true;
  buildLoading     = false;
  deployLoading    = false;
  downloadLoading  = false;

  activeTab: 'overview' | 'builds' | 'deploy' | 'database' | 'settings' = 'overview';

  // ── Auto-refresh for RUNNING builds ───────────────────────
  private pollSub?: Subscription;

  constructor(
    private route:          ActivatedRoute,
    private router:         Router,
    private projectService: ProjectService,
    private http:           HttpClient
  ) {}

  ngOnInit() {
    this.projectId = this.route.snapshot.params['id'];

    this.projectService.getById(this.projectId).subscribe({
      next:  p  => { this.project = p; this.loadingProject = false; },
      error: () => this.loadingProject = false
    });

    this.loadBuildLogs();
  }

  ngOnDestroy() {
    this.pollSub?.unsubscribe();
  }

  // ── Load last 5 builds ─────────────────────────────────────
  loadBuildLogs() {
    this.loadingLogs = true;
    this.http.get<BuildLog[]>(
      `${environment.apiUrl}/projects/${this.projectId}/builds?limit=5`
    ).subscribe({
      next: logs => {
        this.buildLogs  = logs;
        this.loadingLogs = false;
        this.selectedLog = logs[0]; // auto-select latest

        // If any build is RUNNING, start polling every 5s
        const hasRunning = logs.some(l => l.status === 'RUNNING');
        if (hasRunning) this.startPolling();
        else            this.stopPolling();
      },
      error: () => this.loadingLogs = false
    });
  }

  // ── Auto-refresh polling ───────────────────────────────────
  startPolling() {
    if (this.pollSub) return;
    this.pollSub = interval(5000).subscribe(() => {
      this.http.get<BuildLog[]>(
        `${environment.apiUrl}/projects/${this.projectId}/builds?limit=5`
      ).subscribe(logs => {
        this.buildLogs = logs;
        if (this.selectedLog) {
          const updated = logs.find(l => l.id === this.selectedLog!.id);
          if (updated) this.selectedLog = updated;
        }
        const stillRunning = logs.some(l => l.status === 'RUNNING');
        if (!stillRunning) this.stopPolling();
      });
    });
  }

  stopPolling() {
    this.pollSub?.unsubscribe();
    this.pollSub = undefined;
  }

  // ── Latest version info ────────────────────────────────────
  get latestBuild(): BuildLog | undefined {
    return this.buildLogs[0];
  }

  get successCount() { return this.buildLogs.filter(b => b.status === 'SUCCESS').length; }
  get failedCount()  { return this.buildLogs.filter(b => b.status === 'FAILED').length; }

  get buildSuccessRate(): number {
    if (!this.buildLogs.length) return 0;
    return Math.round((this.successCount / this.buildLogs.length) * 100);
  }

  // ── Select a build log to view ─────────────────────────────
  selectLog(log: BuildLog) {
    this.selectedLog = log;
  }

  shortHash(hash?: string): string {
    return hash ? hash.substring(0, 7) : '—';
  }

  buildDuration(log: BuildLog): string {
    if (!log.finishedAt) return 'Running…';
    const ms = new Date(log.finishedAt).getTime() - new Date(log.startedAt).getTime();
    const s  = Math.round(ms / 1000);
    return s < 60 ? `${s}s` : `${Math.floor(s / 60)}m ${s % 60}s`;
  }


  connectingGit = false;
  gitConnectMsg = '';

  connectGitHub(): void {
    this.connectingGit = true;
    this.gitConnectMsg = '';
    this.http.post<any>(`${environment.apiUrl}/projects/${this.projectId}/git/connect`, {})
      .subscribe({
        next: res => {
          this.project!.gitRepoUrl  = res.gitRepoUrl;
          this.project!.gitRepoName = res.gitRepoName;
          this.connectingGit = false;
          this.gitConnectMsg = 'GitHub connected! Invitation sent to ' + this.project!.gitInviteEmail;
          setTimeout(() => this.gitConnectMsg = '', 4000);
        },
        error: err => {
          this.connectingGit = false;
          this.gitConnectMsg = 'Connection failed — check GitHub token in application.yml';
        }
      });
  }

  // ── Navigation ─────────────────────────────────────────────
  openApiJson() {
    this.router.navigate(['/project', this.projectId, 'api-json']);
  }

  // ── Build ──────────────────────────────────────────────────
  build() {
    this.buildLoading = true;
    this.projectService.build(this.projectId).subscribe({
      next: r => {
        this.buildResult  = r;
        this.buildLoading = false;
        this.activeTab    = 'builds';
        this.loadBuildLogs();
      },
      error: () => this.buildLoading = false
    });
  }

  // ── Deploy ─────────────────────────────────────────────────
  deploy() {
    this.deployLoading = true;
    this.projectService.deploy(this.projectId).subscribe({
      next: r => {
        this.deployResult  = r;
        this.deployLoading = false;
        this.activeTab     = 'deploy';
      },
      error: () => this.deployLoading = false
    });
  }

  get emptySlots(): number[] {
    return Array(Math.max(0, 5 - this.buildLogs.length)).fill(0).map((_,i)=>i);
  }

  // ── Download ZIP ───────────────────────────────────────────
  downloadZip() {
    this.downloadLoading = true;
    this.projectService.downloadAngularZip(this.projectId).subscribe({
      next: blob => {
        const url  = URL.createObjectURL(blob);
        const a    = document.createElement('a');
        a.href     = url;
        a.download = `${this.project?.artifactId ?? 'project'}-angular.zip`;
        a.click();
        URL.revokeObjectURL(url);
        this.downloadLoading = false;
      },
      error: () => this.downloadLoading = false
    });
  }
}