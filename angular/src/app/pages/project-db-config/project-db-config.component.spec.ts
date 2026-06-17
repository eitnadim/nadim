import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectDbConfigComponent } from './project-db-config.component';

describe('ProjectDbConfigComponent', () => {
  let component: ProjectDbConfigComponent;
  let fixture: ComponentFixture<ProjectDbConfigComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectDbConfigComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ProjectDbConfigComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
