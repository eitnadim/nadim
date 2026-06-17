export interface Project {
  id:                string;
  ownerId?:          string;
  name:              string;
  description:       string;
  javaVersion:       string;
  buildTool:         string;
  groupId:           string;
  artifactId:        string;
  packageName:       string;
  springBootVersion: string;
  gitInviteEmail:    string;
  gitRepoUrl?:       string;
  gitRepoName?:      string;
  status:            'ACTIVE' | 'BUILDING' | 'IDLE' | 'FAILED';
  createdAt:         string;
  updatedAt:         string;
}

export interface CreateProjectRequest {
  name:              string;
  description:       string;
  javaVersion:       string;
  buildTool:         string;
  groupId:           string;
  artifactId:        string;
  packageName:       string;
  springBootVersion: string;
  gitInviteEmail:    string;
}

export interface BuildResult {
  status:    'SUCCESS' | 'FAILED' | 'RUNNING';
  logs:      string;
  timestamp: string;
}

export interface DeployResult {
  status:    'SUCCESS' | 'FAILED';
  url?:      string;
  logs:      string;
  timestamp: string;
}

export interface CustomClassUploadResponse {
  success:  boolean;
  message:  string;
  filePath: string;
}

// Kept for backward compatibility with project.service.ts
export interface ApiConfig {
  openApiJson:  string;
  generatedAt:  string;
}