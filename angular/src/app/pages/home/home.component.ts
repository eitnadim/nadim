import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { SidebarComponent } from '../../components/sidebar/sidebar.component';
import { ProjectService } from '../../services/project.service';
import { Project } from '../../models/project.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SidebarComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  projects: Project[] = [];
  loading = true;
  showModal = false;
  creating = false;
  createError = '';
  searchText = '';
  form: FormGroup;

  constructor(
    private projectService: ProjectService,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.form = this.fb.group({
      name:              ['', Validators.required],
      description:       [''],
      javaVersion:       ['Java 17', Validators.required],
      buildTool:         ['Maven', Validators.required],
      groupId:           ['com.example', Validators.required],
      artifactId:        ['', Validators.required],
      packageName:       [''],
      springBootVersion: ['3.2.x', Validators.required],
      gitInviteEmail:    ['', [Validators.required, Validators.email]]
    });
  }

  ngOnInit() {
    this.projectService.getAll().subscribe({
      next: p  => { this.projects = p; this.loading = false; },
      error: () => this.loading = false
    });
  }

  get filtered() {
    const s = this.searchText.toLowerCase();
    return s ? this.projects.filter(p =>
      p.name.toLowerCase().includes(s) || (p.description || '').toLowerCase().includes(s)
    ) : this.projects;
  }

  get activeCount()   { return this.projects.filter(p => p.status === 'ACTIVE').length; }
  get buildingCount() { return this.projects.filter(p => p.status === 'BUILDING').length; }
  get failedCount()   { return this.projects.filter(p => p.status === 'FAILED').length; }

  openProject(id: string) { this.router.navigate(['/project', id]); }
  openModal()  { this.showModal = true; this.createError = ''; }
  closeModal() {
    this.showModal = false;
    this.form.reset({ javaVersion: 'Java 17', buildTool: 'Maven', springBootVersion: '3.2.x', groupId: 'com.example' });
  }

  submitCreate() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.creating = true;
    this.projectService.create(this.form.value).subscribe({
      next:  p  => this.router.navigate(['/project', p.id]),
      error: () => { this.createError = 'Failed to create project.'; this.creating = false; }
    });
  }

  statusClass(status: string) {
    return { ACTIVE:'badge-green', BUILDING:'badge-amber', IDLE:'badge-gray', FAILED:'badge-red' }[status] ?? 'badge-gray';
  }
}
