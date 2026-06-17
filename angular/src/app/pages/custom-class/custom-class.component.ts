import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { SidebarComponent } from '../../components/sidebar/sidebar.component';
import { ProjectService } from '../../services/project.service';
import { CustomClassUploadResponse } from '../../models/project.model';

@Component({
  selector: 'app-custom-class',
  standalone: true,
  imports: [CommonModule, SidebarComponent],
  templateUrl: './custom-class.component.html',
  styleUrls: ['./custom-class.component.scss']
})
export class CustomClassComponent implements OnInit {
  projectId = '';
  selectedFile?: File;
  uploading = false;
  result?: CustomClassUploadResponse;
  uploadError = '';
  isDragOver = false;

  constructor(private route: ActivatedRoute, private projectService: ProjectService) {}

  ngOnInit() {
    this.projectId = this.route.snapshot.params['id'];
  }

  onFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) this.setFile(input.files[0]);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.setFile(file);
  }

  onDragOver(event: DragEvent) { event.preventDefault(); this.isDragOver = true; }
  onDragLeave() { this.isDragOver = false; }

  setFile(file: File) {
    if (!file.name.endsWith('.java')) { this.uploadError = 'Only .java files are allowed.'; return; }
    this.selectedFile = file;
    this.uploadError = '';
    this.result = undefined;
  }

  upload() {
    if (!this.selectedFile) return;
    this.uploading = true;
    this.uploadError = '';
    this.projectService.uploadCustomClass(this.projectId, this.selectedFile).subscribe({
      next: r => { this.result = r; this.uploading = false; },
      error: () => { this.uploadError = 'Upload failed. Please try again.'; this.uploading = false; }
    });
  }

  clearFile() { this.selectedFile = undefined; this.result = undefined; this.uploadError = ''; }
}
