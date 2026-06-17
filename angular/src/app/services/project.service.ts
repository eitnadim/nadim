import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Project,
  CreateProjectRequest,
  BuildResult,
  DeployResult,
  CustomClassUploadResponse
} from '../models/project.model';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private api = `${environment.apiUrl}/projects`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Project[]> {
    return this.http.get<Project[]>(this.api);
  }

  getById(id: string): Observable<Project> {
    return this.http.get<Project>(`${this.api}/${id}`);
  }

  create(payload: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.api, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  build(id: string): Observable<BuildResult> {
    return this.http.post<BuildResult>(`${this.api}/${id}/build`, {});
  }

  deploy(id: string): Observable<DeployResult> {
    return this.http.post<DeployResult>(`${this.api}/${id}/deploy`, {});
  }

  downloadAngularZip(id: string): Observable<Blob> {
    return this.http.get(`${this.api}/${id}/download/angular`, { responseType: 'blob' });
  }

  uploadCustomClass(id: string, file: File): Observable<CustomClassUploadResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<CustomClassUploadResponse>(
      `${this.api}/${id}/classes/upload`, form);
  }

  connectGitHub(id: string): Observable<Project> {
    return this.http.post<Project>(`${this.api}/${id}/git/connect`, {});
  }

  getInvitationStatus(id: string, username: string): Observable<{ status: string }> {
    return this.http.get<{ status: string }>(
      `${this.api}/${id}/git/invitation-status?username=${username}`);
  }

  resendInvitation(id: string, username: string): Observable<{ success: boolean }> {
    return this.http.post<{ success: boolean }>(
      `${this.api}/${id}/git/resend-invitation?username=${username}`, {});
  }
}