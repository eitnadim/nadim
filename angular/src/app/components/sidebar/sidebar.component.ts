import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  @Input() projectId?: string;

  constructor(public auth: AuthService) {}

  get user() { return this.auth.getUser(); }
  get initials() {
    const n = this.user?.name ?? '';
    return n.split(' ').map((w: string) => w[0]).join('').toUpperCase().slice(0, 2);
  }
}
