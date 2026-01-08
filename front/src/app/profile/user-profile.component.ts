import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { UserProfile } from '../models/user-profile';
import { UserService } from '../services/user.service';

@Component({
  selector: 'app-user-profile',
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.css']
})
export class UserProfileComponent implements OnInit {
  profile: UserProfile | null = null;
  errorMessage: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    const username = this.route.snapshot.paramMap.get('username');
    if (!username) {
      this.errorMessage = 'Missing username.';
      return;
    }

    this.userService.getProfile(username).subscribe({
      next: (data) => {
        this.profile = data;
      },
      error: (error) => {
        this.errorMessage = `Failed to load profile: ${error.message}`;
        console.error(error);
      }
    });
  }
}
