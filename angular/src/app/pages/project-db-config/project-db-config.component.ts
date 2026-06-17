import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface DbConfig {
  id?:         string;
  projectId?:  string;
  dbHost:      string;
  dbPort:      number;
  dbName:      string;
  dbUsername:  string;
  dbPassword?: string;
  dbSchema:    string;
  sslEnabled:  boolean;
  isActive:    boolean;
  testStatus?: string;
  lastTested?: string;
}

@Component({
  selector: 'app-project-db-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './project-db-config.component.html',
  styleUrl: './project-db-config.component.scss'
})
export class ProjectDbConfigComponent implements OnInit {

  @Input() projectId = '';
  @Output() saved    = new EventEmitter<DbConfig>();

  config: DbConfig = this.empty();
  exists      = false;
  loading     = false;
  saving      = false;
  testing     = false;
  showPw      = false;
  toast: { msg: string; type: 'success' | 'error' | 'info' } | null = null;

  testResult: { success: boolean; message: string; latencyMs?: number } | null = null;

  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  ngOnInit(): void { this.loadConfig(); }

  empty(): DbConfig {
    return {
      dbHost: 'localhost', dbPort: 5432, dbName: '', dbUsername: 'postgres',
      dbPassword: '', dbSchema: 'public', sslEnabled: false, isActive: true
    };
  }

  loadConfig(): void {
    this.loading = true;
    this.http.get<DbConfig>(`${this.apiUrl}/projects/${this.projectId}/db-config`).subscribe({
      next: cfg => {
        if (cfg) {
          this.config = { ...cfg, dbPassword: '' }; // never show saved password
          this.exists = true;
        }
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  save(): void {
    if (!this.config.dbHost || !this.config.dbName || !this.config.dbUsername) {
      this.showToast('Host, database name and username are required', 'error'); return;
    }
    if (!this.exists && !this.config.dbPassword) {
      this.showToast('Password is required for new connection', 'error'); return;
    }
    this.saving = true;
    this.http.post<DbConfig>(
      `${this.apiUrl}/projects/${this.projectId}/db-config`, this.config
    ).subscribe({
      next: res => {
        this.saving  = false;
        this.exists  = true;
        this.config  = { ...res, dbPassword: '' };
        this.showToast('Database connection saved!', 'success');
        this.saved.emit(res);
      },
      error: () => { this.saving = false; this.showToast('Failed to save', 'error'); }
    });
  }

  testConnection(): void {
    this.testing    = true;
    this.testResult = null;
    this.http.post<{ success: boolean; message: string; latencyMs: number }>(
      `${this.apiUrl}/projects/${this.projectId}/db-config/test`, this.config
    ).subscribe({
      next: res => {
        this.testing    = false;
        this.testResult = res;
        this.showToast(
          res.success ? `Connected — ${res.latencyMs}ms` : res.message,
          res.success ? 'success' : 'error'
        );
      },
      error: () => { this.testing = false; this.showToast('Test failed', 'error'); }
    });
  }

  delete(): void {
    if (!confirm('Remove this database connection?')) return;
    this.http.delete(`${this.apiUrl}/projects/${this.projectId}/db-config`).subscribe({
      next: () => { this.config = this.empty(); this.exists = false; this.showToast('Removed', 'info'); },
      error: () => this.showToast('Failed to remove', 'error')
    });
  }

  showToast(msg: string, type: 'success' | 'error' | 'info'): void {
    this.toast = { msg, type };
    setTimeout(() => this.toast = null, 3000);
  }

  get statusClass(): string {
    if (!this.config.testStatus) return 'untested';
    return this.config.testStatus.toLowerCase();
  }
}