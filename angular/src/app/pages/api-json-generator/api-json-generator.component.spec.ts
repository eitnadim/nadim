import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ApiJsonGeneratorComponent } from './api-json-generator.component';

describe('ApiJsonGeneratorComponent', () => {
  let component: ApiJsonGeneratorComponent;
  let fixture: ComponentFixture<ApiJsonGeneratorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiJsonGeneratorComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ApiJsonGeneratorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
