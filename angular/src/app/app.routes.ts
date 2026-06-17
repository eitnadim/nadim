import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { HomeComponent } from './pages/home/home.component';
import { ProjectDashboardComponent } from './pages/project-dashboard/project-dashboard.component';
import { CustomClassComponent } from './pages/custom-class/custom-class.component';
import { ApiJsonGeneratorComponent } from './pages/api-json-generator/api-json-generator.component';
import { ConfigurationsComponent } from './pages/configurations/configurations.component';

export const routes: Routes = [
  { path: '',                           redirectTo: '/login', pathMatch: 'full' },
  { path: 'login',                      component: LoginComponent },
  { path: 'home',                       component: HomeComponent },
  { path: 'configurations',             component: ConfigurationsComponent },
  { path: 'project/:id',                component: ProjectDashboardComponent },
  { path: 'project/:id/custom-class',   component: CustomClassComponent },
  { path: 'project/:id/api-json',       component: ApiJsonGeneratorComponent },
  { path: '**',                         redirectTo: '/home' }
];