import { Component } from '@angular/core';
import { Location } from '@angular/common';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from '../../components/sidebar/sidebar.component';

@Component({
  selector: 'app-configurations',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent],
  templateUrl: './configurations.component.html',
  styleUrls:   ['./configurations.component.scss']
})
export class ConfigurationsComponent {
  constructor(private location: Location) {}

  goBack() { this.location.back(); }

  activeTab: 'jenkins' | 'email' = 'jenkins';
  saved    = false;
  savedMsg = '';

  // Jenkins
  jenkins = { baseUrl: '', token: '', jobName: 'framework-build', username: 'admin' };
  showJenkinsToken = false;
  saveJenkins() { this.showSaved('Jenkins configuration saved'); }

  // SMTP
  smtp = { host: 'smtp.gmail.com', port: 587, username: '', password: '', startTls: true };
  showSmtpPw = false;
  saveSmtp() { this.showSaved('SMTP configuration saved'); }

  showSaved(msg: string) {
    this.savedMsg = msg; this.saved = true;
    setTimeout(() => this.saved = false, 2500);
  }
}