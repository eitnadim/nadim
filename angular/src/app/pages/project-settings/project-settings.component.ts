import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-project-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './project-settings.component.html',
  styleUrl:    './project-settings.component.scss'
})
export class ProjectSettingsComponent implements OnInit {
  @Input() projectId = '';

  activeTab: 'server' | 's3' | 'passito' = 'server';
  saved    = false;
  savedMsg = '';

  // ── Servers ────────────────────────────────────────────────
  servers: any[] = [];
  showAddServer  = false;
  newServer = this.emptyServer();

  emptyServer() {
    return { name: '', ipAddress: '', port: 22, username: 'deploy', env: 'production', isActive: true };
  }

  addServer() {
    if (!this.newServer.name || !this.newServer.ipAddress) return;
    this.servers.push({ ...this.newServer });
    this.newServer     = this.emptyServer();
    this.showAddServer = false;
    this.showSaved('Server added');
  }
  removeServer(i: number) { this.servers.splice(i, 1); }

  // ── S3 ─────────────────────────────────────────────────────
  s3 = {
    bucketName: '', region: 'ap-south-1', baseUrl: '',
    accessKey: '', secretKey: '', pathPrefix: 'uploads/', isActive: true
  };
  showS3Secret = false;
  saveS3()     { this.showSaved('S3 configuration saved'); }

  // ── Passito ────────────────────────────────────────────────
  passitoTab: 'web' | 'mobile' = 'web';
  web = {
    clientId: '', clientSecret: '',
    redirectUri: 'https://app.eit.dev/auth/callback',
    scopes: 'openid profile email', isActive: true
  };
  mobile = {
    clientId: '', clientSecret: '',
    bundleId: '', deepLinkScheme: '',
    scopes: 'openid profile email', isActive: true
  };
  showPassitoSecret = false;
  savePassito()     { this.showSaved('Passito auth configuration saved'); }

  private apiUrl = environment.apiUrl;
  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadServerConfigs();
    this.loadS3Config();
  }

  loadServerConfigs() {
    this.http.get<any[]>(`${this.apiUrl}/server-configs?projectId=${this.projectId}`)
      .subscribe({ next: r => this.servers = r || [], error: () => {} });
  }

  loadS3Config() {
    this.http.get<any>(`${this.apiUrl}/s3-configs?projectId=${this.projectId}`)
      .subscribe({ next: r => { if (r) this.s3 = { ...this.s3, ...r, secretKey: '' }; }, error: () => {} });
  }

  showSaved(msg: string) {
    this.savedMsg = msg;
    this.saved    = true;
    setTimeout(() => this.saved = false, 2500);
  }
}