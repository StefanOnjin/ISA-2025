import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription, interval } from 'rxjs';
import { PopularityTop3 } from '../models/popularity-top3';
import { AuthService } from '../services/auth.service';
import { PopularityService } from '../services/popularity.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, OnDestroy {
  popularity: PopularityTop3 | null = null;
  popularityError = false;
  isLoggedIn = false;
  private refreshSub: Subscription | null = null;

  constructor(
    private authService: AuthService,
    private popularityService: PopularityService
  ) {}

  ngOnInit(): void {
    this.isLoggedIn = this.authService.isAuthenticated();
    if (!this.isLoggedIn) {
      return;
    }

    this.loadPopularityTop3();
    this.refreshSub = interval(120000).subscribe(() => {
      this.loadPopularityTop3();
    });
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  private loadPopularityTop3(): void {
    this.popularityService.getLatestTop3().subscribe({
      next: (response) => {
        this.popularityError = false;
        const sortedVideos = [...(response.videos ?? [])]
          .sort((a, b) => {
            if (b.score !== a.score) {
              return b.score - a.score;
            }
            return a.videoId - b.videoId;
          })
          .slice(0, 3);

        this.popularity = {
          ...response,
          videos: sortedVideos
        };
      },
      error: (error) => {
        if (error.status === 204) {
          this.popularity = null;
          this.popularityError = false;
          return;
        }
        this.popularityError = true;
      }
    });
  }
}
