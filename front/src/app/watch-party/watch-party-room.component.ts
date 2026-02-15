import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { WatchPartyRoom } from '../models/watch-party-room';
import { WatchPartyService } from '../services/watch-party.service';

@Component({
  selector: 'app-watch-party-room',
  templateUrl: './watch-party-room.component.html',
  styleUrls: ['./watch-party-room.component.css']
})
export class WatchPartyRoomComponent implements OnInit, OnDestroy {
  joinCode = '';
  activeRoom: WatchPartyRoom | null = null;
  isConnected = false;
  statusMessage: string | null = null;
  errorMessage: string | null = null;

  private activeRoomSub?: Subscription;
  private connectedSub?: Subscription;
  private errorSub?: Subscription;
  private refreshTimer: number | null = null;

  constructor(
    private watchPartyService: WatchPartyService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.activeRoomSub = this.watchPartyService.activeRoom$.subscribe((room) => {
      this.activeRoom = room;
      this.syncRefreshTimer();
    });
    this.connectedSub = this.watchPartyService.connected$.subscribe((connected) => {
      this.isConnected = connected;
    });
    this.errorSub = this.watchPartyService.errors$.subscribe((error) => {
      this.errorMessage = error;
    });
  }

  ngOnDestroy(): void {
    this.activeRoomSub?.unsubscribe();
    this.connectedSub?.unsubscribe();
    this.errorSub?.unsubscribe();
    this.stopRefreshTimer();
  }

  createRoom(): void {
    this.clearMessages();
    this.watchPartyService.createRoom().subscribe({
      next: (created) => {
        this.joinCode = created.roomCode;
        this.watchPartyService.getRoom(created.roomCode).subscribe({
          next: (room) => {
            this.activeRoom = room;
            this.statusMessage = `Room ${room.roomCode} created.`;
          },
          error: (error) => {
            this.errorMessage = error?.error?.message ?? 'Unable to load room details.';
          }
        });
      },
      error: (error) => {
        this.errorMessage = error?.error?.message ?? 'Failed to create room.';
      }
    });
  }

  joinRoom(): void {
    this.clearMessages();
    const code = this.joinCode.trim().toUpperCase();
    if (!code) {
      this.errorMessage = 'Enter a room code.';
      return;
    }

    this.watchPartyService.joinRoom(code).subscribe({
      next: (room) => {
        this.activeRoom = room;
        this.joinCode = room.roomCode;
        this.statusMessage = `Joined room ${room.roomCode}.`;
      },
      error: (error) => {
        this.errorMessage = error?.error?.message ?? 'Failed to join room.';
      }
    });
  }

  leaveRoom(): void {
    this.watchPartyService.disconnect();
    this.statusMessage = 'Disconnected from watch party room.';
    this.errorMessage = null;
  }

  goToHome(): void {
    this.router.navigate(['/home']);
  }

  private clearMessages(): void {
    this.statusMessage = null;
    this.errorMessage = null;
  }

  private syncRefreshTimer(): void {
    if (!this.activeRoom) {
      this.stopRefreshTimer();
      return;
    }

    if (this.refreshTimer !== null) {
      return;
    }

    this.refreshTimer = window.setInterval(() => {
      if (!this.activeRoom) {
        return;
      }

      this.watchPartyService.getRoom(this.activeRoom.roomCode).subscribe({
        next: (room) => {
          this.activeRoom = room;
        },
        error: () => {
          // Keep the UI stable if a single refresh fails.
        }
      });
    }, 2000);
  }

  private stopRefreshTimer(): void {
    if (this.refreshTimer !== null) {
      window.clearInterval(this.refreshTimer);
      this.refreshTimer = null;
    }
  }
}
