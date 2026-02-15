import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from './services/auth.service';
import { WatchPartyService } from './services/watch-party.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'front';
  private syncSub?: Subscription;

  constructor(
    public authService: AuthService,
    private router: Router,
    private watchPartyService: WatchPartyService
  ) {}

  ngOnInit(): void {
    this.syncSub = this.watchPartyService.syncEvents$.subscribe((event) => {
      if (event.type !== 'VIDEO_SELECTED' || !event.videoId) {
        return;
      }
      this.router.navigate(['/videos', String(event.videoId)]);
    });
  }

  ngOnDestroy(): void {
    this.syncSub?.unsubscribe();
  }

  logout(): void {
    this.watchPartyService.disconnect();
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
